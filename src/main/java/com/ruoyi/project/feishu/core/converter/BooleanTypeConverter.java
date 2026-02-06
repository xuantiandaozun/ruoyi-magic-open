package com.ruoyi.project.feishu.core.converter;

import com.ruoyi.project.feishu.annotation.FieldType;

/**
 * 布尔类型转换器
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
public class BooleanTypeConverter implements FieldTypeConverter<Boolean> {
    
    @Override
    public FieldType getSupportedType() {
        return FieldType.BOOLEAN;
    }
    
    @Override
    public Boolean convertFromFeishu(Object feishuValue, Class<?> targetType) {
        if (feishuValue == null) {
            return null;
        }
        
        if (feishuValue instanceof Boolean) {
            return (Boolean) feishuValue;
        }
        
        if (feishuValue instanceof Number) {
            return ((Number) feishuValue).intValue() != 0;
        }
        
        if (feishuValue instanceof String) {
            String str = ((String) feishuValue).trim().toLowerCase();
            return "true".equals(str) || "1".equals(str) || "yes".equals(str) || "on".equals(str);
        }
        
        return Boolean.valueOf(String.valueOf(feishuValue));
    }
    
    @Override
    public Object convertToFeishu(Boolean javaValue) {
        return javaValue != null && javaValue;
    }
}
