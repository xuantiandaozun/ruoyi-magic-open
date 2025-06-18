package com.ruoyi.project.monitor.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.monitor.domain.SysCache;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
/**
 * 缓存监控
 * 
 * @author ruoyi
 */
@Tag(name = "缓存监控")
@RestController
@RequestMapping("/monitor/cache")
public class CacheController
{
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @SaCheckPermission("monitor:cache:list")
    @GetMapping()
    public AjaxResult getInfo() throws Exception
    {
        Map<String, Object> result = new HashMap<>(3);
        
        // 使用更现代的方式获取Redis信息
        Properties info = redisTemplate.execute(connection -> connection.serverCommands().info(), true);
        Properties commandStats = redisTemplate.execute(connection -> connection.serverCommands().info("commandstats"), true);
        Long dbSize = redisTemplate.execute(RedisServerCommands::dbSize, true);

        result.put("info", info);
        result.put("dbSize", dbSize);

        List<Map<String, String>> pieList = new ArrayList<>();
        if (commandStats != null) {
            commandStats.stringPropertyNames().forEach(key -> {
                Map<String, String> data = new HashMap<>(2);
                String property = commandStats.getProperty(key);
                data.put("name", StrUtil.removePrefix(key, "cmdstat_"));
                data.put("value", StrUtil.subBetween(property, "calls=", ",usec"));
                pieList.add(data);
            });
        }
        result.put("commandStats", pieList);
        return AjaxResult.success(result);
    }

    @SaCheckPermission("monitor:cache:list")
    @GetMapping("/getNames")
    public AjaxResult cache()
    {
        Set<String> keys = redisTemplate.keys("*");
        if (keys == null || keys.isEmpty()) {
            return AjaxResult.success(new ArrayList<>());
        }

        // 提取所有缓存的前缀
        Map<String, String> cacheMap = new HashMap<>();
        for (String key : keys) {
            String prefix;
            if (key.startsWith("satoken:")) {
                // 处理 Sa-Token 相关的缓存
                String[] parts = key.split(":");
                if (parts.length >= 2) {
                    prefix = parts[0] + ":" + parts[1];
                } else {
                    prefix = parts[0];
                }
            } else {
                // 处理其他缓存
                String[] parts = key.split(":");
                prefix = parts[0];
            }
            
            // 设置缓存说明
            String remark = getRemark(prefix);
            cacheMap.put(prefix, remark);
        }

        // 转换为前端需要的格式
        List<SysCache> cacheList = new ArrayList<>();
        cacheMap.forEach((prefix, remark) -> {
            cacheList.add(new SysCache(prefix, remark));
        });

        return AjaxResult.success(cacheList);
    }

    /**
     * 获取缓存说明
     */
    private String getRemark(String prefix) {
        switch (prefix) {
            case "satoken:login:session":
                return "用户信息";
            case "sys_config":
                return "配置信息";
            case "sys_dict":
                return "数据字典";
            case "captcha_codes":
                return "验证码";
            case "repeat_submit":
                return "防重提交";
            case "rate_limit":
                return "限流处理";
            case "pwd_err_cnt":
                return "密码错误次数";
            default:
                return "其他缓存";
        }
    }

    @SaCheckPermission("monitor:cache:list")
    @GetMapping("/getKeys/{cacheName}")
    public AjaxResult getCacheKeys(@PathVariable String cacheName)
    {
        String pattern;
        if (cacheName.equals("satoken:login:session")) {
            pattern = cacheName + ":*";
        } else if (cacheName.startsWith("sys_")) {
            // 系统配置和字典缓存
            pattern = cacheName + "*";
        } else {
            // 其他缓存
            pattern = cacheName + ":*";
        }
        Set<String> cacheKeys = redisTemplate.keys(pattern);
        return AjaxResult.success(new TreeSet<>(cacheKeys != null ? cacheKeys : new HashSet<>()));
    }

    @SaCheckPermission("monitor:cache:list")
    @GetMapping("/getValue/{cacheName}/{cacheKey}")
    public AjaxResult getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey)
    {
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        SysCache sysCache = new SysCache(cacheName, cacheKey, cacheValue);
        return AjaxResult.success(sysCache);
    }

    @SaCheckPermission("monitor:cache:list")
    @DeleteMapping("/clearCacheName/{cacheName}")
    public AjaxResult clearCacheName(@PathVariable String cacheName)
    {
        Collection<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        redisTemplate.delete(cacheKeys);
        return AjaxResult.success();
    }

    @SaCheckPermission("monitor:cache:list")
    @DeleteMapping("/clearCacheKey/{cacheKey}")
    public AjaxResult clearCacheKey(@PathVariable String cacheKey)
    {
        redisTemplate.delete(cacheKey);
        return AjaxResult.success();
    }

    @SaCheckPermission("monitor:cache:list")
    @DeleteMapping("/clearCacheAll")
    public AjaxResult clearCacheAll()
    {
        Collection<String> cacheKeys = redisTemplate.keys("*");
        redisTemplate.delete(cacheKeys);
        return AjaxResult.success();
    }
}
