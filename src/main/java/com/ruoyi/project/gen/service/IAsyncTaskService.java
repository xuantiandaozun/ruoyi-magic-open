package com.ruoyi.project.gen.service;

import com.ruoyi.project.gen.domain.AsyncTaskInfo;

/**
 * 异步任务服务接口
 */
public interface IAsyncTaskService {
    
    /**
     * 保存任务信息
     * 
     * @param taskInfo 任务信息
     */
    void saveTask(AsyncTaskInfo taskInfo);
    
    /**
     * 根据任务ID获取任务信息
     * 
     * @param taskId 任务ID
     * @return 任务信息
     */
    AsyncTaskInfo getTask(String taskId);
    
    /**
     * 更新任务状态
     * 
     * @param taskId 任务ID
     * @param status 任务状态
     */
    void updateTaskStatus(String taskId, String status);
    
    /**
     * 更新任务进度
     * 
     * @param taskId 任务ID
     * @param progress 任务进度
     */
    void updateTaskProgress(String taskId, int progress);
    
    /**
     * 更新任务结果
     * 
     * @param taskId 任务ID
     * @param result 任务结果
     */
    void updateTaskResult(String taskId, String result);
    
    /**
     * 更新任务错误信息
     * 
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void updateTaskError(String taskId, String errorMessage);
    
    /**
     * 更新任务扩展信息
     * 
     * @param taskId 任务ID
     * @param extraInfo 扩展信息
     */
    void updateTaskExtraInfo(String taskId, String extraInfo);
    
    /**
     * 删除任务
     * 
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);
    
    /**
     * 检查任务是否存在
     * 
     * @param taskId 任务ID
     * @return 是否存在
     */
    boolean taskExists(String taskId);
}
