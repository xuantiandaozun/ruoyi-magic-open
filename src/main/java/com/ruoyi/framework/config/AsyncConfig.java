package com.ruoyi.framework.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务配置
 * 
 * @author ruoyi
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = 5;

    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = 10;

    /**
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 100;

    /**
     * 线程名称前缀
     */
    private static final String THREAD_NAME_PREFIX = "Async-";

    /**
     * 配置异步任务执行器
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：线程池创建时初始化的线程数
        executor.setCorePoolSize(CORE_POOL_SIZE);
        
        // 最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        
        // 缓冲队列：用来缓冲执行任务的队列
        executor.setQueueCapacity(QUEUE_CAPACITY);
        
        // 允许线程的空闲时间：当超过了核心线程数之外的线程在空闲时间到达之后会被销毁
        executor.setKeepAliveSeconds(60);
        
        // 线程名称前缀
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        
        // 拒绝策略：由调用线程处理（一般是主线程）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        logger.info("异步任务线程池初始化完成，核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        
        return executor;
    }
}
