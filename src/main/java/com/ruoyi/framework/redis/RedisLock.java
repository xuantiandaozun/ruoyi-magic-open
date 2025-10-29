package com.ruoyi.framework.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 用于防止AI工作流调度任务的重复执行
 * 
 * @author ruoyi
 */
@Component
public class RedisLock {
    
    private static final Logger log = LoggerFactory.getLogger(RedisLock.class);
    
    @Autowired
    @Qualifier("redisLockTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 锁的前缀
     */
    private static final String LOCK_PREFIX = "workflow:lock:";
    
    /**
     * 默认锁过期时间（秒）
     */
    private static final int DEFAULT_EXPIRE_TIME = 300; // 5分钟
    
    /**
     * 释放锁的Lua脚本
     */
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";
    
    /**
     * 获取分布式锁
     * 
     * @param lockKey 锁的键
     * @param lockValue 锁的值（用于释放锁时验证）
     * @param expireTime 锁过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String lockValue, int expireTime) {
        try {
            String key = LOCK_PREFIX + lockKey;
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, lockValue, expireTime, TimeUnit.SECONDS);
            boolean success = Boolean.TRUE.equals(result);
            
            if (success) {
                log.debug("成功获取分布式锁: {}", key);
            } else {
                log.debug("获取分布式锁失败，锁已被占用: {}", key);
            }
            
            return success;
        } catch (Exception e) {
            log.error("获取分布式锁异常: {}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 获取分布式锁（使用默认过期时间）
     * 
     * @param lockKey 锁的键
     * @param lockValue 锁的值
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String lockValue) {
        return tryLock(lockKey, lockValue, DEFAULT_EXPIRE_TIME);
    }
    
    /**
     * 释放分布式锁
     * 
     * @param lockKey 锁的键
     * @param lockValue 锁的值（必须与获取锁时的值一致）
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey, String lockValue) {
        try {
            String key = LOCK_PREFIX + lockKey;
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);
            
            Long result = redisTemplate.execute(script, Collections.singletonList(key), lockValue);
            boolean success = Long.valueOf(1).equals(result);
            
            if (success) {
                log.debug("成功释放分布式锁: {}", key);
            } else {
                log.warn("释放分布式锁失败，锁可能已过期或值不匹配: {}", key);
            }
            
            return success;
        } catch (Exception e) {
            log.error("释放分布式锁异常: {}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 检查锁是否存在
     * 
     * @param lockKey 锁的键
     * @return 锁是否存在
     */
    public boolean isLocked(String lockKey) {
        try {
            String key = LOCK_PREFIX + lockKey;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查分布式锁状态异常: {}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 强制释放锁（不验证锁的值）
     * 注意：此方法应谨慎使用，仅在确认锁已失效时使用
     * 
     * @param lockKey 锁的键
     * @return 是否释放成功
     */
    public boolean forceUnlock(String lockKey) {
        try {
            String key = LOCK_PREFIX + lockKey;
            Boolean result = redisTemplate.delete(key);
            boolean success = Boolean.TRUE.equals(result);
            
            if (success) {
                log.warn("强制释放分布式锁: {}", key);
            }
            
            return success;
        } catch (Exception e) {
            log.error("强制释放分布式锁异常: {}", lockKey, e);
            return false;
        }
    }
}