package com.ruoyi.framework.config;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 配置
 *
 * @author ruoyi
 */
@Configuration
public class JacksonConfig {
    
    @Bean
    @Order(1)
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 全局转化 Long 类型为 String，解决前端 JavaScript Long 精度丢失问题
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            
            // 添加自定义日期反序列化器，支持多种日期格式
            builder.deserializerByType(Date.class, new MultiFormatDateDeserializer());
            
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            // 设置时区
            builder.timeZone(TimeZone.getDefault());
        };
    }
    
    /**
     * 多格式日期反序列化器
     * 支持多种日期格式，解决Spring AI返回ISO 8601格式与@JsonFormat注解不兼容的问题
     */
    public static class MultiFormatDateDeserializer extends JsonDeserializer<Date> {
        
        // 支持的日期格式数组，按优先级排序
        private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",           // 默认格式
            "yyyy-MM-dd'T'HH:mm:ss",         // ISO 8601 本地时间
            "yyyy-MM-dd'T'HH:mm:ss'Z'",      // ISO 8601 UTC时间
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",  // ISO 8601 UTC时间带毫秒
            "yyyy-MM-dd'T'HH:mm:ss.SSS",     // ISO 8601 本地时间带毫秒
            "yyyy/MM/dd HH:mm:ss",           // 斜杠分隔格式
            "yyyy-MM-dd",                    // 仅日期格式
            "yyyy/MM/dd"                     // 斜杠分隔仅日期格式
        };
        
        @Override
        public Date deserialize(JsonParser parser, DeserializationContext context) 
                throws IOException, JsonProcessingException {
            String dateString = parser.getText();
            if (dateString == null || dateString.trim().isEmpty()) {
                return null;
            }
            
            // 去除首尾空白字符
            dateString = dateString.trim();
            
            // 尝试各种日期格式
            for (String pattern : DATE_PATTERNS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                    sdf.setLenient(false);
                    return sdf.parse(dateString);
                } catch (ParseException e) {
                    // 继续尝试下一个格式
                }
            }
            
            // 如果所有格式都失败，抛出异常
            throw new JsonProcessingException("无法解析日期字符串: " + dateString + 
                ", 支持的格式: yyyy-MM-dd HH:mm:ss, yyyy-MM-ddTHH:mm:ssZ 等") {
                @Override
                public String getOriginalMessage() {
                    return getMessage();
                }
            };
        }
    }
}
