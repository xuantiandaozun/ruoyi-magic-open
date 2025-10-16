package com.ruoyi.project.ai.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * AgenticScope - 工作流执行上下文
 * 简化版本，用于在工作流步骤间共享数据
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public class AgenticScope {
    
    /** 共享变量存储 */
    private Map<String, Object> variables;

    public AgenticScope() {
        this.variables = new HashMap<>();
    }

    /**
     * 设置变量
     */
    public void setVariable(String name, Object value) {
        this.variables.put(name, value);
    }

    /**
     * 获取变量
     */
    public Object getVariable(String name) {
        return this.variables.get(name);
    }

    /**
     * 获取字符串类型变量
     */
    public String getStringVariable(String name) {
        Object value = getVariable(name);
        return value != null ? value.toString() : null;
    }

    /**
     * 检查变量是否存在
     */
    public boolean hasVariable(String name) {
        return this.variables.containsKey(name);
    }

    /**
     * 获取所有变量
     */
    public Map<String, Object> getAllVariables() {
        return new HashMap<>(this.variables);
    }

    /**
     * 清空所有变量
     */
    public void clearVariables() {
        this.variables.clear();
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}