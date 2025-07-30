package com.ruoyi.framework.aliyun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云SDK配置类
 * 
 * @author ruoyi
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ruoyi.aliyun")
public class AliyunSdkConfig {
    
    /**
     * 是否启用阿里云SDK封装
     */
    private boolean enabled = true;
    
    /**
     * 客户端连接超时时间（毫秒）
     */
    private int connectTimeout = 10000;
    
    /**
     * 客户端读取超时时间（毫秒）
     */
    private int readTimeout = 30000;
    
    /**
     * 客户端写入超时时间（毫秒）
     */
    private int writeTimeout = 30000;
    
    /**
     * 最大重试次数
     */
    private int maxRetries = 3;
    
    /**
     * 是否启用客户端缓存
     */
    private boolean enableClientCache = true;
    
    /**
     * 客户端缓存过期时间（分钟）
     */
    private int clientCacheExpireMinutes = 60;
    
    /**
     * 默认区域
     */
    private String defaultRegion = "cn-hangzhou";
    
    /**
     * 是否启用SSL
     */
    private boolean enableSsl = true;
    
    /**
     * 用户代理
     */
    private String userAgent = "RuoYi-Magic/1.0";
    
    /**
     * RDS配置
     */
    private RdsConfig rds = new RdsConfig();
    
    /**
     * OSS配置
     */
    private OssConfig oss = new OssConfig();
    
    /**
     * ECS配置
     */
    private EcsConfig ecs = new EcsConfig();
    
    @Data
    public static class RdsConfig {
        /**
         * 是否启用RDS服务
         */
        private boolean enabled = true;
        
        /**
         * RDS API版本
         */
        private String apiVersion = "2014-08-15";
    }
    
    @Data
    public static class OssConfig {
        /**
         * 是否启用OSS服务
         */
        private boolean enabled = true;
        
        /**
         * 默认存储桶名称
         */
        private String defaultBucket;
        
        /**
         * 是否启用HTTPS
         */
        private boolean enableHttps = true;
    }
    
    @Data
    public static class EcsConfig {
        /**
         * 是否启用ECS服务
         */
        private boolean enabled = true;
        
        /**
         * ECS API版本
         */
        private String apiVersion = "2014-05-26";
    }
}