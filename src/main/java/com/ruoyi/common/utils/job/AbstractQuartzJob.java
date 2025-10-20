package com.ruoyi.common.utils.job;

import java.util.Date;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.ScheduleConstants;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.project.monitor.domain.SysJob;
import com.ruoyi.project.monitor.domain.SysJobLog;
import com.ruoyi.project.monitor.service.ISysJobLogService;
import cn.hutool.core.exceptions.ExceptionUtil;

/**
 * 抽象quartz调用
 *
 * @author ruoyi
 */
public abstract class AbstractQuartzJob implements Job
{
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    /**
     * 线程本地变量
     */
    private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        SysJob sysJob = new SysJob();
        BeanUtil.copyProperties(context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES), sysJob);
        try
        {
            before(context, sysJob);
            if (sysJob != null)
            {
                doExecute(context, sysJob);
            }
            after(context, sysJob, null);
        }
        catch (Exception e)
        {
            log.error("任务执行异常  - ：", e);
            after(context, sysJob, e);
        }
    }

    /**
     * 执行前
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     */
    protected void before(JobExecutionContext context, SysJob sysJob)
    {
        threadLocal.set(new Date());
    }

    /**
     * 执行后
     *
     * @param context 工作执行上下文对象
     * @param sysScheduleJob 系统计划任务
     */
    protected void after(JobExecutionContext context, SysJob sysJob, Exception e)
    {
        Date startTime = threadLocal.get();
        threadLocal.remove();

        final SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        
        // 计算执行时间
        Date stopTime = new Date();
        long runMs = stopTime.getTime() - startTime.getTime();
        
        // 使用create_time字段记录开始时间，并在消息中包含执行耗时
        sysJobLog.setCreateTime(startTime);
        sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
        
        if (e != null)
        {
            sysJobLog.setStatus(Constants.FAIL);
            String errorMsg = StrUtil.sub(ExceptionUtil.getMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(errorMsg);
        }
        else
        {
            sysJobLog.setStatus(Constants.SUCCESS);
        }

        // 写入数据库当中
        SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     * @throws Exception 执行过程中的异常
     */
    protected abstract void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}
