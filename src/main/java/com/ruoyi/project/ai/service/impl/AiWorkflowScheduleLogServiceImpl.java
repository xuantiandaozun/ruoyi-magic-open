package com.ruoyi.project.ai.service.impl;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiWorkflowScheduleLog;
import com.ruoyi.project.ai.mapper.AiWorkflowScheduleLogMapper;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleLogService;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.net.NetUtil;

/**
 * AI工作流定时调度日志Service业务层处理
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
@UseDataSource("MASTER")
public class AiWorkflowScheduleLogServiceImpl extends ServiceImpl<AiWorkflowScheduleLogMapper, AiWorkflowScheduleLog>
        implements IAiWorkflowScheduleLogService {

    private static final Logger log = LoggerFactory.getLogger(AiWorkflowScheduleLogServiceImpl.class);

    @Override
    public List<AiWorkflowScheduleLog> listByScheduleId(Long scheduleId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule_log")
            .where(new QueryColumn("schedule_id").eq(scheduleId))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    public List<AiWorkflowScheduleLog> listByWorkflowId(Long workflowId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule_log")
            .where(new QueryColumn("workflow_id").eq(workflowId))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    public List<AiWorkflowScheduleLog> listByStatus(String status) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule_log")
            .where(new QueryColumn("status").eq(status))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    public List<AiWorkflowScheduleLog> listByTriggerType(String triggerType) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule_log")
            .where(new QueryColumn("trigger_type").eq(triggerType))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    public Long logScheduleStart(Long scheduleId, Long workflowId, String triggerType, String inputData) {
        try {
            AiWorkflowScheduleLog scheduleLog = new AiWorkflowScheduleLog();
            scheduleLog.setScheduleId(scheduleId);
            scheduleLog.setWorkflowId(workflowId);
            scheduleLog.setTriggerType(triggerType);
            scheduleLog.setScheduledTime(new Date());
            scheduleLog.setActualStartTime(new Date());
            scheduleLog.setStatus("running");
            scheduleLog.setInputData(inputData);
            scheduleLog.setRetryCount(0);
            scheduleLog.setServerInfo(getServerInfo());
            scheduleLog.setDelFlag("0");
            
            save(scheduleLog);
            
            log.info("记录调度开始日志成功，调度ID：{}，工作流ID：{}", scheduleId, workflowId);
            return scheduleLog.getId();
        } catch (Exception e) {
            log.error("记录调度开始日志失败，调度ID：{}，工作流ID：{}", scheduleId, workflowId, e);
            return null;
        }
    }

    @Override
    public void logScheduleComplete(Long logId, Long executionId, String status, String resultMessage, String outputData) {
        try {
            AiWorkflowScheduleLog scheduleLog = getById(logId);
            if (scheduleLog != null) {
                Date endTime = new Date();
                scheduleLog.setExecutionId(executionId);
                scheduleLog.setActualEndTime(endTime);
                scheduleLog.setStatus(status);
                scheduleLog.setResultMessage(resultMessage);
                scheduleLog.setOutputData(outputData);
                
                // 计算执行耗时
                if (scheduleLog.getActualStartTime() != null) {
                    long duration = endTime.getTime() - scheduleLog.getActualStartTime().getTime();
                    scheduleLog.setExecutionDuration(duration);
                }
                
                updateById(scheduleLog);
                
                log.info("记录调度完成日志成功，日志ID：{}，执行状态：{}", logId, status);
            }
        } catch (Exception e) {
            log.error("记录调度完成日志失败，日志ID：{}", logId, e);
        }
    }

    @Override
    public void logScheduleError(Long logId, String status, String errorMessage) {
        try {
            AiWorkflowScheduleLog scheduleLog = getById(logId);
            if (scheduleLog != null) {
                Date endTime = new Date();
                scheduleLog.setActualEndTime(endTime);
                scheduleLog.setStatus(status);
                scheduleLog.setErrorMessage(errorMessage);
                
                // 计算执行耗时
                if (scheduleLog.getActualStartTime() != null) {
                    long duration = endTime.getTime() - scheduleLog.getActualStartTime().getTime();
                    scheduleLog.setExecutionDuration(duration);
                }
                
                updateById(scheduleLog);
                
                log.info("记录调度错误日志成功，日志ID：{}，错误状态：{}", logId, status);
            }
        } catch (Exception e) {
            log.error("记录调度错误日志失败，日志ID：{}", logId, e);
        }
    }

    @Override
    public int cleanExpiredLogs(int days) {
        try {
            Date expiredDate = DateUtil.offsetDay(new Date(), -days);
            
            QueryWrapper qw = QueryWrapper.create()
                .from("ai_workflow_schedule_log")
                .where(new QueryColumn("create_time").lt(expiredDate));
            
            List<AiWorkflowScheduleLog> expiredLogs = list(qw);
            
            if (!expiredLogs.isEmpty()) {
                // 批量逻辑删除
                expiredLogs.forEach(log -> log.setDelFlag("2"));
                updateBatch(expiredLogs);
                
                log.info("清理过期调度日志成功，清理数量：{}", expiredLogs.size());
                return expiredLogs.size();
            }
            
            return 0;
        } catch (Exception e) {
            log.error("清理过期调度日志失败", e);
            return 0;
        }
    }

    /**
     * 获取服务器信息
     */
    private String getServerInfo() {
        try {
            String hostName = NetUtil.getLocalHostName();
            String hostAddress = NetUtil.getLocalhostStr();
            return hostName + "(" + hostAddress + ")";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}