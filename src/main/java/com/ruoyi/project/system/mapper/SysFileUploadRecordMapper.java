package com.ruoyi.project.system.mapper;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import com.ruoyi.project.system.domain.SysFileUploadRecord;

/**
 * 文件上传记录Mapper接口
 * 
 * @author ruoyi
 * @date 2025-07-11 12:01:15
 */
@UseDataSource("MASTER")
public interface SysFileUploadRecordMapper extends BaseMapper<SysFileUploadRecord> {
    // 遵循MyBatis-Flex规范，保持Mapper接口简洁，复杂查询在Service层实现
}
