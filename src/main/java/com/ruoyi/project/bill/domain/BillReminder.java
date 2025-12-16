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
 * 提醒设置对象 bill_reminder
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("bill_reminder")
public class BillReminder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 提醒ID */
    @Id(keyType = KeyType.Auto)
    private Long reminderId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 提醒类型（0记账提醒 1账单提醒 2预算提醒） */
    @Excel(name = "提醒类型", readConverterExp = "0=记账提醒,1=账单提醒,2=预算提醒")
    private String reminderType;

    /** 提醒名称 */
    @Excel(name = "提醒名称")
    private String reminderName;

    /** 提醒时间 */
    @Excel(name = "提醒时间")
    private LocalTime reminderTime;

    /** 提醒日期（还款日等） */
    @Excel(name = "提醒日期")
    private Integer reminderDay;

    /** 重复类型（0每天 1每周 2每月 3自定义） */
    @Excel(name = "重复类型", readConverterExp = "0=每天,1=每周,2=每月,3=自定义")
    private String repeatType;

    /** 是否启用（0禁用 1启用） */
    @Excel(name = "是否启用", readConverterExp = "0=禁用,1=启用")
    private String enabled;

    /** 删除标志（0正常 1删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
