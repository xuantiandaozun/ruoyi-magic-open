package com.ruoyi.project.ai.dto;

import lombok.Data;

/**
 * 工具执行结果
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
public class ToolExecutionResult {
    
    /** 执行是否成功 */
    private boolean success;
    
    /** 执行结果数据 */
    private String data;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 执行耗时（毫秒） */
    private long executionTime;
    
    /** 工具名称 */
    private String toolName;
    
    /** 工具类型 */
    private String toolType;
    
    public static ToolExecutionResult success(String toolName, String toolType, String data, long executionTime) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(true);
        result.setToolName(toolName);
        result.setToolType(toolType);
        result.setData(data);
        result.setExecutionTime(executionTime);
        return result;
    }
    
    public static ToolExecutionResult failure(String toolName, String toolType, String errorMessage, long executionTime) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(false);
        result.setToolName(toolName);
        result.setToolType(toolType);
        result.setErrorMessage(errorMessage);
        result.setExecutionTime(executionTime);
        return result;
    }
}