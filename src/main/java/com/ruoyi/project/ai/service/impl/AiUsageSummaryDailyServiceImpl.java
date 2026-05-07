package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiUsageSummaryDaily;
import com.ruoyi.project.ai.mapper.AiUsageSummaryDailyMapper;
import com.ruoyi.project.ai.service.IAiUsageSummaryDailyService;

@Service
@UseDataSource("MASTER")
public class AiUsageSummaryDailyServiceImpl extends ServiceImpl<AiUsageSummaryDailyMapper, AiUsageSummaryDaily>
        implements IAiUsageSummaryDailyService {
}
