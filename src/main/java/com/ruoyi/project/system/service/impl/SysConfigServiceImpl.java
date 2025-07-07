package com.ruoyi.project.system.service.impl;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.system.domain.SysConfig;
import com.ruoyi.project.system.mapper.SysConfigMapper;
import com.ruoyi.project.system.service.ISysConfigService;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;

/**
 * 参数配置 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService
{
    @Autowired
    private RedisCache redisCache;

    /**
     * 项目启动时，初始化参数到缓存
     */
    @PostConstruct
    public void init()
    {
        initializeConfigCacheAsync();
    }

    /**
     * 异步初始化参数缓存
     */
    @Async("threadPoolTaskExecutor")
    @Order(1)
    public void initializeConfigCacheAsync()
    {
        loadingConfigCache();
    }

    /**
     * 根据键名查询参数配置信息
     * 
     * @param configKey 参数key
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey)
    {
        String configValue = Convert.toStr(redisCache.getCacheObject(getCacheKey(configKey)));
        if (StrUtil.isNotEmpty(configValue))
        {
            return configValue;
        }
        SysConfig config = this.getOne(QueryWrapper.create()
                .from("sys_config")
                .where(new QueryColumn("config_key").eq(configKey)));
        if (ObjectUtil.isNotNull(config))
        {
            redisCache.setCacheObject(getCacheKey(configKey), config.getConfigValue());
            return config.getConfigValue();
        }
        return "";
    }

    /**
     * 获取验证码开关
     * 
     * @return true开启，false关闭
     */
    @Override
    public boolean selectCaptchaEnabled()
    {
        String captchaEnabled = selectConfigByKey("sys.account.captchaEnabled");
        if (StrUtil.isEmpty(captchaEnabled))
        {
            return true;
        }
        return Convert.toBool(captchaEnabled);
    }

    /**
     * 查询参数配置列表
     * 
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_config")
            .where(new QueryColumn("config_name").like(config.getConfigName(), StrUtil.isNotEmpty(config.getConfigName())))
            .and(new QueryColumn("config_type").eq(config.getConfigType(), StrUtil.isNotEmpty(config.getConfigType())))
            .and(new QueryColumn("config_key").like(config.getConfigKey(), StrUtil.isNotEmpty(config.getConfigKey())));
        return this.list(queryWrapper);
    }

    /**
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache()
    {
        List<SysConfig> configsList = this.list();
        for (SysConfig config : configsList)
        {
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
    }

    /**
     * 清空参数缓存数据
     */
    @Override
    public void clearConfigCache()
    {
        Collection<String> keys = redisCache.keys(CacheConstants.SYS_CONFIG_KEY + "*");
        redisCache.deleteObject(keys);
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache()
    {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfig config)
    {
        Long configId = ObjectUtil.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = this.getOne(QueryWrapper.create()
                .from("sys_config")
                .where(new QueryColumn("config_key").eq(config.getConfigKey())));
        if (ObjectUtil.isNotNull(info) && info.getConfigId().longValue() != configId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 设置cache key
     * 
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_CONFIG_KEY + configKey;
    }
}
