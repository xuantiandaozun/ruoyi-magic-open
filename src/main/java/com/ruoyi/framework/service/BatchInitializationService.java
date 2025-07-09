package com.ruoyi.framework.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.common.utils.job.ScheduleUtils;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;

import com.ruoyi.framework.datasource.DynamicDataSourceManager;
import com.ruoyi.framework.redis.OptimizedRedisCache;
import com.ruoyi.project.monitor.domain.SysJob;
import com.ruoyi.project.monitor.mapper.SysJobMapper;
import com.ruoyi.project.system.domain.SysConfig;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.domain.SysDictData;
import com.ruoyi.project.system.domain.SysDictType;
import com.ruoyi.project.system.mapper.SysConfigMapper;
import com.ruoyi.project.system.mapper.SysDataSourceMapper;
import com.ruoyi.project.system.mapper.SysDictDataMapper;
import com.ruoyi.project.system.mapper.SysDictTypeMapper;
import com.ruoyi.project.system.service.ISysConfigService;
import com.ruoyi.project.system.service.ISysDictTypeService;
import com.ruoyi.project.monitor.service.ISysJobService;

/**
 * 批量初始化服务
 * 用于优化启动时的数据库查询性能
 * 使用ApplicationReadyEvent确保在应用完全启动后再执行初始化
 * 
 * @author ruoyi-magic
 */
@Service
public class BatchInitializationService {
    
    private static final Logger log = LoggerFactory.getLogger(BatchInitializationService.class);
    
    @Autowired
    private SysConfigMapper configMapper;
    
    @Autowired
    private SysDictTypeMapper dictTypeMapper;
    
    @Autowired
    private SysDictDataMapper dictDataMapper;
    
    @Autowired
    private SysDataSourceMapper dataSourceMapper;
    
    @Autowired
    private SysJobMapper jobMapper;
    
    @Autowired
    private DynamicDataSourceManager dataSourceManager;
    
    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private Executor executor;
    
    @Autowired
    private OptimizedRedisCache optimizedRedisCache;
    
    @Lazy
    @Autowired
    private ISysConfigService configService;
    
    @Lazy
    @Autowired
    private ISysDictTypeService dictTypeService;
    
    @Lazy
    @Autowired
    private ISysJobService jobService;
    
    /**
     * 初始化数据容器
     */
    public static class InitializationData {
        private final List<SysConfig> configs;
        private final List<SysDictType> dictTypes;
        private final List<SysDataSource> dataSources;
        private final List<SysJob> jobs;
        
        public InitializationData(List<SysConfig> configs, List<SysDictType> dictTypes, 
                                List<SysDataSource> dataSources, List<SysJob> jobs) {
            this.configs = configs;
            this.dictTypes = dictTypes;
            this.dataSources = dataSources;
            this.jobs = jobs;
        }
        
        public List<SysConfig> getConfigs() { return configs; }
        public List<SysDictType> getDictTypes() { return dictTypes; }
        public List<SysDataSource> getDataSources() { return dataSources; }
        public List<SysJob> getJobs() { return jobs; }
    }
    
    /**
     * 批量加载所有初始化数据
     * 在一个只读事务中完成所有查询，减少数据库交互
     */
    @Transactional(readOnly = true)
    public InitializationData loadAllInitializationData() {
        log.info("开始批量加载初始化数据...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 并行查询所有初始化数据
            CompletableFuture<List<SysConfig>> configsFuture = CompletableFuture
                .supplyAsync(() -> {
                    log.debug("查询系统配置数据...");
                    return configMapper.selectAll();
                }, executor);
            
            CompletableFuture<List<SysDictType>> dictTypesFuture = CompletableFuture
                .supplyAsync(() -> {
                    log.debug("查询字典类型数据...");
                    return dictTypeMapper.selectAll();
                }, executor);
            
            CompletableFuture<List<SysDataSource>> dataSourcesFuture = CompletableFuture
                .supplyAsync(() -> {
                    log.debug("查询数据源配置...");
                    return dataSourceMapper.selectActiveDataSources();
                }, executor);
            
            CompletableFuture<List<SysJob>> jobsFuture = CompletableFuture
                .supplyAsync(() -> {
                    log.debug("查询定时任务配置...");
                    return jobMapper.selectAll();
                }, executor);
            
            // 等待所有查询完成
            CompletableFuture.allOf(configsFuture, dictTypesFuture, dataSourcesFuture, jobsFuture).join();
            
            InitializationData data = new InitializationData(
                configsFuture.get(),
                dictTypesFuture.get(), 
                dataSourcesFuture.get(),
                jobsFuture.get()
            );
            
            long endTime = System.currentTimeMillis();
            log.info("批量加载初始化数据完成，耗时: {}ms, 配置项: {}, 字典类型: {}, 数据源: {}, 定时任务: {}", 
                    endTime - startTime,
                    data.getConfigs().size(),
                    data.getDictTypes().size(), 
                    data.getDataSources().size(),
                    data.getJobs().size());
            
            return data;
            
        } catch (Exception e) {
            log.error("批量加载初始化数据失败", e);
            throw new RuntimeException("批量加载初始化数据失败", e);
        }
    }
    
