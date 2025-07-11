package com.ruoyi.project.system.service.impl;

import java.util.Arrays;
import org.springframework.stereotype.Service;
import com.ruoyi.project.system.mapper.StorageConfigMapper;
import com.ruoyi.project.system.domain.StorageConfig;
import com.ruoyi.project.system.service.IStorageConfigService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mybatisflex.annotation.UseDataSource;

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
    // 可以添加自定义的业务方法
}
