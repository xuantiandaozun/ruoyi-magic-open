package com.ruoyi.project.ai.tool;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果统一格式
 * 所有工具都应该返回此格式的JSON字符串
 * 
 * @author ruoyi-magic
 * @date 2024-12-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolExecutionResult {
    
    /**
     * 操作状态：true=成功，false=失败/空数据
     * 工作流会根据此字段判断是否继续执行
     * 当为false时，工作流将停止执行
     */
    private Boolean success;
    
    /**
     * 操作类型：query(查询)、operation(操作)、save(保存)等
     * 用于区分不同类型的工具操作
     */
    private String operationType;
    
    /**
     * 实际返回的数据内容
     * 可以是任何类型的数据
     */
    private Object data;
    
    /**
     * 消息或错误信息
     * 用于向AI描述操作结果
     */
    private String message;
    
    /**
     * 额外的元数据
     * 如数据总数、分页信息等
     */
    private Object metadata;
    
    /**
     * 将结果对象转换为JSON字符串
     */
    public String toJsonString() {
        return JSONUtil.toJsonStr(this);
    }
    
    /**
     * 创建成功的查询结果
     */
    public static String querySuccess(Object data, String message) {
        return ToolExecutionResult.builder()
            .success(true)
            .operationType("query")
            .data(data)
            .message(message != null ? message : "查询成功")
            .build()
            .toJsonString();
    }
    
    /**
     * 创建成功的操作结果
     */
    public static String operationSuccess(Object data, String message) {
        return ToolExecutionResult.builder()
            .success(true)
            .operationType("operation")
            .data(data)
            .message(message != null ? message : "操作成功")
            .build()
            .toJsonString();
    }
    
    /**
     * 创建成功的保存结果
     */
    public static String saveSuccess(Object data, String message) {
        return ToolExecutionResult.builder()
            .success(true)
            .operationType("save")
            .data(data)
            .message(message != null ? message : "保存成功")
            .build()
            .toJsonString();
    }
    
    /**
     * 创建失败的结果（查询/操作/保存失败，工作流将停止）
     */
    public static String failure(String operationType, String errorMessage) {
        return ToolExecutionResult.builder()
            .success(false)
            .operationType(operationType)
            .data(null)
            .message(errorMessage != null ? errorMessage : "操作失败")
            .build()
            .toJsonString();
    }
    
    /**
     * 创建空数据结果（工作流将停止）
     */
    public static String empty(String operationType, String message) {
        return ToolExecutionResult.builder()
            .success(false)
            .operationType(operationType)
            .data(null)
            .message(message != null ? message : "没有查询到相关数据")
            .build()
            .toJsonString();
    }
}
