package com.ruoyi.project.ai.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.tool.LangChain4jToolRegistry;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于LangChain4j的聊天服务
 * 直接使用ChatModel进行交互，提供基础的聊天和工具调用功能
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
     * 基础聊天功能
     * 
     * @param modelConfigId 模型配置ID
     * @param message 用户消息
     * @return 聊天回复
     */
    public String chat(Long modelConfigId, String message) {
        try {
            log.info("执行聊天: modelConfigId={}", modelConfigId);
            ChatModel chatModel = getChatModel(modelConfigId);
            return chatModel.chat(message);
        } catch (Exception e) {
            log.error("聊天失败: {}", e.getMessage(), e);
            throw new ServiceException("聊天失败: " + e.getMessage());
        }
    }

    /**
     * 带系统提示的聊天功能
     * 
     * @param modelConfigId 模型配置ID
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @return 聊天回复
     */
    public String chatWithSystem(Long modelConfigId, String systemPrompt, String message) {
        try {
            log.info("执行带系统提示的聊天: modelConfigId={}", modelConfigId);
            ChatModel chatModel = getChatModel(modelConfigId);
            
            List<ChatMessage> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(systemPrompt)) {
                messages.add(SystemMessage.from(systemPrompt));
            }
            messages.add(UserMessage.from(message));
            
            ChatResponse response = chatModel.chat(ChatRequest.builder().messages(messages).build());
            return response.aiMessage().text();
        } catch (Exception e) {
            log.error("带系统提示的聊天失败: {}", e.getMessage(), e);
            throw new ServiceException("带系统提示的聊天失败: " + e.getMessage());
        }
    }

    /**
     * 带工具调用的聊天功能
     * 
     * @param modelConfigId 模型配置ID
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @param availableTools 可用工具列表
     * @return 聊天回复
     */
    public String chatWithTools(Long modelConfigId, String systemPrompt, String message, List<String> availableTools) {
        try {
            log.info("执行带工具的聊天: modelConfigId={}, toolCount={}", modelConfigId, availableTools.size());
            ChatModel chatModel = getChatModel(modelConfigId);
            
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(systemPrompt)) {
                messages.add(SystemMessage.from(systemPrompt));
            }
            messages.add(UserMessage.from(message));
            
            // 获取工具规范
            List<ToolSpecification> toolSpecs = toolRegistry.getToolSpecifications(availableTools);
            
            // 构建聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(toolSpecs)
                    .build();
            
            ChatResponse response = chatModel.chat(chatRequest);
            AiMessage aiMessage = response.aiMessage();
            
            // 如果有工具调用，处理工具调用
            if (aiMessage.hasToolExecutionRequests()) {
                return handleToolCallsSync(chatModel, messages, aiMessage);
            }
            
            return aiMessage.text();
        } catch (Exception e) {
            log.error("带工具的聊天失败: {}", e.getMessage(), e);
            throw new ServiceException("带工具的聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 流式聊天功能
     * 
     * @param modelConfigId 模型配置ID
     * @param message 用户消息
     * @param onToken 接收流式响应的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    public void streamChat(Long modelConfigId, String message, Consumer<String> onToken, 
                          Runnable onComplete, Consumer<Throwable> onError) {
        try {
            log.info("执行流式聊天: modelConfigId={}", modelConfigId);
            StreamingChatModel streamingChatModel = getStreamingChatModel(modelConfigId);
            
            streamingChatModel.chat(message, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    onComplete.run();
                }
                
                @Override
                public void onError(Throwable error) {
                    onError.accept(error);
                }
            });
        } catch (Exception e) {
            log.error("流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new ServiceException("流式聊天失败: " + e.getMessage()));
        }
    }

    /**
     * 带系统提示的流式聊天功能
     * 
     * @param modelConfigId 模型配置ID
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @param onToken 接收流式响应的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    public void streamChatWithSystem(Long modelConfigId, String systemPrompt, String message, 
                                   Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            log.info("执行带系统提示的流式聊天: modelConfigId={}", modelConfigId);
            StreamingChatModel streamingChatModel = getStreamingChatModel(modelConfigId);
            
            List<ChatMessage> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(systemPrompt)) {
                messages.add(SystemMessage.from(systemPrompt));
            }
            messages.add(UserMessage.from(message));
            
            ChatRequest chatRequest = ChatRequest.builder().messages(messages).build();
            
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    onComplete.run();
                }
                
                @Override
                public void onError(Throwable error) {
                    onError.accept(error);
                }
            });
        } catch (Exception e) {
            log.error("带系统提示的流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new ServiceException("带系统提示的流式聊天失败: " + e.getMessage()));
        }
    }

    /**
     * 同步处理工具调用
     */
    private String handleToolCallsSync(ChatModel chatModel, List<ChatMessage> messages, 
                                     AiMessage aiMessage) {
        try {
            // 添加AI消息到对话历史
            messages.add(aiMessage);
            
            // 执行工具调用
            aiMessage.toolExecutionRequests().forEach(request -> {
                try {
                    String result = toolRegistry.executeTool(request.name(), request.arguments());
                    messages.add(ToolExecutionResultMessage.from(request, result));
                } catch (Exception e) {
                    log.error("工具调用失败: {}", e.getMessage(), e);
                    messages.add(ToolExecutionResultMessage.from(request, "工具调用失败: " + e.getMessage()));
                }
            });
            
            // 继续对话获取最终回复
            ChatRequest followUpRequest = ChatRequest.builder().messages(messages).build();
            ChatResponse followUpResponse = chatModel.chat(followUpRequest);
            
            return followUpResponse.aiMessage().text();
        } catch (Exception e) {
            log.error("处理工具调用失败: {}", e.getMessage(), e);
            return "处理工具调用时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 根据模型配置ID获取ChatModel
     */
    private ChatModel getChatModel(Long modelConfigId) {
        try {
            AiModelConfig config = getModelConfig(modelConfigId);
            return createChatModelFromConfig(config);
        } catch (Exception e) {
            log.error("获取ChatModel失败: {}", e.getMessage(), e);
            throw new ServiceException("获取ChatModel失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据模型配置ID获取StreamingChatModel
     */
    private StreamingChatModel getStreamingChatModel(Long modelConfigId) {
        try {
            AiModelConfig config = getModelConfig(modelConfigId);
            return createStreamingChatModelFromConfig(config);
        } catch (Exception e) {
            log.error("获取StreamingChatModel失败: {}", e.getMessage(), e);
            throw new ServiceException("获取StreamingChatModel失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取并验证模型配置
     */
    private AiModelConfig getModelConfig(Long modelConfigId) {
        AiModelConfig config = modelConfigService.getById(modelConfigId);
        if (config == null) {
            throw new ServiceException("模型配置不存在: " + modelConfigId);
        }
        
        if (!"Y".equals(config.getEnabled())) {
            throw new ServiceException("模型配置已禁用: " + modelConfigId);
        }
        
        return config;
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
     * 根据配置创建StreamingChatModel
     */
    private StreamingChatModel createStreamingChatModelFromConfig(AiModelConfig config) {
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
                return OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .baseUrl(endpoint)
                        .timeout(Duration.ofMinutes(5))
                        .logRequests(false)
                        .logResponses(true)
                        .customHeaders(Collections.singletonMap("Accept-Charset", "utf-8"))
                        .build();
            } else {
                return OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .timeout(Duration.ofMinutes(5))
                        .logRequests(false)
                        .logResponses(true)
                        .customHeaders(Collections.singletonMap("Accept-Charset", "utf-8"))
                        .build();
            }
        } catch (Exception e) {
            log.error("创建StreamingChatModel失败: {}", e.getMessage(), e);
            throw new ServiceException("创建StreamingChatModel失败: " + e.getMessage());
        }
    }
}