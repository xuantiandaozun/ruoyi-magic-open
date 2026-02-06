package com.ruoyi.project.feishu.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 域名证书监控记录DTO
 * 用于封装域名证书相关的多维表格记录数据
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Data
@Accessors(chain = true)
public class DomainCertRecordDto {
    
    /**
     * 剩余天数
     * 示例: 1.0
     */
    @JsonProperty("剩余天数")
    private Double remainingDays;
    
    /**
     * 域名信息（文本类型）
     * 示例: "www.test.com"
     */
    @JsonProperty("域名")
    private String domain;
    
    /**
     * 备注信息（文本类型）
     * 示例: "测试"
     */
    @JsonProperty("备注")
    private String remark;
    
    /**
     * 过期时间（毫秒级时间戳）
     * 示例: 1770307200000
     */
    @JsonProperty("过期时间")
    private Long expireTime;
    

}