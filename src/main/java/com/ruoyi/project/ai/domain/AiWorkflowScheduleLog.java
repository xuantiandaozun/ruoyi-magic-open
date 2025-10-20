package com.ruoyi.project.ai.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * AI工作流定时调度日志对象 ai_workflow_schedule_log
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_workflow_schedule_log")
public class AiWorkflowScheduleLog extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 调度ID */
    @Excel(name = "调度ID")
    @Column("schedule_id")
    private Long scheduleId;

    /** 工作流ID */
    @Excel(name = "工作流ID")
    @Column("workflow_id")
    private Long workflowId;

    /** 执行ID */
    @Excel(name = "执行ID")
    @Column("execution_id")
    private Long executionId;

    /** 触发类型（scheduled=定时触发 manual=手动触发 retry=重试触发） */
    @Excel(name = "触发类型", readConverterExp = "scheduled=定时触发,manual=手动触发,retry=重试触发")
    @Column("trigger_type")
    private String triggerType;

    /** 计划执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "计划执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Column("scheduled_time")
    private Date scheduledTime;

    /** 实际开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "实际开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Column("actual_start_time")
    private Date actualStartTime;

    /** 实际结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "实际结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Column("actual_end_time")
    private Date actualEndTime;

    /** 执行耗时（毫秒） */
    @Excel(name = "执行耗时")
    @Column("execution_duration")
    private Long executionDuration;

    /** 执行状态（running=运行中 completed=已完成 failed=失败 timeout=超时 cancelled=已取消） */
    @Excel(name = "执行状态", readConverterExp = "running=运行中,completed=已完成,failed=失败,timeout=超时,cancelled=已取消")
    private String status;

    /** 执行结果信息 */
    @Column("result_message")
    private String resultMessage;

    /** 错误信息 */
    @Column("error_message")
    private String errorMessage;

    /** 输入数据（JSON格式） */
    @Column("input_data")
    private String inputData;

    /** 输出数据（JSON格式） */
    @Column("output_data")
    private String outputData;

    /** 重试次数 */
    @Excel(name = "重试次数")
    @Column("retry_count")
    private Integer retryCount;

    /** 最大重试次数 */
    @Excel(name = "最大重试次数")
    @Column("max_retry_count")
    private Integer maxRetryCount;

    /** 服务器信息 */
    @Column("server_info")
    private String serverInfo;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

}