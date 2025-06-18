package com.ruoyi.project.gen.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 异步任务信息
 */
@Data
public class AsyncTaskInfo {
    
    /** 任务ID */
    private String taskId;
    
    /** 任务状态：PENDING-待处理, RUNNING-执行中, SUCCESS-成功, FAILED-失败 */
    private String status;
    
    /** 任务类型 */
    private String taskType;
    
    /** 任务描述 */
    private String description;
    
    /** 进度百分比 */
    private Integer progress;
    
    /** 结果信息 */
    private String result;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 创建人 */
    private String createBy;
    
    /** 扩展信息（JSON格式） */
    private String extraInfo;
    
    public static AsyncTaskInfo createPendingTask(String taskId, String taskType, String description, String createBy) {
        AsyncTaskInfo taskInfo = new AsyncTaskInfo();
        taskInfo.setTaskId(taskId);
        taskInfo.setStatus("PENDING");
        taskInfo.setTaskType(taskType);
        taskInfo.setDescription(description);
        taskInfo.setProgress(0);
        taskInfo.setCreateTime(LocalDateTime.now());
        taskInfo.setCreateBy(createBy);
        return taskInfo;
    }
    
    public void markAsRunning() {
        this.status = "RUNNING";
        this.startTime = LocalDateTime.now();
    }
    
    public void markAsSuccess(String result) {
        this.status = "SUCCESS";
        this.result = result;
        this.progress = 100;
        this.endTime = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
    }
    
    public void updateProgress(Integer progress) {
        this.progress = progress;
    }
}
