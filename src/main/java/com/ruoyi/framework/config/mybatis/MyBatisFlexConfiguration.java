package com.ruoyi.framework.config.mybatis;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Configuration;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.spring.boot.ConfigurationCustomizer;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import com.ruoyi.framework.config.mybatis.handler.MultiFormatDateTypeHandler;
import com.ruoyi.framework.config.mybatis.listener.BaseEntityInsertListener;
import com.ruoyi.framework.config.mybatis.listener.BaseEntityUpdateListener;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * MyBatis-Flex 配置类
 */
@Configuration
public class MyBatisFlexConfiguration implements MyBatisFlexCustomizer, ConfigurationCustomizer {


    
    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 注册插入和更新监听器
        globalConfig.registerInsertListener(new BaseEntityInsertListener(), BaseEntity.class);
        globalConfig.registerUpdateListener(new BaseEntityUpdateListener(), BaseEntity.class);

    }

    @Override
    public void customize(FlexConfiguration configuration) {
        // 注册 LocalDateTime 的 TypeHandler
        configuration.getTypeHandlerRegistry().register(LocalDateTime.class, MultiFormatDateTypeHandler.class);




 
    }


}