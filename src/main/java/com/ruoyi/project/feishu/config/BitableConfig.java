package com.ruoyi.project.feishu.config;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 飞书多维表格配置
 * 用于动态配置多维表格映射
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Data
@Accessors(chain = true)
public class BitableConfig {
    
    /**
     * 配置名称
     */
    private String name;
    
    /**
     * 多维表格应用token
     */
    private String appToken;
    
    /**
     * 数据表ID
     */
    private String tableId;
    
    /**
     * 视图ID
     */
    private String viewId;
    
    /**
     * 实体类全限定名
     */
    private String entityClass;
    
    /**
     * 字段映射列表
     */
    private List<BitableFieldMapping> fieldMappings = new ArrayList<>();
    
    /**
     * 主键字段名（用于数据匹配）
     */
    private String primaryField;
    
    /**
     * 分页大小
     */
    private Integer pageSize = 50;
    
    /**
     * 请求间隔（毫秒）
     */
    private Long requestInterval = 100L;
    
    /**
     * 添加字段映射
     */
    public BitableConfig addFieldMapping(BitableFieldMapping mapping) {
        this.fieldMappings.add(mapping);
        return this;
    }
    
    /**
     * 添加字段映射（快捷方法）
     */
    public BitableConfig addFieldMapping(String feishuField, String javaField) {
        this.fieldMappings.add(new BitableFieldMapping(feishuField, javaField));
        return this;
    }
    
    /**
     * 根据飞书字段名获取映射
     */
    public BitableFieldMapping getMappingByFeishuField(String feishuField) {
        return fieldMappings.stream()
            .filter(m -> feishuField.equals(m.getFeishuFieldName()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 根据Java字段名获取映射
     */
    public BitableFieldMapping getMappingByJavaField(String javaField) {
        return fieldMappings.stream()
            .filter(m -> javaField.equals(m.getJavaFieldName()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取主键映射
     */
    public BitableFieldMapping getPrimaryMapping() {
        if (primaryField != null) {
            return getMappingByJavaField(primaryField);
        }
        return fieldMappings.stream()
            .filter(BitableFieldMapping::isPrimary)
            .findFirst()
            .orElse(null);
    }
}
