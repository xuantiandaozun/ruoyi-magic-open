package com.ruoyi.project.system.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 第三方账号绑定对象 sys_oauth_account
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_oauth_account")
public class SysOauthAccount extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "系统用户ID")
    private Long userId;

    @Excel(name = "第三方平台")
    private String provider;

    @Excel(name = "第三方用户ID")
    private String openId;

    private String unionId;

    @Excel(name = "第三方邮箱")
    private String email;

    @Excel(name = "第三方昵称")
    private String nickname;

    private String avatar;

    private String accessToken;

    private String refreshToken;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date tokenExpireTime;

    private String rawJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date bindTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    @Column(isLogicDelete = true)
    private String delFlag;
}
