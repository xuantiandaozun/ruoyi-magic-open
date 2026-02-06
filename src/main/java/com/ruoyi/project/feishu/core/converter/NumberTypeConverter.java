package com.ruoyi.project.feishu.core.converter;

import com.ruoyi.project.feishu.annotation.FieldType;

/**
 * 数字类型转换器
 * 支持Integer, Long, Double, Float等
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
public class NumberTypeConverter implements FieldTypeConverter<Number> {
    
    @Override
    public FieldType getSupportedType() {
        return FieldType.NUMBER;
    }
    
    @Override
    public Number convertFromFeishu(Object feishuValue, Class<?> targetType) {
        if (feishuValue == null) {
            return null;
        }
        
        if (feishuValue instanceof Number) {
            Number number = (Number) feishuValue;
            
            // 根据目标类型转换
            if (targetType == Integer.class || targetType == int.class) {
                return number.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return number.longValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return number.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return number.floatValue();
            } else if (targetType == Short.class || targetType == short.class) {
                return number.shortValue();
            }
            return number;
        }
        
        // 字符串类型尝试解析
        if (feishuValue instanceof String) {
            String str = (String) feishuValue;
            if (str.trim().isEmpty()) {
                return null;
            }
            try {
                if (targetType == Double.class || targetType == double.class) {
                    return Double.parseDouble(str);
                } else if (targetType == Float.class || targetType == float.class) {
                    return Float.parseFloat(str);
                } else if (targetType == Long.class || targetType == long.class) {
                    return Long.parseLong(str);
                } else {
                    return Integer.parseInt(str);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    @Override
    public Object convertToFeishu(Number javaValue) {
        return javaValue;
    }
}
