package com.ruoyi.project.ai.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.tool.LangChain4jToolRegistry;
import com.ruoyi.project.ai.util.ToolResultProcessor;
import com.ruoyi.project.system.service.ISysConfigService;

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
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
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

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ChatExecutionResult {
        private String content;
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;
        private String finishReason;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private IAiModelConfigService modelConfigService;
    
    @Autowired
    private LangChain4jToolRegistry toolRegistry;

    // 重试配置
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 500;
    private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(90);

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
        ChatModel chatModel = getChatModel(modelConfigId);
        
        // 构建消息列表
        List<ChatMessage> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(UserMessage.from(message));
        
        return executeChatWithMessages(chatModel, messages, availableTools, true);
    }

    public String chatWithMessages(Long modelConfigId, List<ChatMessage> messages) {
        try {
            return executeChatWithMessages(getChatModel(modelConfigId), messages, null, true);
        } catch (Exception e) {
            log.error("使用消息列表聊天失败: {}", e.getMessage(), e);
            throw new ServiceException("使用消息列表聊天失败: " + e.getMessage());
        }
    }

    public ChatExecutionResult chatWithMessagesDetailed(Long modelConfigId, List<ChatMessage> messages) {
        try {
            return executeChatWithMessagesDetailed(getChatModel(modelConfigId), messages, null, true);
        } catch (Exception e) {
            log.error("使用消息列表聊天失败: {}", e.getMessage(), e);
            throw new ServiceException("使用消息列表聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用消息列表执行聊天，支持递归工具调用
     */
    private String executeChatWithMessages(ChatModel chatModel, List<ChatMessage> messages, 
                                         List<String> availableTools) throws Exception {
        return executeChatWithMessages(chatModel, messages, availableTools, true);
    }

    private String executeChatWithMessages(ChatModel chatModel, List<ChatMessage> messages, 
                                         List<String> availableTools, boolean allowRecovery) throws Exception {
        return executeChatWithMessagesDetailed(chatModel, messages, availableTools, allowRecovery).getContent();
    }

    private ChatExecutionResult executeChatWithMessagesDetailed(ChatModel chatModel, List<ChatMessage> messages,
            List<String> availableTools, boolean allowRecovery) throws Exception {
        // 构建聊天请求
        ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);
        
        // 如果有工具，添加工具规范
        if (availableTools != null && !availableTools.isEmpty()) {
            List<ToolSpecification> toolSpecs = toolRegistry.getToolSpecifications(availableTools);
            requestBuilder.toolSpecifications(toolSpecs);
        }
        
        ChatRequest chatRequest = requestBuilder.build();
        
        AtomicBoolean toolInvoked = new AtomicBoolean(false);
        AtomicBoolean toolSuccessAll = new AtomicBoolean(true);

        ChatResponse completeResponse = chatModel.chat(chatRequest);
        if (completeResponse != null && completeResponse.aiMessage() != null) {
            AiMessage aiMessage = completeResponse.aiMessage();
            if (aiMessage.hasToolExecutionRequests()) {
                log.debug("模型触发工具调用: toolCount={}, tools={}",
                    aiMessage.toolExecutionRequests().size(),
                    aiMessage.toolExecutionRequests().stream()
                        .map(req -> req.name())
                        .collect(Collectors.joining(",")));
                messages.add(aiMessage);

                aiMessage.toolExecutionRequests().forEach(toolRequest -> {
                    try {
                        log.debug("执行工具: name={}, argsLength={}, argsSnippet={}",
                            toolRequest.name(),
                            toolRequest.arguments() != null ? toolRequest.arguments().length() : 0,
                            abbreviate(toolRequest.arguments(), 300));
                        String result = toolRegistry.executeTool(toolRequest.name(), toolRequest.arguments());
                        toolInvoked.set(true);
                        if (!ToolResultProcessor.isSuccess(result)) {
                            toolSuccessAll.set(false);
                        }
                        if (result == null) {
                            result = "{\"success\":false,\"operationType\":\"unknown\",\"message\":\"工具返回null\"}";
                            log.warn("工具返回null结果: {}, 已转换为失败格式", toolRequest.name());
                        }
                        messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                        log.debug("工具调用成功: name={}, resultLength={}, resultSnippet={}",
                                toolRequest.name(),
                                result != null ? result.length() : 0,
                                abbreviate(result, 500));
                    } catch (Exception e) {
                        log.error("工具调用失败: {}", e.getMessage(), e);
                        String errorResult = "{\"success\":false,\"operationType\":\"error\",\"message\":\"工具调用失败: " + e.getMessage() + "\"}";
                        messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                    }
                });

                return executeChatWithMessagesDetailed(chatModel, messages, availableTools, allowRecovery);
            }
        }

        String response = completeResponse != null && completeResponse.aiMessage() != null
                ? completeResponse.aiMessage().text()
                : null;
        String trimmedResponse = response != null ? response.trim() : null;
        if ("TOOL_EXECUTION_FAILED".equals(trimmedResponse)
                && toolInvoked.get()
                && toolSuccessAll.get()
                && allowRecovery) {
            log.warn("模型返回 TOOL_EXECUTION_FAILED 但工具执行均成功，触发一次恢复重试");
            messages.add(SystemMessage.from("工具均已成功返回，请继续完成任务。禁止返回TOOL_EXECUTION_FAILED。"));
            return executeChatWithMessagesDetailed(chatModel, messages, availableTools, false);
        }
        log.debug("模型最终响应: length={}, snippet={}",
                response != null ? response.length() : 0,
                abbreviate(response, 500));
        TokenUsage tokenUsage = completeResponse == null ? null : completeResponse.tokenUsage();
        return new ChatExecutionResult(
                response,
                tokenUsage == null ? 0 : tokenUsage.inputTokenCount(),
                tokenUsage == null ? 0 : tokenUsage.outputTokenCount(),
                tokenUsage == null ? 0 : tokenUsage.totalTokenCount(),
                completeResponse == null || completeResponse.finishReason() == null ? null : completeResponse.finishReason().name());
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

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
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
        streamChatWithMessagesDetailed(streamingChatModel, messages, availableTools, onToken,
                result -> onComplete.run(), onError);
    }

    public void streamChatWithMessagesDetailed(Long modelConfigId, List<ChatMessage> messages,
            Consumer<String> onToken, Consumer<ChatExecutionResult> onComplete, Consumer<Throwable> onError) {
        try {
            streamChatWithMessagesDetailed(getStreamingChatModel(modelConfigId), messages, null, onToken, onComplete, onError);
        } catch (Exception e) {
            log.error("使用消息列表流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new ServiceException("使用消息列表流式聊天失败: " + e.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // 客户端工具代理模式（Client-side tool proxy）
    // 服务端透传工具定义给 LLM，不自己执行 tool，把 tool_calls 原样返回给调用方
    // -----------------------------------------------------------------------

    /**
     * 代理模式非流式聊天：将客户端提供的 ToolSpecification 传给 LLM，
     * LLM 决策后若要调用工具则直接返回含 tool_calls 的 AiMessage，由调用方处理。
     */
    public ProxyChatExecutionResult proxyChat(Long modelConfigId, List<ChatMessage> messages,
            List<ToolSpecification> clientToolSpecs) {
        try {
            ChatModel chatModel = getChatModel(modelConfigId);
            ChatRequest.Builder builder = ChatRequest.builder().messages(messages);
            if (clientToolSpecs != null && !clientToolSpecs.isEmpty()) {
                builder.toolSpecifications(clientToolSpecs);
            }
            ChatResponse response = chatModel.chat(builder.build());
            AiMessage aiMessage = response != null ? response.aiMessage() : null;
            TokenUsage tokenUsage = response != null ? response.tokenUsage() : null;
            String content = aiMessage != null ? aiMessage.text() : null;
            // 若有 tool_calls，content 通常为 null，finishReason 为 TOOL_EXECUTION_REQUESTED
            String finishReason = (response != null && response.finishReason() != null)
                    ? response.finishReason().name() : null;
            return new ProxyChatExecutionResult(
                    content,
                    tokenUsage == null ? 0 : tokenUsage.inputTokenCount(),
                    tokenUsage == null ? 0 : tokenUsage.outputTokenCount(),
                    tokenUsage == null ? 0 : tokenUsage.totalTokenCount(),
                    finishReason,
                    aiMessage != null && aiMessage.hasToolExecutionRequests()
                            ? aiMessage.toolExecutionRequests() : null);
        } catch (Exception e) {
            log.error("代理模式聊天失败: {}", e.getMessage(), e);
            throw new ServiceException("代理模式聊天失败: " + e.getMessage());
        }
    }

    /**
     * 代理模式流式聊天：将客户端提供的 ToolSpecification 传给 LLM，
     * 流式输出 token；若 LLM 返回 tool_calls 则通过 onComplete 回调携带 tool_calls，
     * 由调用方决定如何处理（不在服务端执行）。
     */
    public void proxyChatStream(Long modelConfigId, List<ChatMessage> messages,
            List<ToolSpecification> clientToolSpecs,
            Consumer<String> onToken,
            Consumer<ProxyChatExecutionResult> onComplete,
            Consumer<Throwable> onError) {
        try {
            StreamingChatModel streamModel = getStreamingChatModel(modelConfigId);
            ChatRequest.Builder builder = ChatRequest.builder().messages(messages);
            if (clientToolSpecs != null && !clientToolSpecs.isEmpty()) {
                builder.toolSpecifications(clientToolSpecs);
            }
            streamModel.chat(builder.build(), new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }

                @Override
                public void onPartialToolCall(PartialToolCall partialToolCall) {
                    // 流式 tool_calls 分片，由外层 SSE 逻辑处理
                    log.debug("代理模式部分工具调用: id={}, name={}, argsFragment={}",
                            partialToolCall.id(), partialToolCall.name(), partialToolCall.partialArguments());
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    try {
                        AiMessage aiMessage = response != null ? response.aiMessage() : null;
                        TokenUsage tokenUsage = response != null ? response.tokenUsage() : null;
                        String finishReason = (response != null && response.finishReason() != null)
                                ? response.finishReason().name() : null;
                        List<ToolExecutionRequest> toolCalls = (aiMessage != null && aiMessage.hasToolExecutionRequests())
                                ? aiMessage.toolExecutionRequests() : null;
                        onComplete.accept(new ProxyChatExecutionResult(
                                aiMessage != null ? aiMessage.text() : null,
                                tokenUsage == null ? 0 : tokenUsage.inputTokenCount(),
                                tokenUsage == null ? 0 : tokenUsage.outputTokenCount(),
                                tokenUsage == null ? 0 : tokenUsage.totalTokenCount(),
                                finishReason,
                                toolCalls));
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
            log.error("代理模式流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new ServiceException("代理模式流式聊天失败: " + e.getMessage()));
        }
    }

    /**
     * 代理模式的执行结果，在 ChatExecutionResult 基础上增加了 tool_calls 字段。
     */
    @lombok.Getter
    public static class ProxyChatExecutionResult extends ChatExecutionResult {
        /** LLM 要求执行的工具调用列表，null 表示正常文本回复 */
        private final List<ToolExecutionRequest> toolCalls;

        public ProxyChatExecutionResult(String content, Integer inputTokens, Integer outputTokens,
                Integer totalTokens, String finishReason, List<ToolExecutionRequest> toolCalls) {
            super(content, inputTokens, outputTokens, totalTokens, finishReason);
            this.toolCalls = toolCalls;
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    private void streamChatWithMessagesDetailed(StreamingChatModel streamingChatModel, List<ChatMessage> messages,
                                      List<String> availableTools, Consumer<String> onToken,
                                      Consumer<ChatExecutionResult> onComplete, Consumer<Throwable> onError) {
        try {
            ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);

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
                    log.debug("部分工具调用: {}", partialToolCall);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        if (completeResponse != null && completeResponse.aiMessage() != null) {
                            AiMessage aiMessage = completeResponse.aiMessage();
                            if (aiMessage.hasToolExecutionRequests()) {
                                messages.add(aiMessage);

                                aiMessage.toolExecutionRequests().forEach(toolRequest -> {
                                    try {
                                        String result = toolRegistry.executeTool(toolRequest.name(), toolRequest.arguments());
                                        messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                                        log.debug("工具调用成功: {} -> {}", toolRequest.name(), result);
                                    } catch (Exception e) {
                                        log.error("工具调用失败: {}", e.getMessage(), e);
                                        messages.add(ToolExecutionResultMessage.from(toolRequest, "工具调用失败: " + e.getMessage()));
                                    }
                                });

                                streamChatWithMessagesDetailed(streamingChatModel, messages, availableTools, onToken, onComplete, onError);
                                return;
                            }
                        }

                        TokenUsage tokenUsage = completeResponse == null ? null : completeResponse.tokenUsage();
                        String content = completeResponse != null && completeResponse.aiMessage() != null
                                ? completeResponse.aiMessage().text()
                                : null;
                        onComplete.accept(new ChatExecutionResult(
                                content,
                                tokenUsage == null ? 0 : tokenUsage.inputTokenCount(),
                                tokenUsage == null ? 0 : tokenUsage.outputTokenCount(),
                                tokenUsage == null ? 0 : tokenUsage.totalTokenCount(),
                                completeResponse == null || completeResponse.finishReason() == null ? null : completeResponse.finishReason().name()));
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

    public void streamChatWithMessages(Long modelConfigId, List<ChatMessage> messages,
            Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            streamChatWithMessages(getStreamingChatModel(modelConfigId), messages, null, onToken, onComplete, onError);
        } catch (Exception e) {
            log.error("使用消息列表流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new ServiceException("使用消息列表流式聊天失败: " + e.getMessage()));
        }
    }

    /**
     * 根据模型配置ID获取非流式ChatModel
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
            String apiKey = resolveApiKey(config);
            String model = config.getModel();
            String endpoint = config.getEndpoint();
            
            if (StrUtil.isBlank(apiKey)) {
                throw new ServiceException("API Key不能为空");
            }
            
            if (StrUtil.isBlank(model)) {
                throw new ServiceException("模型名称不能为空");
            }

            OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(model)
                    .timeout(CHAT_TIMEOUT)
                    .logRequests(false)
                    .logResponses(true)
                    .customHeaders(Collections.singletonMap("Accept-Charset", "utf-8"));

            if (StrUtil.isNotBlank(endpoint)) {
                builder.baseUrl(endpoint);
            }

            applyModelExtraParams(builder, config.getExtraParams());
            
            return builder.build();
        } catch (Exception e) {
            log.error("创建StreamingChatModel失败: {}", e.getMessage(), e);
            throw new ServiceException("创建StreamingChatModel失败: " + e.getMessage());
        }
    }

    /**
     * 根据配置创建非流式ChatModel
     */
    private ChatModel createChatModelFromConfig(AiModelConfig config) {
        try {
            String apiKey = resolveApiKey(config);
            String model = config.getModel();
            String endpoint = config.getEndpoint();

            if (StrUtil.isBlank(apiKey)) {
                throw new ServiceException("API Key不能为空");
            }

            if (StrUtil.isBlank(model)) {
                throw new ServiceException("模型名称不能为空");
            }

            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(model)
                    .timeout(CHAT_TIMEOUT)
                    .logRequests(false)
                    .logResponses(true)
                    .customHeaders(Collections.singletonMap("Accept-Charset", "utf-8"));

            if (StrUtil.isNotBlank(endpoint)) {
                builder.baseUrl(endpoint);
            }

            applyModelExtraParams(builder, config.getExtraParams());

            return builder.build();
        } catch (Exception e) {
            log.error("创建ChatModel失败: {}", e.getMessage(), e);
            throw new ServiceException("创建ChatModel失败: " + e.getMessage());
        }
    }

    private void applyModelExtraParams(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder, String extraParams) {
        ModelExtraParams params = parseModelExtraParams(extraParams);
        applyParsedModelExtraParams(builder, params);
    }

    private void applyModelExtraParams(OpenAiChatModel.OpenAiChatModelBuilder builder, String extraParams) {
        ModelExtraParams params = parseModelExtraParams(extraParams);
        applyParsedModelExtraParams(builder, params);
    }

    private ModelExtraParams parseModelExtraParams(String extraParams) {
        ModelExtraParams params = new ModelExtraParams();
        if (StrUtil.isBlank(extraParams)) {
            return params;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(extraParams);
            if (root == null || !root.isObject()) {
                return params;
            }

            params.temperature = readDouble(root, "temperature");
            params.topP = readDouble(root, "topP", "top_p");
            params.maxTokens = readInt(root, "maxTokens", "max_tokens");
            params.enableThinking = readBoolean(root, "enable_thinking");

            Map<String, Object> customParameters = new HashMap<>();
            if (root.has("enable_thinking")) {
                customParameters.put("enable_thinking", params.enableThinking != null ? params.enableThinking : false);
            }
            if (root.has("chat_template_kwargs")) {
                customParameters.put("chat_template_kwargs", toJavaObject(root.get("chat_template_kwargs")));
            }
            if (root.has("reasoning_effort")) {
                customParameters.put("reasoning_effort", root.get("reasoning_effort").asText());
            }

            if (!customParameters.isEmpty()) {
                params.requestParameters = OpenAiChatRequestParameters.builder()
                        .customParameters(customParameters)
                        .build();
            }
        } catch (Exception e) {
            log.warn("解析模型extra_params失败，忽略扩展参数: {}", e.getMessage());
        }
        return params;
    }

    private void applyParsedModelExtraParams(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder,
            ModelExtraParams params) {
        if (params.temperature != null) {
            builder.temperature(params.temperature);
        }
        if (params.topP != null) {
            builder.topP(params.topP);
        }
        if (params.maxTokens != null) {
            builder.maxTokens(params.maxTokens);
        }
        if (Boolean.TRUE.equals(params.enableThinking)) {
            builder.returnThinking(true);
        }
        if (params.requestParameters != null) {
            builder.defaultRequestParameters(params.requestParameters);
        }
    }

    private void applyParsedModelExtraParams(OpenAiChatModel.OpenAiChatModelBuilder builder,
            ModelExtraParams params) {
        if (params.temperature != null) {
            builder.temperature(params.temperature);
        }
        if (params.topP != null) {
            builder.topP(params.topP);
        }
        if (params.maxTokens != null) {
            builder.maxTokens(params.maxTokens);
        }
        if (Boolean.TRUE.equals(params.enableThinking)) {
            builder.returnThinking(true);
        }
        if (params.requestParameters != null) {
            builder.defaultRequestParameters(params.requestParameters);
        }
    }

    private static class ModelExtraParams {
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
        private Boolean enableThinking;
        private OpenAiChatRequestParameters requestParameters;
    }

    private Double readDouble(JsonNode root, String... keys) {
        for (String key : keys) {
            if (root.has(key) && root.get(key).isNumber()) {
                return root.get(key).asDouble();
            }
        }
        return null;
    }

    private Integer readInt(JsonNode root, String... keys) {
        for (String key : keys) {
            if (root.has(key) && root.get(key).isNumber()) {
                return root.get(key).asInt();
            }
        }
        return null;
    }

    private Boolean readBoolean(JsonNode root, String... keys) {
        for (String key : keys) {
            if (root.has(key) && root.get(key).isBoolean()) {
                return root.get(key).asBoolean();
            }
        }
        return null;
    }

    private Object toJavaObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject() || node.isArray()) {
            return OBJECT_MAPPER.convertValue(node, Object.class);
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        return node.asText();
    }

    private String resolveApiKey(AiModelConfig config) {
        if (config == null) {
            return null;
        }
        String apiKeyRef = config.getApiKeyRef();
        if (StrUtil.isNotBlank(apiKeyRef)) {
            String keyName = apiKeyRef;
            if (StrUtil.startWithIgnoreCase(apiKeyRef, "env:")) {
                keyName = apiKeyRef.substring(4);
                String value = System.getenv(keyName);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            } else if (StrUtil.startWithIgnoreCase(apiKeyRef, "config:")) {
                keyName = apiKeyRef.substring(7);
                String value = SpringUtils.getBean(ISysConfigService.class).selectConfigByKey(keyName);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            } else if (StrUtil.startWithIgnoreCase(apiKeyRef, "sys:")) {
                keyName = apiKeyRef.substring(4);
                String value = System.getProperty(keyName);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            } else {
                String envValue = System.getenv(keyName);
                if (StrUtil.isNotBlank(envValue)) {
                    return envValue;
                }
                String propertyValue = System.getProperty(keyName);
                if (StrUtil.isNotBlank(propertyValue)) {
                    return propertyValue;
                }
                String configValue = SpringUtils.getBean(ISysConfigService.class).selectConfigByKey(keyName);
                if (StrUtil.isNotBlank(configValue)) {
                    return configValue;
                }
            }
            log.warn("模型配置 apiKeyRef 未解析到有效密钥，回退到明文apiKey: configId={}, ref={}", config.getId(), apiKeyRef);
        }
        return config.getApiKey();
    }
}
