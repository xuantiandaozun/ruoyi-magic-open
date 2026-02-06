package com.ruoyi.project.monitor.domain;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;

import lombok.Data;

/**
 * 域名证书监控对象 domain_cert_monitor
 * 
 * @author ruoyi
 * @date 2025-12-04
 */
@Data
@Table("domain_cert_monitor")
public class DomainCertMonitor implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 域名 */
    @Excel(name = "域名")
    private String domain;

    /** 端口号，默认443 */
    @Excel(name = "端口号")
    private Integer port;

    /** 证书颁发者 */
    @Excel(name = "证书颁发者")
    private String issuer;

    /** 证书主体 */
    @Excel(name = "证书主体")
    private String subject;

    /** 证书过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "证书过期时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 剩余天数 */
    @Excel(name = "剩余天数")
    private Integer daysRemaining;

    /** 状态（0-正常 1-即将过期 2-已过期 3-检测失败） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=即将过期,2=已过期,3=检测失败")
    private String status;

    /** 最后检测时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后检测时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheckTime;

    /** 最后通知时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastNotifyTime;

    /** 是否开启通知（0-关闭 1-开启） */
    @Excel(name = "是否开启通知", readConverterExp = "0=关闭,1=开启")
    private String notifyEnabled;

    /** 提前多少天通知 */
    @Excel(name = "提前通知天数")
    private Integer notifyDays;

    /** 错误信息 */
    private String errorMessage;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @Column(onInsertValue = "now()")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    /** 飞书多维表格记录ID（不持久化到数据库） */
    @Column(ignore = true)
    private transient String feishuRecordId;

    /** 状态常量 */
    @Column(ignore = true)
    public static final String STATUS_NORMAL = "0";
    @Column(ignore = true)
    public static final String STATUS_EXPIRING = "1";
    @Column(ignore = true)
    public static final String STATUS_EXPIRED = "2";
    @Column(ignore = true)
    public static final String STATUS_CHECK_FAILED = "3";
}
