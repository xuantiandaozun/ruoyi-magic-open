package com.ruoyi.project.ai.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.mybatisflex.core.BaseMapper;
import com.ruoyi.project.ai.domain.AiUsageSummaryDaily;

public interface AiUsageSummaryDailyMapper extends BaseMapper<AiUsageSummaryDaily> {

    /**
     * 按 (user_id, product_type, summary_date) 做 upsert。
     * <p>
     * 需要表上存在 unique key:
     *   UNIQUE KEY `uk_user_product_date` (`user_id`, `product_type`, `summary_date`)
     */
    @Update("INSERT INTO ai_usage_summary_daily " +
            "(summary_date, user_id, product_type, provider, model_name, " +
            " request_count, success_count, failed_count, " +
            " input_tokens, output_tokens, cached_input_tokens, total_tokens, " +
            " image_count, estimated_cost, currency, create_time, update_time) " +
            "VALUES (CURDATE(), #{userId}, #{productType}, #{provider}, #{modelName}, " +
            " #{requestDelta}, #{successDelta}, #{failedDelta}, " +
            " #{inputTokensDelta}, #{outputTokensDelta}, 0, #{totalTokensDelta}, " +
            " 0, 0, 'USD', NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            " request_count  = request_count  + VALUES(request_count), " +
            " success_count  = success_count  + VALUES(success_count), " +
            " failed_count   = failed_count   + VALUES(failed_count), " +
            " input_tokens   = input_tokens   + VALUES(input_tokens), " +
            " output_tokens  = output_tokens  + VALUES(output_tokens), " +
            " total_tokens   = total_tokens   + VALUES(total_tokens), " +
            " update_time    = NOW()")
    void upsertDailyRecord(@Param("userId") Long userId,
                           @Param("productType") String productType,
                           @Param("provider") String provider,
                           @Param("modelName") String modelName,
                           @Param("requestDelta") int requestDelta,
                           @Param("successDelta") int successDelta,
                           @Param("failedDelta") int failedDelta,
                           @Param("inputTokensDelta") long inputTokensDelta,
                           @Param("outputTokensDelta") long outputTokensDelta,
                           @Param("totalTokensDelta") long totalTokensDelta);
}
