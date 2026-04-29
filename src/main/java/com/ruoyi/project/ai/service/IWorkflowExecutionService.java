package com.ruoyi.project.ai.service;

import java.util.Map;

/**
 * 工作流执行服务接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IWorkflowExecutionService {

    /**
     * 根据工作流ID执行工作流
     * 
     * @param workflowId 工作流ID
     * @param inputData  输入数据
     * @return 执行结果
     */
    Map<String, Object> executeWorkflow(Long workflowId, Map<String, Object> inputData);
}
