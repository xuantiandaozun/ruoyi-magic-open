package com.ruoyi.project.ai.dto;

import javax.validation.constraints.NotNull;

/**
 * 工作流更新请求对象
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public class WorkflowUpdateRequest {
    
    /**
     * 工作流ID
     */
    @NotNull(message = "工作流ID不能为空")
    private Long id;
    
    /**
     * 工作流名称
     */
    private String workflowName;
    
    /**
     * 工作流描述
     */
    private String description;
    
    /**
     * 是否启用（1启用 0禁用）
     */
    private Integer enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}