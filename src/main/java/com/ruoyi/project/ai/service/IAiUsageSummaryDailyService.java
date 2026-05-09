package com.ruoyi.project.ai.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiUsageSummaryDaily;

public interface IAiUsageSummaryDailyService extends IService<AiUsageSummaryDaily> {

    /**
     * 实时 upsert 每日用量汇总（按 user_id + product_type + summary_date 聚合）
     * <p>
     * 对应 MySQL：INSERT ... ON DUPLICATE KEY UPDATE
     * 需要表上存在 unique key: (user_id, product_type, summary_date)
     */
    void upsertDailyRecord(Long userId, String productType, String provider, String modelName,
                           int requestDelta, int successDelta, int failedDelta,
                           long inputTokensDelta, long outputTokensDelta, long totalTokensDelta);
}
