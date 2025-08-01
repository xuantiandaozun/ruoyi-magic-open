package com.ruoyi.project.feishu.domain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 飞书文档DTO
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Data
@Accessors(chain = true)
public class FeishuDocDto {
    
    /** 文档token */
    @JsonProperty("token")
    private String token;
    
    /** 文档名称 */
    @JsonProperty("name")
    private String name;
    
    /** 文档类型 */
    @JsonProperty("type")
    private String type;
    
    /** 父级文件夹token */
    @JsonProperty("parent_token")
    private String parentToken;
    
    /** 文档URL */
    @JsonProperty("url")
    private String url;
    
    /** 创建时间戳 */
    @JsonProperty("created_time")
    private String createdTime;
    
    /** 修改时间戳 */
    @JsonProperty("modified_time")
    private String modifiedTime;
    
    /** 拥有者ID */
    @JsonProperty("owner_id")
    private String ownerId;
    
    /** 创建时间（转换后） */
    private LocalDateTime createTime;
    
    /** 修改时间（转换后） */
    private LocalDateTime updateTime;
    
    /** 是否为文件夹 */
    private Boolean isFolder;
    
    /** 文档内容（缓存） */
    private String content;
    
    /** 关联的密钥名称 */
    private String keyName;
    
    /**
     * 判断是否为文件夹
     * @return 是否为文件夹
     */
    public Boolean getIsFolder() {
        if (isFolder == null && type != null) {
            isFolder = "folder".equals(type);
        }
        return isFolder;
    }
    
    /**
     * 获取文档类型的中文描述
     * @return 文档类型描述
     */
    public String getTypeDescription() {
        if (type == null) {
            return "未知";
        }
        
        switch (type) {
            case "folder":
                return "文件夹";
            case "doc":
                return "文档";
            case "docx":
                return "新版文档";
            case "sheet":
                return "电子表格";
            case "bitable":
                return "多维表格";
            case "mindnote":
                return "思维笔记";
            case "file":
                return "文件";
            case "slides":
                return "演示文稿";
            default:
                return type;
        }
    }
    
    /**
     * 转换时间戳为LocalDateTime
     * @param timestamp 时间戳（秒）
     * @return LocalDateTime
     */
    public static LocalDateTime convertTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(timestamp);
            return LocalDateTime.ofEpochSecond(seconds, 0, java.time.ZoneOffset.ofHours(8));
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 设置创建时间并转换
     * @param createdTime 创建时间戳
     */
    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
        this.createTime = convertTimestamp(createdTime);
    }
    
    /**
     * 设置修改时间并转换
     * @param modifiedTime 修改时间戳
     */
    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
        this.updateTime = convertTimestamp(modifiedTime);
    }
}