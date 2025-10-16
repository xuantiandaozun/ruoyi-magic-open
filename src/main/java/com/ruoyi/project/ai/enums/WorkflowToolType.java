package com.ruoyi.project.ai.enums;

/**
 * 工作流工具类型枚举
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public enum WorkflowToolType {
    
    /** GitHub趋势查询工具 */
    GITHUB_TRENDING("github_trending", "GitHub趋势查询", "查询GitHub今日/本周/本月趋势仓库"),
    
    /** GitHub仓库搜索工具 */
    GITHUB_SEARCH("github_search", "GitHub仓库搜索", "根据关键词搜索GitHub仓库"),
    
    /** 数据库查询工具 */
    DATABASE_QUERY("database_query", "数据库查询", "执行数据库查询操作"),
    
    /** HTTP请求工具 */
    HTTP_REQUEST("http_request", "HTTP请求", "发送HTTP请求获取数据"),
    
    /** 文件操作工具 */
    FILE_OPERATION("file_operation", "文件操作", "读取、写入文件内容");
    
    private final String code;
    private final String name;
    private final String description;
    
    WorkflowToolType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取工具类型
     */
    public static WorkflowToolType getByCode(String code) {
        for (WorkflowToolType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}