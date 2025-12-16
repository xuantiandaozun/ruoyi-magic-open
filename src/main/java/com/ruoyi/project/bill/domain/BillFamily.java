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
 * 家庭组对象 bill_family
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("bill_family")
public class BillFamily extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 家庭组ID */
    @Id(keyType = KeyType.Auto)
    private Long familyId;

    /** 家庭组名称 */
    @Excel(name = "家庭组名称")
    private String familyName;

    /** 家庭组邀请码 */
    @Excel(name = "邀请码")
    private String familyCode;

    /** 创建者ID */
    @Excel(name = "创建者ID")
    private Long creatorId;

    /** 成员数量 */
    @Excel(name = "成员数量")
    private Integer memberCount;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0正常 1删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
