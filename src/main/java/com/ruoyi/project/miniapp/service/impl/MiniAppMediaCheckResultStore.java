package com.ruoyi.project.miniapp.service.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.miniapp.domain.vo.MiniAppMediaCheckResult;

import cn.hutool.core.util.StrUtil;

/**
 * 缓存微信图片异步检测结果，供业务轮询与回调写入
 */
@Component
public class MiniAppMediaCheckResultStore {

    private static final String CACHE_KEY_PREFIX = "miniapp:media_check:trace:";
    private static final int CACHE_TTL_MINUTES = 30;

    private final RedisCache redisCache;

    public MiniAppMediaCheckResultStore(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public void save(MiniAppMediaCheckResult result) {
        if (result == null || StrUtil.isBlank(result.getTraceId())) {
            return;
        }
        redisCache.setCacheObject(buildKey(result.getTraceId()), result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public MiniAppMediaCheckResult get(String traceId) {
        if (StrUtil.isBlank(traceId)) {
            return null;
        }
        return redisCache.getCacheObject(buildKey(traceId));
    }

    private String buildKey(String traceId) {
        return CACHE_KEY_PREFIX + traceId;
    }
}
