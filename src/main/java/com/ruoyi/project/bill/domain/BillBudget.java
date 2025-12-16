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
 * 预算对象 bill_budget
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("bill_budget")
public class BillBudget extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 预算ID */
    @Id(keyType = KeyType.Auto)
    private Long budgetId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 家庭组ID */
    @Excel(name = "家庭组ID")
    private Long familyId;

    /** 预算类型（0月度 1年度） */
    @Excel(name = "预算类型", readConverterExp = "0=月度,1=年度")
    private String budgetType;

    /** 分类ID（为空表示总预算） */
    @Excel(name = "分类ID")
    private Long categoryId;

    /** 预算金额 */
    @Excel(name = "预算金额")
    private BigDecimal budgetAmount;

    /** 实际支出 */
    @Excel(name = "实际支出")
    private BigDecimal actualAmount;

    /** 预算年份 */
    @Excel(name = "预算年份")
    private Integer budgetYear;

    /** 预算月份 */
    @Excel(name = "预算月份")
    private Integer budgetMonth;

    /** 状态（0正常 1已完成 2已超支） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=已完成,2=已超支")
    private String status;

    /** 删除标志（0正常 1删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
