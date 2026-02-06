package com.ruoyi.project.feishu.annotation;

import java.lang.annotation.*;

/**
 * 飞书多维表格实体注解
 * 用于标记Java实体类对应飞书多维表格
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BitableEntity {
    
    /**
     * 多维表格应用token
     */
    String appToken() default "";
    
    /**
     * 数据表ID
     */
    String tableId() default "";
    
    /**
     * 视图ID（可选）
     */
    String viewId() default "";
    
    /**
     * 实体描述
     */
    String description() default "";
    
    /**
     * 配置key名称（用于从配置中心获取）
     */
    String configKey() default "";
}
