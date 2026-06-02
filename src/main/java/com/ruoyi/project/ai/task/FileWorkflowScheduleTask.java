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

    /** 工作流最长预期执行时间（秒），超时后视为僵死并自动清理 */
    private static final int WORKFLOW_LOCK_SECONDS = 5400;

    /** running 日志超过该分钟数仍未结束，视为僵死（正常执行约 10-25 分钟） */
    private static final int STALE_RUNNING_MINUTES = 30;

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

    /**
     * 人工释放工作流执行锁，并结束卡住的 running 日志。
     */
    public Map<String, Object> forceRelease(String workflowKey) {
        String lockKey = buildLockKey(workflowKey);
        boolean lockReleased = false;
        if (redisLock.isLocked(lockKey)) {
            lockReleased = redisLock.forceUnlock(lockKey);
        }
        int failedLogs = runLogService.forceFailRunningLogs(workflowKey,
                "人工释放工作流锁，上次执行可能异常中断");

        Map<String, Object> result = new HashMap<>();
        result.put("workflowKey", workflowKey);
        result.put("lockReleased", lockReleased);
        result.put("failedRunningLogs", failedLogs);
        log.warn("人工释放文件化工作流锁: workflow={}, lockReleased={}, failedRunningLogs={}",
                workflowKey, lockReleased, failedLogs);
        return result;
    }

    private void executeInternal(String workflowKey, String triggerType) {
        long startTime = System.currentTimeMillis();
        String lockKey = buildLockKey(workflowKey);
        recoverStaleExecution(workflowKey, lockKey);

        String lockValue = UUID.randomUUID().toString();
        if (!tryAcquireWorkflowLock(lockKey, lockValue, workflowKey)) {
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

    /**
     * 清理僵死的 Redis 锁和 running 日志，避免进程异常退出后无法再次执行。
     */
    private void recoverStaleExecution(String workflowKey, String lockKey) {
        boolean staleRunning = runLogService.hasStaleRunningLog(workflowKey, STALE_RUNNING_MINUTES);
        boolean locked = redisLock.isLocked(lockKey);
        boolean hasRunning = runLogService.hasRunningLog(workflowKey);

        if (staleRunning) {
            int cleaned = runLogService.markStaleRunningAsFailed(workflowKey, STALE_RUNNING_MINUTES,
                    "检测到僵死运行记录，已自动清理");
            log.warn("清理僵死工作流运行日志: workflow={}, cleaned={}", workflowKey, cleaned);
        }

        if (locked && (!hasRunning || staleRunning)) {
            boolean released = redisLock.forceUnlock(lockKey);
            log.warn("清理僵死工作流 Redis 锁: workflow={}, released={}", workflowKey, released);
        }
    }

    private boolean tryAcquireWorkflowLock(String lockKey, String lockValue, String workflowKey) {
        if (redisLock.tryLock(lockKey, lockValue, WORKFLOW_LOCK_SECONDS)) {
            return true;
        }
        recoverStaleExecution(workflowKey, lockKey);
        return redisLock.tryLock(lockKey, lockValue, WORKFLOW_LOCK_SECONDS);
    }

    private String buildLockKey(String workflowKey) {
        return "file_workflow_" + workflowKey;
    }
}
