package com.ruoyi.framework.redis;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import com.ruoyi.project.system.domain.SysConfig;
import com.ruoyi.project.system.domain.SysDictType;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 优化的Redis缓存工具类
 * 支持批量操作和管道操作，提升缓存写入性能
 * 
 * @author ruoyi-magic
 */
@Component
public class OptimizedRedisCache {
    
    private static final Logger log = LoggerFactory.getLogger(OptimizedRedisCache.class);
    
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    
    @Autowired
    private RedisCache redisCache;
    
    /**
     * 批量加载系统配置到缓存
     * 使用Redis Pipeline提升性能
     * 
     * @param configs 配置列表
     */
    public void batchLoadConfigCache(List<SysConfig> configs) {
        if (CollUtil.isEmpty(configs)) {
            return;
        }
        
        log.info("开始批量加载 {} 个系统配置到缓存", configs.size());
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用Pipeline批量操作
            redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    for (SysConfig config : configs) {
                        if (StrUtil.isNotBlank(config.getConfigKey()) && StrUtil.isNotBlank(config.getConfigValue())) {
                            String cacheKey = getCacheKey(config.getConfigKey());
                            byte[] keyBytes = cacheKey.getBytes();
                            byte[] valueBytes = config.getConfigValue().getBytes();
                            connection.set(keyBytes, valueBytes);
                        }
                    }
                    return null;
                }
            });
            
            long endTime = System.currentTimeMillis();
            log.info("批量加载系统配置完成，耗时: {}ms", endTime - startTime);
            
        } catch (Exception e) {
            log.error("批量加载系统配置失败", e);
            // 降级到单个操作
            fallbackLoadConfigCache(configs);
        }
    }
    
    /**
     * 批量加载字典数据到缓存
     * 
     * @param dictTypes 字典类型列表
     */
    public void batchLoadDictCache(List<SysDictType> dictTypes) {
        if (CollUtil.isEmpty(dictTypes)) {
            return;
        }
        
        log.info("开始批量加载 {} 个字典类型到缓存", dictTypes.size());
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用Pipeline批量操作
            redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    for (SysDictType dictType : dictTypes) {
                        if (StrUtil.isNotBlank(dictType.getDictType()) && CollUtil.isNotEmpty(dictType.getDictDataList())) {
                            String cacheKey = getDictCacheKey(dictType.getDictType());
                            // 序列化字典数据列表
                            try {
                                byte[] keyBytes = cacheKey.getBytes();
                                @SuppressWarnings("unchecked")
                                RedisSerializer<Object> serializer = (RedisSerializer<Object>) redisTemplate.getDefaultSerializer();
                                byte[] valueBytes = serializer.serialize(dictType.getDictDataList());
                                if (valueBytes != null) {
                                    connection.set(keyBytes, valueBytes);
                                }
                            } catch (Exception e) {
                                log.warn("序列化字典数据失败: {}", dictType.getDictType(), e);
                            }
                        }
                    }
                    return null;
                }
            });
            
            long endTime = System.currentTimeMillis();
            log.info("批量加载字典数据完成，耗时: {}ms", endTime - startTime);
            
        } catch (Exception e) {
            log.error("批量加载字典数据失败", e);
            // 降级到单个操作
            fallbackLoadDictCache(dictTypes);
        }
    }
    
    /**
     * 批量设置缓存（通用方法）
     * 
     * @param cacheData 缓存数据映射
     * @param expireTime 过期时间（秒），null表示不过期
     */
    public void batchSetCache(Map<String, Object> cacheData, Long expireTime) {
        if (CollUtil.isEmpty(cacheData)) {
            return;
        }
        
        log.debug("批量设置 {} 个缓存项", cacheData.size());
        
        try {
            redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    for (Map.Entry<String, Object> entry : cacheData.entrySet()) {
                        try {
                            byte[] keyBytes = entry.getKey().getBytes();
                            @SuppressWarnings("unchecked")
                            RedisSerializer<Object> serializer = (RedisSerializer<Object>) redisTemplate.getDefaultSerializer();
                            byte[] valueBytes = serializer.serialize(entry.getValue());
                            if (valueBytes != null) {
                                if (expireTime != null && expireTime > 0) {
                                    connection.setEx(keyBytes, expireTime, valueBytes);
                                } else {
                                    connection.set(keyBytes, valueBytes);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("设置缓存项失败: {}", entry.getKey(), e);
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("批量设置缓存失败", e);
            // 降级到单个操作
            fallbackSetCache(cacheData, expireTime);
        }
    }
    
    /**
     * 降级处理：单个加载配置缓存
     */
    private void fallbackLoadConfigCache(List<SysConfig> configs) {
        log.info("使用降级方案单个加载配置缓存");
        for (SysConfig config : configs) {
            if (StrUtil.isNotBlank(config.getConfigKey()) && StrUtil.isNotBlank(config.getConfigValue())) {
                redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
            }
        }
    }
    
    /**
     * 降级处理：单个加载字典缓存
     */
    private void fallbackLoadDictCache(List<SysDictType> dictTypes) {
        log.info("使用降级方案单个加载字典缓存");
        for (SysDictType dictType : dictTypes) {
            if (StrUtil.isNotBlank(dictType.getDictType()) && CollUtil.isNotEmpty(dictType.getDictDataList())) {
                redisCache.setCacheObject(getDictCacheKey(dictType.getDictType()), dictType.getDictDataList());
            }
        }
    }
    
    /**
     * 降级处理：单个设置缓存
     */
    private void fallbackSetCache(Map<String, Object> cacheData, Long expireTime) {
        log.info("使用降级方案单个设置缓存");
        for (Map.Entry<String, Object> entry : cacheData.entrySet()) {
            if (expireTime != null && expireTime > 0) {
                redisCache.setCacheObject(entry.getKey(), entry.getValue(), expireTime.intValue(), TimeUnit.SECONDS);
            } else {
                redisCache.setCacheObject(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * 获取配置缓存键
     */
    private String getCacheKey(String configKey) {
        return "sys_config:" + configKey;
    }
    
    /**
     * 获取字典缓存键
     */
    private String getDictCacheKey(String dictType) {
        return "sys_dict:" + dictType;
    }
}