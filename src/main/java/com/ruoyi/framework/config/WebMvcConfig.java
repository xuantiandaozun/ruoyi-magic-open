package com.ruoyi.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import cn.dev33.satoken.interceptor.SaInterceptor;

/**
 * WebMvc配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

        @SuppressWarnings("null")
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                /** knife4j配置 */
                registry.addResourceHandler("doc.html")
                                .addResourceLocations("classpath:/META-INF/resources/");
                registry.addResourceHandler("/webjars/**")
                                .addResourceLocations("classpath:/META-INF/resources/webjars/");
                registry.addResourceHandler("/swagger-ui/**")
                                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
                registry.addResourceHandler("/v3/api-docs/**")
                                .addResourceLocations("classpath:/META-INF/resources/");
                // 添加下面这一行来处理favicon.ico
                registry.addResourceHandler("/static/favicon.ico")
                        .addResourceLocations("classpath:/static/")
                        .setCachePeriod(3600);

        }

        @SuppressWarnings("null")
        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                                .allowedOriginPatterns("*")
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(true)
                                .maxAge(3600);
        }

        @SuppressWarnings("null")
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
                // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
                registry.addInterceptor(new SaInterceptor(handle -> {
                        // 登录校验拦截
                        cn.dev33.satoken.stp.StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录接口
                        "/login",
                        "/register",
                        "/captchaImage",
                        // 飞书回调
                        "/feishu/callback",
                        "/system/user/profile/feishu/callback",
                        // MCP 和 SSE
                        "/mcp/messages",
                        "/sse",
                        "/v2/**",
                        // 下载接口
                        "/common/download/**",
                        // API 文档
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/*/api-docs",
                        "/doc.html",
                        // magic-api 接口
                        "/magic/**",
                        "/api/**"
                );
        }
}