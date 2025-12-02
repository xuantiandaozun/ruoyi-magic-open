package com.ruoyi.framework.security.sign;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * API 签名配置
 * 
 * @author ruoyi
 * @date 2025-12-02
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api.sign")
public class ApiSignConfig {

    /**
     * 是否启用签名验证
     */
    private Boolean enabled = false;

    /**
     * 签名密钥映射 (clientId -> secret)
     */
    private Map<String, String> secrets = new HashMap<>();

    /**
     * 时间戳允许的误差（毫秒），默认5分钟
     */
    private Long timestampTolerance = 300000L;

    /**
     * 根据客户端ID获取密钥
     */
    public String getSecretByClientId(String clientId) {
        return secrets.get(clientId);
    }
}
