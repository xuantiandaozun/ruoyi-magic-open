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
@Table("mini_user_auth")
public class MiniUserAuth extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long miniUserId;

    private Long miniAppId;

    private String platform;

    private String openid;

    private String unionid;

    private String sessionKey;

    private String nickname;

    private String avatar;

    private String rawJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date bindTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    private String status;

    private String delFlag;
}
