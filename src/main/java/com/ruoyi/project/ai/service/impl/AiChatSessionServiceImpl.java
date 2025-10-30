package com.ruoyi.project.ai.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.ai.domain.AiChatSession;
import com.ruoyi.project.ai.mapper.AiChatSessionMapper;
import com.ruoyi.project.ai.service.IAiChatMessageService;
import com.ruoyi.project.ai.service.IAiChatSessionService;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

/**
 * AI聊天会话Service业务层处理
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
public class AiChatSessionServiceImpl extends ServiceImpl<AiChatSessionMapper, AiChatSession> implements IAiChatSessionService {
    
    @Autowired
    @Lazy
    private IAiChatMessageService aiChatMessageService;



    /**
     * 根据用户ID查询会话列表
     * 
     * @param userId 用户ID
     * @return AI聊天会话集合
     */
    @Override
    public List<AiChatSession> selectSessionsByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId)
                .eq("del_flag", "0")
                .orderBy("last_message_time", false);
        return list(queryWrapper);
    }

    /**
     * 根据用户ID和会话类型查询会话列表
     * 
     * @param userId 用户ID
     * @param sessionType 会话类型
     * @return AI聊天会话集合
     */
    @Override
    public List<AiChatSession> selectSessionsByUserIdAndType(Long userId, String sessionType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId)
                .eq("session_type", sessionType)
                .eq("del_flag", "0")
                .orderBy("last_message_time", false);
        return list(queryWrapper);
    }



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
    @Override
    public AiChatSession createChatSession(String sessionName, Long userId, Long modelConfigId, String systemPrompt, String sessionType) {
        AiChatSession session = new AiChatSession();
        
        // 如果没有提供会话名称，生成默认名称
        if (StrUtil.isEmpty(sessionName)) {
            sessionName = "聊天会话 " + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm");
        }
        
        session.setSessionName(sessionName);
        session.setUserId(userId);
        session.setModelConfigId(modelConfigId);
        session.setSystemPrompt(systemPrompt);
        session.setSessionType(StrUtil.isEmpty(sessionType) ? "chat" : sessionType);
        session.setStatus("0"); // 正常状态
        session.setMessageCount(0);
        session.setLastMessageTime(new Date());
        session.setDelFlag("0");
        session.setCreateBy(SecurityUtils.getUsername());
        
        save(session);
        
        // 如果有系统提示词，添加系统消息
        if (StrUtil.isNotEmpty(systemPrompt)) {
            aiChatMessageService.addSystemMessage(session.getId(), systemPrompt, null);
            // 更新消息数量
            updateSessionMessageInfo(session.getId(), 1);
        }
        
        return session;
    }

    /**
     * 更新会话的最后消息时间和消息数量
     * 
     * @param sessionId 会话ID
     * @param messageCount 消息数量
     * @return 结果
     */
    @Override
    public int updateSessionMessageInfo(Long sessionId, Integer messageCount) {
        AiChatSession session = new AiChatSession();
        session.setId(sessionId);
        session.setMessageCount(messageCount);
        session.setLastMessageTime(new Date());
        session.setUpdateBy(SecurityUtils.getUsername());
        return updateById(session) ? 1 : 0;
    }

    /**
     * 结束会话
     * 
     * @param sessionId 会话ID
     * @return 结果
     */
    @Override
    public int endSession(Long sessionId) {
        AiChatSession session = new AiChatSession();
        session.setId(sessionId);
        session.setStatus("1"); // 已结束状态
        session.setUpdateBy(SecurityUtils.getUsername());
        return updateById(session) ? 1 : 0;
    }

    /**
     * 重新激活会话
     * 
     * @param sessionId 会话ID
     * @return 结果
     */
    @Override
    public int reactivateSession(Long sessionId) {
        AiChatSession session = new AiChatSession();
        session.setId(sessionId);
        session.setStatus("0"); // 正常状态
        session.setUpdateBy(SecurityUtils.getUsername());
        return updateById(session) ? 1 : 0;
    }

    /**
     * 根据状态查询用户会话数量
     * 
     * @param userId 用户ID
     * @param status 状态
     * @return 会话数量
     */
    @Override
    public int countSessionsByUserIdAndStatus(Long userId, String status) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId)
                .eq("status", status)
                .eq("del_flag", "0");
        return (int) count(queryWrapper);
    }

    /**
     * 检查用户是否可以创建新会话
     * 
     * @param userId 用户ID
     * @return 是否可以创建
     */
    @Override
    public boolean canCreateNewSession(Long userId) {
        // 检查用户活跃会话数量，这里可以根据业务需求设置限制
        int activeSessionCount = countSessionsByUserIdAndStatus(userId, "0");
        // 假设每个用户最多可以有10个活跃会话
        return activeSessionCount < 10;
    }

    /**
     * 获取用户的默认会话（如果不存在则创建）
     * 
     * @param userId 用户ID
     * @param modelConfigId 模型配置ID
     * @return 默认会话
     */
    @Override
    public AiChatSession getOrCreateDefaultSession(Long userId, Long modelConfigId) {
        // 查找用户最近的活跃会话
        List<AiChatSession> sessions = selectSessionsByUserIdAndType(userId, "chat");
        
        if (!sessions.isEmpty()) {
            AiChatSession latestSession = sessions.get(0);
            // 如果最近的会话状态正常，直接返回
            if ("0".equals(latestSession.getStatus())) {
                return latestSession;
            }
        }
        
        // 如果没有活跃会话，创建新的默认会话
        return createChatSession(null, userId, modelConfigId, null, "chat");
    }
}