package com.ruoyi.project.feishu.core.converter;

import com.ruoyi.project.feishu.annotation.FieldType;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 文本类型转换器
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
public class TextTypeConverter implements FieldTypeConverter<String> {
    
    @Override
    public FieldType getSupportedType() {
        return FieldType.TEXT;
    }
    
    @Override
    public String convertFromFeishu(Object feishuValue, Class<?> targetType) {
        if (feishuValue == null) {
            return null;
        }
        
        // 如果是字符串，直接返回
        if (feishuValue instanceof String) {
            return (String) feishuValue;
        }
        
        // 如果是List（多行文本/富文本格式）
        if (feishuValue instanceof List) {
            List<?> fieldList = (List<?>) feishuValue;
            if (!fieldList.isEmpty()) {
                Object firstElement = fieldList.get(0);
                if (firstElement instanceof String) {
                    return (String) firstElement;
                }
                // 如果是Map类型（飞书富文本格式：[{text: "content", type: "text"}]）
                if (firstElement instanceof Map) {
                    Map<?, ?> elementMap = (Map<?, ?>) firstElement;
                    if (elementMap.containsKey("text")) {
                        Object textValue = elementMap.get("text");
                        return textValue != null ? String.valueOf(textValue) : null;
                    }
                }
            }
        }
        
        // 其他类型转字符串
        return String.valueOf(feishuValue);
    }
    
    @Override
    public Object convertToFeishu(String javaValue) {
        return javaValue;
    }
}
