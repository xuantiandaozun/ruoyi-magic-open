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
@Table("mini_user")
public class MiniUser extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String nickname;

    private String avatar;

    private String mobile;

    private String email;

    private String sourceAppCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    private String status;

    @Column(isLogicDelete = true)
    private String delFlag;
}
