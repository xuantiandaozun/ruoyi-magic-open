package com.ruoyi.project.ai.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于LangChain4j的聊天服务
 * 使用StreamingChatModel进行交互，提供基础的聊天和工具调用功能
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

    // 重试配置
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

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
            return chatWithRetry(modelConfigId, null, message, null, MAX_RETRIES);
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
            return chatWithRetry(modelConfigId, systemPrompt, message, null, MAX_RETRIES);
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
            return chatWithRetry(modelConfigId, systemPrompt, message, availableTools, MAX_RETRIES);
        } catch (Exception e) {
            log.error("带工具的聊天失败: {}", e.getMessage(), e);
            throw new ServiceException("带工具的聊天失败: " + e.getMessage());
        }
    }

    /**
     * 带重试机制的聊天核心方法
     */
    private String chatWithRetry(Long modelConfigId, String systemPrompt, String message, 
                                List<String> availableTools, int retries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                log.debug("聊天尝试 {}/{}", attempt, retries);
                return executeChatSync(modelConfigId, systemPrompt, message, availableTools);
            } catch (Exception e) {
                lastException = e;
                log.warn("聊天尝试 {}/{} 失败: {}", attempt, retries, e.getMessage());
                
                if (attempt < retries) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ServiceException("聊天被中断");
                    }
                }
            }
        }
        
        throw new ServiceException("聊天失败，已重试 " + retries + " 次: " + lastException.getMessage());
    }

    /**
     * 同步执行聊天
     */
    private String executeChatSync(Long modelConfigId, String systemPrompt, String message, 
                                  List<String> availableTools) throws Exception {
        StreamingChatModel streamingChatModel = getStreamingChatModel(modelConfigId);
        
        // 构建消息列表
        List<ChatMessage> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(UserMessage.from(message));
        
        return executeChatWithMessages(streamingChatModel, messages, availableTools);
    }
    
    /**
     * 使用消息列表执行聊天，支持递归工具调用
     */
    private String executeChatWithMessages(StreamingChatModel streamingChatModel, List<ChatMessage> messages, 
                                         List<String> availableTools) throws Exception {
        // 构建聊天请求
        ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);
        
        // 如果有工具，添加工具规范
        if (availableTools != null && !availableTools.isEmpty()) {
            List<ToolSpecification> toolSpecs = toolRegistry.getToolSpecifications(availableTools);
            requestBuilder.toolSpecifications(toolSpecs);
        }
        
        ChatRequest chatRequest = requestBuilder.build();
        
        // 使用CompletableFuture来同步等待流式响应
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder responseBuilder = new StringBuilder();
        
        streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null) {
                    responseBuilder.append(partialResponse);
                }
            }
            
            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                // 记录部分工具调用信息
                //log.debug("部分工具调用: {}", partialToolCall);
            }
            
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 检查是否有工具调用请求
                    if (completeResponse != null && completeResponse.aiMessage() != null) {
                        AiMessage aiMessage = completeResponse.aiMessage();
                        if (aiMessage.hasToolExecutionRequests()) {
                            // 添加AI消息到对话历史
                            messages.add(aiMessage);
                            
                            // 执行所有工具调用
                             aiMessage.toolExecutionRequests().forEach(toolRequest -> {
                                 try {
                                     String result = toolRegistry.executeTool(toolRequest.name(), toolRequest.arguments());
                                     
                                     // 如果工具执行结果为null，转换为失败的统一格式
                                     if (result == null) {
                                         result = "{\"success\":false,\"operationType\":\"unknown\",\"message\":\"工具返回null\"}";
                                         log.warn("工具返回null结果: {}, 已转换为失败格式", toolRequest.name());
                                     }
                                     
                                     // 创建工具执行结果消息
                                     messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                                     log.debug("工具调用成功: {} -> {}", toolRequest.name(), result);
                                 } catch (Exception e) {
                                     log.error("工具调用失败: {}", e.getMessage(), e);
                                     // 工具调用异常，返回失败格式
                                     String errorResult = "{\"success\":false,\"operationType\":\"error\",\"message\":\"工具调用失败: " + e.getMessage() + "\"}";
                                     messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                                 }
                             });
                            
                            // 递归继续对话
                            try {
                                String finalResult = executeChatWithMessages(streamingChatModel, messages, availableTools);
                                future.complete(finalResult);
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                            return;
                        }
                    }
                    
                    // 没有工具调用，返回文本响应
                    String response = responseBuilder.toString();
                    if (StrUtil.isBlank(response) && completeResponse != null && completeResponse.aiMessage() != null) {
                        response = completeResponse.aiMessage().text();
                    }
                    future.complete(response);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
            
            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });
        
        // 等待响应完成，设置超时时间
        try {
            return future.get(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new ServiceException("聊天响应超时或失败: " + e.getMessage());
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
     * 带工具调用的流式聊天功能
     * 
     * @param modelConfigId 模型配置ID
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @param availableTools 可用工具列表
     * @param onToken 接收流式响应的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    public void streamChatWithTools(Long modelConfigId, String systemPrompt, String message, 
                                   List<String> availableTools, Consumer<String> onToken, 
                                   Runnable onComplete, Consumer<Throwable> onError) {
        try {
            log.info("执行带工具的流式聊天: modelConfigId={}, toolCount={}", modelConfigId, availableTools.size());
            StreamingChatModel streamingChatModel = getStreamingChatModel(modelConfigId);
            
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(systemPrompt)) {
                messages.add(SystemMessage.from(systemPrompt));
            }
            messages.add(UserMessage.from(message));
            
            streamChatWithMessages(streamingChatModel, messages, availableTools, onToken, onComplete, onError);
        } catch (Exception e) {
            log.error("带工具的流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new ServiceException("带工具的流式聊天失败: " + e.getMessage()));
        }
    }
    
    /**
     * 使用消息列表执行流式聊天，支持递归工具调用
     */
    private void streamChatWithMessages(StreamingChatModel streamingChatModel, List<ChatMessage> messages, 
                                       List<String> availableTools, Consumer<String> onToken, 
                                       Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 构建聊天请求
            ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);
            
            // 如果有工具，添加工具规范
            if (availableTools != null && !availableTools.isEmpty()) {
                List<ToolSpecification> toolSpecs = toolRegistry.getToolSpecifications(availableTools);
                requestBuilder.toolSpecifications(toolSpecs);
            }
            
            ChatRequest chatRequest = requestBuilder.build();
            
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onPartialToolCall(PartialToolCall partialToolCall) {
                    // 记录部分工具调用信息
                    log.debug("部分工具调用: {}", partialToolCall);
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        // 检查是否有工具调用请求
                        if (completeResponse != null && completeResponse.aiMessage() != null) {
                            AiMessage aiMessage = completeResponse.aiMessage();
                            if (aiMessage.hasToolExecutionRequests()) {
                                // 添加AI消息到对话历史
                                messages.add(aiMessage);
                                
                                // 执行所有工具调用
                                aiMessage.toolExecutionRequests().forEach(toolRequest -> {
                                    try {
                                        String result = toolRegistry.executeTool(toolRequest.name(), toolRequest.arguments());
                                        // 创建工具执行结果消息
                                        messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                                        log.debug("工具调用成功: {} -> {}", toolRequest.name(), result);
                                    } catch (Exception e) {
                                        log.error("工具调用失败: {}", e.getMessage(), e);
                                        messages.add(ToolExecutionResultMessage.from(toolRequest, "工具调用失败: " + e.getMessage()));
                                    }
                                });
                                
                                // 递归调用，继续对话
                                streamChatWithMessages(streamingChatModel, messages, availableTools, onToken, onComplete, onError);
                                return;
                            }
                        }
                        
                        // 没有工具调用，完成对话
                        onComplete.run();
                    } catch (Exception e) {
                        onError.accept(e);
                    }
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