    /**
     * 异步批量加载初始化数据
     */
    @Async("threadPoolTaskExecutor")
    public CompletableFuture<InitializationData> loadAllInitializationDataAsync() {
        return CompletableFuture.completedFuture(loadAllInitializationData());
    }
    
    /**
     * 批量初始化数据源（异步并行处理）
     */
    @Async("threadPoolTaskExecutor")
    public CompletableFuture<Void> batchInitializeDataSources(List<SysDataSource> dataSources) {
        log.info("开始批量初始化 {} 个数据源", dataSources.size());
        long startTime = System.currentTimeMillis();
        
        try {
            // 并行初始化数据源，每个数据源的连接测试独立进行
            List<CompletableFuture<Boolean>> futures = dataSources.stream()
                .map(dataSource -> CompletableFuture.supplyAsync(() -> {
                    try {
                        boolean success = dataSourceManager.addOrUpdateDataSource(dataSource);
                        if (success) {
                            log.debug("数据源 [{}] 初始化成功", dataSource.getName());
                        } else {
                            log.warn("数据源 [{}] 初始化失败", dataSource.getName());
                        }
                        return success;
                    } catch (Exception e) {
                        log.error("数据源 [{}] 初始化异常: {}", dataSource.getName(), e.getMessage());
                        return false;
                    }
                }, executor))
                .collect(Collectors.toList());
            
            // 等待所有数据源初始化完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            return allOf.thenApply(v -> {
                long successCount = futures.stream()
                    .mapToLong(future -> {
                        try {
                            return future.get() ? 1 : 0;
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();
                
                long endTime = System.currentTimeMillis();
                log.info("数据源批量初始化完成，成功: {}/{}, 耗时: {}ms", 
                        successCount, dataSources.size(), endTime - startTime);
                return null;
            });
            
        } catch (Exception e) {
            log.error("批量初始化数据源时发生错误", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 应用启动完成后的初始化处理
     * 监听ApplicationReadyEvent事件，确保在Spring应用完全启动后再执行
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("threadPoolTaskExecutor")
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("应用启动完成，开始执行批量初始化...");
        
        try {
            // 批量加载初始化数据（只执行一次）
            InitializationData data = loadAllInitializationData();
            
            // 并行执行各种初始化任务，直接传递数据避免重复查询
            CompletableFuture<Void> configCacheInitFuture = CompletableFuture.runAsync(() -> {
                initializeConfigCacheWithData(data.getConfigs());
            }, executor);
            
            CompletableFuture<Void> dictCacheInitFuture = CompletableFuture.runAsync(() -> {
                initializeDictCacheWithData(data.getDictTypes());
            }, executor);
            
            CompletableFuture<Void> jobInitFuture = CompletableFuture.runAsync(() -> {
                initializeJobsWithData(data.getJobs());
            }, executor);
            
            CompletableFuture<Void> dataSourceInitFuture = batchInitializeDataSources(data.getDataSources());
            
            // 等待所有初始化任务完成
            CompletableFuture.allOf(configCacheInitFuture, dictCacheInitFuture, jobInitFuture, dataSourceInitFuture)
                .thenRun(() -> {
                    log.info("所有批量初始化任务完成");
                })
                .exceptionally(throwable -> {
                    log.error("批量初始化过程中发生错误", throwable);
                    return null;
                });
                
        } catch (Exception e) {
            log.error("应用启动后初始化失败", e);
        }
    }
    
    /**
     * 初始化缓存数据
     */
    @Async("threadPoolTaskExecutor")
    public CompletableFuture<Void> initializeCaches(InitializationData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("开始初始化缓存数据...");
                
                // 批量加载配置缓存
                if (!data.getConfigs().isEmpty()) {
                    optimizedRedisCache.batchLoadConfigCache(data.getConfigs());
                }
                
                // 批量加载字典缓存
                if (!data.getDictTypes().isEmpty()) {
                    optimizedRedisCache.batchLoadDictCache(data.getDictTypes());
                }
                
                log.info("缓存数据初始化完成");
                
            } catch (Exception e) {
                log.error("缓存数据初始化失败", e);
                throw new RuntimeException("缓存数据初始化失败", e);
            }
        }, executor);
    }
    
