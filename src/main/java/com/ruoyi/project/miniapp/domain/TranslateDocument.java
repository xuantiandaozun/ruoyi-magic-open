package com.ruoyi.project.miniapp.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("translate_document")
public class TranslateDocument extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long miniUserId;

    private Long miniAppId;

    private String sourceType;

    private String originalName;

    private String fileExt;

    private String mimeType;

    private Long fileSize;

    private String contentHash;

    private String sourceOssUrl;

    private String sourceOssKey;

    private String parseStatus;

    private String status;

    @Column(isLogicDelete = true)
    private String delFlag;
}
