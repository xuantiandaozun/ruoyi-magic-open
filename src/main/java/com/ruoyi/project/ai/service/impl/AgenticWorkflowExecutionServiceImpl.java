package com.ruoyi.project.ai.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.domain.AiWorkflow;
import com.ruoyi.project.ai.domain.AiWorkflowExecution;
import com.ruoyi.project.ai.domain.AiWorkflowStep;
import com.ruoyi.project.ai.dto.AgenticScope;
import com.ruoyi.project.ai.dto.WorkflowExecuteRequest;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiWorkflowExecutionService;
import com.ruoyi.project.ai.service.IAiWorkflowService;
import com.ruoyi.project.ai.service.IAiWorkflowStepService;
import com.ruoyi.project.ai.service.IWorkflowExecutionService;
import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;
import com.ruoyi.project.ai.tool.LangChain4jToolRegistry;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;


/**
 * 基于LangChain4j Agent模式的工作流执行服务实现类
 * AI可以主动决定何时调用哪个工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service("agenticWorkflowExecutionService")
public class AgenticWorkflowExecutionServiceImpl implements IWorkflowExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(AgenticWorkflowExecutionServiceImpl.class);
    
    @Autowired
    private IAiWorkflowService workflowService;
    
    @Autowired
    private IAiWorkflowStepService stepService;
    
    @Autowired
    private IAiWorkflowExecutionService executionService;
    
    @Autowired
    private IAiModelConfigService modelConfigService;
    
    @Autowired
    private LangChain4jToolRegistry toolRegistry;
    
    @Autowired
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
            Map<String, Object> result;
            
            // 4. 根据工作流类型选择执行方式
            String workflowType = workflow.getType();
            if ("langchain4j_agent".equals(workflowType)) {
                // 使用LangChain4j Agent服务执行
                result = executeWithLangChain4jAgent(workflow, steps, inputData);
            } else {
                // 使用传统方式执行
                result = executeWithTraditionalAgent(steps, inputData);
            }
            
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
            // 检查是否只有一个步骤（简单Agent）
            if (steps.size() == 1) {
                AiWorkflowStep step = steps.get(0);
                return executeSingleAgentStep(step, inputData);
            }
            
            // 多步骤工作流，使用顺序工作流
            return executeSequentialWorkflow(steps, inputData);
            
        } catch (Exception e) {
            log.error("LangChain4j Agent执行失败: {}", e.getMessage(), e);
            throw new ServiceException("LangChain4j Agent执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行单个Agent步骤
     */
    private Map<String, Object> executeSingleAgentStep(AiWorkflowStep step, Map<String, Object> inputData) {
        // 获取ChatModel
        ChatModel chatModel = getChatModel(step.getModelConfigId());
        
        // 准备工具列表
        List<String> toolNames = getToolNamesForStep(step);
        
        // 准备输入
        String userInput = prepareUserInputFromMap(inputData);
        String systemPrompt = StrUtil.isNotEmpty(step.getSystemPrompt()) ? step.getSystemPrompt() : "你是一个智能助手，请根据用户输入提供帮助。";
        
        // 使用LangChain4j Agent服务创建简单Agent
        UntypedAgent agent = agentService.createSimpleAgent(step.getModelConfigId(), systemPrompt, toolNames);
        
        // 准备Agent输入
        Map<String, Object> agentInput = new HashMap<>();
        agentInput.put("userInput", userInput);
        agentInput.put("systemPrompt", systemPrompt);
        
        // 调用Agent执行
        Object agentResult = agent.invoke(agentInput);
        Map<String, Object> result = new HashMap<>();
        result.put("result", agentResult);
        
        // 处理输出变量
        if (StrUtil.isNotEmpty(step.getOutputVariable())) {
            Map<String, Object> output = new HashMap<>();
            output.put(step.getOutputVariable(), result.get("result"));
            return output;
        }
        
        return result;
    }
    
    /**
     * 执行顺序工作流
     */
    private Map<String, Object> executeSequentialWorkflow(List<AiWorkflowStep> steps, Map<String, Object> inputData) {
        // 创建AgenticScope
        AgenticScope scope = new AgenticScope();
        
        // 将输入数据放入scope
        if (inputData != null) {
            for (Map.Entry<String, Object> entry : inputData.entrySet()) {
                scope.setVariable(entry.getKey(), entry.getValue());
            }
        }
        
        // 构建Agent列表
        List<UntypedAgent> agents = new ArrayList<>();
        
        for (AiWorkflowStep step : steps) {
            if (!"1".equals(step.getEnabled())) {
                continue; // 跳过禁用的步骤
            }
            
            // 获取ChatModel
            ChatModel chatModel = getChatModel(step.getModelConfigId());
            
            // 准备工具列表
            List<String> toolNames = getToolNamesForStep(step);
            
            // 创建Agent
            UntypedAgent agent = agentService.createSimpleAgent(
                step.getModelConfigId(), 
                step.getSystemPrompt(),
                toolNames
            );
            
            agents.add(agent);
        }
        
        // 执行顺序工作流 - 使用AgenticServices创建顺序工作流
        UntypedAgent sequentialAgent = agentService.createSequentialWorkflow(
            steps.get(0).getModelConfigId(), 
            steps.stream().map(step -> new LangChain4jAgentService.AgentConfig(
                step.getStepName(),
                step.getSystemPrompt(),
                "请根据输入数据和系统提示完成任务", // 默认用户提示词
                step.getOutputVariable(),
                getToolNamesForStep(step)
            )).collect(Collectors.toList())
        );
        
        Map<String, Object> result = new HashMap<>();
        Object workflowResult = sequentialAgent.invoke(inputData);
        result.put("result", workflowResult);
        
        return result;
    }
    
    /**
     * 使用传统方式执行工作流
     */
    private Map<String, Object> executeWithTraditionalAgent(List<AiWorkflowStep> steps, Map<String, Object> inputData) {
        // 4. 初始化Agent上下文
        AgentContext context = new AgentContext();
        
        // 将输入数据放入上下文
        if (inputData != null) {
            context.putAll(inputData);
        }
        
        // 5. 顺序执行每个Agent步骤
        for (AiWorkflowStep step : steps) {
            if (!"1".equals(step.getEnabled())) {
                continue; // 跳过禁用的步骤
            }
            
            executeAgentStep(step, context);
        }
        
        return context.getVariables();
    }
    
    /**
     * 获取步骤的工具名称列表
     */
    private List<String> getToolNamesForStep(AiWorkflowStep step) {
        List<String> toolNames = new ArrayList<>();
        
        if ("Y".equals(step.getToolEnabled())) {
            if (StrUtil.isNotEmpty(step.getToolType())) {
                // 指定工具类型
                toolNames.add(step.getToolType());
            } else {
                // 获取所有可用工具
                toolNames.addAll(toolRegistry.getAllToolNames());
            }
        }
        
        return toolNames;
    }
    
    /**
     * 从Map准备用户输入
     */
    private String prepareUserInputFromMap(Map<String, Object> inputData) {
        if (inputData == null || inputData.isEmpty()) {
            return "请处理当前任务。";
        }
        
        StringBuilder inputBuilder = new StringBuilder();
        
        // 检查是否有标准的用户输入字段
        if (inputData.containsKey("userInput")) {
            inputBuilder.append(inputData.get("userInput").toString());
        } else if (inputData.containsKey("input")) {
            inputBuilder.append(inputData.get("input").toString());
        } else if (inputData.containsKey("message")) {
            inputBuilder.append(inputData.get("message").toString());
        } else {
            // 将所有输入数据组合成文本
            inputBuilder.append("请处理以下信息：\n");
            inputData.forEach((key, value) -> {
                inputBuilder.append(key).append(": ").append(value).append("\n");
            });
        }
        
        return inputBuilder.toString();
    }
    
    /**
     * 执行Agent步骤
     */
    private void executeAgentStep(AiWorkflowStep step, AgentContext context) {
        log.info("执行Agent步骤: {}", step.getStepName());
        
        try {
            // 1. 获取ChatModel
            ChatModel chatModel = getChatModel(step.getModelConfigId());
            
            // 2. 准备消息列表
            List<ChatMessage> messages = new ArrayList<>();
            
            // 3. 添加系统消息
            if (StrUtil.isNotEmpty(step.getSystemPrompt())) {
                messages.add(SystemMessage.from(step.getSystemPrompt()));
            }
            
            // 4. 准备用户输入
            String userInput = prepareUserInput(step, context);
            messages.add(UserMessage.from(userInput));
            
            // 5. 获取可用工具
            List<ToolSpecification> tools = getAvailableTools(step);
            
            // 6. 构建聊天请求
            ChatRequest.Builder requestBuilder = ChatRequest.builder()
                    .messages(messages);
            
            if (!tools.isEmpty()) {
                requestBuilder.toolSpecifications(tools);
            }
            
            // 7. 执行聊天请求
            ChatResponse response = chatModel.chat(requestBuilder.build());
            AiMessage aiMessage = response.aiMessage();
            
            // 8. 处理工具调用（如果有）
            if (aiMessage.hasToolExecutionRequests()) {
                handleToolExecutions(aiMessage, messages, chatModel, tools);
                
                // 重新发送请求获取最终回复
                ChatRequest finalRequest = ChatRequest.builder()
                        .messages(messages)
                        .toolSpecifications(tools)
                        .build();
                
                ChatResponse finalResponse = chatModel.chat(finalRequest);
                aiMessage = finalResponse.aiMessage();
            }
            
            // 9. 处理输出
            processStepOutput(step, aiMessage.text(), context);
            
        } catch (Exception e) {
            log.error("执行Agent步骤失败: {}", e.getMessage(), e);
            throw new ServiceException("Agent步骤执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理工具执行
     */
    private void handleToolExecutions(AiMessage aiMessage, List<ChatMessage> messages, 
                                    ChatModel chatModel, List<ToolSpecification> tools) {
        
        aiMessage.toolExecutionRequests().forEach(toolRequest -> {
            String toolName = toolRequest.name();
            String arguments = toolRequest.arguments();
            
            log.info("AI请求执行工具: {} with arguments: {}", toolName, arguments);
            
            try {
                // 执行工具
                String result = toolRegistry.executeTool(toolName, arguments);
                
                // 添加工具执行结果到消息列表
                messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                
                log.info("工具执行成功: {} -> {}", toolName, result);
                
            } catch (Exception e) {
                log.error("工具执行失败: {}", e.getMessage(), e);
                String errorResult = "工具执行失败: " + e.getMessage();
                messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
            }
        });
    }
    
    /**
     * 获取可用工具
     */
    private List<ToolSpecification> getAvailableTools(AiWorkflowStep step) {
        List<ToolSpecification> tools = new ArrayList<>();
        
        // 如果步骤启用了工具
        if ("Y".equals(step.getToolEnabled())) {
            if (StrUtil.isNotEmpty(step.getToolType())) {
                // 获取指定工具
                ToolSpecification tool = toolRegistry.getToolSpecification(step.getToolType());
                if (tool != null) {
                    tools.add(tool);
                }
            } else {
                // 获取所有可用工具
                tools.addAll(toolRegistry.getAllToolSpecifications());
            }
        }
        
        return tools;
    }
    
    /**
     * 准备用户输入
     */
    private String prepareUserInput(AiWorkflowStep step, AgentContext context) {
        StringBuilder inputBuilder = new StringBuilder();
        
        // 如果有输入变量配置，从上下文中获取
        if (StrUtil.isNotEmpty(step.getInputVariable())) {
            Object inputValue = context.get(step.getInputVariable());
            if (inputValue != null) {
                inputBuilder.append(inputValue.toString());
            }
        }
        
        // 如果没有具体输入，使用通用提示
        if (inputBuilder.length() == 0) {
            inputBuilder.append("请根据当前上下文信息进行处理。");
            
            // 添加上下文信息
            if (!context.getVariables().isEmpty()) {
                inputBuilder.append("\n\n当前上下文信息：\n");
                context.getVariables().forEach((key, value) -> {
                    inputBuilder.append(key).append(": ").append(value).append("\n");
                });
            }
        }
        
        return inputBuilder.toString();
    }
    
    /**
     * 处理步骤输出
     */
    private void processStepOutput(AiWorkflowStep step, String output, AgentContext context) {
        if (StrUtil.isNotEmpty(step.getOutputVariable())) {
            context.put(step.getOutputVariable(), output);
        }
        
        log.info("Agent步骤 {} 输出: {}", step.getStepName(), output);
    }
    
    /**
     * 获取ChatModel
     */
    private ChatModel getChatModel(Long modelConfigId) {
        try {
            // 根据模型配置ID获取配置
            AiModelConfig config = modelConfigService.getById(modelConfigId);
            if (config == null) {
                throw new ServiceException("模型配置不存在: " + modelConfigId);
            }
            
            if (!"Y".equals(config.getEnabled())) {
                throw new ServiceException("模型配置已禁用: " + modelConfigId);
            }
            
            // 使用LangChain4j创建ChatModel
            return createChatModelFromConfig(config);
            
        } catch (Exception e) {
            log.error("获取ChatModel失败: {}", e.getMessage(), e);
            throw new ServiceException("获取ChatModel失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据配置创建ChatModel
     */
    private ChatModel createChatModelFromConfig(AiModelConfig config) {
        try {
            String apiKey = config.getApiKey();
            String model = config.getModel();
            String endpoint = config.getEndpoint();
            
            if (StrUtil.isBlank(apiKey)) {
                throw new ServiceException("API Key不能为空");
            }
            
            if (StrUtil.isBlank(model)) {
                throw new ServiceException("模型名称不能为空");
            }
            
            // 统一走 OpenAI 兼容接口；若 endpoint 非空，则使用自定义 baseUrl
            if (StrUtil.isNotBlank(endpoint)) {
                return OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .baseUrl(endpoint)
                        .build();
            } else {
                return OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .build();
            }
        } catch (Exception e) {
            log.error("创建ChatModel失败: {}", e.getMessage(), e);
            throw new ServiceException("创建ChatModel失败: " + e.getMessage());
        }
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
    
    /**
     * Agent上下文类
     */
    public static class AgentContext {
        private final Map<String, Object> variables = new HashMap<>();
        
        public void put(String key, Object value) {
            variables.put(key, value);
        }
        
        public Object get(String key) {
            return variables.get(key);
        }
        
        public void putAll(Map<String, Object> map) {
            variables.putAll(map);
        }
        
        public Map<String, Object> getVariables() {
            return new HashMap<>(variables);
        }
    }
}