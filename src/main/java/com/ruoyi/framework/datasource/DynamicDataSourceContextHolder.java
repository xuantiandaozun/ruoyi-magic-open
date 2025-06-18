package com.ruoyi.framework.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mybatisflex.core.datasource.DataSourceKey;

/**
 * 数据源切换处理
 * 
 * @author ruoyi
 */
public class DynamicDataSourceContextHolder
{
    public static final Logger log = LoggerFactory.getLogger(DynamicDataSourceContextHolder.class);
    
    /** 主数据源标识 */
    public static final String MASTER = "MASTER";
    


    /**
     * 设置数据源的变量
     */
    public static void setDataSourceType(String dsType)
    {
        log.info("切换到{}数据源", dsType);
        DataSourceKey.use(dsType);
    }

    /**
     * 获得数据源的变量
     */
    public static String getDataSourceType()
    {
        return DataSourceKey.get();
    }

    /**
     * 清空数据源变量
     */
    public static void clearDataSourceType()
    {
        DataSourceKey.clear();
    }
}

