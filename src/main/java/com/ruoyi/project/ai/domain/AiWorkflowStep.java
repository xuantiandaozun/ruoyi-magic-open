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
 * AI工作流步骤配置对象 ai_workflow_step
 * 定义工作流中每个AI代理的配置
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_workflow_step")
public class AiWorkflowStep extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 步骤ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 工作流ID */
    @Excel(name = "工作流ID")
    @Column("workflow_id")
    private Long workflowId;

    /** 步骤名称 */
    @Excel(name = "步骤名称")
    @Column("step_name")
    private String stepName;

    /** 步骤描述 */
    @Excel(name = "步骤描述")
    @Column("step_description")
    private String description;

    /** 步骤顺序（执行顺序） */
    @Excel(name = "执行顺序")
    @Column("step_order")
    private Integer stepOrder;

    /** AI模型配置ID */
    @Excel(name = "模型配置ID")
    @Column("model_config_id")
    private Long modelConfigId;

    /** 系统提示词 */
    @Column("system_prompt")
    private String systemPrompt;

    /** 输入变量名（从AgenticScope中读取的变量名） */
    @Excel(name = "输入变量名")
    @Column("input_variable")
    private String inputVariable;

    /** 输出变量名（存储到AgenticScope中的变量名） */
    @Excel(name = "输出变量名")
    @Column("output_variable")
    private String outputVariable;

    /** 是否启用 */
    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    /** 步骤配置JSON（存储额外的配置参数） */
    @Column("config_json")
    private String configJson;

    /** 工具类型（如github_trending、database_query等） */
    @Excel(name = "工具类型")
    @Column("tool_type")
    private String toolType;

    /** 工具参数JSON（存储工具执行所需的参数） */
    @Column("tool_parameters")
    private String toolParameters;

    /** 是否启用工具（Y=启用工具 N=不启用工具，默认为N） */
    @Excel(name = "是否启用工具", readConverterExp = "Y=是,N=否")
    @Column("tool_enabled")
    private String toolEnabled;
}