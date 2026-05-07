package com.ruoyi.project.ai.domain;

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
 * AI 对外 API Key。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_open_api_key")
public class AiOpenApiKey extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "名称")
    private String name;

    @Excel(name = "Key前缀")
    private String keyPrefix;

    private String keyHash;

    private String salt;

    @Excel(name = "状态")
    private String status;

    @Excel(name = "启用")
    private String enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUsedAt;

    private String lastUsedIp;

    private String allowedModels;

    private Long requestCount;

    private Long successCount;

    private Long failedCount;

    private Long inputTokens;

    private Long outputTokens;

    private Long totalTokens;

    @Column(isLogicDelete = true)
    private String delFlag;
}
