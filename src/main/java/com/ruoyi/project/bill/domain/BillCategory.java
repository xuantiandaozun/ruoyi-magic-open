package com.ruoyi.project.bill.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账单分类对象 bill_category
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("bill_category")
public class BillCategory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @Id(keyType = KeyType.Auto)
    private Long categoryId;

    /** 分类名称 */
    @Excel(name = "分类名称")
    private String categoryName;

    /** 分类类型（0支出 1收入） */
    @Excel(name = "分类类型", readConverterExp = "0=支出,1=收入")
    private String categoryType;

    /** 父分类ID（0为一级分类） */
    @Excel(name = "父分类ID")
    private Long parentId;

    /** 图标 */
    @Excel(name = "图标")
    private String icon;

    /** 颜色 */
    @Excel(name = "颜色")
    private String color;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 是否系统分类（0否 1是） */
    @Excel(name = "是否系统分类", readConverterExp = "0=否,1=是")
    private String isSystem;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0正常 1删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
