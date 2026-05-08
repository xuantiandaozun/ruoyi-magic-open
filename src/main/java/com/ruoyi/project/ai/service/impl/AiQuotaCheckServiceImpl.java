package com.ruoyi.project.ai.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.domain.AiUsageQuota;
import com.ruoyi.project.ai.domain.AiUsageSummaryDaily;
import com.ruoyi.project.ai.service.IAiQuotaCheckService;
import com.ruoyi.project.ai.service.IAiUsageQuotaService;
import com.ruoyi.project.ai.service.IAiUsageSummaryDailyService;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 调用配额检查服务实现
 * <p>
 * 配额优先级：
 * 1. 先查 ai_usage_quota 中 user_id = userId 的个人配额（精准覆盖）
 * 2. 若无个人配额，查 user_tier + product_type 的等级配额
 * 3. 与 ai_usage_summary_daily 中今日用量对比
 */
@Slf4j
@Service
public class AiQuotaCheckServiceImpl implements IAiQuotaCheckService {

    @Autowired
    private IAiUsageQuotaService quotaService;

    @Autowired
    private IAiUsageSummaryDailyService summaryDailyService;

    @Override
    public void checkAndConsume(Long userId, String userTier, String productType) {
        AiUsageQuota quota = resolveQuota(userId, userTier, productType);
        if (quota == null) {
            // 未配置配额规则 → 放行（宽松策略，避免误杀）
            log.debug("未找到配额配置，放行 userId={} tier={} type={}", userId, userTier, productType);
            return;
        }
        if (!"Y".equals(quota.getEnabled())) {
            log.debug("配额规则已禁用，放行 userId={}", userId);
            return;
        }

        QuotaUsageInfo usage = getTodayUsage(userId, productType);

        // 检查请求次数
        if (quota.getRequestLimit() != null && quota.getRequestLimit() > 0) {
            if (usage.getTodayUsedRequests() >= quota.getRequestLimit()) {
                throw new ServiceException(String.format(
                        "今日 AI 调用次数已达上限（%d 次），请明天再来", quota.getRequestLimit()));
            }
        }

        // 检查 Token 上限
        if (quota.getTokenLimit() != null && quota.getTokenLimit() > 0) {
            if (usage.getTodayUsedTokens() >= quota.getTokenLimit()) {
                throw new ServiceException(String.format(
                        "今日 Token 用量已达上限（%d），请明天再来", quota.getTokenLimit()));
            }
        }
    }

    @Override
    public QuotaUsageInfo getUsageInfo(Long userId, String userTier, String productType) {
        AiUsageQuota quota = resolveQuota(userId, userTier, productType);
        int requestLimit = (quota != null && quota.getRequestLimit() != null) ? quota.getRequestLimit() : -1;
        long tokenLimit = (quota != null && quota.getTokenLimit() != null) ? quota.getTokenLimit() : -1L;

        QuotaUsageInfo usage = getTodayUsage(userId, productType);
        return new QuotaUsageInfo(
                usage.getTodayUsedRequests(),
                requestLimit,
                usage.getTodayUsedTokens(),
                tokenLimit);
    }

    // -----------------------------------------------------------------------
    // 私有方法
    // -----------------------------------------------------------------------

    /**
     * 解析配额规则：个人配额 > 等级配额
     */
    private AiUsageQuota resolveQuota(Long userId, String userTier, String productType) {
        // 1. 个人专属配额
        if (userId != null) {
            QueryWrapper qw = QueryWrapper.create()
                    .from("ai_usage_quota")
                    .where("user_id = " + userId)
                    .and("product_type = '" + productType + "'")
                    .and("enabled = 'Y'")
                    .and("del_flag = '0'")
                    .limit(1);
            AiUsageQuota personal = quotaService.getOne(qw);
            if (personal != null) {
                return personal;
            }
        }

        // 2. 等级配额
        if (StrUtil.isNotBlank(userTier)) {
            QueryWrapper qw = QueryWrapper.create()
                    .from("ai_usage_quota")
                    .where("user_id IS NULL")
                    .and("user_tier = '" + userTier + "'")
                    .and("product_type = '" + productType + "'")
                    .and("enabled = 'Y'")
                    .and("del_flag = '0'")
                    .limit(1);
            return quotaService.getOne(qw);
        }
        return null;
    }

    /**
     * 获取用户今日用量（汇总表）
     */
    private QuotaUsageInfo getTodayUsage(Long userId, String productType) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        QueryWrapper qw = QueryWrapper.create()
                .from("ai_usage_summary_daily")
                .where("user_id = " + userId)
                .and("product_type = '" + productType + "'")
                .and("DATE(summary_date) = '" + today + "'");

        java.util.List<AiUsageSummaryDaily> list = summaryDailyService.list(qw);

        int totalRequests = list.stream()
                .mapToInt(r -> r.getRequestCount() == null ? 0 : r.getRequestCount())
                .sum();
        long totalTokens = list.stream()
                .mapToLong(r -> r.getTotalTokens() == null ? 0L : r.getTotalTokens())
                .sum();

        return new QuotaUsageInfo(totalRequests, -1, totalTokens, -1L);
    }
}
