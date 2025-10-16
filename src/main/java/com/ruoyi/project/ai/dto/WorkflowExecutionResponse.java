package com.ruoyi.project.ai.dto;

import java.util.Map;

/**
 * 工作流执行结果响应对象
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public class WorkflowExecutionResponse {
    
    /**
     * 执行ID
     */
    private Long executionId;
    
    /**
     * 工作流ID
     */
    private Long workflowId;
    
    /**
     * 执行状态（SUCCESS, FAILED, RUNNING）
     */
    private String status;
    
    /**
     * 输出数据
     */
    private Map<String, Object> outputData;
    
    /**
     * 错误信息（如果执行失败）
     */
    private String errorMessage;
    
    /**
     * 执行开始时间
     */
    private Long startTime;
    
    /**
     * 执行结束时间
     */
    private Long endTime;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long duration;

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getOutputData() {
        return outputData;
    }

    public void setOutputData(Map<String, Object> outputData) {
        this.outputData = outputData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}