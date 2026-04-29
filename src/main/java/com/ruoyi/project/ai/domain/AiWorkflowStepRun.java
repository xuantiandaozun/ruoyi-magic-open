package com.ruoyi.project.ai.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI工作流步骤执行记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_workflow_step_run")
public class AiWorkflowStepRun extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("execution_id")
    private Long executionId;

    @Column("workflow_key")
    private String workflowKey;

    @Column("step_key")
    private String stepKey;

    @Column("step_name")
    private String stepName;

    @Column("step_order")
    private Integer stepOrder;

    @Column("model_config_id")
    private Long modelConfigId;

    private String status;

    @Column("attempt_count")
    private Integer attemptCount;

    @Column("duration_ms")
    private Long durationMs;

    @Column("input_snapshot")
    private String inputSnapshot;

    @Column("output_snapshot")
    private String outputSnapshot;

    @Column("error_message")
    private String errorMessage;

    @Column(isLogicDelete = true)
    private String delFlag;
}
