package com.ruoyi.project.miniapp.service.impl;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TranslateTaskRecoveryStarter {

    private final TranslateTaskServiceImpl translateTaskService;

    public TranslateTaskRecoveryStarter(TranslateTaskServiceImpl translateTaskService) {
        this.translateTaskService = translateTaskService;
    }

    @Async("threadPoolTaskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void recoverTasksOnStartup() {
        try {
            int recovered = translateTaskService.retryRecoverableTasksOnStartup();
            if (recovered > 0) {
                log.info("应用启动后自动恢复翻译任务完成，重试 {} 个未完成任务", recovered);
            } else {
                log.info("应用启动后未发现需要恢复的翻译任务");
            }
        } catch (Exception e) {
            log.error("应用启动后恢复翻译任务失败: {}", e.getMessage(), e);
        }
    }
}
