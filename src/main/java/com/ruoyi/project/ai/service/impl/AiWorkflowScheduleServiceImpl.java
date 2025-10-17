package com.ruoyi.project.ai.service.impl;

import java.util.Date;
import java.util.List;

import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.domain.AiWorkflowSchedule;
import com.ruoyi.project.ai.mapper.AiWorkflowScheduleMapper;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleService;
import com.ruoyi.project.monitor.domain.SysJob;
import com.ruoyi.project.monitor.service.ISysJobService;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

/**
 * AI工作流定时调度Service业务层处理
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
@UseDataSource("MASTER")
public class AiWorkflowScheduleServiceImpl extends ServiceImpl<AiWorkflowScheduleMapper, AiWorkflowSchedule>
        implements IAiWorkflowScheduleService {

    private static final Logger log = LoggerFactory.getLogger(AiWorkflowScheduleServiceImpl.class);

    @Autowired
    private ISysJobService sysJobService;

    @Override
    public List<AiWorkflowSchedule> listByWorkflowId(Long workflowId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule")
            .where(new QueryColumn("workflow_id").eq(workflowId))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    public List<AiWorkflowSchedule> listByEnabled(String enabled) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule")
            .where(new QueryColumn("enabled").eq(enabled))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    public List<AiWorkflowSchedule> listByStatus(String status) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule")
            .where(new QueryColumn("status").eq(status))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    @Transactional
    public boolean startSchedule(Long scheduleId) {
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                throw new ServiceException("调度配置不存在");
            }

            // 创建Quartz任务
            SysJob job = createQuartzJob(schedule);
            sysJobService.insertJob(job);

            // 更新调度状态
            schedule.setStatus("0"); // 正常
            schedule.setEnabled("Y"); // 启用
            updateNextExecutionTime(scheduleId);
            updateById(schedule);

            log.info("启动工作流调度任务成功，调度ID：{}", scheduleId);
            return true;
        } catch (Exception e) {
            log.error("启动工作流调度任务失败，调度ID：{}", scheduleId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean pauseSchedule(Long scheduleId) {
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                throw new ServiceException("调度配置不存在");
            }

            // 暂停Quartz任务
            String jobName = "WORKFLOW_SCHEDULE_" + scheduleId;
            String jobGroup = "WORKFLOW_SCHEDULE";
            sysJobService.pauseJob(createQuartzJob(schedule));

            // 更新调度状态
            schedule.setStatus("1"); // 暂停
            updateById(schedule);

            log.info("暂停工作流调度任务成功，调度ID：{}", scheduleId);
            return true;
        } catch (Exception e) {
            log.error("暂停工作流调度任务失败，调度ID：{}", scheduleId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean resumeSchedule(Long scheduleId) {
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                throw new ServiceException("调度配置不存在");
            }

            // 恢复Quartz任务
            sysJobService.resumeJob(createQuartzJob(schedule));

            // 更新调度状态
            schedule.setStatus("0"); // 正常
            updateNextExecutionTime(scheduleId);
            updateById(schedule);

            log.info("恢复工作流调度任务成功，调度ID：{}", scheduleId);
            return true;
        } catch (Exception e) {
            log.error("恢复工作流调度任务失败，调度ID：{}", scheduleId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteSchedule(Long scheduleId) {
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                throw new ServiceException("调度配置不存在");
            }

            // 删除Quartz任务
            sysJobService.deleteJob(createQuartzJob(schedule));

            // 逻辑删除调度配置
            schedule.setDelFlag("2");
            updateById(schedule);

            log.info("删除工作流调度任务成功，调度ID：{}", scheduleId);
            return true;
        } catch (Exception e) {
            log.error("删除工作流调度任务失败，调度ID：{}", scheduleId, e);
            return false;
        }
    }

    @Override
    public boolean executeOnce(Long scheduleId) {
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                throw new ServiceException("调度配置不存在");
            }

            // 立即执行Quartz任务
            sysJobService.run(createQuartzJob(schedule));

            log.info("立即执行工作流调度任务成功，调度ID：{}", scheduleId);
            return true;
        } catch (Exception e) {
            log.error("立即执行工作流调度任务失败，调度ID：{}", scheduleId, e);
            return false;
        }
    }

    @Override
    public void updateNextExecutionTime(Long scheduleId) {
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null || StrUtil.isBlank(schedule.getCronExpression())) {
                return;
            }

            CronExpression cronExpression = new CronExpression(schedule.getCronExpression());
            Date nextTime = cronExpression.getNextValidTimeAfter(new Date());
            
            schedule.setNextExecutionTime(nextTime);
            updateById(schedule);
        } catch (Exception e) {
            log.error("更新下次执行时间失败，调度ID：{}", scheduleId, e);
        }
    }

    @Override
    public List<AiWorkflowSchedule> listEnabledSchedules() {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule")
            .where(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    /**
     * 创建Quartz任务对象
     */
    private SysJob createQuartzJob(AiWorkflowSchedule schedule) {
        SysJob job = new SysJob();
        job.setJobName("WORKFLOW_SCHEDULE_" + schedule.getId());
        job.setJobGroup("WORKFLOW_SCHEDULE");
        job.setInvokeTarget("workflowScheduleTask.execute(" + schedule.getId() + ")");
        job.setCronExpression(schedule.getCronExpression());
        job.setMisfirePolicy(schedule.getMisfirePolicy());
        job.setConcurrent(schedule.getConcurrent());
        job.setStatus("0"); // 正常状态
        return job;
    }
}