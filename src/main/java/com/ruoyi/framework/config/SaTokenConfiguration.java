package com.ruoyi.framework.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 */
@Configuration
public class SaTokenConfiguration implements WebMvcConfigurer {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 注册 Sa-Token 全局过滤器
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
            .addInclude("/**")  // 拦截所有请求
            .setAuth(obj -> {
                // 登录验证 -- 拦截所有路由，并排除/login 用于开放登录
                SaRouter.match("/**")
                    // 排除登录接口
                    .notMatch("/login")  
                    .notMatch("/captchaImage")
                    .notMatch("/register")
                    // 排除下载接口
                    .notMatch("/common/download/**")
                    // 排除API文档
                    .notMatch("/v3/api-docs/**")
                    .notMatch("/swagger-ui/**")
                    .notMatch("/swagger-resources/**")
                    .notMatch("/swagger-ui.html")
                    .notMatch("/webjars/**")
                    .notMatch("/*/api-docs")
                    .notMatch("/doc.html")
                    .notMatch("/druid/**")
                    .check(r -> StpUtil.checkLogin());
            })
            .setError(e -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) SaHolder.getResponse().getSource();
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(SaResult.error(e.getMessage())));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                return null;
            });
    }
}