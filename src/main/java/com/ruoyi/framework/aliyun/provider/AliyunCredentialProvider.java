package com.ruoyi.framework.aliyun.provider;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.project.system.domain.SysSecretKey;
import com.ruoyi.project.system.service.ISysSecretKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云凭证提供者
 * 负责从数据库中获取和管理阿里云密钥信息
 * 
 * @author ruoyi
 */
@Slf4j
@Component
public class AliyunCredentialProvider {
    
    @Autowired
    private ISysSecretKeyService sysSecretKeyService;
    
    /**
     * 获取所有有效的阿里云凭证
     * 
     * @return 凭证列表
     */
    public List<AliyunCredential> getAllCredentials() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("provider_brand", "aliyun")
                .eq("status", "0")
                .eq("del_flag", "0");
        
        List<SysSecretKey> secretKeys = sysSecretKeyService.list(queryWrapper);
        
        return secretKeys.stream()
                .map(this::convertToCredential)
                .filter(AliyunCredential::isValid)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据密钥ID获取凭证
     * 
     * @param secretKeyId 密钥ID
     * @return 凭证信息
     */
    public AliyunCredential getCredentialById(Long secretKeyId) {
        SysSecretKey secretKey = sysSecretKeyService.getById(secretKeyId);
        if (secretKey == null || !"aliyun".equals(secretKey.getProviderBrand()) 
            || !"0".equals(secretKey.getStatus()) || !"0".equals(secretKey.getDelFlag())) {
            return null;
        }
        
        return convertToCredential(secretKey);
    }
    
    /**
     * 根据区域获取凭证列表
     * 
     * @param region 区域
     * @return 凭证列表
     */
    public List<AliyunCredential> getCredentialsByRegion(String region) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("provider_brand", "aliyun")
                .eq("status", "0")
                .eq("del_flag", "0");
        
        if (StringUtils.hasText(region)) {
            queryWrapper.eq("region", region);
        }
        
        List<SysSecretKey> secretKeys = sysSecretKeyService.list(queryWrapper);
        
        return secretKeys.stream()
                .map(this::convertToCredential)
                .filter(AliyunCredential::isValid)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据密钥类型获取凭证列表
     * 
     * @param keyType 密钥类型
     * @return 凭证列表
     */
    public List<AliyunCredential> getCredentialsByKeyType(String keyType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("provider_brand", "aliyun")
                .eq("status", "0")
                .eq("del_flag", "0");
        
        if (StringUtils.hasText(keyType)) {
            queryWrapper.eq("key_type", keyType);
        }
        
        List<SysSecretKey> secretKeys = sysSecretKeyService.list(queryWrapper);
        
        return secretKeys.stream()
                .map(this::convertToCredential)
                .filter(AliyunCredential::isValid)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取默认凭证（第一个有效的凭证）
     * 
     * @return 默认凭证
     */
    public AliyunCredential getDefaultCredential() {
        List<AliyunCredential> credentials = getAllCredentials();
        return credentials.isEmpty() ? null : credentials.get(0);
    }
    
    /**
     * 获取指定区域的默认凭证
     * 
     * @param region 区域
     * @return 默认凭证
     */
    public AliyunCredential getDefaultCredentialByRegion(String region) {
        List<AliyunCredential> credentials = getCredentialsByRegion(region);
        return credentials.isEmpty() ? null : credentials.get(0);
    }
    
    /**
     * 验证凭证是否有效
     * 
     * @param credential 凭证信息
     * @return 是否有效
     */
    public boolean validateCredential(AliyunCredential credential) {
        if (credential == null) {
            return false;
        }
        
        return StringUtils.hasText(credential.getAccessKeyId()) 
                && StringUtils.hasText(credential.getAccessKeySecret())
                && StringUtils.hasText(credential.getRegion());
    }
    
    /**
     * 刷新凭证缓存（如果有的话）
     */
    public void refreshCredentials() {
        log.info("刷新阿里云凭证缓存");
        // 这里可以添加缓存刷新逻辑
    }
    
    /**
     * 转换SysSecretKey为AliyunCredential
     * 
     * @param secretKey 系统密钥
     * @return 阿里云凭证
     */
    private AliyunCredential convertToCredential(SysSecretKey secretKey) {
        if (secretKey == null) {
            return AliyunCredential.builder().valid(false).build();
        }
        
        boolean isValid = StringUtils.hasText(secretKey.getAccessKey()) 
                && StringUtils.hasText(secretKey.getSecretKey())
                && StringUtils.hasText(secretKey.getRegion());
        
        return AliyunCredential.builder()
                .accessKeyId(secretKey.getAccessKey())
                .accessKeySecret(secretKey.getSecretKey())
                .region(secretKey.getRegion())
                .secretKeyId(secretKey.getId())
                .keyName(secretKey.getKeyName())
                .valid(isValid)
                .build();
    }
}