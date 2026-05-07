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
 * AI使用明细记录对象 ai_usage_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_usage_record")
public class AiUsageRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "请求ID")
    private String requestId;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "会话ID")
    private Long sessionId;

    @Excel(name = "应用编码")
    private String appCode;

    @Excel(name = "对外API Key ID")
    private Long openApiKeyId;

    @Excel(name = "对外API Key名称")
    private String openApiKeyName;

    @Excel(name = "产品类型")
    private String productType;

    @Excel(name = "模型配置ID")
    private Long modelConfigId;

    @Excel(name = "模型厂商")
    private String provider;

    @Excel(name = "模型名称")
    private String modelName;

    @Excel(name = "输入Token")
    private Integer inputTokens;

    @Excel(name = "输出Token")
    private Integer outputTokens;

    @Excel(name = "缓存输入Token")
    private Integer cachedInputTokens;

    @Excel(name = "总Token")
    private Integer totalTokens;

    @Excel(name = "图片数量")
    private Integer imageCount;

    @Excel(name = "预估成本")
    private BigDecimal estimatedCost;

    @Excel(name = "币种")
    private String currency;

    @Excel(name = "调用状态")
    private String status;

    private String errorCode;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @Excel(name = "耗时毫秒")
    private Long durationMs;

    private String clientIp;

    private String userAgent;

    private String requestMeta;

    private String responseMeta;

    @Column(isLogicDelete = true)
    private String delFlag;
}
