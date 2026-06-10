package com.ruoyi.project.wechatmp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 微信公众号 API 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.mp")
public class WechatMpProperties {

    private boolean enabled = false;

    private String appId;

    private String appSecret;

    private String token;

    private String aesKey;

    private String author = "";

    /** 博客站点公网地址，用于阅读原文链接 */
    private String publicBaseUrl = "https://ai.zhoudw.vip";

    /** 自动回复使用的 AI 模型配置ID（对应 ai_model_config.id） */
    private Long aiModelConfigId = 55L;

    /** 自动回复系统提示词 */
    private String aiSystemPrompt = "你是一个友好的智能助手，负责回答微信公众号用户的问题。请用简洁、专业、友好的中文回复。";

    public boolean isConfigured() {
        return enabled
            && appId != null && !appId.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }
}
