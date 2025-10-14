package com.ruoyi.project.ai.service;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ruoyi.project.ai.dto.AiChatMessage;

/**
 * AI服务接口
 * 支持多种AI模型
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiService {
    
    /**
     * 基础聊天对话
     * 
     * @param message 用户消息
     * @return AI回复
     */
    String chat(String message);
    
    /**
     * 基础聊天对话
     * 
     * @param message 用户消息
     * @param returnJson 是否返回JSON格式
     * @return AI回复
     */
    String chat(String message, boolean returnJson);
    
    /**
     * 带系统提示的聊天对话
     * 
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @return AI回复
     */
    String chatWithSystem(String systemPrompt, String message);
    
    /**
     * 带系统提示的聊天对话
     * 
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @param returnJson 是否返回JSON格式
     * @return AI回复
     */
    String chatWithSystem(String systemPrompt, String message, boolean returnJson);
    
    /**
     * 使用指定模型配置的聊天对话
     * 
     * @param message 用户消息
     * @param systemPrompt 系统提示
     * @param modelConfigId 模型配置ID
     * @return AI回复
     */
    String chatWithModelConfig(String message, String systemPrompt, Long modelConfigId);
    
    /**
     * 多轮对话
     * 
     * @param messages 消息列表，格式：[{"role": "user", "content": "消息内容"}]
     * @return AI回复
     */
    String chatWithHistory(List<String> messages);
    
    /**
     * 图文理解（视觉对话）
     * 支持豆包等具有视觉能力的模型
     * 
     * @param message 用户消息
     * @param imageUrls 图片URL列表
     * @return AI回复
     */
    String chatVision(String message, List<String> imageUrls);
    
    /**
     * 文生图（图像生成）
     * 支持豆包等具有图像生成能力的模型
     * 
     * @param prompt 图像生成提示词
     * @param size 图像尺寸（如：512x512）
     * @param guidanceScale 引导比例
     * @param seed 随机种子
     * @param watermark 是否添加水印
     * @return 生成的图像URL
     */
    String generateImage(String prompt, String size, Double guidanceScale, Integer seed, Boolean watermark);
    
    /**
     * 文本向量化
     * 
     * @param texts 文本数组
     * @return 向量化结果JSON字符串
     */
    String embeddingText(String[] texts);
    
    /**
     * 图文向量化
     * 支持豆包等具有图文向量化能力的模型
     * 
     * @param text 文本内容
     * @param imageUrl 图片URL
     * @return 向量化结果JSON字符串
     */
    String embeddingVision(String text, String imageUrl);
    
    /**
     * 批量推理
     * 
     * @param prompt 推理提示
     * @return 推理结果
     */
    String batchChat(String prompt);
    
    /**
     * 创建视频生成任务（豆包专用）
     * 
     * @param prompt 视频生成提示
     * @param imageUrl 参考图片URL（可选）
     * @return 任务ID
     */
    String createVideoTask(String prompt, String imageUrl);
    
    /**
     * 查询视频生成任务状态（豆包专用）
     * 
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    String getVideoTaskStatus(String taskId);
    
    /**
     * 文本分词和Token分析
     * 
     * @param texts 文本数组
     * @return 分词结果
     */
    String tokenization(String[] texts);
    
    /**
     * 创建上下文缓存
     * 
     * @param messages 消息列表
     * @return 缓存ID
     */
    String createContext(List<String> messages);
    
    /**
     * 基于上下文缓存的对话
     * 
     * @param message 用户消息
     * @param contextId 上下文缓存ID
     * @return AI回复
     */
    String chatWithContext(String message, String contextId);
    
    /**
     * 获取当前使用的AI模型名称
     * 
     * @return 模型名称
     */
    String getCurrentModel();
    
    /**
     * 切换AI模型
     * 
     * @param modelType 模型类型（DOUBAO、OPENAI、DEEPSEEK等）
     * @return 是否切换成功
     */
    boolean switchModel(String modelType);
    
    /**
     * 流式聊天对话
     * 
     * @param message 用户消息
     * @param onToken 接收到token时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 流式带系统提示的聊天对话
     * 
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @param onToken 接收到token时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChatWithSystem(String systemPrompt, String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 使用指定模型配置的流式聊天对话
     * 
     * @param message 用户消息
     * @param systemPrompt 系统提示
     * @param modelConfigId 模型配置ID
     * @param onToken 接收到token时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChatWithModelConfig(String message, String systemPrompt, Long modelConfigId, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 使用指定模型配置的流式聊天对话（支持聊天历史）
     * 
     * @param message 用户消息
     * @param systemPrompt 系统提示
     * @param chatHistory 聊天历史
     * @param modelConfigId 模型配置ID
     * @param onToken 接收到token时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, Long modelConfigId, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 使用指定模型配置的流式聊天对话（支持聊天历史和工具调用回调）
     * 
     * @param message 用户消息
     * @param systemPrompt 系统提示
     * @param chatHistory 聊天历史
     * @param modelConfigId 模型配置ID
     * @param onToken 接收到token时的回调
     * @param onToolCall 工具调用时的回调
     * @param onToolResult 工具结果时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, Long modelConfigId, Consumer<String> onToken, BiConsumer<String, String> onToolCall, BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * 使用指定模型配置的流式聊天对话（支持工具调用回调）
     * 
     * @param message 用户消息
     * @param systemPrompt 系统提示
     * @param modelConfigId 模型配置ID
     * @param onToken 接收到token时的回调
     * @param onToolCall 工具调用时的回调
     * @param onToolResult 工具结果时的回调
     * @param onComplete 完成时的回调
     * @param onError 出错时的回调
     */
    void streamChatWithModelConfig(String message, String systemPrompt, Long modelConfigId, Consumer<String> onToken, BiConsumer<String, String> onToolCall, BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError);
}