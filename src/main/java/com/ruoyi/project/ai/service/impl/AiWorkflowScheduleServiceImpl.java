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
        long startTime = System.currentTimeMillis();
        log.info("开始启动调度任务，scheduleId: {}", scheduleId);
        
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("启动调度失败：未找到调度记录，scheduleId: {}", scheduleId);
                return false;
            }

            // 检查是否已存在相同的任务
            String jobName = "WORKFLOW_SCHEDULE_" + scheduleId;
            SysJob existingJob = sysJobService.selectJobByName(jobName);
            if (existingJob != null) {
                log.warn("调度任务已存在，跳过创建。jobName: {}, scheduleId: {}", jobName, scheduleId);
                // 更新调度状态为运行中
                schedule.setStatus("1");
                schedule.setUpdateTime(new Date());
                boolean updated = updateById(schedule);
                long duration = System.currentTimeMillis() - startTime;
                log.info("调度任务状态更新完成，scheduleId: {}, 耗时: {}ms", scheduleId, duration);
                return updated;
            }

            // 创建Quartz任务
            SysJob job = createQuartzJob(schedule);
            sysJobService.insertJob(job);
            
            Long jobId = job.getJobId();
            if (jobId != null && jobId > 0) {
                // 更新调度状态
                schedule.setStatus("1");
                schedule.setUpdateTime(new Date());
                boolean result = updateById(schedule);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("调度任务启动成功，scheduleId: {}, jobId: {}, jobName: {}, 耗时: {}ms", 
                        scheduleId, jobId, jobName, duration);
                return result;
            } else {
                log.error("调度任务启动失败：Quartz任务创建失败，scheduleId: {}", scheduleId);
                return false;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("调度任务启动异常，scheduleId: {}, 耗时: {}ms", scheduleId, duration, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean pauseSchedule(Long scheduleId) {
        long startTime = System.currentTimeMillis();
        log.info("开始暂停调度任务，scheduleId: {}", scheduleId);
        
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("暂停调度失败：未找到调度记录，scheduleId: {}", scheduleId);
                return false;
            }

            String jobName = "WORKFLOW_SCHEDULE_" + scheduleId;
            
            try {
                // 根据任务名称查找 SysJob 对象
                SysJob job = sysJobService.selectJobByName(jobName);
                if (job == null) {
                    log.warn("暂停调度失败：未找到对应的Quartz任务，jobName: {}, scheduleId: {}", jobName, scheduleId);
                    return false;
                }
                
                sysJobService.pauseJob(job);
                schedule.setStatus("0");
                schedule.setUpdateTime(new Date());
                boolean result = updateById(schedule);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("调度任务暂停成功，scheduleId: {}, jobName: {}, 耗时: {}ms", 
                        scheduleId, jobName, duration);
                return result;
            } catch (Exception e) {
                log.error("暂停Quartz任务失败，jobName: {}, scheduleId: {}", jobName, scheduleId, e);
                return false;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("调度任务暂停异常，scheduleId: {}, 耗时: {}ms", scheduleId, duration, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean resumeSchedule(Long scheduleId) {
        long startTime = System.currentTimeMillis();
        log.info("开始恢复调度任务，scheduleId: {}", scheduleId);
        
        try {
            AiWorkflowSchedule schedule = getById(scheduleId);
            if (schedule == null) {
                log.warn("恢复调度失败：未找到调度记录，scheduleId: {}", scheduleId);
                return false;
            }

            String jobName = "WORKFLOW_SCHEDULE_" + scheduleId;
            
            try {
                // 根据任务名称查找 SysJob 对象
                SysJob job = sysJobService.selectJobByName(jobName);
                if (job == null) {
                    log.warn("恢复调度失败：未找到对应的Quartz任务，jobName: {}, scheduleId: {}", jobName, scheduleId);
                    return false;
                }
                
                sysJobService.resumeJob(job);
                schedule.setStatus("1");
                schedule.setUpdateTime(new Date());
                boolean result = updateById(schedule);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("调度任务恢复成功，scheduleId: {}, jobName: {}, 耗时: {}ms", 
                        scheduleId, jobName, duration);
                return result;
            } catch (Exception e) {
                log.error("恢复Quartz任务失败，jobName: {}, scheduleId: {}", jobName, scheduleId, e);
                return false;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("调度任务恢复异常，scheduleId: {}, 耗时: {}ms", scheduleId, duration, e);
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

            // 查询已存在的Quartz任务
            String jobName = "WORKFLOW_SCHEDULE_" + scheduleId;
            String jobGroup = "WORKFLOW_SCHEDULE";
            
            QueryWrapper jobQuery = QueryWrapper.create()
                .from("sys_job")
                .where(new QueryColumn("job_name").eq(jobName))
                .and(new QueryColumn("job_group").eq(jobGroup));
            
            SysJob existingJob = sysJobService.getOne(jobQuery);
            
            if (existingJob != null) {
                // 删除Quartz任务
                sysJobService.deleteJob(existingJob);
            }

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

            // 查询已存在的Quartz任务
            String jobName = "WORKFLOW_SCHEDULE_" + scheduleId;
            String jobGroup = "WORKFLOW_SCHEDULE";
            
            QueryWrapper jobQuery = QueryWrapper.create()
                .from("sys_job")
                .where(new QueryColumn("job_name").eq(jobName))
                .and(new QueryColumn("job_group").eq(jobGroup));
            
            SysJob existingJob = sysJobService.getOne(jobQuery);
            
            if (existingJob != null) {
                // 立即执行Quartz任务
                sysJobService.run(existingJob);
            }

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
     * 创建Quartz任务对象（用于系统初始化）
     */
    public SysJob createQuartzJobForInit(AiWorkflowSchedule schedule) {
        return createQuartzJob(schedule);
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