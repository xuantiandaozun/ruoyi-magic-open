package com.ruoyi.project.gen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

public class LoggingObservationHandler implements ObservationHandler<Observation.Context> {
    private static final Logger log = LoggerFactory.getLogger(LoggingObservationHandler.class);
    
    @Override
    public void onStart(Observation.Context context) {
        log.info("=== AI 调用开始 ===");

    }
    
    @Override
    public void onStop(Observation.Context context) {
        log.info("=== AI 调用结束 ===");
        log.info("观察名称: {}", context.getName());
        
        // 如果有错误，打印错误信息
        Throwable error = context.getError();
        if (error != null) {
            log.error("调用过程中发生错误: ", error);
        }
    }
    
    @Override
    public void onEvent(Observation.Event event, Observation.Context context) {
        log.info("=== AI 调用事件 ===");
        log.info("事件名称: {}", event.getName());
        log.info("事件上下文: {}", event.getContextualName());
    }
    
    @Override
    public boolean supportsContext(Observation.Context context) {
        return true; // 支持所有上下文
    }
}