package com.ruoyi.framework.datasource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ruoyi.framework.service.BatchInitializationService;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.service.ISysDataSourceService;

/**
 * 动态数据源初始化器，在应用启动时从数据库加载数据源配置
 * 
 * @author ruoyi-magic
 */
@Component
public class DynamicDataSourceInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceInitializer.class);

    @Autowired
    private ISysDataSourceService dataSourceService;
    
    @Autowired
    private BatchInitializationService batchInitializationService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 异步初始化动态数据源
        initializeDataSourcesAsync();
    }
    
    /**
     * 异步初始化动态数据源
     */
    @Async("threadPoolTaskExecutor")
    @Order(3)
    public void initializeDataSourcesAsync() {
        log.info("开始异步初始化动态数据源...");
        
        try {
            // 查询状态为正常的数据源
            SysDataSource queryParam = new SysDataSource();
            queryParam.setStatus("0");
            List<SysDataSource> dataSources = dataSourceService.selectSysDataSourceList(queryParam);
            
            // 过滤掉主数据源
            dataSources.removeIf(ds -> "MASTER".equals(ds.getName()));
            
            log.info("检测到 {} 个数据源配置需要加载", dataSources.size());
            
            if (!dataSources.isEmpty()) {
                // 使用批量初始化服务进行优化的初始化
                CompletableFuture<Void> batchInitFuture = batchInitializationService.batchInitializeDataSources(dataSources);
                
                // 设置超时时间，避免无限等待
                batchInitFuture.get(5, TimeUnit.MINUTES);
                
                log.info("动态数据源批量异步初始化完成");
            } else {
                log.info("没有需要初始化的动态数据源");
            }
        } catch (Exception e) {
            log.error("异步初始化动态数据源时发生错误: {}", e.getMessage(), e);
        }
    }
}
