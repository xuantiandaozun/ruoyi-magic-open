package com.ruoyi.project.ai.strategy;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ruoyi.project.ai.domain.AiChatMessage;

/**
 * AI客户端策略接口
 * 封装具体模型厂商能力，供 AiServiceImpl 委派调用
 */
public interface AiClientStrategy {
    
    // ==================== 同步聊天方法 ====================
    
    /**
     * 基础聊天
     */
    String chat(String message);
    
    /**
     * 带系统提示的聊天
     */
    String chatWithSystem(String systemPrompt, String message, boolean returnJson);
    
    /**
     * 带聊天历史的聊天（核心方法，支持多轮对话）
     */
    String chatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory);
    
    // ==================== 流式聊天方法 ====================
    
    /**
     * 基础流式聊天
     */
    void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 带系统提示的流式聊天
     */
    void streamChatWithSystem(String systemPrompt, String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 带模型配置的流式聊天
     */
    void streamChatWithModelConfig(String message, String systemPrompt, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 带聊天历史的流式聊天（核心方法，支持多轮对话）
     */
    void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 带聊天历史的流式聊天（支持工具调用回调）
     */
    void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, 
                              Consumer<String> onToken, BiConsumer<String, String> onToolCall, 
                              BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 带模型配置的流式聊天（支持工具调用回调）
     */
    void streamChatWithModelConfig(String message, String systemPrompt, 
                                  Consumer<String> onToken, BiConsumer<String, String> onToolCall, 
                                  BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError);
    
    // ==================== 基础方法 ====================
    
    /**
     * 获取模型名称
     */
    String getModelName();
}