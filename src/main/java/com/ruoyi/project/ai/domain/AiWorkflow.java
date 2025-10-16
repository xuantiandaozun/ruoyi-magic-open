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
 * AI工作流配置对象 ai_workflow
 * 基于LangChain4j的顺序工作流模式
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_workflow")
public class AiWorkflow extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 工作流ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 工作流名称 */
    @Excel(name = "工作流名称")
    @Column("workflow_name")
    private String name;

    /** 工作流描述 */
    @Excel(name = "工作流描述")
    @Column("workflow_description")
    private String description;

    /** 工作流类型（sequential=顺序工作流） */
    @Excel(name = "工作流类型")
    @Column("workflow_type")
    private String type;

    /** 工作流版本 */
    @Excel(name = "版本")
    @Column("workflow_version")
    private String version;

    /** 是否启用 */
    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    /** 创建者用户ID */
    @Column("user_id")
    private Long userId;

    /** 工作流配置JSON（存储额外的配置参数） */
    @Column("config_json")
    private String configJson;
}