package com.ruoyi.project.ai.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI工作流执行记录对象 ai_workflow_execution
 * 简化版本，记录工作流的基本执行信息
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_workflow_execution")
public class AiWorkflowExecution extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 执行记录ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 工作流ID */
    @Excel(name = "工作流ID")
    @Column("workflow_id")
    private Long workflowId;

    /** 执行状态（running=运行中, completed=已完成, failed=失败） */
    @Excel(name = "执行状态")
    private String status;

    /** 输入数据（JSON格式） */
    @Column("input_data")
    private String inputData;

    /** 输出数据（JSON格式） */
    @Column("output_data")
    private String outputData;

    /** 错误信息 */
    @Column("error_message")
    private String errorMessage;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}