package com.ruoyi.framework.datasource;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.mapper.SysDataSourceMapper;

/**
 * 动态数据源初始化器，在应用启动时从数据库加载数据源配置
 * 
 * @author ruoyi-magic
 */
@Component
public class DynamicDataSourceInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceInitializer.class);

    @Autowired
    private SysDataSourceMapper dataSourceMapper;
    
    @Autowired
    private DynamicDataSourceManager dataSourceManager;
    
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
            // 使用MyBatisFlex查询状态为正常的数据源
            QueryWrapper queryWrapper = QueryWrapper.create()
                .from("sys_data_source")
                .where(new QueryColumn("status").eq("0"))
                .and(new QueryColumn("name").ne("MASTER")); // 主数据源由Spring Boot自动配置
            
            List<SysDataSource> dataSources = dataSourceMapper.selectListByQuery(queryWrapper);
            log.info("检测到 {} 个数据源配置需要加载", dataSources.size());
            
            for (SysDataSource dataSource : dataSources) {
                // 直接使用SysDataSource对象进行添加，不再转换为DataSourceInfo
                boolean success = dataSourceManager.addOrUpdateDataSource(dataSource);
                if (success) {
                    log.info("数据源 [{}] 加载成功", dataSource.getName());
                } else {
                    log.error("数据源 [{}] 加载失败", dataSource.getName());
                }
            }
            
            log.info("动态数据源异步初始化完成");
        } catch (Exception e) {
            log.error("异步初始化动态数据源时发生错误: {}", e.getMessage(), e);
        }
    }
}
