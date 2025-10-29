package com.ruoyi.common.utils;

import java.util.Collection;
import java.util.List;
import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.system.domain.SysDictData;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.collection.CollUtil;

/**
 * 字典工具类
 * 
 * @author ruoyi
 */
public class DictUtils
{
    /**
     * 分隔符
     */
    public static final String SEPARATOR = ",";

    /**
     * 设置字典缓存
     * 
     * @param key 参数键
     * @param dictDatas 字典数据列表
     */
    public static void setDictCache(String key, List<SysDictData> dictDatas)
    {
        SpringUtils.getBean(RedisCache.class).setCacheObject(getCacheKey(key), dictDatas);
    }

    /**
     * 获取字典缓存
     * 
     * @param key 参数键
     * @return dictDatas 字典数据列表
     */
    public static List<SysDictData> getDictCache(String key)
    {
        Object cacheObject = SpringUtils.getBean(RedisCache.class).getCacheObject(getCacheKey(key));
        if (cacheObject != null)
        {
            try {
                // 如果缓存对象是JSONArray类型，直接转换
                if (cacheObject instanceof JSONArray) {
                    JSONArray arrayCache = (JSONArray) cacheObject;
                    return arrayCache.toList(SysDictData.class);
                }
                // 如果缓存对象是List类型，直接返回
                else if (cacheObject instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<SysDictData> listCache = (List<SysDictData>) cacheObject;
                    return listCache;
                }
                // 如果是其他类型，尝试通过JSON转换
                else {
                    String jsonStr = com.alibaba.fastjson2.JSON.toJSONString(cacheObject);
                    return com.alibaba.fastjson2.JSON.parseArray(jsonStr, SysDictData.class);
                }
            } catch (Exception e) {
                // 如果转换失败，记录警告并返回null，让调用方从数据库重新加载
                org.slf4j.LoggerFactory.getLogger(DictUtils.class)
                    .warn("字典缓存类型转换失败，key: {}, cacheType: {}, 将从数据库重新加载", 
                          key, cacheObject.getClass().getSimpleName(), e);
                // 删除有问题的缓存
                removeDictCache(key);
                return null;
            }
        }
        return null;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue)
    {
        if (StrUtil.isEmpty(dictValue))
        {
            return StrUtil.EMPTY;
        }
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     * 
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel)
    {
        if (StrUtil.isEmpty(dictLabel))
        {
            return StrUtil.EMPTY;
        }
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (CollUtil.isEmpty(datas))
        {
            return StrUtil.EMPTY;
        }
        if (StrUtil.containsAny(dictValue, separator))
        {
            for (SysDictData dict : datas)
            {
                for (String value : StrUtil.split(dictValue, separator))
                {
                    if (value.equals(dict.getDictValue()))
                    {
                        propertyString.append(dict.getDictLabel()).append(separator);
                        break;
                    }
                }
            }
        }
        else
        {
            for (SysDictData dict : datas)
            {
                if (dictValue.equals(dict.getDictValue()))
                {
                    return dict.getDictLabel();
                }
            }
        }
        return StrUtil.removeSuffix(propertyString.toString(), separator);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     * 
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (CollUtil.isEmpty(datas))
        {
            return StrUtil.EMPTY;
        }
        if (StrUtil.containsAny(dictLabel, separator))
        {
            for (SysDictData dict : datas)
            {
                for (String label : StrUtil.split(dictLabel, separator))
                {
                    if (label.equals(dict.getDictLabel()))
                    {
                        propertyString.append(dict.getDictValue()).append(separator);
                        break;
                    }
                }
            }
        }
        else
        {
            for (SysDictData dict : datas)
            {
                if (dictLabel.equals(dict.getDictLabel()))
                {
                    return dict.getDictValue();
                }
            }
        }
        return StrUtil.removeSuffix(propertyString.toString(), separator);
    }

    /**
     * 根据字典类型获取字典所有值
     *
     * @param dictType 字典类型
     * @return 字典值
     */
    public static String getDictValues(String dictType)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (CollUtil.isEmpty(datas))
        {
            return StrUtil.EMPTY;
        }
        for (SysDictData dict : datas)
        {
            propertyString.append(dict.getDictValue()).append(SEPARATOR);
        }
        return StrUtil.removeSuffix(propertyString.toString(), SEPARATOR);
    }

    /**
     * 根据字典类型获取字典所有标签
     *
     * @param dictType 字典类型
     * @return 字典值
     */
    public static String getDictLabels(String dictType)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (CollUtil.isEmpty(datas))
        {
            return StrUtil.EMPTY;
        }
        for (SysDictData dict : datas)
        {
            propertyString.append(dict.getDictLabel()).append(SEPARATOR);
        }
        return StrUtil.removeSuffix(propertyString.toString(), SEPARATOR);
    }

    /**
     * 删除指定字典缓存
     * 
     * @param key 字典键
     */
    public static void removeDictCache(String key)
    {
        SpringUtils.getBean(RedisCache.class).deleteObject(getCacheKey(key));
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache()
    {
        Collection<String> keys = SpringUtils.getBean(RedisCache.class).keys(CacheConstants.SYS_DICT_KEY + "*");
        SpringUtils.getBean(RedisCache.class).deleteObject(keys);
    }

    /**
     * 设置cache key
     * 
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_DICT_KEY + configKey;
    }
}
