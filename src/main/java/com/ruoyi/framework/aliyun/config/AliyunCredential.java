package com.ruoyi.framework.aliyun.config;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 阿里云凭证信息
 * 
 * @author ruoyi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliyunCredential {
    
    /**
     * 访问密钥ID
     */
    private String accessKeyId;
    
    /**
     * 访问密钥Secret
     */
    private String accessKeySecret;
    
    /**
     * 区域
     */
    private String region;
    
    /**
     * 密钥ID（关联sys_secret_key表）
     */
    private Long secretKeyId;
    
    /**
     * 密钥名称
     */
    private String keyName;
    
    /**
     * 是否有效
     */
    private boolean valid = true;
    
    /**
     * 创建凭证
     */
    public static AliyunCredential of(String accessKeyId, String accessKeySecret, String region) {
        return AliyunCredential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .region(region)
                .build();
    }
    
    /**
     * 创建凭证（带密钥ID）
     */
    public static AliyunCredential of(String accessKeyId, String accessKeySecret, String region, Long secretKeyId) {
        return AliyunCredential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .region(region)
                .secretKeyId(secretKeyId)
                .build();
    }
}