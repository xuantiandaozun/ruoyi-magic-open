package com.ruoyi.project.ai.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;

import lombok.Data;

/**
 * AI每日用量汇总对象 ai_usage_summary_daily
 */
@Data
@Table("ai_usage_summary_daily")
public class AiUsageSummaryDaily implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "汇总日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date summaryDate;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "产品类型")
    private String productType;

    @Excel(name = "模型厂商")
    private String provider;

    @Excel(name = "模型名称")
    private String modelName;

    @Excel(name = "请求次数")
    private Integer requestCount;

    @Excel(name = "成功次数")
    private Integer successCount;

    @Excel(name = "失败次数")
    private Integer failedCount;

    @Excel(name = "输入Token")
    private Long inputTokens;

    @Excel(name = "输出Token")
    private Long outputTokens;

    @Excel(name = "缓存输入Token")
    private Long cachedInputTokens;

    @Excel(name = "总Token")
    private Long totalTokens;

    @Excel(name = "图片数量")
    private Integer imageCount;

    @Excel(name = "预估成本")
    private BigDecimal estimatedCost;

    @Excel(name = "币种")
    private String currency;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
