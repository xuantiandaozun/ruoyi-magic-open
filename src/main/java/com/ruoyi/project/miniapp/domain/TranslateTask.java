package com.ruoyi.project.miniapp.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("translate_task")
public class TranslateTask extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long miniUserId;

    private Long miniAppId;

    private Long documentId;

    private String productType;

    private String sceneCode;

    private String sourceLanguage;

    private String targetLanguage;

    private String outputFormat;

    private String status;

    private Integer progress;

    private Long modelConfigId;

    private String modelName;

    private String resultOssUrl;

    private String resultOssKey;

    private String previewOssUrl;

    private String errorCode;

    private String errorMessage;

    private Integer retryCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedAt;

    @Column(isLogicDelete = true)
    private String delFlag;
}
