package com.ruoyi.framework.config.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import com.ruoyi.framework.aspectj.lang.annotation.Anonymous;

/**
 * 设置Anonymous注解允许匿名访问的url
 * 
 * @author ruoyi
 */
@Configuration
public class PermitAllUrlProperties implements InitializingBean, ApplicationContextAware
{
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    private ApplicationContext applicationContext;

    private List<String> urls = new ArrayList<>();

    public String ASTERISK = "*";

    @SuppressWarnings("null")
    @Override
    public void afterPropertiesSet()
    {
        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法上边的注解 替代path variable 为 *
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            Optional.ofNullable(method).ifPresent(anonymous -> {
                Set<String> patterns = getPatterns(info);
                patterns.forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK)));
            });

            // 获取类上边的注解, 替代path variable 为 *
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            Optional.ofNullable(controller).ifPresent(anonymous -> {
                Set<String> patterns = getPatterns(info);
                patterns.forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK)));
            });
        });
    }

    /**
     * 获取请求映射的路径模式
     * 兼容 Spring Boot 3.x 的新 API
     */
    private Set<String> getPatterns(RequestMappingInfo info)
    {
        // Spring Boot 3.x 使用 PathPatternsCondition
        if (info.getPathPatternsCondition() != null)
        {
            return info.getPathPatternsCondition().getPatterns()
                    .stream()
                    .map(PathPattern::getPatternString)
                    .collect(java.util.stream.Collectors.toSet());
        }
        // 兼容旧版本
        else if (info.getPatternsCondition() != null)
        {
            return info.getPatternsCondition().getPatterns();
        }
        return java.util.Collections.emptySet();
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException
    {
        this.applicationContext = context;
    }

    public List<String> getUrls()
    {
        return urls;
    }

    public void setUrls(List<String> urls)
    {
        this.urls = urls;
    }
}
