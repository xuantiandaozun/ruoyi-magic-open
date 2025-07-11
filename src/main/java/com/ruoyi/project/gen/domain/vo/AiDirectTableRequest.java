package com.ruoyi.project.gen.domain.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能直接建表请求DTO
 * 用于AI直接创建表并同步到数据库，不返回实体
 */
@Data
public class AiDirectTableRequest {
    
    /**
     * 建表需求
     */
    @NotBlank(message = "建表需求不能为空")
    private String requirement;
    
    /**
     * 包名
     */
    @NotBlank(message = "包名不能为空")
    private String packageName;

    /**
     * 模块名
     */
    @NotBlank(message = "模块名不能为空")
    private String moduleName;
    
    /**
     * 数据源名称
     */
    private String dataSource = "MASTER"; // 默认使用MASTER数据源
}