package com.ruoyi.project.ai.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.agent.SimpleToolAgent;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.tool.LangChain4jToolRegistry;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于LangChain4j的Agent服务
 * 提供Agent构建、工具调用和AgenticScope管理功能
 * 
 * @author ruoyi
 */
@Slf4j
@Service
public class LangChain4jAgentService {

    @Autowired
    private IAiModelConfigService modelConfigService;
    
    @Autowired
    private LangChain4jToolRegistry toolRegistry;

    /**
     * 创建一个简单的Agent，可以调用工具
     * 
     * @param modelConfigId 模型配置ID
     * @param systemPrompt 系统提示
     * @param availableTools 可用工具列表
     * @return UntypedAgent
     */
    public UntypedAgent createSimpleAgent(Long modelConfigId, String systemPrompt, List<String> availableTools) {
        try {
            // 获取ChatModel
            ChatModel chatModel = getChatModel(modelConfigId);
            
            // 获取工具规范
            List<ToolSpecification> toolSpecs = toolRegistry.getToolSpecifications(availableTools);
            
            // 创建Agent
            return AgenticServices
                    .agentBuilder(SimpleToolAgent.class)
                    .chatModel(chatModel)
                    .tools(toolRegistry.getToolsAsObjects(availableTools))
                    .build();
                    
        } catch (Exception e) {
            log.error("创建Agent失败: {}", e.getMessage(), e);
            throw new ServiceException("创建Agent失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建顺序工作流Agent
     * 
     * @param modelConfigId 模型配置ID
     * @param agentConfigs Agent配置列表
     * @return UntypedAgent
     */
    public UntypedAgent createSequentialWorkflow(Long modelConfigId, List<AgentConfig> agentConfigs) {
        try {
            ChatModel chatModel = getChatModel(modelConfigId);
            
            // 创建子Agent列表
            UntypedAgent[] subAgents = agentConfigs.stream()
                    .map(config -> createConfiguredAgent(chatModel, config))
                    .toArray(UntypedAgent[]::new);
            
            // 创建顺序工作流
            return AgenticServices
                    .sequenceBuilder()
                    .subAgents(subAgents)
                    .outputName("result")
                    .build();
                    
        } catch (Exception e) {
            log.error("创建顺序工作流失败: {}", e.getMessage(), e);
            throw new ServiceException("创建顺序工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建循环工作流Agent
     * 
     * @param modelConfigId 模型配置ID
     * @param agentConfigs Agent配置列表
     * @param maxIterations 最大迭代次数
     * @param exitConditionField 退出条件字段
     * @param exitThreshold 退出阈值
     * @return UntypedAgent
     */
    public UntypedAgent createLoopWorkflow(Long modelConfigId, List<AgentConfig> agentConfigs, 
                                          int maxIterations, String exitConditionField, double exitThreshold) {
        try {
            ChatModel chatModel = getChatModel(modelConfigId);
            
            // 创建子Agent列表
            UntypedAgent[] subAgents = agentConfigs.stream()
                    .map(config -> createConfiguredAgent(chatModel, config))
                    .toArray(UntypedAgent[]::new);
            
            // 创建循环工作流
            return AgenticServices
                    .loopBuilder()
                    .subAgents(subAgents)
                    .maxIterations(maxIterations)
                    .exitCondition(agenticScope -> {
                        Object value = agenticScope.readState(exitConditionField, 0.0);
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue() >= exitThreshold;
                        }
                        return false;
                    })
                    .build();
                    
        } catch (Exception e) {
            log.error("创建循环工作流失败: {}", e.getMessage(), e);
            throw new ServiceException("创建循环工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建条件工作流Agent
     * 
     * @param modelConfigId 模型配置ID
     * @param routerConfig 路由Agent配置
     * @param conditionalAgents 条件Agent配置映射
     * @return UntypedAgent
     */
    public UntypedAgent createConditionalWorkflow(Long modelConfigId, AgentConfig routerConfig, 
                                                 Map<String, AgentConfig> conditionalAgents) {
        try {
            ChatModel chatModel = getChatModel(modelConfigId);
            
            // 创建路由Agent
            UntypedAgent routerAgent = createConfiguredAgent(chatModel, routerConfig);
            
            // 创建条件工作流构建器
            var conditionalBuilder = AgenticServices.conditionalBuilder();
            
            // 添加条件Agent
            conditionalAgents.forEach((condition, config) -> {
                UntypedAgent agent = createConfiguredAgent(chatModel, config);
                conditionalBuilder.subAgents(
                    agenticScope -> condition.equals(agenticScope.readState("category", "")), 
                    agent
                );
            });
            
            UntypedAgent conditionalAgent = conditionalBuilder.build();
            
            // 创建顺序工作流：路由 -> 条件执行
            return AgenticServices
                    .sequenceBuilder()
                    .subAgents(routerAgent, conditionalAgent)
                    .outputName("result")
                    .build();
                    
        } catch (Exception e) {
            log.error("创建条件工作流失败: {}", e.getMessage(), e);
            throw new ServiceException("创建条件工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据配置创建Agent
     */
    private UntypedAgent createConfiguredAgent(ChatModel chatModel, AgentConfig config) {
        return AgenticServices
                .agentBuilder(SimpleToolAgent.class)
                .chatModel(chatModel)
                .tools(toolRegistry.getToolsAsObjects(config.getAvailableTools()))
                .outputName(config.getOutputName())
                .build();
    }
    
    /**
     * 根据模型配置ID获取ChatModel
     */
    private ChatModel getChatModel(Long modelConfigId) {
        try {
            AiModelConfig config = modelConfigService.getById(modelConfigId);
            if (config == null) {
                throw new ServiceException("模型配置不存在: " + modelConfigId);
            }
            
            if (!"Y".equals(config.getEnabled())) {
                throw new ServiceException("模型配置已禁用: " + modelConfigId);
            }
            
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
            
            // 统一走 OpenAI 兼容接口
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
     * Agent配置类
     */
    public static class AgentConfig {
        private String name;
        private String systemPrompt;
        private String userPrompt;
        private String outputName;
        private List<String> availableTools;
        
        // 构造函数
        public AgentConfig(String name, String systemPrompt, String userPrompt, String outputName, List<String> availableTools) {
            this.name = name;
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            this.outputName = outputName;
            this.availableTools = availableTools;
        }
        
        // Getters
        public String getName() { return name; }
        public String getSystemPrompt() { return systemPrompt; }
        public String getUserPrompt() { return userPrompt; }
        public String getOutputName() { return outputName; }
        public List<String> getAvailableTools() { return availableTools; }
    }
}