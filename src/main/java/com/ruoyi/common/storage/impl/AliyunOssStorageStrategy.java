package com.ruoyi.common.storage.impl;

import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.storage.FileStorageStrategy;
import com.ruoyi.framework.config.CloudStorageConfig;
import com.ruoyi.project.system.domain.StorageConfig;
import com.ruoyi.project.system.service.IStorageConfigService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云OSS文件存储策略实现
 *
 * @author ruoyi
 */
@Component("aliyunOssStorageStrategy")
@Slf4j
public class AliyunOssStorageStrategy implements FileStorageStrategy {

    @Autowired
    private CloudStorageConfig cloudStorageConfig;
    
    @Autowired
    private IStorageConfigService storageConfigService;

    /**
     * 获取阿里云配置
     * 优先从数据库读取，如果数据库没有配置则使用YML配置
     */
    private AliyunConfig getAliyunConfig() {
        try {
            // 1. 优先查询数据库中的阿里云配置
            QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("storage_type", "aliyun")
                .eq("is_default", "Y")
                .eq("status", "0")
                .eq("del_flag", "0");
            
            StorageConfig dbConfig = storageConfigService.getOne(queryWrapper);
            
            if (dbConfig != null && StrUtil.isNotEmpty(dbConfig.getConfigData())) {
                // 解析数据库配置
                Map<String, Object> configData = JSONUtil.toBean(dbConfig.getConfigData(), Map.class);
                AliyunConfig config = new AliyunConfig();
                config.setAccessKeyId((String) configData.get("accessKeyId"));
                config.setAccessKeySecret((String) configData.get("accessKeySecret"));
                config.setBucketName((String) configData.get("bucketName"));
                config.setEndpoint((String) configData.get("endpoint"));
                config.setPrefix((String) configData.getOrDefault("prefix", ""));
                config.setCustomDomain((String) configData.get("customDomain"));
                return config;
            }
        } catch (Exception e) {
            // 数据库查询失败，记录日志但不影响功能
            System.err.println("查询数据库阿里云配置失败，使用YML配置: " + e.getMessage());
        }
        
        // 2. 回退到YML配置
        CloudStorageConfig.AliyunConfig ymlConfig = cloudStorageConfig.getAliyun();
        AliyunConfig config = new AliyunConfig();
        config.setAccessKeyId(ymlConfig.getAccessKeyId());
        config.setAccessKeySecret(ymlConfig.getAccessKeySecret());
        config.setBucketName(ymlConfig.getBucketName());
        config.setEndpoint(ymlConfig.getEndpoint());
        config.setPrefix(ymlConfig.getPrefix());
        config.setCustomDomain(ymlConfig.getCustomDomain());
        return config;
    }

    /**
     * 获取OSS客户端
     */
    private OSS getOssClient() {
        AliyunConfig config = getAliyunConfig();
        return new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKeyId(), config.getAccessKeySecret());
    }
    
    /**
     * 阿里云配置类
     */
    private static class AliyunConfig {
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private String endpoint;
        private String prefix = "";
        private String customDomain;
        
        // Getters and Setters
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
        public String getBucketName() { return bucketName; }
        public void setBucketName(String bucketName) { this.bucketName = bucketName; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public String getCustomDomain() { return customDomain; }
        public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }
    }

    @Override
    public String upload(MultipartFile file, String fileName) throws Exception {
        OSS ossClient = null;
        try {
            ossClient = getOssClient();
            AliyunConfig aliyun = getAliyunConfig();
            
            // 构建完整的文件路径
            String objectName = aliyun.getPrefix() + fileName;
            
            // 上传文件
            InputStream inputStream = file.getInputStream();
            PutObjectRequest putObjectRequest = new PutObjectRequest(aliyun.getBucketName(), objectName, inputStream);
            ossClient.putObject(putObjectRequest);
            
            return getFileUrl(fileName);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public boolean delete(String fileName) {
        OSS ossClient = null;
        try {
            ossClient = getOssClient();
            AliyunConfig aliyun = getAliyunConfig();
            String objectName = aliyun.getPrefix() + fileName;
            ossClient.deleteObject(aliyun.getBucketName(), objectName);
            return true;
        } catch (Exception e) {
            log.error("删除文件失败: {}", fileName, e);
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        AliyunConfig aliyun = getAliyunConfig();
        if (aliyun.getCustomDomain() != null && !aliyun.getCustomDomain().isEmpty()) {
            return aliyun.getCustomDomain() + "/" + aliyun.getPrefix() + fileName;
        } else {
            return "https://" + aliyun.getBucketName() + "." + aliyun.getEndpoint().replace("https://", "").replace("http://", "") + "/" + aliyun.getPrefix() + fileName;
        }
    }

    @Override
    public boolean exists(String fileName) {
        OSS ossClient = null;
        try {
            ossClient = getOssClient();
            AliyunConfig config = getAliyunConfig();
            
            String objectName = config.getPrefix() + fileName;
            return ossClient.doesObjectExist(config.getBucketName(), objectName);
        } catch (Exception e) {
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public String getStorageType() {
        return "aliyun";
    }
}