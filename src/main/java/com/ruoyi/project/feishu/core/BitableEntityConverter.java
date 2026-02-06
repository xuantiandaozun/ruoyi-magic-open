package com.ruoyi.project.feishu.core;

import com.ruoyi.project.feishu.annotation.BitableField;
import com.ruoyi.project.feishu.annotation.FieldType;
import com.ruoyi.project.feishu.config.BitableConfig;
import com.ruoyi.project.feishu.config.BitableFieldMapping;
import com.ruoyi.project.feishu.core.converter.FieldTypeConverter;
import com.ruoyi.project.feishu.core.converter.FieldTypeConverterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 飞书多维表格实体转换器
 * 支持通过注解或配置进行实体与飞书数据的双向转换
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Slf4j
public class BitableEntityConverter {
    
    /**
     * 将飞书记录转换为Java实体
     * 
     * @param fields 飞书字段数据
     * @param entityClass 目标实体类
     * @param config 字段映射配置（可选）
     * @return 转换后的实体对象
     */
    public static <T> T convertToEntity(Map<String, Object> fields, Class<T> entityClass, BitableConfig config) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            
            // 获取所有字段
            Map<String, Field> javaFieldMap = getAllFields(entityClass);
            
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                String feishuFieldName = entry.getKey();
                Object feishuValue = entry.getValue();
                
