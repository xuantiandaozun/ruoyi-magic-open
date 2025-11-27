package com.ruoyi.project.feishu.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.feishu.domain.FeishuMessageRecord;
import com.ruoyi.project.feishu.mapper.FeishuMessageRecordMapper;
import com.ruoyi.project.feishu.service.IFeishuMessageRecordService;

/**
 * 飞书消息发送记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-11-27
 */
@Service
public class FeishuMessageRecordServiceImpl extends ServiceImpl<FeishuMessageRecordMapper, FeishuMessageRecord> implements IFeishuMessageRecordService {
}
