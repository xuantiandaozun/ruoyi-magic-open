package com.ruoyi.project.feishu.domain.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 飞书多维表格记录DTO
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Data
@Accessors(chain = true)
public class FeishuBitableRecordDto {
    
    /** 记录ID */
    @JsonProperty("record_id")
    private String recordId;
    
    /** 创建人信息 */
    @JsonProperty("created_by")
    private PersonInfo createdBy;
    
    /** 创建时间（时间戳） */
    @JsonProperty("created_time")
    private Long createdTime;
    
    /** 记录字段数据 */
    @JsonProperty("fields")
    private Map<String, Object> fields;
    
    /** 最后修改人信息 */
    @JsonProperty("last_modified_by")
    private PersonInfo lastModifiedBy;
    
    /** 最后修改时间（时间戳） */
    @JsonProperty("last_modified_time")
    private Long lastModifiedTime;
    
    /** 记录分享链接 */
    @JsonProperty("shared_url")
    private String sharedUrl;
    
    /** 记录链接 */
    @JsonProperty("record_url")
    private String recordUrl;
    
    /**
     * 人员信息内部类
     */
    @Data
    public static class PersonInfo {
        /** 人员ID */
        private String id;
        
        /** 中文姓名 */
        private String name;
        
        /** 英文姓名 */
        @JsonProperty("en_name")
        private String enName;
        
        /** 邮箱 */
        private String email;
        
        /** 头像链接 */
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
}