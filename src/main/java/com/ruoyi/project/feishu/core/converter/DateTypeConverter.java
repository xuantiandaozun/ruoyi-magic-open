package com.ruoyi.project.feishu.core.converter;

import com.ruoyi.project.feishu.annotation.FieldType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期类型转换器
 * 飞书返回的是毫秒级时间戳
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
public class DateTypeConverter implements FieldTypeConverter<Date> {
    
    @Override
    public FieldType getSupportedType() {
        return FieldType.DATE;
    }
    
    @Override
    public Date convertFromFeishu(Object feishuValue, Class<?> targetType) {
        if (feishuValue == null) {
            return null;
        }
        
        // 飞书返回的是毫秒级时间戳（Number类型）
        if (feishuValue instanceof Number) {
            long timestamp = ((Number) feishuValue).longValue();
            return new Date(timestamp);
        }
        
        // 如果是字符串，尝试解析
        if (feishuValue instanceof String) {
            String str = (String) feishuValue;
            if (str.trim().isEmpty()) {
                return null;
            }
            
            // 尝试作为时间戳解析
            try {
                long timestamp = Long.parseLong(str);
                return new Date(timestamp);
            } catch (NumberFormatException ignored) {
            }
            
            // 尝试作为日期字符串解析
            String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd"};
            for (String pattern : patterns) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                    return sdf.parse(str);
                } catch (ParseException ignored) {
                }
            }
        }
        
        return null;
    }
    
    @Override
    public Object convertToFeishu(Date javaValue) {
        if (javaValue == null) {
            return null;
        }
        // 返回毫秒级时间戳
        return javaValue.getTime();
    }
}
