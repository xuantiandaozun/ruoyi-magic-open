package com.ruoyi.project.bill.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账单记录对象 bill_record
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("bill_record")
public class BillRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 账单ID */
    @Id(keyType = KeyType.Auto)
    private Long recordId;

    /** 记账人ID */
    @Excel(name = "记账人ID")
    private Long userId;

    /** 家庭组ID */
    @Excel(name = "家庭组ID")
    private Long familyId;

    /** 类型（0支出 1收入） */
    @Excel(name = "类型", readConverterExp = "0=支出,1=收入")
    private String recordType;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 分类ID */
    @Excel(name = "分类ID")
    private Long categoryId;

    /** 账户ID */
    @Excel(name = "账户ID")
    private Long accountId;

    /** 记账日期 */
    @Excel(name = "记账日期", dateFormat = "yyyy-MM-dd")
    private LocalDate recordDate;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 图片凭证（JSON数组） */
    private String images;

    /** 地点（可选） */
    @Excel(name = "地点")
    private String location;

    /** 删除标志（0正常 1删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    /** 查询参数：开始日期（不映射到数据库） */
    @Column(ignore = true)
    private String startDate;

    /** 查询参数：结束日期（不映射到数据库） */
    @Column(ignore = true)
    private String endDate;
}
