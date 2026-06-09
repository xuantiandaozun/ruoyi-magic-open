package com.ruoyi.framework.security.sign;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 为签名接口提供可重复读取的请求体
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiSignBodyCacheFilter extends OncePerRequestFilter {

    private final ApiSignConfig apiSignConfig;

    public ApiSignBodyCacheFilter(ApiSignConfig apiSignConfig) {
        this.apiSignConfig = apiSignConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!apiSignConfig.requiresBodyHash(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
    }
}
