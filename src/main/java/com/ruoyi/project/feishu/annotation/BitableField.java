package com.ruoyi.project.feishu.annotation;

import java.lang.annotation.*;

/**
 * 飞书多维表格字段映射注解
 * 用于标记Java实体类字段与飞书多维表格字段的映射关系
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BitableField {
    
    /**
     * 飞书多维表格中的字段名（支持中文或英文）
     * 如果不指定，默认使用Java字段名
     */
    String value() default "";
    
    /**
     * 飞书字段名（与value等效，用于更清晰语义）
     */
    String name() default "";
    
    /**
     * 字段类型
     * 用于数据转换：text-文本, number-数字, date-日期, boolean-布尔值, array-数组等
     */
    FieldType type() default FieldType.AUTO;
    
    /**
     * 是否为主键/唯一标识字段
     * 用于数据同步时的匹配
     */
    boolean primary() default false;
    
    /**
     * 日期格式（仅当type=DATE时有效）
     * 用于日期字符串格式化
     */
    String dateFormat() default "yyyy-MM-dd HH:mm:ss";
    
    /**
     * 是否忽略该字段
     */
    boolean ignore() default false;
    
    /**
     * 字段顺序（用于排序）
     */
    int order() default 0;
}
