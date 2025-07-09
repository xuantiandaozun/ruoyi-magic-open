package com.ruoyi.framework.monitor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动性能监控组件
 * 用于监控应用启动过程中各个阶段的性能指标
 * 
 * @author ruoyi
 */
@Component
public class StartupPerformanceMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(StartupPerformanceMonitor.class);
    
    private final ConcurrentHashMap<String, Long> startTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> endTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    private long applicationStartTime;
    
    public StartupPerformanceMonitor() {
        this.applicationStartTime = System.currentTimeMillis();
    }
    
    /**
     * 记录阶段开始时间
     */
    public void recordStart(String phase) {
        startTimes.put(phase, System.currentTimeMillis());
        log.debug("启动阶段 [{}] 开始", phase);
    }
    
    /**
     * 记录阶段结束时间并计算耗时
     */
    public void recordEnd(String phase) {
        long endTime = System.currentTimeMillis();
        endTimes.put(phase, endTime);
        
        Long startTime = startTimes.get(phase);
        if (startTime != null) {
            long duration = endTime - startTime;
            log.info("启动阶段 [{}] 完成，耗时: {}ms", phase, duration);
        }
    }
    
    /**
     * 增加计数器
     */
    public void incrementCounter(String counterName) {
        counters.computeIfAbsent(counterName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 设置计数器值
     */
    public void setCounter(String counterName, long value) {
        counters.computeIfAbsent(counterName, k -> new AtomicLong(0)).set(value);
    }
    
    /**
     * 获取阶段耗时
     */
    public long getPhaseDuration(String phase) {
        Long startTime = startTimes.get(phase);
        Long endTime = endTimes.get(phase);
        
        if (startTime != null && endTime != null) {
            return endTime - startTime;
        }
        return -1;
    }
    
    /**
     * 获取计数器值
     */
    public long getCounterValue(String counterName) {
        AtomicLong counter = counters.get(counterName);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * 应用启动完成事件监听
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        long totalStartupTime = System.currentTimeMillis() - applicationStartTime;
        
        log.info("=== 应用启动性能报告 ===");
        log.info("总启动时间: {}ms", totalStartupTime);
        
        // 输出各阶段性能数据
        startTimes.keySet().forEach(phase -> {
            long duration = getPhaseDuration(phase);
            if (duration > 0) {
                double percentage = (double) duration / totalStartupTime * 100;
                log.info("阶段 [{}]: {}ms (占比: {:.1f}%)", phase, duration, percentage);
            }
        });
        
        // 输出计数器数据
        counters.forEach((name, counter) -> {
            log.info("计数器 [{}]: {}", name, counter.get());
        });
        
        // 性能建议
        providePerfomanceAdvice(totalStartupTime);
        
        log.info("=== 启动性能报告结束 ===");
    }
    
    /**
     * 提供性能优化建议
     */
    private void providePerfomanceAdvice(long totalStartupTime) {
        if (totalStartupTime > 30000) { // 超过30秒
            log.warn("启动时间较长，建议检查以下方面：");
            log.warn("1. 数据库连接配置和网络延迟");
            log.warn("2. 缓存预热策略");
            log.warn("3. 线程池配置");
            log.warn("4. 是否有阻塞的同步操作");
        } else if (totalStartupTime > 15000) { // 超过15秒
            log.info("启动时间适中，可考虑进一步优化异步初始化策略");
        } else {
            log.info("启动性能良好");
        }
    }
    
    /**
     * 清理监控数据
     */
    public void clear() {
        startTimes.clear();
        endTimes.clear();
        counters.clear();
    }
}