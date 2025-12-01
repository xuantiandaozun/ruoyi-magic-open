package com.ruoyi.project.ai.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.exception.ServiceException;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolSpecification;

/**
 * LangChain4j工具注册器
 * 管理所有可用的工具，提供工具规范和执行功能
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class LangChain4jToolRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(LangChain4jToolRegistry.class);
    
    private final Map<String, ToolSpecification> toolSpecifications = new HashMap<>();
    private final Map<String, LangChain4jTool> tools = new HashMap<>();
    
    /**
     * 通过构造函数注入所有工具实现
     */
    @Autowired
    public LangChain4jToolRegistry(List<LangChain4jTool> toolList) {
        for (LangChain4jTool tool : toolList) {
            String toolName = tool.getToolName();
            toolSpecifications.put(toolName, tool.getToolSpecification());
            tools.put(toolName, tool);
            log.info("注册LangChain4j工具: {}", toolName);
        }
    }
    
    /**
     * 获取所有工具规范
     */
    public List<ToolSpecification> getAllToolSpecifications() {
        return new ArrayList<>(toolSpecifications.values());
    }
    
    /**
     * 根据工具名称获取工具规范
     */
    public ToolSpecification getToolSpecification(String toolName) {
        return toolSpecifications.get(toolName);
    }
    
    /**
     * 获取所有可用的工具名称
     */
    public List<String> getAllToolNames() {
        return new ArrayList<>(tools.keySet());
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasToolByName(String toolName) {
        return tools.containsKey(toolName);
    }
    
    /**
     * 执行工具
     * 
     * @param toolName 工具名称
     * @param arguments 工具参数（JSON字符串）
     * @return 执行结果
     */
    public String executeTool(String toolName, String arguments) {
        LangChain4jTool tool = tools.get(toolName);
        if (tool == null) {
            log.warn("[LangChain4jToolRegistry] 未找到工具: {}", toolName);
            return ToolExecutionResult.failure("operation", "未找到工具: " + toolName);
        }
        
        Map<String, Object> params;
        try {
            params = parseArguments(arguments);
        } catch (ServiceException e) {
            // 参数解析失败，记录日志并返回统一失败结果
            log.warn("[LangChain4jToolRegistry] 工具参数格式错误: name={}, rawArgs={}, error={}", toolName, arguments, e.getMessage());
            return ToolExecutionResult.failure("operation", "工具参数格式错误: " + e.getMessage());
        }
        
        long start = System.currentTimeMillis();
        try {
            String result = tool.execute(params);
            long cost = System.currentTimeMillis() - start;
            int length = result != null ? result.length() : 0;
            // 成功时只打印精简信息，不输出具体结果内容
            log.info("[LangChain4jToolRegistry] 工具执行成功: name={}, cost={}ms, resultLength={}",
                    toolName, cost, length);
            return result;
        } catch (IllegalArgumentException e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[LangChain4jToolRegistry] 工具参数错误: name={}, cost={}ms, params={}, error={}",
                    toolName, cost, arguments, e.getMessage());
            return ToolExecutionResult.failure("operation", "工具参数错误: " + e.getMessage());
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("[LangChain4jToolRegistry] 工具执行异常: name={}, cost={}ms, params={}, error={}",
                    toolName, cost, arguments, e.getMessage(), e);
            return ToolExecutionResult.failure("operation", "工具执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据工具类型获取工具
     */
    public LangChain4jTool getToolByName(String toolName) {
        return tools.get(toolName);
    }
    
    /**
     * 解析工具参数
     */
    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return JSONUtil.toBean(arguments, Map.class);
        } catch (Exception e) {
            log.error("解析工具参数失败: {}", arguments, e);
            throw new ServiceException("工具参数格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取工具信息
     */
    public Map<String, Object> getToolInfo(String toolName) {
        LangChain4jTool tool = tools.get(toolName);
        if (tool == null) {
            return null;
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("name", tool.getToolName());
        info.put("description", tool.getToolDescription());
        info.put("specification", tool.getToolSpecification());
        
        return info;
    }
    
    /**
     * 获取所有工具信息
     */
    public List<Map<String, Object>> getAllToolsInfo() {
        List<Map<String, Object>> toolsInfo = new ArrayList<>();
        
        for (String toolName : tools.keySet()) {
            Map<String, Object> info = getToolInfo(toolName);
            if (info != null) {
                toolsInfo.add(info);
            }
        }
        
        return toolsInfo;
    }
    
    /**
     * 根据工具名称列表获取工具规范
     * 
     * @param toolNames 工具名称列表
     * @return 工具规范列表
     */
    public List<ToolSpecification> getToolSpecifications(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<ToolSpecification> specifications = new ArrayList<>();
        for (String toolName : toolNames) {
            if (tools.containsKey(toolName)) {
                specifications.add(tools.get(toolName).getToolSpecification());
            }
        }
        return specifications;
    }
    
    /**
     * 根据工具名称列表获取工具对象
     * 
     * @param toolNames 工具名称列表
     * @return 工具对象数组
     */
    public Object[] getToolsAsObjects(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return new Object[0];
        }
        
        List<Object> toolObjects = new ArrayList<>();
        for (String toolName : toolNames) {
            if (tools.containsKey(toolName)) {
                toolObjects.add(tools.get(toolName));
            }
        }
        return toolObjects.toArray();
    }
}