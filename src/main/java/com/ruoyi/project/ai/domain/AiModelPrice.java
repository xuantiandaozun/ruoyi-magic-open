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
 * AI模型价格对象 ai_model_price
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_model_price")
public class AiModelPrice extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "模型配置ID")
    private Long modelConfigId;

    @Excel(name = "模型厂商")
    private String provider;

    @Excel(name = "模型名称")
    private String modelName;

    @Excel(name = "币种")
    private String currency;

    @Column("input_price_per_1m_tokens")
    private BigDecimal inputPricePer1mTokens;

    @Column("output_price_per_1m_tokens")
    private BigDecimal outputPricePer1mTokens;

    @Column("cached_input_price_per_1m_tokens")
    private BigDecimal cachedInputPricePer1mTokens;

    private BigDecimal imagePricePerUnit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date effectiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    private String sourceUrl;

    @Column(isLogicDelete = true)
    private String delFlag;
}
