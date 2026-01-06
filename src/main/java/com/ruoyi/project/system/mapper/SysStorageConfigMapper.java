package com.ruoyi.project.system.mapper;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import com.ruoyi.project.system.domain.SysStorageConfig;

/**
 * 存储配置Mapper接口
 * 
 * @author ruoyi
 * @date 2025-07-11 11:32:00
 */
@UseDataSource("MASTER")
public interface SysStorageConfigMapper extends BaseMapper<SysStorageConfig> {
    // 遵循MyBatis-Flex规范，保持Mapper接口简洁，复杂查询在Service层实现
}
