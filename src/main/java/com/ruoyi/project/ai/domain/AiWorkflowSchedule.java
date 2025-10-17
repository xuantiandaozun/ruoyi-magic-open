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
 * AI工作流定时调度配置对象 ai_workflow_schedule
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_workflow_schedule")
public class AiWorkflowSchedule extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 调度ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 工作流ID */
    @Excel(name = "工作流ID")
    @Column("workflow_id")
    private Long workflowId;

    /** 调度名称 */
    @Excel(name = "调度名称")
    @Column("schedule_name")
    private String scheduleName;

    /** cron表达式 */
    @Excel(name = "cron表达式")
    @Column("cron_expression")
    private String cronExpression;

    /** 是否启用（Y=是 N=否） */
    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    /** 状态（0正常 1暂停） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=暂停")
    private String status;

    /** 输入数据模板（JSON格式） */
    @Column("input_data_template")
    private String inputDataTemplate;

    /** 执行超时时间（秒） */
    @Excel(name = "执行超时时间")
    @Column("execution_timeout")
    private Integer executionTimeout;

    /** 重试次数 */
    @Excel(name = "重试次数")
    @Column("retry_count")
    private Integer retryCount;

    /** 最大执行次数（0表示无限制） */
    @Excel(name = "最大执行次数")
    @Column("max_execution_count")
    private Integer maxExecutionCount;

    /** 调度开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "调度开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Column("schedule_start_time")
    private Date scheduleStartTime;

    /** 调度结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "调度结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Column("schedule_end_time")
    private Date scheduleEndTime;

    /** 上次执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "上次执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Column("last_execution_time")
    private Date lastExecutionTime;

    /** 下次执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "下次执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Column("next_execution_time")
    private Date nextExecutionTime;

    /** 错误处理策略（1=立即执行 2=执行一次 3=放弃执行） */
    @Excel(name = "错误处理策略", readConverterExp = "1=立即执行,2=执行一次,3=放弃执行")
    @Column("misfire_policy")
    private String misfirePolicy;

    /** 是否允许并发执行（Y=是 N=否） */
    @Excel(name = "是否允许并发执行", readConverterExp = "Y=是,N=否")
    private String concurrent;

    /** 优先级（数字越大优先级越高） */
    @Excel(name = "优先级")
    private Integer priority;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    /** 创建者用户ID */
    @Column("user_id")
    private Long userId;

    /** 工作流名称（关联查询字段） */
    private String workflowName;

    /** 执行次数统计 */
    private Integer executionCount;
}