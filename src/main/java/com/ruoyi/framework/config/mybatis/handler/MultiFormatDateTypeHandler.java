package com.ruoyi.framework.config.mybatis.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 多种日期格式转换为LocalDateTime的处理器
 * 
 * 解决多种日期类型转换问题：
 * 1. "Could not set property 'xxx' with value 'yyyy-MM-ddTHH:mm:ss'"
 * 2. "argument type mismatch" 当数据库返回Timestamp但实体类字段为LocalDateTime时
 */
@MappedTypes(LocalDateTime.class)
public class MultiFormatDateTypeHandler extends BaseTypeHandler<LocalDateTime> {
    
    private static final Logger log = LoggerFactory.getLogger(MultiFormatDateTypeHandler.class);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
        if (parameter != null) {
            ps.setTimestamp(i, Timestamp.valueOf(parameter));
        } else {
            ps.setNull(i, jdbcType.TYPE_CODE);
        }
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        try {
            Object obj = rs.getObject(columnName);
            return convertToLocalDateTime(obj);
        } catch (Exception e) {
            log.warn("Error converting column {} to LocalDateTime: {}", columnName, e.getMessage());
            return null;
        }
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            // 先尝试直接获取Timestamp
            Timestamp timestamp = rs.getTimestamp(columnIndex);
            if (timestamp != null) {
                return timestamp.toLocalDateTime();
            }
            
            // 如果为null，尝试获取其他类型
            Object obj = rs.getObject(columnIndex);
            return convertToLocalDateTime(obj);
        } catch (Exception e) {
            log.warn("Error converting column index {} to LocalDateTime: {}", columnIndex, e.getMessage());
            return null;
        }
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            // 先尝试直接获取Timestamp
            Timestamp timestamp = cs.getTimestamp(columnIndex);
            if (timestamp != null) {
                return timestamp.toLocalDateTime();
            }
            
            // 如果为null，尝试获取其他类型
            Object obj = cs.getObject(columnIndex);
            return convertToLocalDateTime(obj);
        } catch (Exception e) {
            log.warn("Error converting callable statement column index {} to LocalDateTime: {}", columnIndex, e.getMessage());
            return null;
        }
    }
    
    /**
     * 将多种类型转换为LocalDateTime
     * 
     * @param obj 原始对象
     * @return LocalDateTime对象
     */
    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
        } else if (obj instanceof Timestamp) {
            return ((Timestamp) obj).toLocalDateTime();
        } else if (obj instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) obj).getTime()).toLocalDateTime();
        } else if (obj instanceof String) {
            // 字符串转LocalDateTime的逻辑
            try {
                LocalDateTime localDateTime = parseDateTime((String) obj);
                if (localDateTime != null) {
                    return localDateTime;
                }
            } catch (Exception e) {
                log.warn("Error parsing date string: {}", obj);
            }
        }
        
        log.warn("Unsupported date type: {}", obj.getClass().getName());
        return null;
    }
    
    /**
     * 将字符串转换为LocalDateTime
     * 
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime对象，如果转换失败则返回null
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        // 支持多种日期时间格式
        java.time.format.DateTimeFormatter[] formatters = {
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME,               // 2024-12-27T10:06:44
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"), // 2025-08-06 12:07:30.0
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),  // 2024-12-27 10:06:44
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),  // 2024/12/27 10:06:44
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),           // 2024-12-27 (使用当天的00:00:00时间)
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")            // 2024/12/27 (使用当天的00:00:00时间)
        };
        
        for (java.time.format.DateTimeFormatter formatter : formatters) {
            try {
                // 对于完整日期时间格式，尝试直接解析为LocalDateTime
                if (formatter.equals(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME) ||
                    formatter.toString().contains("HH:mm:ss")) {
                    return LocalDateTime.parse(dateTimeStr, formatter);
                } else {
                    // 对于仅日期格式，解析为LocalDate并添加时间部分
                    java.time.LocalDate date = java.time.LocalDate.parse(dateTimeStr, formatter);
                    return LocalDateTime.of(date, java.time.LocalTime.MIDNIGHT);
                }
            } catch (java.time.format.DateTimeParseException e) {
                // 继续尝试下一个格式
                if (log.isTraceEnabled()) {
                    log.trace("Failed to parse date '{}' with formatter {}", dateTimeStr, formatter);
                }
            }
        }
        
        // 如果所有格式都解析失败，记录警告并返回null
        log.warn("Unable to parse date string: {}", dateTimeStr);
        return null;
    }
}