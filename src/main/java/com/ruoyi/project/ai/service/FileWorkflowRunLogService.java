package com.ruoyi.project.ai.service;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件化工作流运行日志服务。
 */
@Slf4j
@Service
public class FileWorkflowRunLogService {

    private final JdbcTemplate jdbcTemplate;

    public FileWorkflowRunLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long logStart(String workflowKey, Long workflowId, String triggerType, Map<String, Object> inputData) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                        INSERT INTO ai_file_workflow_run_log
                        (workflow_key, workflow_id, trigger_type, status, start_time, input_data, create_time)
                        VALUES (?, ?, ?, 'running', ?, ?, ?)
                        """, java.sql.Statement.RETURN_GENERATED_KEYS);
                Timestamp now = new Timestamp(System.currentTimeMillis());
                ps.setString(1, workflowKey);
                if (workflowId == null) {
                    ps.setNull(2, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(2, workflowId);
                }
                ps.setString(3, triggerType);
                ps.setTimestamp(4, now);
                ps.setString(5, JSONUtil.toJsonStr(inputData));
                ps.setTimestamp(6, now);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            return key != null ? key.longValue() : null;
        } catch (DataAccessException e) {
            log.warn("文件化工作流运行日志写入失败，可能尚未执行日志表SQL: {}", e.getMessage());
            return null;
        }
    }

    public void logComplete(Long logId, Map<String, Object> outputData, long durationMs) {
        updateEnd(logId, "completed", "执行成功", JSONUtil.toJsonStr(outputData), null, durationMs);
    }

    public void logFailed(Long logId, String errorMessage, long durationMs) {
        updateEnd(logId, "failed", "执行失败", null, errorMessage, durationMs);
    }

    public List<Map<String, Object>> listLogs(String workflowKey, Long workflowId, int pageNum, int pageSize) {
        try {
            int safePageNum = Math.max(pageNum, 1);
            int safePageSize = Math.min(Math.max(pageSize, 1), 100);
            int offset = (safePageNum - 1) * safePageSize;
            return jdbcTemplate.queryForList("""
                    SELECT id, workflow_key AS workflowKey, workflow_id AS workflowId, trigger_type AS triggerType,
                           status, start_time AS startTime, end_time AS endTime, duration_ms AS durationMs,
                           message, error_message AS errorMessage, create_time AS createTime
                    FROM ai_file_workflow_run_log
                    WHERE workflow_key = ? OR workflow_id = ?
                    ORDER BY create_time DESC
                    LIMIT ? OFFSET ?
                    """, workflowKey, workflowId, safePageSize, offset);
        } catch (DataAccessException e) {
            log.warn("文件化工作流运行日志查询失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public long countLogs(String workflowKey, Long workflowId) {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM ai_file_workflow_run_log
                    WHERE workflow_key = ? OR workflow_id = ?
                    """, Long.class, workflowKey, workflowId);
            return count != null ? count : 0L;
        } catch (DataAccessException e) {
            log.warn("文件化工作流运行日志统计失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 是否存在运行中的日志。
     */
    public boolean hasRunningLog(String workflowKey) {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM ai_file_workflow_run_log
                    WHERE workflow_key = ? AND status = 'running'
                    """, Long.class, workflowKey);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.warn("文件化工作流运行中日志检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 是否存在超过指定分钟仍未结束的运行日志。
     */
    public boolean hasStaleRunningLog(String workflowKey, int staleMinutes) {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM ai_file_workflow_run_log
                    WHERE workflow_key = ?
                      AND status = 'running'
                      AND start_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)
                    """, Long.class, workflowKey, staleMinutes);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.warn("文件化工作流僵死日志检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 将僵死的 running 日志标记为 failed。
     */
    public int markStaleRunningAsFailed(String workflowKey, int staleMinutes, String errorMessage) {
        try {
            return jdbcTemplate.update("""
                    UPDATE ai_file_workflow_run_log
                    SET status = 'failed',
                        end_time = NOW(),
                        duration_ms = TIMESTAMPDIFF(MICROSECOND, start_time, NOW()) / 1000,
                        message = '执行失败',
                        error_message = ?
                    WHERE workflow_key = ?
                      AND status = 'running'
                      AND start_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)
                    """, errorMessage, workflowKey, staleMinutes);
        } catch (DataAccessException e) {
            log.warn("文件化工作流僵死日志清理失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 强制结束指定工作流所有 running 日志（人工解锁场景）。
     */
    public int forceFailRunningLogs(String workflowKey, String errorMessage) {
        try {
            return jdbcTemplate.update("""
                    UPDATE ai_file_workflow_run_log
                    SET status = 'failed',
                        end_time = NOW(),
                        duration_ms = TIMESTAMPDIFF(MICROSECOND, start_time, NOW()) / 1000,
                        message = '执行失败',
                        error_message = ?
                    WHERE workflow_key = ?
                      AND status = 'running'
                    """, errorMessage, workflowKey);
        } catch (DataAccessException e) {
            log.warn("文件化工作流 running 日志强制结束失败: {}", e.getMessage());
            return 0;
        }
    }

    public Map<String, Object> statistics(String workflowKey, Long workflowId, int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        int safeDays = Math.max(days, 1);
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                    SELECT
                        COUNT(*) AS totalCount,
                        SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) AS successCount,
                        SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END) AS failCount,
                        SUM(CASE WHEN status = 'running' THEN 1 ELSE 0 END) AS runningCount,
                        AVG(CASE WHEN status = 'completed' THEN duration_ms ELSE NULL END) AS avgDurationMs
                    FROM ai_file_workflow_run_log
                    WHERE (workflow_key = ? OR workflow_id = ?)
                      AND create_time >= DATE_SUB(NOW(), INTERVAL ? DAY)
                    """, workflowKey, workflowId, safeDays);
            result.putAll(row);
        } catch (DataAccessException e) {
            log.warn("文件化工作流运行日志统计查询失败: {}", e.getMessage());
            result.put("totalCount", 0);
            result.put("successCount", 0);
            result.put("failCount", 0);
            result.put("runningCount", 0);
            result.put("avgDurationMs", null);
        }
        long total = asLong(result.get("totalCount"));
        long success = asLong(result.get("successCount"));
        double successRate = total > 0 ? success * 100.0 / total : 0.0;
        result.put("successRate", Math.round(successRate * 100.0) / 100.0);
        result.put("days", safeDays);
        return result;
    }

    private void updateEnd(Long logId, String status, String message, String outputData, String errorMessage, long durationMs) {
        if (logId == null) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    UPDATE ai_file_workflow_run_log
                    SET status = ?, end_time = ?, duration_ms = ?, message = ?, output_data = ?, error_message = ?
                    WHERE id = ?
                    """, status, new Date(), durationMs, message, outputData, errorMessage, logId);
        } catch (DataAccessException e) {
            log.warn("文件化工作流运行日志更新失败: {}", e.getMessage());
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value));
    }
}
