package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiChatSession;

/**
 * AI聊天会话Service接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiChatSessionService extends IService<AiChatSession> {

    /**
     * 根据用户ID查询会话列表
     * 
     * @param userId 用户ID
     * @return AI聊天会话集合
     */
    public List<AiChatSession> selectSessionsByUserId(Long userId);

    /**
     * 根据用户ID和会话类型查询会话列表
     * 
     * @param userId 用户ID
     * @param sessionType 会话类型
     * @return AI聊天会话集合
     */
    public List<AiChatSession> selectSessionsByUserIdAndType(Long userId, String sessionType);

    /**
     * 创建新的聊天会话
     * 
     * @param sessionName 会话名称
     * @param userId 用户ID
     * @param modelConfigId 模型配置ID
     * @param systemPrompt 系统提示词
     * @param sessionType 会话类型
     * @return 新创建的会话
     */
    public AiChatSession createChatSession(String sessionName, Long userId, Long modelConfigId, String systemPrompt, String sessionType);

    /**
     * 更新会话的最后消息时间和消息数量
     * 
     * @param sessionId 会话ID
     * @param messageCount 消息数量
     * @return 结果
     */
    public int updateSessionMessageInfo(Long sessionId, Integer messageCount);

    /**
     * 结束会话
     * 
     * @param sessionId 会话ID
     * @return 结果
     */
    public int endSession(Long sessionId);

    /**
     * 重新激活会话
     * 
     * @param sessionId 会话ID
     * @return 结果
     */
    public int reactivateSession(Long sessionId);

    /**
     * 根据状态查询用户会话数量
     * 
     * @param userId 用户ID
     * @param status 状态
     * @return 会话数量
     */
    public int countSessionsByUserIdAndStatus(Long userId, String status);

    /**
     * 检查用户是否可以创建新会话
     * 
     * @param userId 用户ID
     * @return 是否可以创建
     */
    public boolean canCreateNewSession(Long userId);

    /**
     * 获取用户的默认会话（如果不存在则创建）
     * 
     * @param userId 用户ID
     * @param modelConfigId 模型配置ID
     * @return 默认会话
     */
    public AiChatSession getOrCreateDefaultSession(Long userId, Long modelConfigId);
}