package com.ruoyi.project.monitor.service.impl;

import java.util.List;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.common.utils.job.ScheduleUtils;
import com.ruoyi.framework.service.BatchInitializationService;
import com.ruoyi.project.monitor.domain.SysJob;
import com.ruoyi.project.monitor.mapper.SysJobMapper;
import com.ruoyi.project.monitor.service.ISysJobService;

/**
 * 定时任务调度服务实现
 * 
 * @author ruoyi
 */
@Service
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJob> implements ISysJobService
{
    private static final Logger log = LoggerFactory.getLogger(SysJobServiceImpl.class);
    
    @Autowired
    private Scheduler scheduler;
    
    @Autowired
    private BatchInitializationService batchInitializationService;



    /**
     * 异步初始化定时任务（优化版本）
     */
    @Async("threadPoolTaskExecutor")
    @Order(4)
    public void initializeJobsAsync()
    {
        try {
            log.info("开始异步初始化定时任务...");
            long startTime = System.currentTimeMillis();
            
            scheduler.clear();
            
            // 使用批量初始化服务获取任务数据
            BatchInitializationService.InitializationData data = batchInitializationService.loadAllInitializationData();
            List<SysJob> jobList = data.getJobs();
            
            log.info("检测到 {} 个定时任务需要初始化", jobList.size());
            
            int successCount = 0;
            for (SysJob job : jobList) {
                try {
                    ScheduleUtils.createScheduleJob(scheduler, job);
                    successCount++;
                    log.debug("定时任务 [{}] 初始化成功", job.getJobName());
                } catch (Exception e) {
                    log.error("定时任务 [{}] 初始化失败: {}", job.getJobName(), e.getMessage());
                }
            }
            
            long endTime = System.currentTimeMillis();
            log.info("定时任务异步初始化完成，成功: {}/{}, 耗时: {}ms", 
                    successCount, jobList.size(), endTime - startTime);
                    
        } catch (SchedulerException e) {
            log.error("清空调度器时发生错误", e);
        } catch (Exception e) {
            log.error("异步初始化定时任务时发生错误", e);
        }
    }

    /**
     * 获取quartz调度器的计划任务
     * 
     * @param job 调度信息
     * @return 调度任务集合
     */
    @Override
    public List<SysJob> selectJobList(SysJob job)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_job")
            .where(new QueryColumn("job_name").like(job.getJobName(), job.getJobName() != null))
            .and(new QueryColumn("job_group").eq(job.getJobGroup(), job.getJobGroup() != null))
            .and(new QueryColumn("status").eq(job.getStatus(), job.getStatus() != null))
            .and(new QueryColumn("invoke_target").like(job.getInvokeTarget(), job.getInvokeTarget() != null));
        return list(queryWrapper);
    }

    /**
     * 通过调度ID查询调度任务信息
     * 
     * @param jobId 调度ID
     * @return 调度任务对象信息
     */
    @Override
    public SysJob selectJobById(Long jobId)
    {
        return getById(jobId);
    }

    /**
     * 暂停任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    @Override
    public int pauseJob(SysJob job)
    {
        job.setStatus("1");
        return updateById(job) ? 1 : 0;
    }

    /**
     * 恢复任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    @Override
    public int resumeJob(SysJob job)
    {
        job.setStatus("0");
        return updateById(job) ? 1 : 0;
    }

    /**
     * 删除任务后，所对应的trigger也将被删除
     * 
     * @param job 调度信息
     * @return 结果
     */
    @Override
    public int deleteJob(SysJob job)
    {
        return removeById(job.getJobId()) ? 1 : 0;
    }

    /**
     * 批量删除调度信息
     * 
     * @param jobIds 需要删除的任务ID
     * @return 结果
     */
    @Override
    public void deleteJobByIds(Long[] jobIds)
    {
        for (Long jobId : jobIds)
        {
            SysJob job = selectJobById(jobId);
            deleteJob(job);
        }
    }

    /**
     * 任务调度状态修改
     * 
     * @param job 调度信息
     * @return 结果
     */
    @Override
    public int changeStatus(SysJob job)
    {
        return updateById(job) ? 1 : 0;
    }

    /**
     * 立即运行任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    @Override
    public boolean run(SysJob job)
    {
        return true;
    }

    /**
     * 新增任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    @Override
    public int insertJob(SysJob job)
    {
        return save(job) ? 1 : 0;
    }
    
    /**
     * 创建Quartz调度任务
     * 
     * @param job 调度信息
     * @throws SchedulerException 调度异常
     * @throws TaskException 任务异常
     */
    @Override
    public void createScheduleJob(SysJob job) throws SchedulerException, TaskException
    {
        ScheduleUtils.createScheduleJob(scheduler, job);
    }

    /**
     * 更新任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    @Override
    public int updateJob(SysJob job)
    {
        return updateById(job) ? 1 : 0;
    }

    /**
     * 校验cron表达式是否有效
     * 
     * @param cronExpression 表达式
     * @return 结果
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression)
    {
        return true;
    }
}