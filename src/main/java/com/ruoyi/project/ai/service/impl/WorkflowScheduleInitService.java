package com.ruoyi.project.ai.service.impl;

import java.util.List;

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

    @Autowired
    private IAiWorkflowScheduleService scheduleService;

    @Autowired
    private ISysJobService sysJobService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化工作流调度任务...");

        try {
            // 获取所有启用的调度配置
            List<AiWorkflowSchedule> enabledSchedules = scheduleService.listEnabledSchedules();
            
            if (enabledSchedules.isEmpty()) {
                log.info("没有找到启用的工作流调度任务");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            // 逐个启动调度任务
            for (AiWorkflowSchedule schedule : enabledSchedules) {
                try {
                    // 先创建Quartz任务
                    SysJob job = scheduleService.createQuartzJobForInit(schedule);
                    sysJobService.insertJob(job);
                    sysJobService.createScheduleJob(job);
                    
                    // 更新下次执行时间
                    scheduleService.updateNextExecutionTime(schedule.getId());
                    
                    successCount++;
                    log.info("启动工作流调度任务成功：{} (ID: {})", 
                        schedule.getScheduleName(), schedule.getId());
                } catch (Exception e) {
                    failCount++;
                    log.error("启动工作流调度任务异常：{} (ID: {})", 
                        schedule.getScheduleName(), schedule.getId(), e);
                }
            }

            log.info("工作流调度任务初始化完成，总数：{}，成功：{}，失败：{}", 
                enabledSchedules.size(), successCount, failCount);

        } catch (Exception e) {
            log.error("初始化工作流调度任务失败", e);
        }
    }
}