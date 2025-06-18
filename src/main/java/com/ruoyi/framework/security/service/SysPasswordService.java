package com.ruoyi.framework.security.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.exception.user.UserPasswordRetryLimitExceedException;
import com.ruoyi.framework.redis.RedisCache;

/**
 * 登录密码方法
 */
@Component
public class SysPasswordService {
    @Autowired
    private RedisCache redisCache;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 登录账户密码错误次数缓存键名
     * 
     * @param username 用户名
     * @return 缓存键key
     */
    private String getCacheKey(String username) {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    public void validate(String username, String password, String correctPassword) {
        Integer retryCount = redisCache.getCacheObject(getCacheKey(username));

        if (retryCount == null) {
            retryCount = 0;
        }

        if (retryCount >= CacheConstants.PASSWORD_MAX_RETRY_COUNT) {
            throw new UserPasswordRetryLimitExceedException(CacheConstants.PASSWORD_MAX_RETRY_COUNT);
        }

        if (!matches(password, correctPassword)) {
            retryCount = retryCount + 1;
            redisCache.setCacheObject(getCacheKey(username), retryCount, CacheConstants.PASSWORD_LOCK_TIME, TimeUnit.MINUTES);
            throw new UserPasswordRetryLimitExceedException(retryCount);
        }
        else {
            clearLoginRecordCache(username);
        }
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void clearLoginRecordCache(String loginName) {
        if (redisCache.hasKey(getCacheKey(loginName))) {
            redisCache.deleteObject(getCacheKey(loginName));
        }
    }
} 