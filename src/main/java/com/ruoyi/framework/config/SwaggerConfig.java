package com.ruoyi.framework.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Knife4j 的接口配置
 * 
 * @author ruoyi
 */
@Configuration
@EnableKnife4j
public class SwaggerConfig {
    /** 系统基础配置 */
    @Autowired
    private RuoYiConfig ruoyiConfig;

    /** 是否开启swagger */
    @Value("${swagger.enabled}")
    private boolean enabled;

    /** 设置请求的统一前缀 */
    @Value("${swagger.pathMapping}")
    private String pathMapping;

    /**
     * 创建API
     */
    @Bean
    public OpenAPI createRestApi() {
        // 定义安全模式
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("authorization");

        // 全局安全需求
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("authorization");

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("authorization", securityScheme))
                .info(new Info()
                        .title("接口文档")
                        .contact(new Contact()
                                .name(ruoyiConfig.getName()))
                        .version("版本号:" + ruoyiConfig.getVersion()))
                .addSecurityItem(securityRequirement);
    }
    
    /**
     * 系统接口分组
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("系统接口")
                .pathsToMatch("/**")
                .pathsToExclude("/magic/**") // 排除Magic API路径
                .build();
    }
}