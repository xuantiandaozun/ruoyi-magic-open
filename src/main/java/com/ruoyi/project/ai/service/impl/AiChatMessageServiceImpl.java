package com.ruoyi.project.ai.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiChatMessage;
import com.ruoyi.project.ai.mapper.AiChatMessageMapper;
import com.ruoyi.project.ai.service.IAiChatMessageService;
import com.ruoyi.project.ai.service.IAiChatSessionService;

import cn.hutool.core.util.StrUtil;

/**
 * AI聊天消息Service业务层处理
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
public class AiChatMessageServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage> implements IAiChatMessageService {
    
    @Autowired
    @Lazy
    private IAiChatSessionService aiChatSessionService;

    /**
     * 查询AI聊天消息列表
     * 
     * @param aiChatMessage AI聊天消息
     * @return AI聊天消息
     */
    @Override
    public List<AiChatMessage> selectAiChatMessageList(AiChatMessage aiChatMessage) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        
        if (aiChatMessage.getSessionId() != null) {
            queryWrapper.eq("session_id", aiChatMessage.getSessionId());
        }
        if (StrUtil.isNotEmpty(aiChatMessage.getMessageRole())) {
            queryWrapper.eq("message_role", aiChatMessage.getMessageRole());
        }
        if (StrUtil.isNotEmpty(aiChatMessage.getMessageType())) {
            queryWrapper.eq("message_type", aiChatMessage.getMessageType());
        }
        if (StrUtil.isNotEmpty(aiChatMessage.getIsDeleted())) {
            queryWrapper.eq("is_deleted", aiChatMessage.getIsDeleted());
        }
        
        queryWrapper.orderBy("message_order", true);
        
        return list(queryWrapper);
    }

    /**
     * 根据会话ID查询消息列表
     * 
     * @param sessionId 会话ID
     * @return AI聊天消息集合
     */
    @Override
    public List<AiChatMessage> selectMessagesBySessionId(Long sessionId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("session_id", sessionId)
                .eq("is_deleted", "0")
                .orderBy("message_order", true);
        
        return list(queryWrapper);
    }

    /**
     * 根据会话ID和消息角色查询消息列表
     * 
     * @param sessionId 会话ID
     * @param messageRole 消息角色
     * @return AI聊天消息集合
     */
    @Override
    public List<AiChatMessage> selectMessagesBySessionIdAndRole(Long sessionId, String messageRole) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("session_id", sessionId)
                .eq("message_role", messageRole)
                .eq("is_deleted", "0")
                .orderBy("message_order", true);
        
        return list(queryWrapper);
    }



    /**
     * 删除会话的所有消息
     * 
     * @param sessionId 会话ID
     * @return 结果
     */
    @Override
    public boolean deleteMessagesBySessionId(Long sessionId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("session_id", sessionId);
        return remove(queryWrapper);
    }

    /**
     * 添加用户消息
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param messageType 消息类型
     * @param parentMessageId 父消息ID
     * @return 新增的消息
     */
    @Override
    public AiChatMessage addUserMessage(Long sessionId, String content, String messageType, Long parentMessageId) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setParentMessageId(parentMessageId);
        message.setMessageRole("user");
        message.setMessageContent(content);
        message.setMessageType(StrUtil.isEmpty(messageType) ? "text" : messageType);
        message.setMessageOrder(getNextMessageOrder(sessionId));
        message.setIsDeleted("0");
        
        save(message);
        
        // 更新会话信息
        updateSessionAfterMessage(sessionId);
        
        return message;
    }

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
    @Override
    public AiChatMessage addAssistantMessage(Long sessionId, String content, Long modelConfigId, Integer tokenCount, String toolCalls, Long parentMessageId) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setParentMessageId(parentMessageId);
        message.setMessageRole("assistant");
        message.setMessageContent(content);
        message.setMessageType("text");
        message.setModelConfigId(modelConfigId);
        message.setTokenCount(tokenCount);
        message.setToolCalls(toolCalls);
        message.setMessageOrder(getNextMessageOrder(sessionId));
        message.setIsDeleted("0");
        
        save(message);
        
        // 更新会话信息
        updateSessionAfterMessage(sessionId);
        
        return message;
    }

    /**
     * 添加系统消息
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param parentMessageId 父消息ID
     * @return 新增的消息
     */
    @Override
    public AiChatMessage addSystemMessage(Long sessionId, String content, Long parentMessageId) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setParentMessageId(parentMessageId);
        message.setMessageRole("system");
        message.setMessageContent(content);
        message.setMessageType("text");
        message.setMessageOrder(getNextMessageOrder(sessionId));
        message.setIsDeleted("0");
        
        save(message);
        
        // 更新会话信息
        updateSessionAfterMessage(sessionId);
        
        return message;
    }

    /**
     * 统计会话消息数量
     * 
     * @param sessionId 会话ID
     * @return 消息数量
     */
    @Override
    public int countMessagesBySessionId(Long sessionId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("session_id", sessionId)
                .eq("is_deleted", "0");
        return (int) count(queryWrapper);
    }

    /**
     * 获取会话中最大的消息顺序号
     * 
     * @param sessionId 会话ID
     * @return 最大消息顺序号
     */
    @Override
    public Integer getMaxMessageOrderBySessionId(Long sessionId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("session_id", sessionId)
                .eq("is_deleted", "0")
                .orderBy("message_order", false)
                .limit(1);
        AiChatMessage result = getOne(queryWrapper);
        return result != null && result.getMessageOrder() != null ? result.getMessageOrder() : 0;
    }

    /**
     * 根据会话ID和父消息ID查询子消息列表
     * 
     * @param sessionId 会话ID
     * @param parentMessageId 父消息ID
     * @return AI聊天消息集合
     */
    @Override
    public List<AiChatMessage> selectChildMessagesByParentId(Long sessionId, Long parentMessageId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("session_id", sessionId)
                .eq("parent_message_id", parentMessageId)
                .eq("is_deleted", "0")
                .orderBy("message_order", true);
        return list(queryWrapper);
    }

    /**
     * 构建会话的消息历史（用于AI调用）
     * 
     * @param sessionId 会话ID
     * @param includeSystem 是否包含系统消息
     * @return 消息历史列表
     */
    @Override
    public List<AiChatMessage> buildMessageHistory(Long sessionId, boolean includeSystem) {
        List<AiChatMessage> messages = selectMessagesBySessionId(sessionId);
        
        if (!includeSystem) {
            // 过滤掉系统消息
            messages = messages.stream()
                    .filter(msg -> !"system".equals(msg.getMessageRole()))
                    .collect(Collectors.toList());
        }
        
        return messages;
    }

    /**
     * 清理会话中的旧消息（保留最近的N条）
     * 
     * @param sessionId 会话ID
     * @param keepCount 保留的消息数量
     * @return 清理的消息数量
     */
    @Override
    public int cleanOldMessages(Long sessionId, int keepCount) {
        List<AiChatMessage> messages = selectMessagesBySessionId(sessionId);
        
        if (messages.size() <= keepCount) {
            return 0; // 不需要清理
        }
        
        // 计算需要删除的消息数量
        int deleteCount = messages.size() - keepCount;
        
        // 获取需要删除的消息ID（保留最新的keepCount条）
        Long[] deleteIds = messages.stream()
                .limit(deleteCount)
                .map(AiChatMessage::getId)
                .toArray(Long[]::new);
        
        // 批量删除旧消息
        removeByIds(List.of(deleteIds));
        
        // 更新会话消息数量
        updateSessionAfterMessage(sessionId);
        
        return deleteCount;
    }

    /**
     * 获取下一个消息顺序号
     * 
     * @param sessionId 会话ID
     * @return 下一个消息顺序号
     */
    private Integer getNextMessageOrder(Long sessionId) {
        return getMaxMessageOrderBySessionId(sessionId) + 1;
    }

    /**
     * 消息添加后更新会话信息
     * 
     * @param sessionId 会话ID
     */
    private void updateSessionAfterMessage(Long sessionId) {
        int messageCount = countMessagesBySessionId(sessionId);
        aiChatSessionService.updateSessionMessageInfo(sessionId, messageCount);
    }
}