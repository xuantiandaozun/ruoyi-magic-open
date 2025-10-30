package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiChatMessage;

/**
 * AI聊天消息Service接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiChatMessageService extends IService<AiChatMessage>
{
    /**
     * 查询AI聊天消息列表
     * 
     * @param aiChatMessage AI聊天消息
     * @return AI聊天消息集合
     */
    public List<AiChatMessage> selectAiChatMessageList(AiChatMessage aiChatMessage);

    /**
     * 根据会话ID查询消息列表
     * 
     * @param sessionId 会话ID
     * @return AI聊天消息集合
     */
    public List<AiChatMessage> selectMessagesBySessionId(Long sessionId);

    /**
     * 根据会话ID和消息角色查询消息列表
     * 
     * @param sessionId 会话ID
     * @param messageRole 消息角色
     * @return AI聊天消息集合
     */
    public List<AiChatMessage> selectMessagesBySessionIdAndRole(Long sessionId, String messageRole);

    /**
     * 根据会话ID删除所有消息
     * 
     * @param sessionId 会话ID
     * @return 结果
     */
    public boolean deleteMessagesBySessionId(Long sessionId);

    /**
     * 添加用户消息
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param messageType 消息类型
     * @param parentMessageId 父消息ID
     * @return 新增的消息
     */
    public AiChatMessage addUserMessage(Long sessionId, String content, String messageType, Long parentMessageId);

    /**
     * 添加AI助手消息
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param modelConfigId 模型配置ID
     * @param tokenCount Token数量
     * @param toolCalls 工具调用信息
     * @param parentMessageId 父消息ID
     * @return 新增的消息
     */
    public AiChatMessage addAssistantMessage(Long sessionId, String content, Long modelConfigId, Integer tokenCount, String toolCalls, Long parentMessageId);

    /**
     * 添加系统消息
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param parentMessageId 父消息ID
     * @return 新增的消息
     */
    public AiChatMessage addSystemMessage(Long sessionId, String content, Long parentMessageId);

    /**
     * 统计会话消息数量
     * 
     * @param sessionId 会话ID
     * @return 消息数量
     */
    public int countMessagesBySessionId(Long sessionId);

    /**
     * 获取会话中最大的消息顺序号
     * 
     * @param sessionId 会话ID
     * @return 最大消息顺序号
     */
    public Integer getMaxMessageOrderBySessionId(Long sessionId);

    /**
     * 根据会话ID和父消息ID查询子消息列表
     * 
     * @param sessionId 会话ID
     * @param parentMessageId 父消息ID
     * @return AI聊天消息集合
     */
    public List<AiChatMessage> selectChildMessagesByParentId(Long sessionId, Long parentMessageId);

    /**
     * 构建会话的消息历史（用于AI调用）
     * 
     * @param sessionId 会话ID
     * @param includeSystem 是否包含系统消息
     * @return 消息历史列表
     */
    public List<AiChatMessage> buildMessageHistory(Long sessionId, boolean includeSystem);

    /**
     * 清理会话中的旧消息（保留最近的N条）
     * 
     * @param sessionId 会话ID
     * @param keepCount 保留的消息数量
     * @return 清理的消息数量
     */
    public int cleanOldMessages(Long sessionId, int keepCount);
}