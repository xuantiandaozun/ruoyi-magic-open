package com.ruoyi.project.ai.service.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.ruoyi.project.ai.domain.AiWorkflowSchedule;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleService;
import com.ruoyi.project.monitor.domain.SysJob;
import com.ruoyi.project.monitor.service.ISysJobService;

/**
 * 工作流调度初始化服务
 * 系统启动时自动加载所有启用的调度任务
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
public class WorkflowScheduleInitService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowScheduleInitService.class);
    
    // 防止重复初始化的标志
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    @Autowired
    private IAiWorkflowScheduleService scheduleService;

    @Autowired
    private ISysJobService sysJobService;

    @Override
    public void run(String... args) throws Exception {
        // 幂等性检查：防止重复初始化
        if (!initialized.compareAndSet(false, true)) {
            log.warn("工作流调度任务已经初始化过，跳过重复初始化");
            return;
        }

        log.info("开始初始化工作流调度任务...");

        try {
            long startTime = System.currentTimeMillis();
            log.info("开始初始化AI工作流调度任务...");
            
            // 获取所有启用的调度配置
            List<AiWorkflowSchedule> enabledSchedules = scheduleService.listEnabledSchedules();
            log.info("发现 {} 个启用的AI工作流调度任务", enabledSchedules.size());
            
            if (enabledSchedules.isEmpty()) {
                log.info("没有启用的AI工作流调度任务需要初始化");
                return;
            }

            int successCount = 0;
            int failCount = 0;
            int skipCount = 0;

            // 逐个启动调度任务
            for (AiWorkflowSchedule schedule : enabledSchedules) {
                long taskStartTime = System.currentTimeMillis();
                try {
                    // 创建任务名称
                    String jobName = "WORKFLOW_SCHEDULE_" + schedule.getId();
                    String jobGroup = "AI_WORKFLOW";
                    
                    log.debug("正在处理调度任务：ID={}, Name={}, CronExpression={}", 
                        schedule.getId(), schedule.getScheduleName(), schedule.getCronExpression());
                    
                    // 检查任务是否已存在
                    SysJob existingJob = sysJobService.selectJobByName(jobName);
                    if (existingJob != null) {
                        log.debug("调度任务已存在，跳过注册：{} (状态：{})", jobName, existingJob.getStatus());
                        skipCount++;
                        continue;
                    }
                    
                    // 创建新的Quartz任务
                    SysJob job = scheduleService.createQuartzJobForInit(schedule);
                    log.debug("创建Quartz任务对象：{}", jobName);
                    
                    // 插入任务到数据库
                    sysJobService.insertJob(job);
                    log.debug("任务已插入数据库：{}", jobName);
                    
                    // 创建Quartz调度
                    sysJobService.createScheduleJob(job);
                    log.debug("Quartz调度任务已创建：{}", jobName);
                    
                    // 更新下次执行时间
                    scheduleService.updateNextExecutionTime(schedule.getId());
                    
                    long taskEndTime = System.currentTimeMillis();
                    successCount++;
                    log.info("AI工作流调度任务初始化成功：{} (耗时：{}ms)", jobName, taskEndTime - taskStartTime);
                        
                } catch (Exception e) {
                    failCount++;
                    log.error("AI工作流调度任务初始化失败，调度ID：{}，调度名称：{}，错误信息：{}", 
                        schedule.getId(), schedule.getScheduleName(), e.getMessage(), e);
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("AI工作流调度任务初始化完成，总耗时：{}ms，成功：{}，跳过：{}，失败：{}，总计：{}", 
                endTime - startTime, successCount, skipCount, failCount, enabledSchedules.size());
            
            // 记录初始化统计信息
            if (failCount > 0) {
                log.warn("存在 {} 个调度任务初始化失败，请检查相关配置", failCount);
            }
            
            if (successCount > 0) {
                log.info("成功初始化 {} 个AI工作流调度任务，系统已准备就绪", successCount);
            }

        } catch (Exception e) {
            log.error("AI工作流调度任务初始化过程中发生严重错误", e);
            // 重置初始化标志，允许下次重试
            initialized.set(false);
            throw new RuntimeException("AI工作流调度任务初始化失败，系统已重置为未初始化状态", e);
        }
    }
    
    /**
     * 重置初始化状态（用于测试或手动重新初始化）
     */
    public void resetInitializationState() {
        initialized.set(false);
        log.info("工作流调度初始化状态已重置");
    }
}