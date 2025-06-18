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
 * 定时任务调度日志表 sys_job_log
 * 
 * @author ruoyi
 */
@Data
@Accessors(prefix = "")
@EqualsAndHashCode(callSuper = true)
@Table("sys_job_log")
public class SysJobLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    @Excel(name = "日志序号")
    @Id(keyType = KeyType.Auto)
    public Long jobLogId;

    /** 任务名称 */
    @Excel(name = "任务名称")
    public String jobName;

    /** 任务组名 */
    @Excel(name = "任务组名")
    public String jobGroup;

    /** 调用目标字符串 */
    @Excel(name = "调用目标字符串")
    public String invokeTarget;

    /** 日志信息 */
    @Excel(name = "日志信息")
    public String jobMessage;

    /** 执行状态（0正常 1失败） */
    @Excel(name = "执行状态", readConverterExp = "0=正常,1=失败")
    public String status;

    /** 异常信息 */
    @Excel(name = "异常信息")
    public String exceptionInfo;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date startTime;

    /** 停止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date stopTime;
}
