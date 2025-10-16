package com.ruoyi.project.ai.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.domain.AiWorkflow;
import com.ruoyi.project.ai.domain.AiWorkflowExecution;
import com.ruoyi.project.ai.domain.AiWorkflowStep;
import com.ruoyi.project.ai.dto.AgenticScope;
import com.ruoyi.project.ai.dto.WorkflowExecuteRequest;
import com.ruoyi.project.ai.service.IAiService;
import com.ruoyi.project.ai.service.IAiWorkflowExecutionService;
import com.ruoyi.project.ai.service.IAiWorkflowService;
import com.ruoyi.project.ai.service.IAiWorkflowStepService;
import com.ruoyi.project.ai.service.IWorkflowExecutionService;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.LangChain4jToolRegistry;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

/**
 * 工作流执行服务实现类
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
public class WorkflowExecutionServiceImpl implements IWorkflowExecutionService {
    
    @Autowired
    private IAiWorkflowService workflowService;
    
    @Autowired
    private IAiWorkflowStepService stepService;
    
    @Autowired
    private IAiWorkflowExecutionService executionService;
    
    @Autowired
    private IAiService aiService;
    
    @Autowired
    private LangChain4jToolRegistry toolRegistry;
    
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
            // 4. 初始化AgenticScope
            AgenticScope scope = new AgenticScope();
            
            // 将输入数据放入scope
            if (inputData != null) {
                for (Map.Entry<String, Object> entry : inputData.entrySet()) {
                    scope.setVariable(entry.getKey(), entry.getValue());
                }
            }
            
            // 5. 顺序执行每个步骤
            for (AiWorkflowStep step : steps) {
                if (!"1".equals(step.getEnabled())) {
                    continue; // 跳过禁用的步骤
                }
                
                executeStep(step, scope);
            }
            
            // 6. 更新执行状态为完成
            execution.setStatus("completed");
            execution.setOutputData(convertMapToJson(scope.getVariables()));
            executionService.updateById(execution);
            
            return scope.getVariables();
            
        } catch (Exception e) {
            // 7. 处理执行失败
            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            executionService.updateById(execution);
            throw new ServiceException("工作流执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行单个步骤
     */
    private void executeStep(AiWorkflowStep step, AgenticScope scope) {
        try {
            // 1. 检查是否启用工具
            if ("Y".equals(step.getToolEnabled()) && StrUtil.isNotEmpty(step.getToolType())) {
                // 执行工具调用
                executeStepWithTool(step, scope);
            } else {
                // 执行普通AI调用
                executeStepWithAI(step, scope);
            }
            
        } catch (Exception e) {
            throw new ServiceException("步骤 [" + step.getStepName() + "] 执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行带工具的步骤
     */
    private void executeStepWithTool(AiWorkflowStep step, AgenticScope scope) {
        try {
            // 1. 解析工具参数
            Map<String, Object> toolParameters = parseToolParameters(step.getToolParameters());
            
            // 获取工具实例
            LangChain4jTool tool = toolRegistry.getToolByName(step.getToolType());
            if (tool == null) {
                throw new ServiceException("未找到工具: " + step.getToolType());
            }
            
            // 3. 验证工具参数
            if (!tool.validateParameters(toolParameters)) {
                throw new ServiceException("工具参数验证失败");
            }
            
            // 4. 执行工具调用
            String toolResult = tool.execute(toolParameters);
            
            // 5. 将工具结果存储到scope中
            String toolResultVarName = step.getOutputVariable() + "_tool_result";
            scope.setVariable(toolResultVarName, toolResult);
            
            // 6. 准备AI输入（包含工具结果）
            String inputContent = prepareStepInputWithTool(step, scope, tool.getToolName(), toolResult);
            
            // 7. 调用AI服务处理工具结果
            String response = aiService.chatWithModelConfig(inputContent, step.getSystemPrompt(), step.getModelConfigId());
            
            // 8. 处理输出
            processStepOutput(step, response, scope);
            
        } catch (Exception e) {
            throw new ServiceException("工具执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行普通AI步骤
     */
    private void executeStepWithAI(AiWorkflowStep step, AgenticScope scope) {
        // 1. 准备输入内容
        String inputContent = prepareStepInput(step, scope);
        
        // 2. 调用AI服务
        String response = aiService.chatWithModelConfig(inputContent, step.getSystemPrompt(), step.getModelConfigId());
        
        // 3. 处理输出
        processStepOutput(step, response, scope);
    }
    
    /**
     * 解析工具参数
     */
    private Map<String, Object> parseToolParameters(String toolParametersJson) {
        if (StrUtil.isEmpty(toolParametersJson)) {
            return new java.util.HashMap<>();
        }
        
        try {
            return JSONUtil.toBean(toolParametersJson, Map.class);
        } catch (Exception e) {
            throw new ServiceException("工具参数JSON格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 准备步骤输入
     */
    private String prepareStepInput(AiWorkflowStep step, AgenticScope scope) {
        String inputVarName = step.getInputVariable();
        if (StrUtil.isEmpty(inputVarName)) {
            return ""; // 没有指定输入变量，返回空字符串
        }
        
        Object inputValue = scope.getVariable(inputVarName);
        return inputValue != null ? inputValue.toString() : "";
    }
    
    /**
     * 准备包含工具结果的步骤输入
     */
    private String prepareStepInputWithTool(AiWorkflowStep step, AgenticScope scope, String toolName, String toolResult) {
        StringBuilder inputBuilder = new StringBuilder();
        
        // 1. 添加原始输入内容
        String originalInput = prepareStepInput(step, scope);
        if (StrUtil.isNotEmpty(originalInput)) {
            inputBuilder.append("用户输入: ").append(originalInput).append("\n\n");
        }
        
        // 2. 添加工具执行结果
        inputBuilder.append("工具执行结果:\n");
        inputBuilder.append("工具名称: ").append(toolName).append("\n");
        inputBuilder.append("工具类型: ").append(step.getToolType()).append("\n");
        inputBuilder.append("结果数据: ").append(toolResult).append("\n\n");
        inputBuilder.append("请基于以上工具执行结果进行分析和处理。");
        
        return inputBuilder.toString();
    }
    
    /**
     * 处理步骤输出
     */
    private void processStepOutput(AiWorkflowStep step, String response, AgenticScope scope) {
        String outputVarName = step.getOutputVariable();
        if (StrUtil.isNotEmpty(outputVarName)) {
            scope.setVariable(outputVarName, response);
        }
    }
    
    /**
     * 将Map转换为JSON字符串
     */
    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        try {
            // 简单的JSON转换，实际项目中可以使用Jackson或Gson
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}