package com.ruoyi.project.feishu.core.converter;

import com.ruoyi.project.feishu.annotation.FieldType;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段类型转换器注册中心
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Slf4j
public class FieldTypeConverterRegistry {
    
    private static final Map<FieldType, FieldTypeConverter<?>> converters = new ConcurrentHashMap<>();
    private static final Map<Class<?>, FieldTypeConverter<?>> classConverters = new ConcurrentHashMap<>();
    
    static {
        // 注册默认转换器
        registerDefaultConverters();
    }
    
    /**
     * 注册默认转换器
     */
    private static void registerDefaultConverters() {
        // 文本类型
        register(FieldType.TEXT, new TextTypeConverter());
        register(FieldType.MULTI_LINE_TEXT, new TextTypeConverter());
        
        // 数字类型
        register(FieldType.NUMBER, new NumberTypeConverter());
        
        // 日期类型
        register(FieldType.DATE, new DateTypeConverter());
        register(FieldType.DATE_TIME, new DateTypeConverter());
        
        // 布尔类型
        register(FieldType.BOOLEAN, new BooleanTypeConverter());
    }
    
    /**
     * 注册转换器
     */
    public static void register(FieldType type, FieldTypeConverter<?> converter) {
        converters.put(type, converter);
    }
    
    /**
     * 根据类型获取转换器
     */
    @SuppressWarnings("unchecked")
    public static <T> FieldTypeConverter<T> getConverter(FieldType type) {
        return (FieldTypeConverter<T>) converters.get(type);
    }
    
    /**
     * 根据Java类型获取转换器
     */
    @SuppressWarnings("unchecked")
    public static <T> FieldTypeConverter<T> getConverter(Class<T> javaType) {
        // 先从类型映射中查找
        FieldTypeConverter<?> converter = classConverters.get(javaType);
        if (converter != null) {
            return (FieldTypeConverter<T>) converter;
        }
        
        // 根据类型推断
        if (String.class.isAssignableFrom(javaType)) {
            return (FieldTypeConverter<T>) getConverter(FieldType.TEXT);
        } else if (Number.class.isAssignableFrom(javaType) || javaType.isPrimitive()) {
            return (FieldTypeConverter<T>) getConverter(FieldType.NUMBER);
        } else if (Date.class.isAssignableFrom(javaType)) {
            return (FieldTypeConverter<T>) getConverter(FieldType.DATE);
        } else if (Boolean.class.isAssignableFrom(javaType) || javaType == boolean.class) {
            return (FieldTypeConverter<T>) getConverter(FieldType.BOOLEAN);
        }
        
        return null;
    }
    
    /**
     * 自动检测字段类型
     */
    public static FieldType detectFieldType(Class<?> javaType) {
        if (String.class.isAssignableFrom(javaType)) {
            return FieldType.TEXT;
        } else if (Number.class.isAssignableFrom(javaType) || 
                   (javaType.isPrimitive() && javaType != boolean.class && javaType != char.class)) {
            return FieldType.NUMBER;
        } else if (Date.class.isAssignableFrom(javaType)) {
            return FieldType.DATE;
        } else if (Boolean.class.isAssignableFrom(javaType) || javaType == boolean.class) {
            return FieldType.BOOLEAN;
        }
        return FieldType.TEXT;
    }
}
