package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiWorkflowStep;

/**
 * AI工作流步骤Service接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiWorkflowStepService extends IService<AiWorkflowStep> {
    
    /**
     * 根据工作流ID查询步骤列表（按顺序排序）
     * 
     * @param workflowId 工作流ID
     * @return AI工作流步骤集合
     */
    List<AiWorkflowStep> selectByWorkflowId(Long workflowId);
    
    /**
     * 根据工作流ID和启用状态查询步骤列表
     * 
     * @param workflowId 工作流ID
     * @param enabled 启用状态
     * @return 步骤列表
     */
    List<AiWorkflowStep> selectByWorkflowIdAndEnabled(Long workflowId, String enabled);
}