    /**
     * 使用已有数据初始化配置缓存（避免重复查询数据库）
     */
    private void initializeConfigCacheWithData(List<SysConfig> configs) {
        try {
            log.info("开始初始化系统配置缓存...");
            long startTime = System.currentTimeMillis();
            
            // 使用优化的Redis缓存批量加载
            optimizedRedisCache.batchLoadConfigCache(configs);
            
            long endTime = System.currentTimeMillis();
            log.info("系统配置缓存初始化完成，加载 {} 个配置项，耗时: {}ms", 
                    configs.size(), endTime - startTime);
                    
        } catch (Exception e) {
            log.error("初始化系统配置缓存失败", e);
            // 降级到Service层的原有方案
            configService.loadingConfigCache();
        }
    }
    
    /**
     * 使用已有数据初始化字典缓存（避免重复查询数据库）
     */
    private void initializeDictCacheWithData(List<SysDictType> dictTypes) {
        try {
            log.info("开始初始化字典缓存...");
            long startTime = System.currentTimeMillis();
            
            // 处理字典数据，为每个字典类型关联对应的字典数据
            List<SysDictType> dictTypesWithData = processDictTypesWithData(dictTypes);
            
            // 使用优化的Redis缓存批量加载
            optimizedRedisCache.batchLoadDictCache(dictTypesWithData);
            
            long endTime = System.currentTimeMillis();
            log.info("字典缓存初始化完成，加载 {} 个字典类型，耗时: {}ms", 
                    dictTypesWithData.size(), endTime - startTime);
                    
        } catch (Exception e) {
            log.error("初始化字典缓存失败", e);
            // 降级到Service层的原有方案
            dictTypeService.loadingDictCache();
        }
    }
    
    /**
     * 处理字典类型，为每个类型关联对应的字典数据
     */
    private List<SysDictType> processDictTypesWithData(List<SysDictType> dictTypes) {
        // 查询所有字典数据
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dict_data")
            .where(new QueryColumn("status").eq("0"))
            .orderBy(new QueryColumn("dict_sort").asc());
               
        List<SysDictData> allDictData = dictDataMapper.selectListByQuery(queryWrapper);
        
        // 按字典类型分组
        Map<String, List<SysDictData>> dictDataMap = allDictData.stream()
                .collect(Collectors.groupingBy(SysDictData::getDictType));
        
        // 为每个字典类型设置对应的字典数据
        for (SysDictType dictType : dictTypes) {
            List<SysDictData> dictDataList = dictDataMap.get(dictType.getDictType());
            if (dictDataList != null && !dictDataList.isEmpty()) {
                dictType.setDictDataList(dictDataList.stream()
                    .sorted(Comparator.comparing(SysDictData::getDictSort))
                    .collect(Collectors.toList()));
            }
        }
        
        return dictTypes;
    }
    
    /**
     * 使用已有数据初始化定时任务（避免重复查询数据库）
     */
    private void initializeJobsWithData(List<SysJob> jobs) {
        try {
            log.info("开始初始化定时任务...");
            long startTime = System.currentTimeMillis();
            
            // 清空调度器
            Scheduler scheduler = SpringUtils.getBean(Scheduler.class);
            scheduler.clear();
            
            log.info("检测到 {} 个定时任务需要初始化", jobs.size());
            
            int successCount = 0;
            for (SysJob job : jobs) {
                try {
                    ScheduleUtils.createScheduleJob(scheduler, job);
                    successCount++;
                    log.debug("定时任务 [{}] 初始化成功", job.getJobName());
                } catch (Exception e) {
                    log.error("定时任务 [{}] 初始化失败: {}", job.getJobName(), e.getMessage());
                }
            }
            
            long endTime = System.currentTimeMillis();
            log.info("定时任务初始化完成，成功: {}/{}, 耗时: {}ms", 
                    successCount, jobs.size(), endTime - startTime);
                    
        } catch (Exception e) {
            log.error("初始化定时任务时发生错误", e);
        }
    }
}