package com.ruoyi.project.feishu.domain.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 飞书消息DTO
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Data
@Accessors(chain = true)
public class FeishuMessageDto {

    /** 接收者ID */
    private String receiveId;

    /** 接收者ID类型（user_id、email、open_id、union_id、chat_id） */
    private String receiveIdType = "user_id";

    /**
     * 消息类型（text、post、image、file、audio、media、sticker、interactive、share_chat、share_user）
     */
    private String msgType = "text";

    /** 消息内容 */
    private String content;

    /** 消息UUID（可选，用于幂等） */
    private String uuid;

    /**
     * 转义 JSON 字符串中的特殊字符
     * 
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\") // 反斜杠必须最先转义
                .replace("\"", "\\\"") // 双引号
                .replace("\n", "\\n") // 换行符
                .replace("\r", "\\r") // 回车符
                .replace("\t", "\\t") // 制表符
                .replace("\b", "\\b") // 退格符
                .replace("\f", "\\f"); // 换页符
    }

    /**
     * 创建文本消息
     * 
     * @param receiveId     接收者ID
     * @param receiveIdType 接收者ID类型
     * @param text          文本内容
     * @return FeishuMessageDto
     */
    public static FeishuMessageDto createTextMessage(String receiveId, String receiveIdType, String text) {
        return new FeishuMessageDto()
                .setReceiveId(receiveId)
                .setReceiveIdType(receiveIdType)
                .setMsgType("text")
                .setContent("{\"text\":\"" + escapeJson(text) + "\"}");
    }

    /**
     * 创建富文本消息
     * 
     * @param receiveId     接收者ID
     * @param receiveIdType 接收者ID类型
     * @param title         标题
     * @param content       内容
     * @return FeishuMessageDto
     */
    public static FeishuMessageDto createPostMessage(String receiveId, String receiveIdType, String title,
            String content) {
        String postContent = String.format(
                "{\"zh_cn\":{\"title\":\"%s\",\"content\":[[{\"tag\":\"text\",\"text\":\"%s\"}]]}}",
                escapeJson(title),
                escapeJson(content));

        return new FeishuMessageDto()
                .setReceiveId(receiveId)
                .setReceiveIdType(receiveIdType)
                .setMsgType("post")
                .setContent(postContent);
    }
}