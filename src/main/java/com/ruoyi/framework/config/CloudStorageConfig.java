package com.ruoyi.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 云存储配置
 *
 * @author ruoyi
 */
@Data
@Component
@ConfigurationProperties(prefix = "ruoyi.cloud-storage")
public class CloudStorageConfig {

    /**
     * 存储类型：local-本地存储, aliyun-阿里云OSS, tencent-腾讯云COS, amazon-亚马逊S3, azure-微软Azure
     */
    private String type = "local";

    /**
     * 阿里云OSS配置
     */
    private AliyunConfig aliyun = new AliyunConfig();

    /**
     * 腾讯云COS配置
     */
    private TencentConfig tencent = new TencentConfig();

    /**
     * 亚马逊S3配置
     */
    private AmazonConfig amazon = new AmazonConfig();

    /**
     * 微软Azure配置
     */
    private AzureConfig azure = new AzureConfig();

    /**
     * 阿里云OSS配置
     */
    @Data
    public static class AliyunConfig {
        /**
         * 访问密钥ID
         */
        private String accessKeyId;

        /**
         * 访问密钥Secret
         */
        private String accessKeySecret;

        /**
         * 存储桶名称
         */
        private String bucketName;

        /**
         * 端点地址
         */
        private String endpoint;

        /**
         * 文件前缀路径
         */
        private String prefix = "";

        /**
         * 自定义域名（可选）
         */
        private String customDomain;
    }

    /**
     * 腾讯云COS配置
     */
    @Data
    public static class TencentConfig {
        /**
         * 访问密钥ID
         */
        private String secretId;

        /**
         * 访问密钥Key
         */
        private String secretKey;

        /**
         * 存储桶名称
         */
        private String bucketName;

        /**
         * 地域
         */
        private String region;

        /**
         * 文件前缀路径
         */
        private String prefix = "";

        /**
         * 自定义域名（可选）
         */
        private String customDomain;
    }

    /**
     * 亚马逊S3配置
     */
    @Data
    public static class AmazonConfig {
        /**
         * 访问密钥ID
         */
        private String accessKeyId;

        /**
         * 访问密钥Secret
         */
        private String accessKeySecret;

        /**
         * 存储桶名称
         */
        private String bucketName;

        /**
         * 地域
         */
        private String region;

        /**
         * 端点地址（可选）
         */
        private String endpoint;

        /**
         * 文件前缀路径
         */
        private String prefix = "";

        /**
         * 自定义域名（可选）
         */
        private String customDomain;
    }

    /**
     * 微软Azure配置
     */
    @Data
    public static class AzureConfig {
        /**
         * 连接字符串
         */
        private String connectionString;

        /**
         * 容器名称
         */
        private String containerName;

        /**
         * 文件前缀路径
         */
        private String prefix = "";

        /**
         * 自定义域名（可选）
         */
        private String customDomain;
    }
}