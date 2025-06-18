package com.ruoyi.framework.datasource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource; // 确保导入

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.config.properties.DruidProperties;
import com.ruoyi.project.system.domain.SysDataSource;

/**
 * 动态数据源管理
 * 
 * @author ruoyi-magic
 */
@Component
public class DynamicDataSourceManager {
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceManager.class);

    @Autowired
    private DruidProperties druidProperties;
    
    @Autowired
    private FlexDataSource flexDataSource; // 直接使用 FlexDataSource

    // 用于跟踪由 DynamicDataSourceManager 管理的数据源
    private final Map<String, DataSource> managedDataSources = new ConcurrentHashMap<>();
    
    /**
     * 添加或更新数据源
     * 
     * @param sysDataSource 数据源信息
     * @return 是否成功
     */
    public boolean addOrUpdateDataSource(SysDataSource sysDataSource) {
        String key = sysDataSource.getName();
        try {
            // 创建数据源
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(sysDataSource.getUrl());
            dataSource.setUsername(sysDataSource.getUsername());
            dataSource.setPassword(sysDataSource.getPassword());
            dataSource.setDriverClassName(sysDataSource.getDriverClassName());
            
            druidProperties.dataSource(dataSource);
            
            // 初始化并检测连接是否有效
            dataSource.init();
            if (!dataSource.isEnable()) {
                log.warn("数据源 {} (URL: {}) 初始化后未启用。", key, sysDataSource.getUrl());
                return false;
            }
            
            DataSource oldDataSource = managedDataSources.get(key);
            
            flexDataSource.addDataSource(key, dataSource);
            managedDataSources.put(key, dataSource); // 更新 managedDataSources
            
            if (oldDataSource instanceof DruidDataSource && oldDataSource != dataSource) {
                ((DruidDataSource) oldDataSource).close();
                log.info("已关闭旧的数据源实例: {}", key);
            }
            
            log.info("添加或更新数据源 {} 成功", key);
            return true;
        } catch (Exception e) {
            log.error("添加或更新数据源 {} 失败: {}", key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 移除数据源
     */
    public boolean removeDataSource(String name) {
        if ("MASTER".equalsIgnoreCase(name)) {
            throw new ServiceException("不能移除主数据源");
        }
        
        try {
            DataSource removedDataSource = managedDataSources.get(name);
            
            if (removedDataSource == null) {
                log.warn("尝试移除一个在管理器中不存在的数据源: {}", name);
                // 即使管理器中没有，也尝试从 FlexDataSource 中移除，以防外部直接操作
                flexDataSource.removeDatasource(name); 
                return false;
            }
            
            flexDataSource.removeDatasource(name); // 从 FlexDataSource 移除
            managedDataSources.remove(name); // 从 managedDataSources 移除
            
            if (removedDataSource instanceof DruidDataSource) {
                ((DruidDataSource) removedDataSource).close();
                log.info("已关闭数据源 {}", name);
            }
            
            log.info("移除数据源 {} 成功", name);
            return true;
        } catch (Exception e) {
            log.error("移除数据源 {} 失败: {}", name, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 判断数据源是否存在
     */
    public boolean dataSourceExists(String name) {
        return managedDataSources.containsKey(name);
    }
    
    /**
     * 清除所有数据源信息（不包括MASTER）
     */
    public void clearDataSources() {
        // 创建一个副本以避免ConcurrentModificationException
        new ConcurrentHashMap<>(managedDataSources).keySet().forEach(key -> {
            if (!"MASTER".equalsIgnoreCase(key)) {
                removeDataSource(key);
            }
        });
        
        log.info("已清除所有由管理器管理的非主数据源。");
    }
}

