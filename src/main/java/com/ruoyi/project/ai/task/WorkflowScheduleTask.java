package com.ruoyi.project.ai.task;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ruoyi.framework.redis.RedisLock;
import com.ruoyi.project.ai.domain.AiWorkflowSchedule;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleLogService;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleService;
import com.ruoyi.project.ai.service.IWorkflowExecutionService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

/**
 * 工作流定时调度任务执行类
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component("workflowScheduleTask")
public class WorkflowScheduleTask {

    private static final Logger log = LoggerFactory.getLogger(WorkflowScheduleTask.class);

    @Autowired
    private IAiWorkflowScheduleService scheduleService;

    @Autowired
    private IAiWorkflowScheduleLogService scheduleLogService;

    @Autowired
    private IWorkflowExecutionService workflowExecutionService;

    @Autowired
    private RedisLock redisLock;

    /**
     * 执行工作流调度任务
     * 
     * @param scheduleId 调度ID
     */
    public void execute(Integer scheduleId) {
        log.info("开始执行工作流调度任务，调度ID：{}", scheduleId);
        
        // 转换为Long类型
        executeInternal(Long.valueOf(scheduleId), "scheduled");
    }
    
    /**
     * 执行工作流调度任务（内部方法）
     * 
     * @param scheduleId 调度ID
     */
    private void executeInternal(Long scheduleId) {
        executeInternal(scheduleId, "scheduled");
    }
    
    /**
     * 执行工作流调度任务（内部方法）
     * 
     * @param scheduleId 调度ID
     * @param triggerType 触发类型：scheduled-定时触发，manual-手动触发
     */
    private void executeInternal(Long scheduleId, String triggerType) {
        long startTime = System.currentTimeMillis();
        log.info("开始执行工作流调度任务，调度ID：{}，触发类型：{}", scheduleId, triggerType);

        Long logId = null;
        boolean success = false;
        String errorMessage = null;
        Map<String, Object> executionMetrics = new HashMap<>();
        
        try {
            // 记录执行开始时间
            executionMetrics.put("startTime", new Date(startTime));
            executionMetrics.put("triggerType", triggerType);
            
            // 1. 获取调度配置并进行状态检查
            long configLoadStart = System.currentTimeMillis();
            AiWorkflowSchedule schedule = scheduleService.getById(scheduleId);
            long configLoadDuration = System.currentTimeMillis() - configLoadStart;
            executionMetrics.put("configLoadDuration", configLoadDuration);
            
            if (schedule == null) {
                errorMessage = "调度配置不存在";
                log.error("调度配置不存在，调度ID：{}", scheduleId);
                return;
            }

            log.info("调度配置加载完成，调度ID：{}，工作流ID：{}，耗时：{}ms", 
                    scheduleId, schedule.getWorkflowId(), configLoadDuration);

            // 提前进行状态检查，避免不必要的处理
            if (!"Y".equals(schedule.getEnabled()) || !"0".equals(schedule.getStatus())) {
                log.warn("调度任务已禁用或暂停，调度ID：{}，状态：enabled={}, status={}", 
                    scheduleId, schedule.getEnabled(), schedule.getStatus());
                return;
            }

            // 2. 检查执行次数限制
            if (schedule.getMaxExecutionCount() != null && schedule.getMaxExecutionCount() > 0) {
                Integer executionCount = schedule.getExecutionCount() != null ? schedule.getExecutionCount() : 0;
                if (executionCount >= schedule.getMaxExecutionCount()) {
                    log.warn("已达到最大执行次数限制，调度ID：{}，当前执行次数：{}，最大执行次数：{}", 
                        scheduleId, executionCount, schedule.getMaxExecutionCount());
                    
                    // 自动禁用调度
                    schedule.setEnabled("N");
                    scheduleService.updateById(schedule);
                    
                    log.info("调度任务已自动禁用，调度ID：{}", scheduleId);
                    return;
                }
            }

            // 3. 创建执行日志记录
            long logCreateStart = System.currentTimeMillis();
            logId = scheduleLogService.logScheduleStart(scheduleId, schedule.getWorkflowId(), triggerType, null);
            long logCreateDuration = System.currentTimeMillis() - logCreateStart;
            executionMetrics.put("logCreateDuration", logCreateDuration);
            
            log.info("执行日志创建完成，调度ID：{}，日志ID：{}，耗时：{}ms", 
                    scheduleId, logId, logCreateDuration);

            // 4. 准备执行参数
            Map<String, Object> executionParams = new HashMap<>();
            
            // 添加调度相关参数
            executionParams.put("scheduleId", scheduleId);
            executionParams.put("triggerType", triggerType);
            executionParams.put("executionTime", new Date());

            // 5. 执行工作流
            long workflowExecutionStart = System.currentTimeMillis();
            log.info("开始执行工作流，调度ID：{}，工作流ID：{}，参数：{}", 
                    scheduleId, schedule.getWorkflowId(), JSONUtil.toJsonStr(executionParams));
            
            Map<String, Object> result = workflowExecutionService.executeWorkflow(
                schedule.getWorkflowId(), 
                executionParams
            );
            
            long workflowExecutionDuration = System.currentTimeMillis() - workflowExecutionStart;
            executionMetrics.put("workflowExecutionDuration", workflowExecutionDuration);
            
            success = true;
            log.info("工作流执行完成，调度ID：{}，工作流ID：{}，耗时：{}ms，结果：{}", 
                    scheduleId, schedule.getWorkflowId(), workflowExecutionDuration, 
                    JSONUtil.toJsonStr(result));

            // 6. 更新执行日志
            if (logId != null) {
                long logUpdateStart = System.currentTimeMillis();
                Long executionId = result.get("executionId") != null ? Long.valueOf(result.get("executionId").toString()) : null;
                scheduleLogService.logScheduleComplete(logId, executionId, "success", "工作流执行成功", JSONUtil.toJsonStr(result));
                long logUpdateDuration = System.currentTimeMillis() - logUpdateStart;
                executionMetrics.put("logUpdateDuration", logUpdateDuration);
                
                log.info("执行日志更新完成，调度ID：{}，日志ID：{}，耗时：{}ms", 
                        scheduleId, logId, logUpdateDuration);
            }

            // 7. 更新调度信息
            long scheduleUpdateStart = System.currentTimeMillis();
            updateScheduleAfterExecution(schedule, true);
            long scheduleUpdateDuration = System.currentTimeMillis() - scheduleUpdateStart;
            executionMetrics.put("scheduleUpdateDuration", scheduleUpdateDuration);
            
            log.info("调度信息更新完成，调度ID：{}，耗时：{}ms", scheduleId, scheduleUpdateDuration);

        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            log.error("工作流调度任务执行失败，调度ID：{}", scheduleId, e);

            // 更新执行日志（如果已创建）
            if (logId != null) {
                try {
                    scheduleLogService.logScheduleError(logId, "failed", e.getMessage());
                } catch (Exception logException) {
                    log.error("更新执行日志失败，调度ID：{}，日志ID：{}", scheduleId, logId, logException);
                }
            }

            // 更新调度信息
            try {
                AiWorkflowSchedule schedule = scheduleService.getById(scheduleId);
                if (schedule != null) {
                    updateScheduleAfterExecution(schedule, false);
                }
            } catch (Exception updateException) {
                log.error("更新调度信息失败，调度ID：{}", scheduleId, updateException);
            }
        } finally {
            // 记录总执行时间和性能指标
            long totalDuration = System.currentTimeMillis() - startTime;
            executionMetrics.put("endTime", new Date());
            executionMetrics.put("totalDuration", totalDuration);
            executionMetrics.put("success", success);
            executionMetrics.put("errorMessage", errorMessage);
            
            log.info("工作流调度任务执行完成，调度ID：{}，触发类型：{}，成功：{}，总耗时：{}ms，性能指标：{}", 
                    scheduleId, triggerType, success, totalDuration, JSONUtil.toJsonStr(executionMetrics));
        }
    }

    /**
     * 执行后更新调度信息
     */
    private void updateScheduleAfterExecution(AiWorkflowSchedule schedule, boolean success) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("开始更新调度信息，调度ID：{}，执行结果：{}", schedule.getId(), success ? "成功" : "失败");
            
            // 更新上次执行时间
            schedule.setLastExecutionTime(new Date());

            // 更新执行次数
            Integer executionCount = schedule.getExecutionCount() != null ? schedule.getExecutionCount() : 0;
            schedule.setExecutionCount(executionCount + 1);

            // 更新下次执行时间
            scheduleService.updateNextExecutionTime(schedule.getId());

            scheduleService.updateById(schedule);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("调度信息更新完成，调度ID：{}，执行次数：{}，执行结果：{}，耗时：{}ms", 
                    schedule.getId(), schedule.getExecutionCount(), 
                    success ? "成功" : "失败", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("更新调度信息失败，调度ID：{}，耗时：{}ms", schedule.getId(), duration, e);
        }
    }

    /**
     * 手动执行工作流调度任务（异步）
     * 
     * @param scheduleId 调度ID
     */
    @Async
    public void executeManually(Long scheduleId) {
        long startTime = System.currentTimeMillis();
        log.info("开始手动执行工作流调度任务，调度ID：{}", scheduleId);
        
        // 使用分布式锁防止重复执行
        String lockKey = "schedule_" + scheduleId;
        String lockValue = UUID.randomUUID().toString();
        
        // 尝试获取锁，锁过期时间设置为10分钟
        long lockAcquireStart = System.currentTimeMillis();
        if (!redisLock.tryLock(lockKey, lockValue, 600)) {
            long lockAcquireDuration = System.currentTimeMillis() - lockAcquireStart;
            log.warn("工作流调度任务正在执行中，跳过本次手动执行，调度ID：{}，锁获取耗时：{}ms", 
                    scheduleId, lockAcquireDuration);
            return;
        }
        
        long lockAcquireDuration = System.currentTimeMillis() - lockAcquireStart;
        log.info("成功获取分布式锁，开始执行工作流调度任务，调度ID：{}，锁获取耗时：{}ms", 
                scheduleId, lockAcquireDuration);
        
        try {
            // 复用定时任务的执行逻辑，但标记为手动执行
            executeInternal(scheduleId, "manual");
        } finally {
            // 确保释放锁
            long lockReleaseStart = System.currentTimeMillis();
            if (!redisLock.unlock(lockKey, lockValue)) {
                long lockReleaseDuration = System.currentTimeMillis() - lockReleaseStart;
                log.warn("释放分布式锁失败，调度ID：{}，锁可能已过期，释放耗时：{}ms", 
                        scheduleId, lockReleaseDuration);
            } else {
                long lockReleaseDuration = System.currentTimeMillis() - lockReleaseStart;
                long totalDuration = System.currentTimeMillis() - startTime;
                log.info("手动执行工作流调度任务完成，调度ID：{}，锁释放耗时：{}ms，总耗时：{}ms", 
                        scheduleId, lockReleaseDuration, totalDuration);
            }
        }
    }
}