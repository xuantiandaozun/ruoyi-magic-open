package com.ruoyi.project.ai.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI生图任务对象 ai_image_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_image_task")
public class AiImageTask extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "请求ID")
    private String requestId;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "模型配置ID")
    private Long modelConfigId;

    @Excel(name = "模型厂商")
    private String provider;

    @Excel(name = "模型名称")
    private String modelName;

    private String prompt;

    private String negativePrompt;

    @Excel(name = "图片尺寸")
    private String size;

    @Excel(name = "图片数量")
    private Integer imageCount;

    @Excel(name = "任务状态")
    private String status;

    private String resultJson;

    private String errorMessage;

    @Excel(name = "预估成本")
    private BigDecimal estimatedCost;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @Excel(name = "耗时毫秒")
    private Long durationMs;

    @Column(isLogicDelete = true)
    private String delFlag;
}
