package com.ruoyi.framework.config.magic.swagger;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "magic-api.springdoc")
public class MagicApiSpringDocProperties {
    
    /**
     * 是否启用Magic API文档
     */
    private boolean enabled = true;
    
    /**
     * 分组名称
     */
    private String groupName = "magic-api";
    
    /**
     * 文档标题
     */
    private String title = "Magic API接口文档";
    
    /**
     * 文档描述
     */
    private String description = "Magic API动态生成的接口文档";
    
    /**
     * 文档版本
     */
    private String version = "1.0.0";
    
    /**
     * API路径前缀
     */
    private String apiPrefix = "/magic";
    
    // getter和setter方法
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getApiPrefix() {
        return apiPrefix;
    }
    
    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }
}
