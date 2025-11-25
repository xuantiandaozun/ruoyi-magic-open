package com.ruoyi.project.ai.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.domain.AiWorkflow;
import com.ruoyi.project.ai.domain.AiWorkflowExecution;
import com.ruoyi.project.ai.domain.AiWorkflowStep;
import com.ruoyi.project.ai.dto.WorkflowExecuteRequest;
import com.ruoyi.project.ai.service.IAiWorkflowExecutionService;
import com.ruoyi.project.ai.service.IAiWorkflowService;
import com.ruoyi.project.ai.service.IAiWorkflowStepService;
import com.ruoyi.project.ai.service.IWorkflowExecutionService;
import com.ruoyi.project.ai.tool.LangChain4jToolRegistry;
import com.ruoyi.project.ai.util.PromptVariableProcessor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;


/**
 * 基于LangChain4j Agent模式的工作流执行服务实现类
 * 统一使用LangChain4j Agent服务执行工作流，支持工具调用和顺序执行
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
public class AgenticWorkflowExecutionServiceImpl implements IWorkflowExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(AgenticWorkflowExecutionServiceImpl.class);
    
    @Autowired
    private IAiWorkflowService workflowService;

    @Autowired
    private IAiWorkflowStepService stepService;

    @Autowired
    private IAiWorkflowExecutionService executionService;

    @Autowired
    private LangChain4jToolRegistry toolRegistry;    @Autowired
    private LangChain4jAgentService agentService;
    
    @Override
    @Transactional
    public Map<String, Object> executeWorkflow(WorkflowExecuteRequest request) {
        return executeWorkflow(request.getWorkflowId(), request.getInputData());
    }
    
    @Override
    @Transactional
    public Map<String, Object> executeWorkflow(Long workflowId, Map<String, Object> inputData) {
        // 1. 获取工作流配置
        AiWorkflow workflow = workflowService.getById(workflowId);
        if (workflow == null) {
            throw new ServiceException("工作流不存在");
        }
        
        if (!"1".equals(workflow.getEnabled())) {
            throw new ServiceException("工作流已禁用");
        }
        
        // 2. 获取工作流步骤
        List<AiWorkflowStep> steps = stepService.selectByWorkflowId(workflowId);
        if (steps.isEmpty()) {
            throw new ServiceException("工作流没有配置步骤");
        }
        
        // 按顺序排序
        steps.sort(Comparator.comparing(AiWorkflowStep::getStepOrder));
        
        // 3. 创建执行记录
        AiWorkflowExecution execution = new AiWorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setStatus("running");
        execution.setInputData(convertMapToJson(inputData));
        executionService.save(execution);
        
        try {
            // 4. 使用LangChain4j Agent服务执行工作流
            Map<String, Object> result = executeWithLangChain4jAgent(workflow, steps, inputData);
            
            // 5. 更新执行状态为完成
            execution.setStatus("completed");
            execution.setOutputData(convertMapToJson(result));
            executionService.updateById(execution);
            
            return result;
            
        } catch (Exception e) {
            // 6. 处理执行失败
            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            executionService.updateById(execution);
            throw new ServiceException("工作流执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用LangChain4j Agent服务执行工作流
     */
    private Map<String, Object> executeWithLangChain4jAgent(AiWorkflow workflow, 
                                                           List<AiWorkflowStep> steps, 
                                                           Map<String, Object> inputData) {
        try {
            log.info("使用LangChain4j Agent执行工作流: {}", workflow.getName());
            
            // 检查是否只有一个步骤（简单Agent）
            if (steps.size() == 1) {
                AiWorkflowStep step = steps.get(0);
                return executeSingleAgentStep(step, inputData);
            }
            
            // 多步骤工作流，使用顺序工作流
            return executeSequentialWorkflow(steps, inputData);
            
        } catch (Exception e) {
            log.error("Agent执行过程中发生异常: {}", e.getMessage(), e);
            throw new ServiceException("LangChain4j Agent执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行单个Agent步骤
     */
    private Map<String, Object> executeSingleAgentStep(AiWorkflowStep step, Map<String, Object> inputData) {
        // 准备工具列表
        List<String> toolNames = getToolNamesForStep(step);
        
        // 处理用户提示词和变量整合
        String processedUserPrompt = processUserPromptWithVariables(step, inputData);
        
        // 准备输入
        String userInput;
        if (inputData == null || inputData.isEmpty()) {
            userInput = processedUserPrompt;
        } else if (inputData.containsKey("userInput")) {
            userInput = inputData.get("userInput").toString();
        } else if (inputData.containsKey("input")) {
            userInput = inputData.get("input").toString();
        } else if (inputData.containsKey("message")) {
            userInput = inputData.get("message").toString();
        } else {
            // 使用处理后的用户提示词，并附加输入数据
            StringBuilder inputBuilder = new StringBuilder(processedUserPrompt);
            inputBuilder.append("\n\n输入数据：\n");
            inputData.forEach((key, value) -> {
                inputBuilder.append(key).append(": ").append(value).append("\n");
            });
            userInput = inputBuilder.toString();
        }
        String systemPrompt = StrUtil.isNotEmpty(step.getSystemPrompt()) ? step.getSystemPrompt() : "你是一个智能助手，请根据用户输入提供帮助。";
        
        // 自动增强提示词：处理工具返回的统一格式
        if (toolNames != null && !toolNames.isEmpty()) {
            systemPrompt += "\n\n【工具调用规则】\n" +
                "所有工具都会返回JSON格式的结果，包含以下结构：\n" +
                "{\n" +
                "  \"success\": true/false,   // true表示成功，false表示失败或空数据\n" +
                "  \"operationType\": \"query/operation/save\",  // 操作类型\n" +
                "  \"data\": {...},           // 实际数据内容\n" +
                "  \"message\": \"...\"       // 操作说明或错误信息\n" +
                "}\n\n" +
                "【重要规则】\n" +
                "1. 如果工具返回的success字段为false，说明操作失败或没有查询到数据\n" +
                "2. 当success为false时，请直接回复 \"TOOL_EXECUTION_FAILED\" (不包含引号)，不要尝试重新解释或生成其他内容\n" +
                "3. 这样做可以让系统知道数据获取失败，需要停止整个工作流\n" +
                "4. 只有当success为true时，才能使用返回的data数据继续你的任务";
        }
        
        try {
            log.info("开始执行Agent步骤: stepId={}, modelConfigId={}, toolCount={}", 
                    step.getId(), step.getModelConfigId(), toolNames != null ? toolNames.size() : 0);
            log.debug("系统提示: {}", systemPrompt);
            log.debug("用户输入: {}", userInput);
            log.debug("可用工具: {}", toolNames);
            
            // 使用LangChain4j Agent服务直接进行聊天
            log.debug("开始调用chatWithTools...");
            String agentResult;
            if (toolNames != null && !toolNames.isEmpty()) {
                // 有工具时使用chatWithTools
                agentResult = agentService.chatWithTools(step.getModelConfigId(), systemPrompt, userInput, toolNames);
            } else {
                // 无工具时使用chatWithSystem
                agentResult = agentService.chatWithSystem(step.getModelConfigId(), systemPrompt, userInput);
            }
            
            log.info("Agent执行成功: stepId={}, resultType={}", 
                    step.getId(), agentResult != null ? agentResult.getClass().getSimpleName() : "null");
            log.debug("Agent执行结果: {}", agentResult);
            
            // 检查是否触发了工具失败规则
            if ("TOOL_EXECUTION_FAILED".equals(agentResult)) {
                log.warn("步骤 {} 工具执行失败，停止工作流", step.getStepName());
                throw new ServiceException("工具执行失败或没有查询到数据，工作流已停止");
            }
            
            if (agentResult == null) {
                log.warn("Agent执行结果为null: stepId={}", step.getId());
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("result", agentResult);
            
            // 处理输出变量
            if (StrUtil.isNotEmpty(step.getOutputVariable())) {
                Map<String, Object> output = new HashMap<>();
                output.put(step.getOutputVariable(), result.get("result"));
                return output;
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Agent执行失败: stepId={}, error={}", step.getId(), e.getMessage(), e);
            throw new ServiceException("LangChain4j Agent执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行顺序工作流
     */
    private Map<String, Object> executeSequentialWorkflow(List<AiWorkflowStep> steps, Map<String, Object> inputData) {
        Map<String, Object> currentData = new HashMap<>();
        if (inputData != null) {
            currentData.putAll(inputData);
        }
        
        String lastResult = null;
        
        // 按顺序执行每个步骤
        for (AiWorkflowStep step : steps) {
            if (!"1".equals(step.getEnabled())) {
                continue; // 跳过禁用的步骤
            }
            
            log.info("执行顺序工作流步骤: stepId={}, stepName={}", step.getId(), step.getStepName());
            
            try {
                // 执行单个步骤
                Map<String, Object> stepResult = executeSingleAgentStep(step, currentData);
                
                // 更新当前数据
                if (stepResult != null) {
                    currentData.putAll(stepResult);
                    
                    // 保存最后一个步骤的结果
                    if (stepResult.containsKey("result")) {
                        lastResult = stepResult.get("result").toString();
                    }
                    
                    // 如果步骤有输出变量，将结果保存到该变量
                    if (StrUtil.isNotEmpty(step.getOutputVariable()) && stepResult.containsKey("result")) {
                        currentData.put(step.getOutputVariable(), stepResult.get("result"));
                    }
                }
                
                log.info("步骤执行成功: stepId={}, stepName={}", step.getId(), step.getStepName());
                
            } catch (Exception e) {
                log.error("步骤执行失败: stepId={}, stepName={}, error={}", 
                         step.getId(), step.getStepName(), e.getMessage(), e);
                throw new ServiceException("顺序工作流执行失败，步骤: " + step.getStepName() + ", 错误: " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("result", lastResult);
        result.putAll(currentData);
        
        return result;
    }
    
    
    /**
     * 获取步骤的工具名称列表
     */
    private List<String> getToolNamesForStep(AiWorkflowStep step) {
        List<String> toolNames = new ArrayList<>();
        
        if ("Y".equals(step.getToolEnabled())) {
            if (StrUtil.isNotEmpty(step.getToolTypes())) {
                // 指定多个工具类型，用逗号分隔
                String[] types = step.getToolTypes().split(",");
                for (String type : types) {
                    String trimmedType = type.trim();
                    if (StrUtil.isNotEmpty(trimmedType)) {
                        toolNames.add(trimmedType);
                    }
                }
            } else {
                // 获取所有可用工具
                toolNames.addAll(toolRegistry.getAllToolNames());
            }
        }
        
        return toolNames;
    }
    
    /**
     * 处理用户提示词和变量整合
     */
    private String processUserPromptWithVariables(AiWorkflowStep step, Map<String, Object> inputData) {
        String userPrompt = StrUtil.isNotEmpty(step.getUserPrompt()) ? step.getUserPrompt() : "请根据输入数据和系统提示完成任务";
        
        // 使用 PromptVariableProcessor 进行变量处理
        if (inputData != null && !inputData.isEmpty()) {
            try {
                userPrompt = PromptVariableProcessor.processVariables(userPrompt, inputData);
                log.debug("处理后的用户提示词: {}", userPrompt);
            } catch (Exception e) {
                log.warn("处理用户提示词变量时出错: {}, 使用原始提示词", e.getMessage());
            }
        }
        
        // 如果用户提示词中没有变量，但有输入变量配置，则自动添加输入数据
        if (StrUtil.isNotEmpty(step.getInputVariable()) && inputData != null && 
            !PromptVariableProcessor.hasVariables(userPrompt)) {
            
            StringBuilder enhancedPrompt = new StringBuilder(userPrompt);
            enhancedPrompt.append("\n\n可用的输入数据：\n");
            
            String[] inputVars = step.getInputVariable().split(",");
            for (String var : inputVars) {
                var = var.trim();
                if (inputData.containsKey(var)) {
                    enhancedPrompt.append("- ").append(var).append(": ").append(inputData.get(var)).append("\n");
                }
            }
            
            userPrompt = enhancedPrompt.toString();
        }
        
        return userPrompt;
    }
    
    /**
     * 转换Map为JSON字符串
     */
    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        return JSONUtil.toJsonStr(map);
    }

}