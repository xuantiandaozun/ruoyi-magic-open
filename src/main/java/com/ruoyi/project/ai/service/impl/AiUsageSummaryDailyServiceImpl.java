package com.ruoyi.project.ai.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AiUsageSummaryDailyMapper summaryDailyMapper;

    @Override
    public void upsertDailyRecord(Long userId, String productType, String provider, String modelName,
                                  int requestDelta, int successDelta, int failedDelta,
                                  long inputTokensDelta, long outputTokensDelta, long totalTokensDelta) {
        summaryDailyMapper.upsertDailyRecord(userId, productType, provider, modelName,
                requestDelta, successDelta, failedDelta,
                inputTokensDelta, outputTokensDelta, totalTokensDelta);
    }
}
