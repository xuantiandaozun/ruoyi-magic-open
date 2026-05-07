package com.ruoyi.project.ai.task;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.service.IAiModelConfigService;

import lombok.extern.slf4j.Slf4j;

/**
 * OpenRouter 免费模型池同步任务。
 */
@Slf4j
@Component
public class OpenRouterFreeModelSyncTask {

    private final IAiModelConfigService modelConfigService;

    @Value("${openrouter.free-model-sync.enabled:true}")
    private boolean enabled;

    public OpenRouterFreeModelSyncTask(IAiModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @Scheduled(cron = "${openrouter.free-model-sync.cron:0 15 0/2 * * ?}")
    public void execute() {
        if (!enabled) {
            log.debug("OpenRouter 免费模型同步任务已关闭");
            return;
        }
        try {
            Map<String, Object> result = modelConfigService.syncOpenRouterFreeModels();
            log.info("OpenRouter 免费模型同步完成: {}", result);
        } catch (Exception e) {
            log.error("OpenRouter 免费模型同步失败", e);
        }
    }
}
