package com.ruoyi.framework.service;

import java.util.Comparator;
import java.util.HashMap;
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

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.utils.job.ScheduleUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.datasource.DynamicDataSourceManager;
import com.ruoyi.framework.redis.OptimizedRedisCache;
import com.ruoyi.project.ai.domain.AiWorkflowSchedule;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleService;
import com.ruoyi.project.monitor.domain.SysJob;
import com.ruoyi.project.monitor.mapper.SysJobMapper;
import com.ruoyi.project.monitor.service.ISysJobService;
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

import cn.hutool.core.collection.CollUtil;

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
    
    @Lazy
    @Autowired
    private IAiWorkflowScheduleService workflowScheduleService;
    
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
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("threadPoolTaskExecutor")
    public void onApplicationReady(ApplicationReadyEvent event) {
        long overallStartTime = System.currentTimeMillis();
        log.info("应用启动完成，开始执行批量初始化...");
        
        try {
            // 1. 异步加载所有初始化数据
            long dataLoadStart = System.currentTimeMillis();
            CompletableFuture<InitializationData> dataFuture = loadAllInitializationDataAsync();
            InitializationData data = dataFuture.get();
            long dataLoadDuration = System.currentTimeMillis() - dataLoadStart;
            
            log.info("初始化数据加载完成，耗时: {}ms，数据统计: 配置{}个，字典类型{}个，数据源{}个，定时任务{}个", 
                    dataLoadDuration, 
                    data.getConfigs().size(), 
                    data.getDictTypes().size(), 
                    data.getDataSources().size(), 
                    data.getJobs().size());
            
            // 2. 并行初始化缓存和数据源
            long parallelInitStart = System.currentTimeMillis();
            CompletableFuture<Void> cacheInitFuture = initializeCaches(data);
            CompletableFuture<Void> dataSourceInitFuture = batchInitializeDataSources(data.getDataSources());
            
            // 等待缓存和数据源初始化完成
            CompletableFuture.allOf(cacheInitFuture, dataSourceInitFuture).get();
            long parallelInitDuration = System.currentTimeMillis() - parallelInitStart;
            
            log.info("并行初始化（缓存+数据源）完成，耗时: {}ms", parallelInitDuration);
            
            // 3. 初始化定时任务（需要在数据源初始化后进行）
            long jobInitStart = System.currentTimeMillis();
            initializeJobsWithData(data.getJobs());
            long jobInitDuration = System.currentTimeMillis() - jobInitStart;
            
            log.info("定时任务初始化完成，耗时: {}ms", jobInitDuration);
            
            // 4. 记录总体性能指标
            long overallDuration = System.currentTimeMillis() - overallStartTime;
            log.info("批量初始化全部完成！总耗时: {}ms，详细耗时: 数据加载{}ms，并行初始化{}ms，任务初始化{}ms", 
                    overallDuration, dataLoadDuration, parallelInitDuration, jobInitDuration);
                    
        } catch (Exception e) {
            long overallDuration = System.currentTimeMillis() - overallStartTime;
            log.error("批量初始化过程中发生错误，已耗时: {}ms", overallDuration, e);
        }
    }

    /**
     * 异步初始化缓存
     */
    @Async("threadPoolTaskExecutor")
    public CompletableFuture<Void> initializeCaches(InitializationData data) {
        long startTime = System.currentTimeMillis();
        log.info("开始初始化缓存...");
        
        try {
            // 并行初始化各种缓存
            long configCacheStart = System.currentTimeMillis();
            initializeConfigCacheWithData(data.getConfigs());
            long configCacheDuration = System.currentTimeMillis() - configCacheStart;
            
            long dictCacheStart = System.currentTimeMillis();
            initializeDictCacheWithData(data.getDictTypes());
            long dictCacheDuration = System.currentTimeMillis() - dictCacheStart;
            
            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("缓存初始化完成，总耗时: {}ms，详细耗时: 配置缓存{}ms，字典缓存{}ms", 
                    totalDuration, configCacheDuration, dictCacheDuration);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("缓存初始化失败，耗时: {}ms", duration, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 使用已有数据初始化系统配置缓存（避免重复查询数据库）
     */
    private void initializeConfigCacheWithData(List<SysConfig> configs) {
        long startTime = System.currentTimeMillis();
        log.info("开始初始化系统配置缓存，配置数量: {}", configs.size());
        
        try {
            int successCount = 0;
            int errorCount = 0;
            
            // 准备批量缓存数据
            Map<String, Object> cacheData = new HashMap<>();
            
            for (SysConfig config : configs) {
                try {
                    String cacheKey = "sys_config:" + config.getConfigKey();
                    cacheData.put(cacheKey, config.getConfigValue());
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.warn("配置缓存准备失败，key: {}, value: {}", config.getConfigKey(), config.getConfigValue(), e);
                }
            }
            
            // 批量设置缓存
            if (!cacheData.isEmpty()) {
                optimizedRedisCache.batchSetCache(cacheData, null);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("系统配置缓存初始化完成，成功: {}, 失败: {}, 耗时: {}ms", 
                    successCount, errorCount, duration);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("系统配置缓存初始化失败，耗时: {}ms", duration, e);
        }
    }

    /**
     * 使用已有数据初始化字典缓存（避免重复查询数据库）
     */
    private void initializeDictCacheWithData(List<SysDictType> dictTypes) {
        long startTime = System.currentTimeMillis();
        log.info("开始初始化字典缓存，字典类型数量: {}", dictTypes.size());
        
        try {
            int typeSuccessCount = 0;
            int typeErrorCount = 0;
            int dataSuccessCount = 0;
            int dataErrorCount = 0;
            
            // 处理字典类型数据 - 为每个类型关联对应的字典数据
            List<SysDictType> processedDictTypes = processDictTypesWithData(dictTypes);
            
            // 准备批量缓存数据
            Map<String, Object> typeCacheData = new HashMap<>();
            Map<String, Object> dataCacheData = new HashMap<>();
            
            for (SysDictType dictType : processedDictTypes) {
                try {
                    // 使用不同的缓存键来存储 SysDictType 对象，避免与 DictUtils 的键冲突
                    String typeCacheKey = "sys_dict_type:" + dictType.getDictType();
                    typeCacheData.put(typeCacheKey, dictType);
                    typeSuccessCount++;
                    
                    // 同时为 DictUtils 准备字典数据缓存（List<SysDictData>）
                    if (dictType.getDictDataList() != null && !dictType.getDictDataList().isEmpty()) {
                        String dictDataCacheKey = "sys_dict:" + dictType.getDictType();
                        dataCacheData.put(dictDataCacheKey, dictType.getDictDataList());
                        
                        // 准备单个字典数据缓存
                        for (SysDictData dictData : dictType.getDictDataList()) {
                            try {
                                String dataCacheKey = "sys_dict_data:" + dictType.getDictType() + ":" + dictData.getDictValue();
                                dataCacheData.put(dataCacheKey, dictData);
                                dataSuccessCount++;
                            } catch (Exception e) {
                                dataErrorCount++;
                                log.warn("字典数据缓存准备失败，type: {}, value: {}", 
                                        dictType.getDictType(), dictData.getDictValue(), e);
                            }
                        }
                    }
                } catch (Exception e) {
                    typeErrorCount++;
                    log.warn("字典类型缓存准备失败，type: {}", dictType.getDictType(), e);
                }
            }
            
            // 批量设置缓存
            if (!typeCacheData.isEmpty()) {
                optimizedRedisCache.batchSetCache(typeCacheData, null);
            }
            if (!dataCacheData.isEmpty()) {
                optimizedRedisCache.batchSetCache(dataCacheData, null);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("字典缓存初始化完成，字典类型: 成功{}/失败{}, 字典数据: 成功{}/失败{}, 耗时: {}ms", 
                    typeSuccessCount, typeErrorCount, dataSuccessCount, dataErrorCount, duration);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("字典缓存初始化失败，耗时: {}ms", duration, e);
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

        Map<String, List<SysDictData>> dictDataMap = dictDataMapper.selectListByQuery(queryWrapper)
                .stream().collect(Collectors.groupingBy(SysDictData::getDictType));

        // 为每个字典类型设置对应的字典数据
        for (SysDictType dictType : dictTypes) {
            List<SysDictData> dictDataList = dictDataMap.get(dictType.getDictType());
            if (CollUtil.isNotEmpty(dictDataList)) {
                dictType.setDictDataList(dictDataList.stream()
                        .sorted(Comparator.comparing(SysDictData::getDictSort))
                        .collect(Collectors.toList()));
            }
        }

        return dictTypes;
    }

    /**
     * 使用已有数据初始化定时任务（避免重复查询数据库）
     * 包含普通任务和AI工作流调度任务的统一初始化
     */
    private void initializeJobsWithData(List<SysJob> jobs) {
        long startTime = System.currentTimeMillis();
        log.info("开始初始化定时任务，任务总数: {}", jobs.size());
        
        try {
            // 清空调度器
            long schedulerClearStart = System.currentTimeMillis();
            Scheduler scheduler = SpringUtils.getBean(Scheduler.class);
            scheduler.clear();
            long schedulerClearDuration = System.currentTimeMillis() - schedulerClearStart;
            
            log.info("调度器清空完成，耗时: {}ms", schedulerClearDuration);
            
            // 第一阶段：初始化普通定时任务（排除AI工作流调度任务）
            long filterStart = System.currentTimeMillis();
            List<SysJob> regularJobs = jobs.stream()
                .filter(job -> !"AI_WORKFLOW".equals(job.getJobGroup()) && 
                              !"WORKFLOW_SCHEDULE".equals(job.getJobGroup()) &&
                              !job.getJobName().startsWith("WORKFLOW_SCHEDULE_"))
                .collect(Collectors.toList());
            long filterDuration = System.currentTimeMillis() - filterStart;
            
            int excludedCount = jobs.size() - regularJobs.size();
            log.info("任务过滤完成，排除{}个AI工作流调度任务，剩余{}个普通任务需要初始化，耗时: {}ms", 
                    excludedCount, regularJobs.size(), filterDuration);
            
            // 初始化普通定时任务
            long jobCreateStart = System.currentTimeMillis();
            int regularSuccessCount = 0;
            int regularErrorCount = 0;
            
            for (SysJob job : regularJobs) {
                try {
                    ScheduleUtils.createScheduleJob(scheduler, job);
                    regularSuccessCount++;
                    log.debug("普通定时任务 [{}] 初始化成功", job.getJobName());
                } catch (Exception e) {
                    regularErrorCount++;
                    log.error("普通定时任务 [{}] 初始化失败: {}", job.getJobName(), e.getMessage());
                }
            }
            
            long jobCreateDuration = System.currentTimeMillis() - jobCreateStart;
            log.info("普通定时任务初始化完成，成功: {}, 失败: {}, 耗时: {}ms", 
                    regularSuccessCount, regularErrorCount, jobCreateDuration);
            
            // 第二阶段：初始化AI工作流调度任务（从 ai_workflow_schedule 表读取配置）
            long aiJobStart = System.currentTimeMillis();
            int aiSuccessCount = 0;
            int aiErrorCount = 0;
            
            try {
                // 获取所有启用的AI工作流调度配置
                List<AiWorkflowSchedule> enabledSchedules = workflowScheduleService.listEnabledSchedules();
                log.info("发现 {} 个启用的AI工作流调度配置", enabledSchedules.size());
                
                for (AiWorkflowSchedule schedule : enabledSchedules) {
                    try {
                        // 创建Quartz任务对象
                        SysJob aiJob = workflowScheduleService.createQuartzJobForInit(schedule);
                        // 直接在调度器中创建任务（不保存到 sys_job 表）
                        ScheduleUtils.createScheduleJob(scheduler, aiJob);
                        aiSuccessCount++;
                        log.debug("AI工作流调度任务 [{}] 初始化成功", aiJob.getJobName());
                        
                        // 更新下次执行时间
                        workflowScheduleService.updateNextExecutionTime(schedule.getId());
                    } catch (Exception e) {
                        aiErrorCount++;
                        log.error("AI工作流调度任务 [{}] 初始化失败: {}", 
                                "WORKFLOW_SCHEDULE_" + schedule.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("加载AI工作流调度配置失败", e);
            }
            
            long aiJobDuration = System.currentTimeMillis() - aiJobStart;
            long totalDuration = System.currentTimeMillis() - startTime;
            
            log.info("AI工作流调度任务初始化完成，成功: {}, 失败: {}, 耗时: {}ms", 
                    aiSuccessCount, aiErrorCount, aiJobDuration);
            
            log.info("所有定时任务初始化完成，普通任务成功: {}/失败: {}，AI工作流任务成功: {}/失败: {}，总耗时: {}ms", 
                    regularSuccessCount, regularErrorCount, aiSuccessCount, aiErrorCount, totalDuration);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("初始化定时任务时发生错误，耗时: {}ms", duration, e);
        }
    }
}