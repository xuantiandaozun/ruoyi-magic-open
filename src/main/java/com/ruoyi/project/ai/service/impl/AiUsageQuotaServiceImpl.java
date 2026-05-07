package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiUsageQuota;
import com.ruoyi.project.ai.mapper.AiUsageQuotaMapper;
import com.ruoyi.project.ai.service.IAiUsageQuotaService;

@Service
@UseDataSource("MASTER")
public class AiUsageQuotaServiceImpl extends ServiceImpl<AiUsageQuotaMapper, AiUsageQuota>
        implements IAiUsageQuotaService {
}
