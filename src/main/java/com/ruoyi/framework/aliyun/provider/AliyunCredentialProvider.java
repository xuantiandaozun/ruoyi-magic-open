package com.ruoyi.framework.aliyun.provider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.project.secretkey.domain.SecretKeyInfo;
import com.ruoyi.project.secretkey.service.ISecretKeyInfoService;

import lombok.extern.slf4j.Slf4j;

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
    private ISecretKeyInfoService secretKeyInfoService;

    /**
     * 获取所有有效的阿里云凭证
     * 
     * @return 凭证列表
     */
    public List<AliyunCredential> getAllCredentials() {
        return getAllCredentials(null);
    }

    /**
     * 获取所有有效的阿里云凭证
     * 
     * @param regions 区域列表，逗号分隔，为空时获取所有区域的凭证
     * @return 凭证列表
     */
    public List<AliyunCredential> getAllCredentials(String regions) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("provider_brand", "aliyun")
                .eq("status", "0")
                .eq("del_flag", "0");

        List<SecretKeyInfo> secretKeys = secretKeyInfoService.list(queryWrapper);

        return secretKeys.stream()
                .flatMap(secretKey -> convertToCredentials(secretKey).stream())
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
        SecretKeyInfo secretKey = secretKeyInfoService.getById(secretKeyId);
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

        List<SecretKeyInfo> secretKeys = secretKeyInfoService.list(queryWrapper);

        return secretKeys.stream()
                .flatMap(secretKey -> convertToCredentials(secretKey).stream())
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

        List<SecretKeyInfo> secretKeys = secretKeyInfoService.list(queryWrapper);

        return secretKeys.stream()
                .flatMap(secretKey -> convertToCredentials(secretKey).stream())
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

        // region可以为空，不作为有效性验证的必要条件
        return StringUtils.hasText(credential.getAccessKeyId())
                && StringUtils.hasText(credential.getAccessKeySecret());
    }

    /**
     * 刷新凭证缓存（如果有的话）
     */
    public void refreshCredentials() {
        log.info("刷新阿里云凭证缓存");
        // 这里可以添加缓存刷新逻辑
    }

    /**
     * 转换SecretKeyInfo为AliyunCredential
     * 
     * @param secretKey 系统密钥
     * @return 阿里云凭证
     */
    private AliyunCredential convertToCredential(SecretKeyInfo secretKey) {
        if (secretKey == null) {
            return AliyunCredential.builder().valid(false).build();
        }

        // 验证必要字段：accessKey和secretKey不能为空
        boolean isValid = StringUtils.hasText(secretKey.getAccessKey())
                && StringUtils.hasText(secretKey.getSecretKey())
                && StringUtils.hasText(secretKey.getRegion()); // region也不能为空

        return AliyunCredential.builder()
                .accessKeyId(secretKey.getAccessKey())
                .accessKeySecret(secretKey.getSecretKey())
                .region(secretKey.getRegion())
                .secretKeyId(secretKey.getId())
                .keyName(secretKey.getKeyName())
                .valid(isValid)
                .build();
    }

    /**
     * 转换SecretKeyInfo为多个AliyunCredential（支持逗号分隔的region）
     * 
     * @param secretKey 系统密钥
     * @return 阿里云凭证列表
     */
    private List<AliyunCredential> convertToCredentials(SecretKeyInfo secretKey) {
        if (secretKey == null) {
            return List.of(AliyunCredential.builder().valid(false).build());
        }

        // 验证必要字段：accessKey和secretKey不能为空
        if (!StringUtils.hasText(secretKey.getAccessKey()) || !StringUtils.hasText(secretKey.getSecretKey())) {
            return List.of(AliyunCredential.builder().valid(false).build());
        }

        // 处理region字段
        String regionStr = secretKey.getRegion();
        if (!StringUtils.hasText(regionStr)) {
            // region为空，创建一个无效的凭证
            return List.of(AliyunCredential.builder()
                    .accessKeyId(secretKey.getAccessKey())
                    .accessKeySecret(secretKey.getSecretKey())
                    .region(null)
                    .secretKeyId(secretKey.getId())
                    .keyName(secretKey.getKeyName())
                    .valid(false)
                    .build());
        }

        // 分割region字符串
        String[] regions = regionStr.split(",");
        return Arrays.stream(regions)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(region -> AliyunCredential.builder()
                        .accessKeyId(secretKey.getAccessKey())
                        .accessKeySecret(secretKey.getSecretKey())
                        .region(region)
                        .secretKeyId(secretKey.getId())
                        .keyName(secretKey.getKeyName())
                        .valid(true)
                        .build())
                .collect(Collectors.toList());
    }
}