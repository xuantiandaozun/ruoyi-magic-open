package com.ruoyi.common.utils;

import java.util.List;

import org.springframework.stereotype.Component;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.project.system.config.FeishuConfig;
import com.ruoyi.project.system.domain.SysSecretKey;
import com.ruoyi.project.system.service.ISysSecretKeyService;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书配置工具类
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Slf4j
@Component
public class FeishuConfigUtils {
    
    /**
     * 获取飞书配置
     * 
     * @param keyName 指定的密钥名称，如果为空则使用第一个找到的密钥
     * @return 飞书配置对象
     */
    public static FeishuConfig getFeishuConfig(String keyName) {
        try {
            ISysSecretKeyService sysSecretKeyService = SpringUtils.getBean(ISysSecretKeyService.class);
            
            // 查询飞书相关的密钥配置
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq("provider_name", "飞书")
                    .eq("status", "0")
                    .eq("del_flag", "0")
                    .orderBy("create_time", false);
            
            List<SysSecretKey> secretKeys = sysSecretKeyService.list(queryWrapper);
            
            SysSecretKey selectedKey = null;
            
            // 如果指定了keyName，优先查找指定的密钥
            if (StrUtil.isNotBlank(keyName)) {
                selectedKey = secretKeys.stream()
                        .filter(key -> keyName.equals(key.getKeyName()))
                        .findFirst()
                        .orElse(null);
                
                if (selectedKey == null) {
                    log.warn("未找到指定名称的飞书密钥: {}, 将使用第一个可用密钥", keyName);
                }
            }
            
            // 如果没有指定keyName或者没有找到指定的密钥，使用第一个
            if (selectedKey == null && !secretKeys.isEmpty()) {
                selectedKey = secretKeys.get(0);
            }
            
            if (selectedKey != null && StrUtil.isNotEmpty(selectedKey.getAccessKey()) 
                && StrUtil.isNotEmpty(selectedKey.getSecretKey())) {
                
                FeishuConfig feishuConfig = new FeishuConfig(selectedKey.getAccessKey(), selectedKey.getSecretKey());
                
                log.info("飞书配置获取成功，密钥名称: {}, 应用ID: {}", 
                        selectedKey.getKeyName(), selectedKey.getAccessKey());
                        
                return feishuConfig;
            } else {
                log.warn("未找到有效的飞书配置，请在sys_secret_key表中配置飞书应用密钥");
                return null;
            }
        } catch (Exception e) {
            log.error("获取飞书配置失败", e);
            return null;
        }
    }
    
    /**
     * 获取飞书配置（使用默认密钥名称）
     * 
     * @return 飞书配置对象
     */
    public static FeishuConfig getFeishuConfig() {
        return getFeishuConfig(null);
    }
}