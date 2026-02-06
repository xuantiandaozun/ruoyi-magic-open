package com.ruoyi.project.feishu.config;

import com.ruoyi.project.feishu.annotation.FieldType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书多维表格字段映射配置
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Data
@Accessors(chain = true)
public class BitableFieldMapping {
    
    /**
     * 飞书字段名
     */
    private String feishuFieldName;
    
    /**
     * Java字段名
     */
    private String javaFieldName;
    
    /**
     * 字段类型
     */
    private FieldType type;
    
    /**
     * 是否为主键
     */
    private boolean primary;
    
    /**
     * 日期格式
     */
    private String dateFormat;
    
    /**
     * 是否忽略
     */
    private boolean ignore;
    
    /**
     * 字段顺序
     */
    private int order;
    
    /**
     * 扩展属性
     */
    private Map<String, Object> properties = new HashMap<>();
    
    public BitableFieldMapping() {}
    
    public BitableFieldMapping(String feishuFieldName, String javaFieldName) {
        this.feishuFieldName = feishuFieldName;
        this.javaFieldName = javaFieldName;
        this.type = FieldType.AUTO;
    }
    
    public BitableFieldMapping(String feishuFieldName, String javaFieldName, FieldType type) {
        this.feishuFieldName = feishuFieldName;
        this.javaFieldName = javaFieldName;
        this.type = type;
    }
    
    /**
     * 添加扩展属性
     */
    public BitableFieldMapping addProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }
    
    /**
     * 获取扩展属性
     */
    public Object getProperty(String key) {
        return this.properties.get(key);
    }
}
