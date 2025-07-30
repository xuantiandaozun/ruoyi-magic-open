package com.ruoyi.framework.aliyun.manager;

import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.factory.AliyunClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 阿里云客户端管理器
 * 负责管理多个密钥对应的客户端实例
 * 
 * @author ruoyi
 */
@Slf4j
@Component
public class AliyunClientManager {
    
    /**
     * 客户端缓存：serviceType -> (credentialKey -> client)
     */
    private final Map<String, Map<String, Object>> clientCache = new ConcurrentHashMap<>();
    
    /**
     * 工厂映射：serviceType -> factory
     */
    private final Map<String, AliyunClientFactory<?>> factoryMap;
    
    @Autowired
    public AliyunClientManager(List<AliyunClientFactory<?>> factories) {
        this.factoryMap = factories.stream()
                .collect(Collectors.toMap(
                    AliyunClientFactory::getServiceType,
                    Function.identity()
                ));
        log.info("初始化阿里云客户端管理器，支持的服务类型: {}", factoryMap.keySet());
    }
    
    /**
     * 获取客户端实例
     * 
     * @param serviceType 服务类型（如：RDS、OSS、ECS等）
     * @param credential 凭证信息
     * @param <T> 客户端类型
     * @return 客户端实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getClient(String serviceType, AliyunCredential credential) {
        if (!factoryMap.containsKey(serviceType)) {
            throw new IllegalArgumentException("不支持的服务类型: " + serviceType);
        }
        
        String credentialKey = buildCredentialKey(credential);
        
        return (T) clientCache
                .computeIfAbsent(serviceType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(credentialKey, k -> {
                    log.debug("创建{}客户端实例，密钥: {}", serviceType, credential.getKeyName());
                    AliyunClientFactory<T> factory = (AliyunClientFactory<T>) factoryMap.get(serviceType);
                    return factory.createClient(credential);
                });
    }
    
    /**
     * 移除客户端实例
     * 
     * @param serviceType 服务类型
     * @param credential 凭证信息
     */
    @SuppressWarnings("unchecked")
    public void removeClient(String serviceType, AliyunCredential credential) {
        String credentialKey = buildCredentialKey(credential);
        Map<String, Object> serviceClients = clientCache.get(serviceType);
        
        if (serviceClients != null) {
            Object client = serviceClients.remove(credentialKey);
            if (client != null) {
                AliyunClientFactory factory = factoryMap.get(serviceType);
                if (factory != null) {
                    factory.closeClient(client);
                }
                log.debug("移除{}客户端实例，密钥: {}", serviceType, credential.getKeyName());
            }
        }
    }
    
    /**
     * 清理所有客户端实例
     */
    @SuppressWarnings("unchecked")
    public void clearAllClients() {
        log.info("清理所有阿里云客户端实例");
        
        clientCache.forEach((serviceType, serviceClients) -> {
            AliyunClientFactory factory = factoryMap.get(serviceType);
            if (factory != null) {
                serviceClients.values().forEach(client -> factory.closeClient(client));
            }
        });
        
        clientCache.clear();
    }
    
    /**
     * 清理指定服务类型的所有客户端
     * 
     * @param serviceType 服务类型
     */
    @SuppressWarnings("unchecked")
    public void clearServiceClients(String serviceType) {
        Map<String, Object> serviceClients = clientCache.get(serviceType);
        if (serviceClients != null) {
            AliyunClientFactory factory = factoryMap.get(serviceType);
            if (factory != null) {
                serviceClients.values().forEach(client -> factory.closeClient(client));
            }
            serviceClients.clear();
            log.debug("清理{}服务的所有客户端实例", serviceType);
        }
    }
    
    /**
     * 获取支持的服务类型列表
     * 
     * @return 服务类型列表
     */
    public List<String> getSupportedServiceTypes() {
        return List.copyOf(factoryMap.keySet());
    }
    
    /**
     * 构建凭证缓存键
     * 
     * @param credential 凭证信息
     * @return 缓存键
     */
    private String buildCredentialKey(AliyunCredential credential) {
        return String.format("%s:%s:%s", 
            credential.getAccessKeyId(),
            credential.getRegion(),
            credential.getSecretKeyId() != null ? credential.getSecretKeyId() : "default"
        );
    }
}