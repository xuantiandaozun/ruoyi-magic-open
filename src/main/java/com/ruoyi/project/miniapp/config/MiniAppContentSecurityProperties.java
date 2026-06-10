package com.ruoyi.project.miniapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 小程序内容安全配置（微信 msgSecCheck / mediaCheckAsync）
 */
@Data
@Component
@ConfigurationProperties(prefix = "miniapp.translate.content-security")
public class MiniAppContentSecurityProperties {

    /** 是否启用内容安全检测 */
    private boolean enabled = true;

    /**
     * 场景值：1 资料；2 评论；3 论坛；4 社交日志
     * 翻译用户输入建议使用 4
     */
    private int scene = 4;

    /** 图片异步检测等待超时（毫秒） */
    private long mediaCheckTimeoutMs = 15000;

    /** 图片异步检测结果轮询间隔（毫秒） */
    private long mediaCheckPollIntervalMs = 300;
}
