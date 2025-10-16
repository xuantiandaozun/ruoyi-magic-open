package com.ruoyi.project.ai.service;

import java.util.Map;

import com.ruoyi.project.ai.dto.WorkflowExecuteRequest;

/**
 * 工作流执行服务接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IWorkflowExecutionService {
    
    /**
     * 执行工作流
     * 
     * @param request 执行请求
     * @return 执行结果
     */
    Map<String, Object> executeWorkflow(WorkflowExecuteRequest request);
    
    /**
     * 根据工作流ID执行工作流
     * 
     * @param workflowId 工作流ID
     * @param inputData 输入数据
     * @return 执行结果
     */
    Map<String, Object> executeWorkflow(Long workflowId, Map<String, Object> inputData);
}