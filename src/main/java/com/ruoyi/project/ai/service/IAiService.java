package com.ruoyi.project.ai.service;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * AI 服务接口
 * 支持多种 AI 模型
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiService {
    
    /**
     * 基础聊天对话
     * 
     * @param message 用户消息
     * @return AI 回复
     */
    String chat(String message);
    
    /**
     * 基础聊天对话
     * 
     * @param message 用户消息
     * @param returnJson 是否返回 JSON 格式
     * @return AI 回复
     */
    String chat(String message, boolean returnJson);
    
    /**
     * 带系统提示的聊天对话
     * 
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @return AI 回复
     */
    String chatWithSystem(String systemPrompt, String message);
    
    /**
     * 带系统提示的聊天对话
     * 
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @param returnJson 是否返回 JSON 格式
     * @return AI 回复
     */
    String chatWithSystem(String systemPrompt, String message, boolean returnJson);
    
    /**
     * 使用指定模型配置的聊天对话
     * 
     * @param message 用户消息
     * @param systemPrompt 系统提示
     * @param modelConfigId 模型配置 ID
     * @return AI 回复
     */
    String chatWithModelConfig(String message, String systemPrompt, Long modelConfigId);
    
    /**
     * 多轮对话
     * 
     * @param messages 消息列表，格式：["消息 1", "消息 2", ...]
     * @return AI 回复
     */
    String chatWithHistory(List<String> messages);
    
    
    /**
     * 批量推理
     * 
     * @param prompt 推理提示
     * @return 推理结果
     */
    String batchChat(String prompt);
    
    
    
    /**
     * 文本分词和 Token 分析
     * 
     * @param texts 文本数组
     * @return 分词结果
     */
    String tokenization(String[] texts);
    
    /**
     * 创建上下文缓存
     * 
     * @param messages 消息列表
     * @return 缓存 ID
     */
    String createContext(List<String> messages);
    
    /**
     * 基于上下文缓存的对话
     * 
     * @param message 用户消息
     * @param contextId 上下文缓存 ID
     * @return AI 回复
     */
    String chatWithContext(String message, String contextId);
    
    /**
     * 获取当前使用的 AI 模型名称
     * 
     * @return 模型名称
     */
    String getCurrentModel();
    
    /**
     * 切换 AI 模型
     * 
     * @param modelType 模型类型（DOUBAO、OPENAI、DEEPSEEK 等）
     * @return 是否切换成功
     */
    boolean switchModel(String modelType);
    
    /**
     * 流式聊天对话
     * 
     * @param message 用户消息
     * @param onToken 接收到 token 时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 流式带系统提示的聊天对话
     * 
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @param onToken 接收到 token 时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChatWithSystem(String systemPrompt, String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 使用指定模型配置的流式聊天对话
     * 
     * @param message 用户消息
     * @param systemPrompt 系统提示
     * @param modelConfigId 模型配置 ID
     * @param onToken 接收到 token 时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChatWithModelConfig(String message, String systemPrompt, Long modelConfigId, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 使用指定模型配置的流式聊天对话（支持工具调用回调）
     * 
     * @param message 用户消息
     * @param systemPrompt 系统提示
     * @param modelConfigId 模型配置 ID
     * @param onToken 接收到 token 时的回调
     * @param onToolCall 工具调用时的回调
     * @param onToolResult 工具结果时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChatWithModelConfig(String message, String systemPrompt, Long modelConfigId, Consumer<String> onToken, BiConsumer<String, String> onToolCall, BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError);
}