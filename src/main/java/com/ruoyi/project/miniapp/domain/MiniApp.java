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
@Table("mini_app")
public class MiniApp extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String appCode;

    private String appName;

    private String platform;

    private String appId;

    private String appSecret;

    private String token;

    private String aesKey;

    private String enabled;

    private String status;

    @Column(isLogicDelete = true)
    private String delFlag;
}
