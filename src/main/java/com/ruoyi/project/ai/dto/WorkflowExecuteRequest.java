package com.ruoyi.project.ai.dto;

import java.util.Map;

/**
 * 工作流执行请求对象
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public class WorkflowExecuteRequest {
    
    /** 工作流ID */
    private Long workflowId;
    
    /** 输入数据（键值对形式，会存储到AgenticScope中） */
    private Map<String, Object> inputData;
    
    /** 是否异步执行 */
    private Boolean async = false;

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public Map<String, Object> getInputData() {
        return inputData;
    }

    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }
}