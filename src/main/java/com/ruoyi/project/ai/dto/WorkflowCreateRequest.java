package com.ruoyi.project.ai.dto;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 工作流创建请求对象
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public class WorkflowCreateRequest {
    
    /**
     * 工作流名称
     */
    @NotBlank(message = "工作流名称不能为空")
    private String workflowName;
    
    /**
     * 工作流描述
     */
    private String description;
    
    /**
     * 是否启用（1启用 0禁用）
     */
    @NotNull(message = "启用状态不能为空")
    private Integer enabled;
    
    /**
     * 工作流步骤列表
     */
    private List<WorkflowStepCreateRequest> steps;

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

    public List<WorkflowStepCreateRequest> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStepCreateRequest> steps) {
        this.steps = steps;
    }
}