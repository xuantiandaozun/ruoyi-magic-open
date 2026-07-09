package com.ruoyi.project.mediaassistant.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自媒体助手多平台内容草稿。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("media_content_draft")
public class MediaContentDraft extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long draftId;

    private Long sourceId;
    private Long analysisId;
    private String targetPlatform;
    private String contentType;
    private String title;
    private String summary;
    private String content;
    private String tags;
    private String coverPrompt;
    private String status;
    private Long relatedBlogId;
    private String publishResult;
}
