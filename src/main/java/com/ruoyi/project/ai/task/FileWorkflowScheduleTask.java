package com.ruoyi.project.ai.task;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ruoyi.framework.redis.RedisLock;
import com.ruoyi.project.ai.service.FileWorkflowRunLogService;
import com.ruoyi.project.ai.workflow.FileWorkflowEngine;
import com.ruoyi.project.ai.workflow.FileWorkflowDefinitionLoader;
import com.ruoyi.project.ai.workflow.definition.FileWorkflowDefinition;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件化工作流定时任务入口。
 */
@Slf4j
@Component("fileWorkflowScheduleTask")
public class FileWorkflowScheduleTask {

    private final FileWorkflowEngine fileWorkflowEngine;
    private final FileWorkflowDefinitionLoader definitionLoader;
    private final FileWorkflowRunLogService runLogService;
    private final RedisLock redisLock;

    public FileWorkflowScheduleTask(FileWorkflowEngine fileWorkflowEngine,
            FileWorkflowDefinitionLoader definitionLoader,
            FileWorkflowRunLogService runLogService,
            RedisLock redisLock) {
        this.fileWorkflowEngine = fileWorkflowEngine;
        this.definitionLoader = definitionLoader;
        this.runLogService = runLogService;
        this.redisLock = redisLock;
    }

    public void execute(String workflowKey) {
        executeInternal(workflowKey, "scheduled");
    }

    public void executeManual(String workflowKey) {
        executeInternal(workflowKey, "manual");
    }

    private void executeInternal(String workflowKey, String triggerType) {
        long startTime = System.currentTimeMillis();
        String lockKey = "file_workflow_" + workflowKey;
        String lockValue = UUID.randomUUID().toString();

        if (!redisLock.tryLock(lockKey, lockValue, 7200)) {
            log.warn("文件化工作流正在执行中，跳过本次调度: workflow={}", workflowKey);
            return;
        }

        Long logId = null;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("triggerType", triggerType);
            params.put("executionTime", new Date());
            FileWorkflowDefinition definition = definitionLoader.findByKey(workflowKey).orElse(null);
            Long workflowId = definition != null && definition.getLegacyWorkflowIds() != null
                    && !definition.getLegacyWorkflowIds().isEmpty()
                            ? definition.getLegacyWorkflowIds().get(0)
                            : null;
            logId = runLogService.logStart(workflowKey, workflowId, triggerType, params);
            log.info("开始执行文件化工作流任务: workflow={}, triggerType={}", workflowKey, triggerType);
            Map<String, Object> result = fileWorkflowEngine.executeByKey(workflowKey, params);
            runLogService.logComplete(logId, result, System.currentTimeMillis() - startTime);
            log.info("文件化工作流任务执行完成: workflow={}, triggerType={}, durationMs={}, result={}",
                    workflowKey, triggerType, System.currentTimeMillis() - startTime, JSONUtil.toJsonStr(result));
        } catch (Exception e) {
            runLogService.logFailed(logId, e.getMessage(), System.currentTimeMillis() - startTime);
            log.error("文件化工作流任务执行失败: workflow={}, triggerType={}, durationMs={}",
                    workflowKey, triggerType, System.currentTimeMillis() - startTime, e);
        } finally {
            redisLock.unlock(lockKey, lockValue);
        }
    }
}
