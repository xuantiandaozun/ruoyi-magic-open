package com.ruoyi.project.bill.domain;

import java.time.LocalTime;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 记账用户扩展对象 bill_user_profile
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("bill_user_profile")
public class BillUserProfile extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 扩展ID */
    @Id(keyType = KeyType.Auto)
    private Long profileId;

    /** 用户ID（关联sys_user.user_id） */
    @Excel(name = "用户ID")
    private Long userId;

    /** 家庭组ID */
    @Excel(name = "家庭组ID")
    private Long familyId;

    /** 家庭角色（1普通成员 2管理员） */
    @Excel(name = "家庭角色", readConverterExp = "1=普通成员,2=管理员")
    private String familyRole;

    /** 微信openid */
    private String wechatOpenid;

    /** 支付宝用户ID */
    private String alipayUserId;

    /** 默认账户ID */
    @Excel(name = "默认账户ID")
    private Long defaultAccountId;

    /** 预算提醒开关（0关闭 1开启） */
    @Excel(name = "预算提醒开关", readConverterExp = "0=关闭,1=开启")
    private String budgetAlertEnabled;

    /** 每日记账提醒开关（0关闭 1开启） */
    @Excel(name = "每日记账提醒开关", readConverterExp = "0=关闭,1=开启")
    private String dailyRemindEnabled;

    /** 每日提醒时间 */
    @Excel(name = "每日提醒时间")
    private LocalTime dailyRemindTime;

    /** 删除标志（0正常 1删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    // ========== 以下为临时字段，不映射到数据库 ==========

    /** 用户昵称（临时字段，从sys_user关联查询） */
    @Column(ignore = true)
    private String nickName;

    /** 用户头像（临时字段，从sys_user关联查询） */
    @Column(ignore = true)
    private String avatar;

    /** 用户手机号（临时字段，从sys_user关联查询） */
    @Column(ignore = true)
    private String phonenumber;
}
