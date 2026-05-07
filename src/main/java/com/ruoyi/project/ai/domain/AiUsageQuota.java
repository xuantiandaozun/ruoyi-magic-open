package com.ruoyi.project.ai.domain;

import java.math.BigDecimal;
import java.sql.Time;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI用户额度配置对象 ai_usage_quota
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_usage_quota")
public class AiUsageQuota extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "额度编码")
    private String quotaCode;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "用户等级")
    private String userTier;

    @Excel(name = "产品类型")
    private String productType;

    @Excel(name = "额度周期")
    private String quotaPeriod;

    @Excel(name = "请求次数上限")
    private Integer requestLimit;

    @Excel(name = "Token上限")
    private Long tokenLimit;

    @Excel(name = "图片数量上限")
    private Integer imageLimit;

    @Excel(name = "并发任务上限")
    private Integer concurrentLimit;

    @Excel(name = "成本上限")
    private BigDecimal costLimit;

    @Excel(name = "每日重置时间")
    private Time resetTime;

    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    @Column(isLogicDelete = true)
    private String delFlag;
}
