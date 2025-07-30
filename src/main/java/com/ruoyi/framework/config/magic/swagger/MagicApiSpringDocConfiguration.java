package com.ruoyi.framework.config.magic.swagger;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MagicApiSpringDocProperties.class)
@ConditionalOnProperty(name = "magic-api.springdoc.enabled", havingValue = "true", matchIfMissing = true)
public class MagicApiSpringDocConfiguration {
    
    @Autowired
    private MagicApiSpringDocProperties properties;
    
    @Bean
    public GroupedOpenApi magicApiGroup() {
        return GroupedOpenApi.builder()
                .group(properties.getGroupName())
                .pathsToMatch(properties.getApiPrefix() + "/**")
                .addOpenApiCustomizer(magicApiOpenApiCustomizer())
                .build();
    }
    
    @Bean
    public OpenApiCustomizer magicApiOpenApiCustomizer() {
        return new MagicApiOpenApiCustomizer(properties);
    }
    

}
