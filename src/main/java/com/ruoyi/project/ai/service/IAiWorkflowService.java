package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiWorkflow;

/**
 * AI工作流Service接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiWorkflowService extends IService<AiWorkflow> {
    
    /**
     * 根据启用状态查询工作流列表
     * 
     * @param enabled 启用状态
     * @return 工作流列表
     */
    List<AiWorkflow> listByEnabled(String enabled);
    
    /**
     * 根据名称查询工作流
     * 
     * @param name 工作流名称
     * @return 工作流
     */
    AiWorkflow getByName(String name);
}