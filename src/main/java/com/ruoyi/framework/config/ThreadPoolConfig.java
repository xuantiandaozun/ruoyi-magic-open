package com.ruoyi.framework.config;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.ruoyi.common.utils.Threads;

/**
 * 线程池配置。
 * 小内存机器上不要预热大量核心线程：每个线程默认约 1MB 栈，空闲常驻会直接抬高堆外 RSS。
 *
 * @author ruoyi
 **/
@Configuration
public class ThreadPoolConfig
{
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    @Value("${ruoyi.thread-pool.executor.core-size:8}")
    private int corePoolSize;

    @Value("${ruoyi.thread-pool.executor.max-size:32}")
    private int maxPoolSize;

    @Value("${ruoyi.thread-pool.executor.queue-capacity:500}")
    private int queueCapacity;

    @Value("${ruoyi.thread-pool.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${ruoyi.thread-pool.scheduled.core-size:4}")
    private int scheduledCorePoolSize;

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("async-executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("业务异步线程池初始化完成，core={}, max={}, queue={}, keepAlive={}s，允许核心线程回收",
                corePoolSize, maxPoolSize, queueCapacity, keepAliveSeconds);
        return executor;
    }

    /**
     * 执行周期性或延迟任务（操作日志、登录日志等）。
     * 任务会进入无界延迟队列，核心线程按需创建，空闲后回收。
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService()
    {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(scheduledCorePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy())
        {
            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
        executor.setKeepAliveTime(60, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);
        log.info("延迟任务线程池初始化完成，core={}，允许核心线程回收", scheduledCorePoolSize);
        return executor;
    }
}
