package com.ruoyi.framework.datasource;

import org.springframework.stereotype.Component;

/**
 * 数据源工具类
 */
@Component
public class DataSourceUtils {

    /**
     * 使用主数据源执行操作
     *
     * @param action 要执行的操作
     * @return 操作结果
     */
    public <T> T executeWithMaster(DataSourceAction<T> action) {
        String currentDataSource = DynamicDataSourceContextHolder.getDataSourceType();
        try {
            DynamicDataSourceContextHolder.setDataSourceType("MASTER");
            return action.execute();
        } finally {
            if (currentDataSource != null) {
                DynamicDataSourceContextHolder.setDataSourceType(currentDataSource);
            } else {
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        }
    }

    /**
     * 在指定数据源上执行操作
     *
     * @param dataSourceName 数据源名称
     * @param action 要执行的操作
     * @return 操作结果
     */
    public <T> T executeWithDataSource(String dataSourceName, DataSourceAction<T> action) {
        String currentDataSource = DynamicDataSourceContextHolder.getDataSourceType();
        try {
            DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            return action.execute();
        } finally {
            if (currentDataSource != null) {
                DynamicDataSourceContextHolder.setDataSourceType(currentDataSource);
            } else {
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        }
    }

    /**
     * 数据源操作接口
     */
    @FunctionalInterface
    public interface DataSourceAction<T> {
        T execute();
    }
} 