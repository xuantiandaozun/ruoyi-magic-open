package com.ruoyi.project.mediaassistant.domain;

import java.util.Date;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自媒体助手原始素材。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("media_source")
public class MediaSource extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long sourceId;

    private String sourcePlatform;
    private String sourceUrl;
    private String sourceExternalId;
    private String title;
    private String author;
    private String community;
    private String rawContent;
    private String rawComments;
    private String rawPayload;
    private String contentHash;
    private String collectStatus;
    private String failReason;
    private Date collectedAt;
}
