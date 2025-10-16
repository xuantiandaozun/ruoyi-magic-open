package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiWorkflowExecution;

/**
 * AI工作流执行记录Service接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiWorkflowExecutionService extends IService<AiWorkflowExecution> {
    
    /**
     * 根据工作流ID查询执行记录列表
     * 
     * @param workflowId 工作流ID
     * @return 执行记录列表
     */
    List<AiWorkflowExecution> listByWorkflowId(Long workflowId);
    
    /**
     * 根据状态查询执行记录列表
     * 
     * @param status 执行状态
     * @return 执行记录列表
     */
    List<AiWorkflowExecution> listByStatus(String status);
    
    /**
     * 根据工作流ID和状态查询执行记录列表
     * 
     * @param workflowId 工作流ID
     * @param status 执行状态
     * @return 执行记录列表
     */
    List<AiWorkflowExecution> listByWorkflowIdAndStatus(Long workflowId, String status);
}