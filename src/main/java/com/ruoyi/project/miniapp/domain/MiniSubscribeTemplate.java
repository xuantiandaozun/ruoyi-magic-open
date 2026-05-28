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
@Table("mini_subscribe_template")
public class MiniSubscribeTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long miniAppId;

    private String appCode;

    private String sceneCode;

    private String templateId;

    private String templateNo;

    private String title;

    private String pagePath;

    private String fieldConfigJson;

    private String enabled;

    private Integer sortOrder;

    @Column(isLogicDelete = true)
    private String delFlag;
}
