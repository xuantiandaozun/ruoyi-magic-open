package com.ruoyi.project.monitor.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 系统访问记录表 sys_logininfor
 * 
 * @author ruoyi
 */
@Data
@Accessors(prefix = "")  // 避免 lombok 生成的方法带有前缀
@EqualsAndHashCode(callSuper = true)
@Table("sys_logininfor")
public class SysLogininfor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    @Excel(name = "序号")
    @Id(keyType = KeyType.Auto)
    public Long infoId;

    /** 用户账号 */
    @Excel(name = "用户账号")
    public String userName;

    /** 登录状态 0成功 1失败 */
    @Excel(name = "登录状态", readConverterExp = "0=成功,1=失败")
    public String status;

    /** 登录IP地址 */
    @Excel(name = "登录地址")
    public String ipaddr;

    /** 登录地点 */
    @Excel(name = "登录地点")
    public String loginLocation;

    /** 浏览器类型 */
    @Excel(name = "浏览器")
    public String browser;

    /** 操作系统 */
    @Excel(name = "操作系统")
    public String os;

    /** 提示消息 */
    @Excel(name = "提示消息")
    public String msg;

    /** 访问时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "访问时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    public Date loginTime;
}