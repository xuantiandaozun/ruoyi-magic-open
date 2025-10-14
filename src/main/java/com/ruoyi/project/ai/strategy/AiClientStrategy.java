package com.ruoyi.project.ai.strategy;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ruoyi.project.ai.dto.AiChatMessage;

/**
 * AI客户端策略接口
 * 封装具体模型厂商能力，供 AiServiceImpl 委派调用
 */
public interface AiClientStrategy {
    String chat(String message);
    String chatWithSystem(String systemPrompt, String message, boolean returnJson);
    String chatWithHistory(List<String> messages);
    String chatVision(String message, List<String> imageUrls);
    String generateImage(String prompt, String size, Double guidanceScale, Integer seed, Boolean watermark);
    String embeddingText(String[] texts);
    String embeddingVision(String text, String imageUrl);
    String batchChat(String prompt);
    String createVideoTask(String prompt, String imageUrl);
    String getVideoTaskStatus(String taskId);
    String tokenization(String[] texts);
    String createContext(List<String> messages);
    String chatWithContext(String message, String contextId);
    String getModelName();
    
    // 流式聊天方法
    void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    void streamChatWithSystem(String systemPrompt, String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    void streamChatWithModelConfig(String message, String systemPrompt, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    // 带工具调用回调的流式聊天方法
    void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, Consumer<String> onToken, BiConsumer<String, String> onToolCall, BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError);
    void streamChatWithModelConfig(String message, String systemPrompt, Consumer<String> onToken, BiConsumer<String, String> onToolCall, BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError);
}