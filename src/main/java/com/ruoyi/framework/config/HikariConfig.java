package com.ruoyi.framework.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import com.mybatisflex.core.datasource.FlexDataSource;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.datasource.DynamicDataSourceContextHolder;
import com.zaxxer.hikari.HikariDataSource;

/**
 * HikariCP 配置多数据源
 * 
 * @author ruoyi
 */
@Configuration
public class HikariConfig
{
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource masterDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName)
    {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        // 其他HikariCP特定配置会通过@ConfigurationProperties自动注入
        return dataSource;
    }
    
    @Bean(name = "dynamicDataSource")
    @Primary
    @Order(2)
    public FlexDataSource dataSource(HikariDataSource masterDataSource)
    {
        // 创建 FlexDataSource 实例，并将 masterDataSource 作为默认数据源
        FlexDataSource flexDataSource = new FlexDataSource(DynamicDataSourceContextHolder.MASTER, masterDataSource);
        
        return flexDataSource;
    }
    
    /**
     * 设置数据源
     * 
     * @param targetDataSources 备选数据源集合
     * @param sourceName 数据源名称
     * @param beanName bean名称
     */
    public void setDataSource(Map<Object, Object> targetDataSources, String sourceName, String beanName)
    {
        try
        {
            DataSource dataSource = SpringUtils.getBean(beanName);
            targetDataSources.put(sourceName, dataSource);
        }
        catch (Exception e)
        {
        }
    }
}