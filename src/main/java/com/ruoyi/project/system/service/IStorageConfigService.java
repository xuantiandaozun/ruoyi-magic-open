package com.ruoyi.project.system.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.system.domain.StorageConfig;

/**
 * 存储配置Service接口
 * 
 * @author ruoyi
 * @date 2025-07-11 11:32:00
 */
public interface IStorageConfigService extends IService<StorageConfig>
{
    /**
     * 新增存储配置（带默认配置唯一性校验）
     * 
     * @param storageConfig 存储配置
     * @return 结果
     */
    boolean saveStorageConfig(StorageConfig storageConfig);
    
    /**
     * 修改存储配置（带默认配置唯一性校验）
     * 
     * @param storageConfig 存储配置
     * @return 结果
     */
    boolean updateStorageConfig(StorageConfig storageConfig);
}
