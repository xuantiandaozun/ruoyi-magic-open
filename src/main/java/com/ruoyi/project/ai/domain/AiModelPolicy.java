package com.ruoyi.project.ai.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI模型可用策略对象 ai_model_policy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_model_policy")
public class AiModelPolicy extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "模型配置ID")
    private Long modelConfigId;

    @Excel(name = "产品类型")
    private String productType;

    @Excel(name = "用户等级")
    private String userTier;

    @Excel(name = "优先级")
    private Integer priority;

    private Integer dailyRequestLimit;

    private Long dailyTokenLimit;

    private Integer dailyImageLimit;

    private Integer maxContextTokens;

    private Integer maxOutputTokens;

    private Long fallbackModelConfigId;

    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    private String configJson;

    @Column(isLogicDelete = true)
    private String delFlag;
}
