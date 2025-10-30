package com.ruoyi.project.ai.dto;

import com.ruoyi.project.ai.domain.AiChatMessage;
import java.util.List;

/**
 * 聊天请求对象
 */
public class ChatRequest {
    private String message;
    private String systemPrompt;
    private Long modelConfigId;
    
    /**
     * 会话ID，用于关联聊天会话
     */
    private Long sessionId;
    
    /**
     * 聊天历史记录，用于多轮对话
     */
    private List<AiChatMessage> chatHistory;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Long getModelConfigId() {
        return modelConfigId;
    }

    public void setModelConfigId(Long modelConfigId) {
        this.modelConfigId = modelConfigId;
    }

    public List<AiChatMessage> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<AiChatMessage> chatHistory) {
        this.chatHistory = chatHistory;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}