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
@Table("mini_feedback")
public class MiniFeedback extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long miniUserId;

    private Long miniAppId;

    private String appCode;

    private String feedbackType;

    private String content;

    private String contact;

    private String images;

    private String status;

    private String replyContent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date replyTime;

    @Column(isLogicDelete = true)
    private String delFlag;
}
