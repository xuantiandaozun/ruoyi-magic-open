package com.ruoyi.project.bill.domain;

import java.math.BigDecimal;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账户对象 bill_account
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("bill_account")
public class BillAccount extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 账户ID */
    @Id(keyType = KeyType.Auto)
    private Long accountId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 账户名称 */
    @Excel(name = "账户名称")
    private String accountName;

    /** 账户类型（0现金 1微信 2支付宝 3银行卡 4信用卡 5其他） */
    @Excel(name = "账户类型", readConverterExp = "0=现金,1=微信,2=支付宝,3=银行卡,4=信用卡,5=其他")
    private String accountType;

    /** 账号（银行卡后四位） */
    @Excel(name = "账号")
    private String accountNo;

    /** 余额 */
    @Excel(name = "余额")
    private BigDecimal balance;

    /** 信用额度（信用卡专用） */
    @Excel(name = "信用额度")
    private BigDecimal creditLimit;

    /** 图标 */
    @Excel(name = "图标")
    private String icon;

    /** 颜色 */
    @Excel(name = "颜色")
    private String color;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0正常 1删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
