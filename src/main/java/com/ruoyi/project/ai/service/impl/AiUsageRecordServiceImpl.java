package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiUsageRecord;
import com.ruoyi.project.ai.mapper.AiUsageRecordMapper;
import com.ruoyi.project.ai.service.IAiUsageRecordService;

@Service
@UseDataSource("MASTER")
public class AiUsageRecordServiceImpl extends ServiceImpl<AiUsageRecordMapper, AiUsageRecord>
        implements IAiUsageRecordService {
}
