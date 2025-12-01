package com.ruoyi.project.ai.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * 工作流调度初始化服务
 * 
 * 注意：此服务已废弃，AI工作流调度任务的初始化已统一合并到 BatchInitializationService 中
 * 保留此类仅用于兼容性，实际不再执行任何初始化逻辑
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 * @deprecated 已由 BatchInitializationService 统一处理
 */
@Service
public class WorkflowScheduleInitService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowScheduleInitService.class);
    
    @Override
    public void run(String... args) throws Exception {
        // 不再执行任何初始化逻辑
        // AI工作流调度任务的初始化已统一在 BatchInitializationService.initializeJobsWithData() 中完成
        log.info("WorkflowScheduleInitService 已废弃，AI工作流调度任务由 BatchInitializationService 统一初始化");
    }
}