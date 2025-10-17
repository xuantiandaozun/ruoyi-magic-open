package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiWorkflowScheduleLog;

/**
 * AI工作流定时调度日志Service接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiWorkflowScheduleLogService extends IService<AiWorkflowScheduleLog> {
    
    /**
     * 根据调度ID查询日志列表
     * 
     * @param scheduleId 调度ID
     * @return 日志列表
     */
    List<AiWorkflowScheduleLog> listByScheduleId(Long scheduleId);
    
    /**
     * 根据工作流ID查询日志列表
     * 
     * @param workflowId 工作流ID
     * @return 日志列表
     */
    List<AiWorkflowScheduleLog> listByWorkflowId(Long workflowId);
    
    /**
     * 根据执行状态查询日志列表
     * 
     * @param status 执行状态
     * @return 日志列表
     */
    List<AiWorkflowScheduleLog> listByStatus(String status);
    
    /**
     * 根据触发类型查询日志列表
     * 
     * @param triggerType 触发类型
     * @return 日志列表
     */
    List<AiWorkflowScheduleLog> listByTriggerType(String triggerType);
    
    /**
     * 记录调度开始
     * 
     * @param scheduleId 调度ID
     * @param workflowId 工作流ID
     * @param triggerType 触发类型
     * @param inputData 输入数据
     * @return 日志ID
     */
    Long logScheduleStart(Long scheduleId, Long workflowId, String triggerType, String inputData);
    
    /**
     * 记录调度完成
     * 
     * @param logId 日志ID
     * @param executionId 执行ID
     * @param status 执行状态
     * @param resultMessage 结果信息
     * @param outputData 输出数据
     */
    void logScheduleComplete(Long logId, Long executionId, String status, String resultMessage, String outputData);
    
    /**
     * 记录调度失败
     * 
     * @param logId 日志ID
     * @param status 执行状态
     * @param errorMessage 错误信息
     */
    void logScheduleError(Long logId, String status, String errorMessage);
    
    /**
     * 清理过期日志
     * 
     * @param days 保留天数
     * @return 清理数量
     */
    int cleanExpiredLogs(int days);
}