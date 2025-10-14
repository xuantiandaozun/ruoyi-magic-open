package com.ruoyi.project.ai.dto;

/**
 * 聊天消息对象
 */
public class AiChatMessage {
    /**
     * 消息角色：user（用户）、assistant（AI助手）、system（系统）
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;

    public AiChatMessage() {
    }

    public AiChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}