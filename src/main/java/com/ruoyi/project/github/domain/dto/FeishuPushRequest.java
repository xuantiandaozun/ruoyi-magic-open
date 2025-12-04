package com.ruoyi.project.github.domain.dto;

import lombok.Data;

/**
 * 飞书消息推送请求
 * 
 * @author ruoyi
 * @date 2025-12-03
 */
@Data
public class FeishuPushRequest {

    /**
     * 消息标题（可选，如果提供则发送富文本消息）
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;
}
