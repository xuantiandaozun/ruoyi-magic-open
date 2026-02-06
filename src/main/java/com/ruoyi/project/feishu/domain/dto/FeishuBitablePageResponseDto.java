package com.ruoyi.project.feishu.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 飞书多维表格分页响应DTO
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Data
@Accessors(chain = true)
public class FeishuBitablePageResponseDto {
    
    /** 是否还有更多数据 */
    @JsonProperty("has_more")
    private Boolean hasMore;
    
    /** 记录列表 */
    @JsonProperty("items")
    private List<FeishuBitableRecordDto> items;
    
    /** 分页标记 */
    @JsonProperty("page_token")
    private String pageToken;
    
    /** 记录总数 */
    @JsonProperty("total")
    private Integer total;
}