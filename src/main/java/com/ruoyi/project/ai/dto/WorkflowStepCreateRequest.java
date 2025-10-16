package com.ruoyi.project.ai.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 工作流步骤创建请求对象
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public class WorkflowStepCreateRequest {
    
    /**
     * 步骤名称
     */
    @NotBlank(message = "步骤名称不能为空")
    private String stepName;
    
    /**
     * 步骤描述
     */
    private String description;
    
    /**
     * 步骤顺序
     */
    @NotNull(message = "步骤顺序不能为空")
    private Integer stepOrder;
    
    /**
     * AI模型配置ID
     */
    @NotNull(message = "AI模型配置ID不能为空")
    private Long modelConfigId;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 输入变量名（多个用逗号分隔）
     */
    private String inputVariables;
    
    /**
     * 输出变量名（多个用逗号分隔）
     */
    private String outputVariables;
    
    /**
     * 是否启用（1启用 0禁用）
     */
    @NotNull(message = "启用状态不能为空")
    private Integer enabled;
    
    /**
     * 配置JSON（额外参数）
     */
    private String configJson;
    
    /**
     * 工具类型（如github_trending、database_query等）
     */
    private String toolType;
    
    /**
     * 工具参数JSON（存储工具执行所需的参数）
     */
    private String toolParameters;
    
    /**
     * 是否启用工具（Y=启用工具 N=不启用工具，默认为N）
     */
    private String toolEnabled;

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public Long getModelConfigId() {
        return modelConfigId;
    }

    public void setModelConfigId(Long modelConfigId) {
        this.modelConfigId = modelConfigId;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getInputVariables() {
        return inputVariables;
    }

    public void setInputVariables(String inputVariables) {
        this.inputVariables = inputVariables;
    }

    public String getOutputVariables() {
        return outputVariables;
    }

    public void setOutputVariables(String outputVariables) {
        this.outputVariables = outputVariables;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public String getToolParameters() {
        return toolParameters;
    }

    public void setToolParameters(String toolParameters) {
        this.toolParameters = toolParameters;
    }

    public String getToolEnabled() {
        return toolEnabled;
    }

    public void setToolEnabled(String toolEnabled) {
        this.toolEnabled = toolEnabled;
    }
}