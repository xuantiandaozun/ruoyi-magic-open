package com.ruoyi.project.system.service.impl;

import java.util.Arrays;
import org.springframework.stereotype.Service;
import com.ruoyi.project.system.mapper.FileUploadRecordMapper;
import com.ruoyi.project.system.domain.FileUploadRecord;
import com.ruoyi.project.system.service.IFileUploadRecordService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mybatisflex.annotation.UseDataSource;

/**
 * 文件上传记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-07-11 12:01:15
 */
@Service
@UseDataSource("MASTER")
public class FileUploadRecordServiceImpl extends ServiceImpl<FileUploadRecordMapper, FileUploadRecord> implements IFileUploadRecordService
{
    // 可以添加自定义的业务方法
}
