package com.ruoyi.project.ai.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI工具调用记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_tool_run")
public class AiToolRun extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("execution_id")
    private Long executionId;

    @Column("step_run_id")
    private Long stepRunId;

    @Column("workflow_key")
    private String workflowKey;

    @Column("step_key")
    private String stepKey;

    @Column("tool_name")
    private String toolName;

    private String status;

    @Column("duration_ms")
    private Long durationMs;

    @Column("arguments_json")
    private String argumentsJson;

    @Column("result_json")
    private String resultJson;

    @Column("error_message")
    private String errorMessage;

    @Column(isLogicDelete = true)
    private String delFlag;
}
