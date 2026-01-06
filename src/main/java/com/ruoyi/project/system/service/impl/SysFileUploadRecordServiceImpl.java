package com.ruoyi.project.system.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.system.domain.SysFileUploadRecord;
import com.ruoyi.project.system.mapper.SysFileUploadRecordMapper;
import com.ruoyi.project.system.service.ISysFileUploadRecordService;

/**
 * 文件上传记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-07-11 12:01:15
 */
@Service
@UseDataSource("MASTER")
public class SysFileUploadRecordServiceImpl extends ServiceImpl<SysFileUploadRecordMapper, SysFileUploadRecord>
        implements ISysFileUploadRecordService {
    // 可以添加自定义的业务方法
}
