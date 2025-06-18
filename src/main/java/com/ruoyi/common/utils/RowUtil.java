package com.ruoyi.common.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mybatisflex.core.row.Row;

import cn.hutool.json.JSONUtil;

/**
 * Row工具类，用于处理MyBatisFlex的Row对象
 * 
 * @author ruoyi
 */
public class RowUtil {

    /**
     * 将Row列表转换为实体类列表
     * 通过先转换为Map列表，再使用JSON转换为实体类列表
     *
     * @param rows Row列表
     * @param entityClass 目标实体类
     * @return 实体类列表
     * @param <T> 实体类泛型
     */
    public static <T> List<T> toEntityList(List<Row> rows, Class<T> entityClass) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        
        // 先转为Map列表
        List<Map<String, Object>> mapList = rows.stream()
            .map(Row::toCamelKeysMap)
            .collect(Collectors.toList());
        
        // 使用JSON转换为实体类列表
        return JSONUtil.toList(JSONUtil.toJsonStr(mapList), entityClass);
    }
    
    /**
     * 将单个Row对象转换为实体类
     * 
     * @param row Row对象
     * @param entityClass 目标实体类
     * @return 实体类对象
     * @param <T> 实体类泛型
     */
    public static <T> T toEntity(Row row, Class<T> entityClass) {
        if (row == null) {
            return null;
        }
        
        // 先转为Map
        Map<String, Object> map = row.toCamelKeysMap();
        
        // 使用JSON转换为实体类
        return JSONUtil.toBean(JSONUtil.toJsonStr(map), entityClass);
    }
}
