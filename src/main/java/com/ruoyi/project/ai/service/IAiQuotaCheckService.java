package com.ruoyi.project.ai.service;

/**
 * AI 调用配额检查服务
 */
public interface IAiQuotaCheckService {

    /**
     * 检查用户是否还有可用配额，超限时抛出 ServiceException
     *
     * @param userId      用户 ID
     * @param userTier    用户等级（free/pro 等）
     * @param productType 产品类型（plugin 等）
     */
    void checkAndConsume(Long userId, String userTier, String productType);

    /**
     * 获取用户今日配额使用情况（不消费，仅查询）
     *
     * @param userId      用户 ID
     * @param userTier    用户等级
     * @param productType 产品类型
     * @return 配额使用信息
     */
    QuotaUsageInfo getUsageInfo(Long userId, String userTier, String productType);

    /** 配额使用情况 VO */
    class QuotaUsageInfo {
        private int todayUsedRequests;
        private int requestLimit;
        private long todayUsedTokens;
        private long tokenLimit;

        public QuotaUsageInfo(int todayUsedRequests, int requestLimit,
                long todayUsedTokens, long tokenLimit) {
            this.todayUsedRequests = todayUsedRequests;
            this.requestLimit = requestLimit;
            this.todayUsedTokens = todayUsedTokens;
            this.tokenLimit = tokenLimit;
        }

        public int getTodayUsedRequests() { return todayUsedRequests; }
        public int getRequestLimit() { return requestLimit; }
        public long getTodayUsedTokens() { return todayUsedTokens; }
        public long getTokenLimit() { return tokenLimit; }
        public int getRemainingRequests() {
            if (requestLimit < 0) return Integer.MAX_VALUE;
            return Math.max(0, requestLimit - todayUsedRequests);
        }
    }
}
