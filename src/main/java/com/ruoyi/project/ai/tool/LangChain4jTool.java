package com.ruoyi.project.ai.tool;

import java.util.Map;

import dev.langchain4j.agent.tool.ToolSpecification;

/**
 * LangChain4j工具接口
 * 定义与LangChain4j兼容的工具结构
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface LangChain4jTool {
    
    /**
     * 获取工具名称
     * 
     * @return 工具名称
     */
    String getToolName();
    
    /**
     * 获取工具描述
     * 
     * @return 工具描述
     */
    String getToolDescription();
    
    /**
     * 获取工具规范
     * 用于LangChain4j的工具调用
     * 
     * @return 工具规范
     */
    ToolSpecification getToolSpecification();
    
    /**
     * 执行工具
     * 
     * @param parameters 工具参数
     * @return 执行结果
     */
    String execute(Map<String, Object> parameters);
    
    /**
     * 验证参数
     * 
     * @param parameters 工具参数
     * @return 验证结果，true表示验证通过
     */
    default boolean validateParameters(Map<String, Object> parameters) {
        return true;
    }
    
    /**
     * 获取工具使用示例
     * 
     * @return 使用示例
     */
    default String getUsageExample() {
        return "暂无使用示例";
    }
}