package com.ruoyi.project.system.domain;

import java.util.Date;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RDS实例管理对象 rds_instance_info
 * 
 * @author ruoyi
 * @date 2025-07-11 17:49:40
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("rds_instance_info")
public class RdsInstanceInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 实例ID */
    @Excel(name = "实例ID")
    private String dbInstanceId;

    /** 实例描述 */
    @Excel(name = "实例描述")
    private String dbInstanceDescription;

    /** 数据库类型 */
    @Excel(name = "数据库类型")
    private String engine;

    /** 数据库版本 */
    @Excel(name = "数据库版本")
    private String engineVersion;

    /** 实例规格 */
    private String dbInstanceClass;

    /** CPU数量 */
    private String dbInstanceCpu;

    /** 内存大小（MB） */
    private Integer dbInstanceMemory;

    /** 存储类型 */
    private String dbInstanceStorageType;

    /** 实例系列 */
    private String category;

    /** 实例状态 */
    @Excel(name = "实例状态")
    private String dbInstanceStatus;

    /** 实例类型 */
    private String dbInstanceType;

    /** 访问模式 */
    private String connectionMode;

    /** 连接地址 */
    private String connectionString;

    /** 付费类型 */
    private String payType;

    /** 锁定状态 */
    private String lockMode;

    /** 锁定原因 */
    private String lockReason;

    /** 释放保护 */
    private String deletionProtection;

    /** 实例创建时间 */
    private Date instanceCreateTime;

    /** 到期时间 */
    private Date expireTime;

    /** 销毁时间 */
    private Date destroyTime;

    /** 主实例ID */
    private String masterInstanceId;

    /** 灾备实例ID */
    private String guardDbInstanceId;

    /** 临时实例ID */
    private String tempDbInstanceId;

    /** 关联的密钥表ID */
    private Long secretKeyId;

    /** 访问密钥（冗余） */
    private String accessKey;

    /** 密钥（冗余） */
    private String secretKey;

    /** 密钥地域（冗余） */
    private String keyRegion;

    /** 密钥状态（冗余） */
    private String keyStatus;

    /** 删除标志（0存在 2删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

}
