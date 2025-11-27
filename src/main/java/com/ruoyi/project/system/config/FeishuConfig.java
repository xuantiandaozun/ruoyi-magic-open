package com.ruoyi.project.system.config;

import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 飞书配置类
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Data
@Component
public class FeishuConfig {
    
    /** 应用ID */
    private String appId;
    
    /** 应用密钥 */
    private String appSecret;
    
    /** 是否启用 */
    private boolean enabled = false;
    
    /** 密钥ID */
    private Long keyId;
    
    /** 密钥名称 */
    private String keyName;
    
    /**
     * 构造函数
     */
    public FeishuConfig() {
    }
    
    /**
     * 构造函数
     * 
     * @param appId 应用ID
     * @param appSecret 应用密钥
     */
    public FeishuConfig(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.enabled = true;
    }
    
    /**
     * 检查配置是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return enabled && appId != null && !appId.trim().isEmpty() 
               && appSecret != null && !appSecret.trim().isEmpty();
    }
}