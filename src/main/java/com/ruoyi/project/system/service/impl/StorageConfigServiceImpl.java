package com.ruoyi.project.system.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.system.domain.StorageConfig;
import com.ruoyi.project.system.mapper.StorageConfigMapper;
import com.ruoyi.project.system.service.IStorageConfigService;

import cn.hutool.core.util.StrUtil;

/**
 * 存储配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-07-11 11:32:00
 */
@Service
@UseDataSource("MASTER")
public class StorageConfigServiceImpl extends ServiceImpl<StorageConfigMapper, StorageConfig> implements IStorageConfigService
{
    
    /**
     * 新增存储配置（带默认配置唯一性校验）
     */
    @Override
    @Transactional
    public boolean saveStorageConfig(StorageConfig storageConfig) {
        // 如果设置为默认配置，需要先将其他配置的默认状态改为N
        if ("Y".equals(storageConfig.getIsDefault())) {
            resetOtherDefaultConfigs();
        }
        
        // 如果没有设置默认状态，默认为N
        if (StrUtil.isBlank(storageConfig.getIsDefault())) {
            storageConfig.setIsDefault("N");
        }
        
        return save(storageConfig);
    }
    
    /**
     * 修改存储配置（带默认配置唯一性校验）
     */
    @Override
    @Transactional
    public boolean updateStorageConfig(StorageConfig storageConfig) {
        // 如果设置为默认配置，需要先将其他配置的默认状态改为N
        if ("Y".equals(storageConfig.getIsDefault())) {
            resetOtherDefaultConfigs(storageConfig.getConfigId());
        }
        
        return updateById(storageConfig);
    }
    
    /**
     * 重置其他配置的默认状态为N
     */
    private void resetOtherDefaultConfigs() {
        resetOtherDefaultConfigs(null);
    }
    
    /**
     * 重置其他配置的默认状态为N（排除指定ID）
     * 
     * @param excludeConfigId 排除的配置ID
     */
    private void resetOtherDefaultConfigs(String excludeConfigId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("is_default", "Y")
            .eq("del_flag", "0");
        
        // 如果有排除的ID，添加条件
        if (StrUtil.isNotBlank(excludeConfigId)) {
            queryWrapper.ne("config_id", excludeConfigId);
        }
        
        // 查询需要重置的配置
        list(queryWrapper).forEach(config -> {
            config.setIsDefault("N");
            updateById(config);
        });
    }
}
