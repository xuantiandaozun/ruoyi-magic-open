package com.ruoyi.framework.security.sign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

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

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 是否启用签名验证
     */
    private Boolean enabled = false;

    /**
     * 签名密钥映射 (clientId -> secret)
     */
    private Map<String, String> secrets = new HashMap<>();

    /**
     * 强制启用签名校验的路径
     */
    private List<String> protectedPaths = new ArrayList<>();

    /**
     * 需要额外校验请求体摘要的路径
     */
    private List<String> bodyHashPaths = new ArrayList<>();

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

    /**
     * 当前路径是否需要签名校验
     */
    public boolean requiresSignature(String requestPath) {
        if (Boolean.TRUE.equals(enabled)) {
            return true;
        }
        if (requestPath == null || protectedPaths == null || protectedPaths.isEmpty()) {
            return false;
        }
        for (String pattern : protectedPaths) {
            if (pattern != null && PATH_MATCHER.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前路径是否需要校验请求体摘要
     */
    public boolean requiresBodyHash(String requestPath) {
        if (requestPath == null || bodyHashPaths == null || bodyHashPaths.isEmpty()) {
            return false;
        }
        for (String pattern : bodyHashPaths) {
            if (pattern != null && PATH_MATCHER.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }
}
