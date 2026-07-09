package com.ruoyi.project.mediaassistant.domain;

import java.math.BigDecimal;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自媒体助手 AI 分析结果。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("media_ai_analysis")
public class MediaAiAnalysis extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long analysisId;

    private Long sourceId;
    private BigDecimal programmerRelevanceScore;
    private BigDecimal valueScore;
    private String originalityRisk;
    private String recommendation;
    private String suitablePlatforms;
    private String topicAngle;
    private String reason;
    private String aiModel;
    private String aiRawResponse;
}
