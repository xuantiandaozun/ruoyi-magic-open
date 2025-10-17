package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiWorkflowSchedule;

/**
 * AI工作流定时调度Service接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiWorkflowScheduleService extends IService<AiWorkflowSchedule> {
    
    /**
     * 根据工作流ID查询调度配置列表
     * 
     * @param workflowId 工作流ID
     * @return 调度配置列表
     */
    List<AiWorkflowSchedule> listByWorkflowId(Long workflowId);
    
    /**
     * 根据启用状态查询调度配置列表
     * 
     * @param enabled 启用状态
     * @return 调度配置列表
     */
    List<AiWorkflowSchedule> listByEnabled(String enabled);
    
    /**
     * 根据状态查询调度配置列表
     * 
     * @param status 状态
     * @return 调度配置列表
     */
    List<AiWorkflowSchedule> listByStatus(String status);
    
    /**
     * 启动调度任务
     * 
     * @param scheduleId 调度ID
     * @return 是否成功
     */
    boolean startSchedule(Long scheduleId);
    
    /**
     * 暂停调度任务
     * 
     * @param scheduleId 调度ID
     * @return 是否成功
     */
    boolean pauseSchedule(Long scheduleId);
    
    /**
     * 恢复调度任务
     * 
     * @param scheduleId 调度ID
     * @return 是否成功
     */
    boolean resumeSchedule(Long scheduleId);
    
    /**
     * 删除调度任务
     * 
     * @param scheduleId 调度ID
     * @return 是否成功
     */
    boolean deleteSchedule(Long scheduleId);
    
    /**
     * 立即执行一次调度任务
     * 
     * @param scheduleId 调度ID
     * @return 是否成功
     */
    boolean executeOnce(Long scheduleId);
    
    /**
     * 更新下次执行时间
     * 
     * @param scheduleId 调度ID
     */
    void updateNextExecutionTime(Long scheduleId);
    
    /**
     * 获取所有启用的调度配置（用于系统启动时初始化）
     * 
     * @return 启用的调度配置列表
     */
    List<AiWorkflowSchedule> listEnabledSchedules();
}