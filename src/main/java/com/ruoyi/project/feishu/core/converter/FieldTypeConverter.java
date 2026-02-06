package com.ruoyi.project.feishu.core.converter;

import com.ruoyi.project.feishu.annotation.FieldType;

/**
 * 飞书字段类型转换器接口
 * 用于在Java类型和飞书类型之间进行转换
 * 
 * @param <T> Java类型
 * @author ruoyi
 * @date 2026-02-06
 */
public interface FieldTypeConverter<T> {
    
    /**
     * 获取支持的字段类型
     */
    FieldType getSupportedType();
    
    /**
     * 将飞书数据转换为Java对象
     * 
     * @param feishuValue 飞书返回的原始值
     * @param targetType 目标Java类型
     * @return 转换后的Java对象
     */
    T convertFromFeishu(Object feishuValue, Class<?> targetType);
    
    /**
     * 将Java对象转换为飞书数据格式
     * 
     * @param javaValue Java对象
     * @return 飞书需要的格式
     */
    Object convertToFeishu(T javaValue);
    
    /**
     * 检查是否支持该类型转换
     * 
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 是否支持
     */
    default boolean supports(Class<?> sourceType, Class<?> targetType) {
        return true;
    }
}
