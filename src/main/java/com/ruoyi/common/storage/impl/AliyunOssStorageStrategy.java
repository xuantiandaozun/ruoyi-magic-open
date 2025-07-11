package com.ruoyi.common.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.ruoyi.common.storage.FileStorageStrategy;
import com.ruoyi.framework.config.CloudStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 阿里云OSS文件存储策略实现
 *
 * @author ruoyi
 */
@Component("aliyunOssStorageStrategy")
@ConditionalOnProperty(name = "ruoyi.cloud-storage.type", havingValue = "aliyun")
public class AliyunOssStorageStrategy implements FileStorageStrategy {

    @Autowired
    private CloudStorageConfig cloudStorageConfig;

    /**
     * 获取OSS客户端
     */
    private OSS getOssClient() {
        CloudStorageConfig.AliyunConfig aliyun = cloudStorageConfig.getAliyun();
        return new OSSClientBuilder().build(aliyun.getEndpoint(), aliyun.getAccessKeyId(), aliyun.getAccessKeySecret());
    }

    @Override
    public String upload(MultipartFile file, String fileName) throws Exception {
        OSS ossClient = null;
        try {
            ossClient = getOssClient();
            CloudStorageConfig.AliyunConfig aliyun = cloudStorageConfig.getAliyun();
            
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
            CloudStorageConfig.AliyunConfig aliyun = cloudStorageConfig.getAliyun();
            String objectName = aliyun.getPrefix() + fileName;
            ossClient.deleteObject(aliyun.getBucketName(), objectName);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        CloudStorageConfig.AliyunConfig aliyun = cloudStorageConfig.getAliyun();
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
            CloudStorageConfig.AliyunConfig aliyun = cloudStorageConfig.getAliyun();
            String objectName = aliyun.getPrefix() + fileName;
            return ossClient.doesObjectExist(aliyun.getBucketName(), objectName);
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