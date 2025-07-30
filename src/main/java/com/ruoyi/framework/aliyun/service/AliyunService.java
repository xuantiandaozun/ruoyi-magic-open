package com.ruoyi.framework.aliyun.service;

import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.manager.AliyunClientManager;
import com.ruoyi.framework.aliyun.provider.AliyunCredentialProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

/**
 * 阿里云服务门面类
 * 提供统一的阿里云服务访问接口
 * 
 * @author ruoyi
 */
@Slf4j
@Service
public class AliyunService {
    
    @Autowired
    private AliyunClientManager clientManager;
    
    @Autowired
    private AliyunCredentialProvider credentialProvider;
    
    /**
     * 使用指定凭证执行操作
     * 
     * @param serviceType 服务类型
     * @param credential 凭证信息
     * @param operation 操作函数
     * @param <T> 客户端类型
     * @param <R> 返回结果类型
     * @return 操作结果
     */
    public <T, R> R executeWithCredential(String serviceType, AliyunCredential credential, Function<T, R> operation) {
        if (!credentialProvider.validateCredential(credential)) {
            throw new IllegalArgumentException("无效的阿里云凭证");
        }
        
        try {
            T client = clientManager.getClient(serviceType, credential);
            return operation.apply(client);
        } catch (Exception e) {
            log.error("执行阿里云{}服务操作失败，密钥: {}", serviceType, credential.getKeyName(), e);
            throw new RuntimeException("阿里云服务操作失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 使用指定密钥ID执行操作
     * 
     * @param serviceType 服务类型
     * @param secretKeyId 密钥ID
     * @param operation 操作函数
     * @param <T> 客户端类型
     * @param <R> 返回结果类型
     * @return 操作结果
     */
    public <T, R> R executeWithSecretKeyId(String serviceType, Long secretKeyId, Function<T, R> operation) {
        AliyunCredential credential = credentialProvider.getCredentialById(secretKeyId);
        if (credential == null) {
            throw new IllegalArgumentException("未找到密钥ID: " + secretKeyId);
        }
        
        return executeWithCredential(serviceType, credential, operation);
    }
    
    /**
     * 使用默认凭证执行操作
     * 
     * @param serviceType 服务类型
     * @param operation 操作函数
     * @param <T> 客户端类型
     * @param <R> 返回结果类型
     * @return 操作结果
     */
    public <T, R> R executeWithDefaultCredential(String serviceType, Function<T, R> operation) {
        AliyunCredential credential = credentialProvider.getDefaultCredential();
        if (credential == null) {
            throw new IllegalArgumentException("未找到可用的阿里云凭证");
        }
        
        return executeWithCredential(serviceType, credential, operation);
    }
    
    /**
     * 使用指定区域的默认凭证执行操作
     * 
     * @param serviceType 服务类型
     * @param region 区域
     * @param operation 操作函数
     * @param <T> 客户端类型
     * @param <R> 返回结果类型
     * @return 操作结果
     */
    public <T, R> R executeWithRegionCredential(String serviceType, String region, Function<T, R> operation) {
        AliyunCredential credential = credentialProvider.getDefaultCredentialByRegion(region);
        if (credential == null) {
            throw new IllegalArgumentException("未找到区域 " + region + " 的可用凭证");
        }
        
        return executeWithCredential(serviceType, credential, operation);
    }
    
    /**
     * 批量执行操作（使用所有可用凭证）
     * 
     * @param serviceType 服务类型
     * @param operation 操作函数
     * @param <T> 客户端类型
     * @param <R> 返回结果类型
     * @return 操作结果列表
     */
    public <T, R> List<R> executeWithAllCredentials(String serviceType, Function<T, R> operation) {
        List<AliyunCredential> credentials = credentialProvider.getAllCredentials();
        if (credentials.isEmpty()) {
            throw new IllegalArgumentException("未找到可用的阿里云凭证");
        }
        
        return credentials.stream()
                .map(credential -> {
                    try {
                        return executeWithCredential(serviceType, credential, operation);
                    } catch (Exception e) {
                        log.warn("使用密钥 {} 执行操作失败: {}", credential.getKeyName(), e.getMessage());
                        return null;
                    }
                })
                .filter(result -> result != null)
                .toList();
    }
    
    /**
     * 获取指定服务类型的客户端
     * 
     * @param serviceType 服务类型
     * @param credential 凭证信息
     * @param <T> 客户端类型
     * @return 客户端实例
     */
    public <T> T getClient(String serviceType, AliyunCredential credential) {
        if (!credentialProvider.validateCredential(credential)) {
            throw new IllegalArgumentException("无效的阿里云凭证");
        }
        
        return clientManager.getClient(serviceType, credential);
    }
    
    /**
     * 获取所有可用的凭证
     * 
     * @return 凭证列表
     */
    public List<AliyunCredential> getAllCredentials() {
        return credentialProvider.getAllCredentials();
    }
    
    /**
     * 获取支持的服务类型
     * 
     * @return 服务类型列表
     */
    public List<String> getSupportedServiceTypes() {
        return clientManager.getSupportedServiceTypes();
    }
    
    /**
     * 清理客户端缓存
     */
    public void clearClientCache() {
        clientManager.clearAllClients();
    }
    
    /**
     * 刷新凭证信息
     */
    public void refreshCredentials() {
        credentialProvider.refreshCredentials();
        // 清理客户端缓存以使用新的凭证
        clientManager.clearAllClients();
    }
}