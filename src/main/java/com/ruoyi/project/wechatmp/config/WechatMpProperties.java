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

    private String author = "";

    /** 博客站点公网地址，用于阅读原文链接 */
    private String publicBaseUrl = "https://ai.zhoudw.vip";

    public boolean isConfigured() {
        return enabled
            && appId != null && !appId.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }
}