                try {
                    // 确定目标Java字段
                    String javaFieldName = null;
                    FieldType fieldType = FieldType.AUTO;
                    
                    if (config != null) {
                        BitableFieldMapping mapping = config.getMappingByFeishuField(feishuFieldName);
                        if (mapping != null) {
                            if (mapping.isIgnore()) {
                                continue;
                            }
                            javaFieldName = mapping.getJavaFieldName();
                            fieldType = mapping.getType();
                        }
                    }
                    
                    // 如果没有配置映射，尝试从注解获取
                    if (javaFieldName == null) {
                        Field field = findFieldByAnnotation(feishuFieldName, javaFieldMap);
                        if (field != null) {
                            javaFieldName = field.getName();
                            BitableField annotation = field.getAnnotation(BitableField.class);
                            if (annotation != null && annotation.type() != FieldType.AUTO) {
                                fieldType = annotation.type();
                            }
                        }
                    }
                    
                    // 执行字段设置
                    if (javaFieldName != null && javaFieldMap.containsKey(javaFieldName)) {
                        Field field = javaFieldMap.get(javaFieldName);
                        setFieldValue(entity, field, feishuValue, fieldType);
                    }
                    
                } catch (Exception e) {
                    log.warn("转换字段失败: {} = {}", feishuFieldName, feishuValue, e);
                }
            }
            
            return entity;
            
        } catch (Exception e) {
            log.error("转换实体失败", e);
            return null;
        }
    }
    
    /**
     * 将Java实体转换为飞书记录字段
     * 
     * @param entity Java实体
     * @param config 字段映射配置（可选）
     * @return 飞书字段数据
     */
    public static Map<String, Object> convertToFeishuFields(Object entity, BitableConfig config) {
        if (entity == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> fields = new HashMap<>();
        Class<?> entityClass = entity.getClass();
        Map<String, Field> javaFieldMap = getAllFields(entityClass);
        
        for (Map.Entry<String, Field> entry : javaFieldMap.entrySet()) {
            String javaFieldName = entry.getKey();
            Field field = entry.getValue();
            
            try {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                // 跳过 transient 字段
                if (java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                
                // 跳过 MyBatis-Flex 的 ignore 字段
                if (isMybatisFlexIgnoreField(field)) {
                    continue;
                }
                
                // 确定飞书字段名和类型
                String feishuFieldName = null;
                FieldType fieldType = FieldType.AUTO;
                boolean hasMapping = false;
                
                // 从配置获取
                if (config != null) {
                    BitableFieldMapping mapping = config.getMappingByJavaField(javaFieldName);
                    if (mapping != null) {
                        if (mapping.isIgnore()) {
                            continue;
                        }
                        feishuFieldName = mapping.getFeishuFieldName();
                        fieldType = mapping.getType();
                        hasMapping = true;
                    }
                }
                
                // 从注解获取
                BitableField annotation = field.getAnnotation(BitableField.class);
                if (annotation != null) {
                    if (annotation.ignore()) {
                        continue;
                    }
                    if (feishuFieldName == null) {
                        feishuFieldName = annotation.value();
                        if (feishuFieldName.isEmpty()) {
                            feishuFieldName = annotation.name();
                        }
                    }
                    if (fieldType == FieldType.AUTO && annotation.type() != FieldType.AUTO) {
                        fieldType = annotation.type();
                    }
                    hasMapping = true;
                }
                
                // 如果没有配置映射，跳过该字段（关键！避免把无关字段传给飞书）
                if (!hasMapping) {
                    if (log.isDebugEnabled()) {
                        log.debug("字段 {} 没有配置映射，跳过", javaFieldName);
                    }
                    continue;
                }
                
                // 确保飞书字段名有效
                if (feishuFieldName == null || feishuFieldName.trim().isEmpty()) {
                    log.warn("字段 {} 的飞书字段名为空，跳过", javaFieldName);
                    continue;
                }
                
                // 获取字段值并转换
                field.setAccessible(true);
                Object javaValue = field.get(entity);
                
                if (javaValue != null) {
                    Object feishuValue = convertToFeishuValue(javaValue, fieldType, field.getType());
                    fields.put(feishuFieldName, feishuValue);
                    if (log.isDebugEnabled()) {
                        log.debug("转换字段: {} -> {} = {}", javaFieldName, feishuFieldName, feishuValue);
                    }
                }
                
            } catch (Exception e) {
                log.warn("转换字段到飞书格式失败: {}", javaFieldName, e);
            }
        }
        
        return fields;
    }
    
    /**
     * 检查是否是 MyBatis-Flex 的 ignore 字段
     */
    private static boolean isMybatisFlexIgnoreField(Field field) {
        try {
            com.mybatisflex.annotation.Column column = field.getAnnotation(com.mybatisflex.annotation.Column.class);
            if (column != null && column.ignore()) {
                return true;
            }
        } catch (Exception e) {
            // 忽略注解获取失败
        }
        return false;
    }
    
    /**
     * 提取主键值
     */
    public static Object extractPrimaryKey(Object entity, BitableConfig config) {
        if (entity == null) {
            return null;
        }
        
        Class<?> entityClass = entity.getClass();
        Map<String, Field> javaFieldMap = getAllFields(entityClass);
        
        // 从配置获取主键
        if (config != null) {
            BitableFieldMapping mapping = config.getPrimaryMapping();
            if (mapping != null && javaFieldMap.containsKey(mapping.getJavaFieldName())) {
                try {
                    Field field = javaFieldMap.get(mapping.getJavaFieldName());
                    field.setAccessible(true);
                    return field.get(entity);
                } catch (IllegalAccessException e) {
                    log.warn("获取主键值失败", e);
                }
            }
        }
        
        // 从注解查找主键字段
        for (Field field : javaFieldMap.values()) {
            BitableField annotation = field.getAnnotation(BitableField.class);
            if (annotation != null && annotation.primary()) {
                try {
                    field.setAccessible(true);
                    return field.get(entity);
                } catch (IllegalAccessException e) {
                    log.warn("获取主键值失败", e);
                }
            }
        }
        
        return null;
    }
    
    /**
     * 设置字段值（带类型转换）
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setFieldValue(Object entity, Field field, Object feishuValue, FieldType fieldType) {
        try {
            field.setAccessible(true);
            Class targetType = field.getType();
            
            Object convertedValue;
            if (fieldType != FieldType.AUTO) {
                // 使用指定的类型转换器
                FieldTypeConverter converter = FieldTypeConverterRegistry.getConverter(fieldType);
                if (converter != null) {
                    convertedValue = converter.convertFromFeishu(feishuValue, targetType);
                } else {
                    convertedValue = feishuValue;
                }
            } else {
                // 自动检测类型
                FieldTypeConverter converter = FieldTypeConverterRegistry.getConverter(targetType);
                if (converter != null) {
                    convertedValue = converter.convertFromFeishu(feishuValue, targetType);
                } else {
                    convertedValue = feishuValue;
                }
            }
            
            field.set(entity, convertedValue);
            
        } catch (Exception e) {
            log.warn("设置字段值失败: {} = {}", field.getName(), feishuValue, e);
        }
    }
    
    /**
     * 转换为飞书值
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object convertToFeishuValue(Object javaValue, FieldType fieldType, Class<?> targetType) {
        if (javaValue == null) {
            return null;
        }
        
        FieldTypeConverter converter = null;
        
        if (fieldType != FieldType.AUTO) {
            converter = FieldTypeConverterRegistry.getConverter(fieldType);
        }
        
        if (converter == null) {
            converter = FieldTypeConverterRegistry.getConverter(targetType);
        }
        
        if (converter != null) {
            return converter.convertToFeishu(javaValue);
        }
        
        return javaValue;
    }
    
    /**
     * 获取类的所有字段（包括父类），排除静态字段
     */
    private static Map<String, Field> getAllFields(Class<?> clazz) {
        Map<String, Field> fields = new HashMap<>();
        
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!fields.containsKey(field.getName())) {
                    fields.put(field.getName(), field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        
        return fields;
    }
    
    /**
     * 根据注解查找字段
     */
    private static Field findFieldByAnnotation(String feishuFieldName, Map<String, Field> fields) {
        for (Field field : fields.values()) {
            BitableField annotation = field.getAnnotation(BitableField.class);
            if (annotation != null) {
                String annotationValue = annotation.value();
                String annotationName = annotation.name();
                
                if (!annotationValue.isEmpty() && annotationValue.equals(feishuFieldName)) {
                    return field;
                }
                if (!annotationName.isEmpty() && annotationName.equals(feishuFieldName)) {
                    return field;
                }
            }
        }
        return null;
    }
}
