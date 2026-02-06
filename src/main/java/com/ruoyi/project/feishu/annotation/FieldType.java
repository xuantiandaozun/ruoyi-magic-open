package com.ruoyi.project.feishu.annotation;

/**
 * 飞书多维表格字段类型枚举
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
public enum FieldType {
    
    /**
     * 自动检测类型
     */
    AUTO,
    
    /**
     * 文本类型
     */
    TEXT,
    
    /**
     * 多行文本
     */
    MULTI_LINE_TEXT,
    
    /**
     * 数字类型（整数或小数）
     */
    NUMBER,
    
    /**
     * 日期类型（毫秒时间戳）
     */
    DATE,
    
    /**
     * 日期时间类型
     */
    DATE_TIME,
    
    /**
     * 布尔类型
     */
    BOOLEAN,
    
    /**
     * 数组/列表类型
     */
    ARRAY,
    
    /**
     * 人员类型
     */
    USER,
    
    /**
     * 附件类型
     */
    ATTACHMENT,
    
    /**
     * 关联类型
     */
    LINK,
    
    /**
     * 单选类型
     */
    SINGLE_SELECT,
    
    /**
     * 多选类型
     */
    MULTI_SELECT,
    
    /**
     * 电话类型
     */
    PHONE,
    
    /**
     * 邮箱类型
     */
    EMAIL,
    
    /**
     * 超链接类型
     */
    URL,
    
    /**
     * 条码类型
     */
    BARCODE,
    
    /**
     * 地理位置类型
     */
    LOCATION,
    
    /**
     * 公式类型
     */
    FORMULA,
    
    /**
     * 级联类型
     */
    CASCADER
}
