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
@Table("translate_segment")
public class TranslateSegment extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long taskId;

    private Integer segmentNo;

    private String segmentType;

    private String sourceText;

    private String translatedText;

    private String placeholderMap;

    private String status;

    private Integer tokenInput;

    private Integer tokenOutput;

    private String errorMessage;

    @Column(isLogicDelete = true)
    private String delFlag;
}
