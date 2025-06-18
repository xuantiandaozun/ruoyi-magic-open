package com.ruoyi.project.monitor.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.monitor.domain.SysJobLog;
import com.ruoyi.project.monitor.mapper.SysJobLogMapper;
import com.ruoyi.project.monitor.service.ISysJobLogService;

/**
 * 定时任务调度日志服务实现
 * 
 * @author ruoyi
 */
@Service
public class SysJobLogServiceImpl extends ServiceImpl<SysJobLogMapper, SysJobLog> implements ISysJobLogService
{
    /**
     * 获取quartz调度器日志的计划任务
     * 
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    @Override
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_job_log")
            .where(new QueryColumn("job_name").like(jobLog.getJobName(), jobLog.getJobName() != null))
            .and(new QueryColumn("job_group").eq(jobLog.getJobGroup(), jobLog.getJobGroup() != null))
            .and(new QueryColumn("status").eq(jobLog.getStatus(), jobLog.getStatus() != null))
            .and(new QueryColumn("invoke_target").like(jobLog.getInvokeTarget(), jobLog.getInvokeTarget() != null));

        // 处理时间范围查询
        Object beginTime = jobLog.getParams().get("beginTime");
        Object endTime = jobLog.getParams().get("endTime");
        if (beginTime != null) {
            queryWrapper.and(new QueryColumn("create_time").ge(beginTime));
        }
        if (endTime != null) {
            queryWrapper.and(new QueryColumn("create_time").le(endTime));
        }

        queryWrapper.orderBy(new QueryColumn("job_log_id").desc());
        return list(queryWrapper);
    }

    /**
     * 通过调度任务日志ID查询调度信息
     * 
     * @param jobLogId 调度任务日志ID
     * @return 调度任务日志对象信息
     */
    @Override
    public SysJobLog selectJobLogById(Long jobLogId)
    {
        return getById(jobLogId);
    }

    /**
     * 新增任务日志
     * 
     * @param jobLog 调度日志信息
     */
    @Override
    public void addJobLog(SysJobLog jobLog)
    {
        save(jobLog);
    }

    /**
     * 批量删除调度日志信息
     * 
     * @param logIds 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteJobLogByIds(Long[] logIds)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_job_log")
            .where(new QueryColumn("job_log_id").in(Arrays.asList(logIds)));
        return remove(queryWrapper) ? 1 : 0;
    }

    /**
     * 删除任务日志
     * 
     * @param jobId 调度日志ID
     * @return 结果
     */
    @Override
    public int deleteJobLogById(Long jobId)
    {
        return removeById(jobId) ? 1 : 0;
    }

    /**
     * 清空任务日志
     */
    @Override
    public void cleanJobLog()
    {
        QueryWrapper queryWrapper = QueryWrapper.create().from("sys_job_log");
        remove(queryWrapper);
    }
}
