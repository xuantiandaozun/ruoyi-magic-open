package com.ruoyi.project.ai.task;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    /**
     * 执行工作流调度任务
     * 
     * @param scheduleId 调度ID
     */
    public void execute(Long scheduleId) {
        log.info("开始执行工作流调度任务，调度ID：{}", scheduleId);

        Long logId = null;
        try {
            // 1. 获取调度配置
            AiWorkflowSchedule schedule = scheduleService.getById(scheduleId);
            if (schedule == null) {
                log.error("调度配置不存在，调度ID：{}", scheduleId);
                return;
            }

            if (!"Y".equals(schedule.getEnabled()) || !"0".equals(schedule.getStatus())) {
                log.warn("调度任务已禁用或暂停，调度ID：{}", scheduleId);
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
                    schedule.setStatus("1");
                    scheduleService.updateById(schedule);
                    return;
                }
            }

            // 3. 检查调度时间范围
            Date now = new Date();
            if (schedule.getScheduleStartTime() != null && now.before(schedule.getScheduleStartTime())) {
                log.warn("当前时间早于调度开始时间，调度ID：{}", scheduleId);
                return;
            }
            if (schedule.getScheduleEndTime() != null && now.after(schedule.getScheduleEndTime())) {
                log.warn("当前时间晚于调度结束时间，调度ID：{}", scheduleId);
                
                // 自动禁用调度
                schedule.setEnabled("N");
                schedule.setStatus("1");
                scheduleService.updateById(schedule);
                return;
            }

            // 4. 准备输入数据
            Map<String, Object> inputData = new HashMap<>();
            if (StrUtil.isNotBlank(schedule.getInputDataTemplate())) {
                try {
                    inputData = JSONUtil.toBean(schedule.getInputDataTemplate(), Map.class);
                } catch (Exception e) {
                    log.warn("解析输入数据模板失败，使用空数据，调度ID：{}", scheduleId, e);
                }
            }

            // 5. 记录调度开始日志
            logId = scheduleLogService.logScheduleStart(
                scheduleId, 
                schedule.getWorkflowId(), 
                "scheduled", 
                JSONUtil.toJsonStr(inputData)
            );

            // 6. 执行工作流
            Map<String, Object> result = workflowExecutionService.executeWorkflow(
                schedule.getWorkflowId(), 
                inputData
            );

            // 7. 记录调度完成日志
            if (logId != null) {
                scheduleLogService.logScheduleComplete(
                    logId, 
                    null, // 执行ID需要从result中获取
                    "completed", 
                    "执行成功", 
                    JSONUtil.toJsonStr(result)
                );
            }

            // 8. 更新调度信息
            updateScheduleAfterExecution(schedule, true);

            log.info("工作流调度任务执行成功，调度ID：{}", scheduleId);

        } catch (Exception e) {
            log.error("工作流调度任务执行失败，调度ID：{}", scheduleId, e);

            // 记录错误日志
            if (logId != null) {
                scheduleLogService.logScheduleError(logId, "failed", e.getMessage());
            }

            // 更新调度信息
            try {
                AiWorkflowSchedule schedule = scheduleService.getById(scheduleId);
                if (schedule != null) {
                    updateScheduleAfterExecution(schedule, false);
                }
            } catch (Exception ex) {
                log.error("更新调度信息失败，调度ID：{}", scheduleId, ex);
            }
        }
    }

    /**
     * 执行后更新调度信息
     */
    private void updateScheduleAfterExecution(AiWorkflowSchedule schedule, boolean success) {
        try {
            // 更新上次执行时间
            schedule.setLastExecutionTime(new Date());

            // 更新执行次数
            Integer executionCount = schedule.getExecutionCount() != null ? schedule.getExecutionCount() : 0;
            schedule.setExecutionCount(executionCount + 1);

            // 更新下次执行时间
            scheduleService.updateNextExecutionTime(schedule.getId());

            scheduleService.updateById(schedule);
        } catch (Exception e) {
            log.error("更新调度信息失败，调度ID：{}", schedule.getId(), e);
        }
    }

    /**
     * 手动执行工作流调度任务
     * 
     * @param scheduleId 调度ID
     */
    public void executeManually(Long scheduleId) {
        log.info("开始手动执行工作流调度任务，调度ID：{}", scheduleId);

        Long logId = null;
        try {
            // 1. 获取调度配置
            AiWorkflowSchedule schedule = scheduleService.getById(scheduleId);
            if (schedule == null) {
                log.error("调度配置不存在，调度ID：{}", scheduleId);
                return;
            }

            // 2. 准备输入数据
            Map<String, Object> inputData = new HashMap<>();
            if (StrUtil.isNotBlank(schedule.getInputDataTemplate())) {
                try {
                    inputData = JSONUtil.toBean(schedule.getInputDataTemplate(), Map.class);
                } catch (Exception e) {
                    log.warn("解析输入数据模板失败，使用空数据，调度ID：{}", scheduleId, e);
                }
            }

            // 3. 记录调度开始日志
            logId = scheduleLogService.logScheduleStart(
                scheduleId, 
                schedule.getWorkflowId(), 
                "manual", 
                JSONUtil.toJsonStr(inputData)
            );

            // 4. 执行工作流
            Map<String, Object> result = workflowExecutionService.executeWorkflow(
                schedule.getWorkflowId(), 
                inputData
            );

            // 5. 记录调度完成日志
            if (logId != null) {
                scheduleLogService.logScheduleComplete(
                    logId, 
                    null, // 执行ID需要从result中获取
                    "completed", 
                    "手动执行成功", 
                    JSONUtil.toJsonStr(result)
                );
            }

            log.info("手动执行工作流调度任务成功，调度ID：{}", scheduleId);

        } catch (Exception e) {
            log.error("手动执行工作流调度任务失败，调度ID：{}", scheduleId, e);

            // 记录错误日志
            if (logId != null) {
                scheduleLogService.logScheduleError(logId, "failed", e.getMessage());
            }
        }
    }
}