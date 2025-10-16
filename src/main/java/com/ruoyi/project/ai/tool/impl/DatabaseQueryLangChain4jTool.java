package com.ruoyi.project.ai.tool.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.project.ai.tool.LangChain4jTool;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的数据库查询工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class DatabaseQueryLangChain4jTool implements LangChain4jTool {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseQueryLangChain4jTool.class);
    
    @Override
    public String getToolName() {
        return "database_query";
    }
    
    @Override
    public String getToolDescription() {
        return "执行数据库查询操作，支持SELECT查询语句";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        // 创建数据库查询工具规范
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("sql", "要执行的SQL查询语句，仅支持SELECT语句")
            .required("sql")
            .build();
        
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(parametersSchema)
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            String sql = (String) parameters.get("sql");
            
            if (StrUtil.isBlank(sql)) {
                return "SQL查询语句不能为空";
            }
            
            // 安全检查：只允许SELECT语句
            String trimmedSql = sql.trim().toLowerCase();
            if (!trimmedSql.startsWith("select")) {
                return "安全限制：仅支持SELECT查询语句";
            }
            
            // 检查是否包含危险操作
            if (containsDangerousOperations(trimmedSql)) {
                return "安全限制：SQL语句包含不允许的操作";
            }
            
            // 执行查询
            List<Row> rows = Db.selectListBySql(sql);
            
            if (rows == null || rows.isEmpty()) {
                return "查询结果为空";
            }
            
            // 格式化结果
            StringBuilder result = new StringBuilder();
            result.append("查询结果（共").append(rows.size()).append("条记录）：\n\n");
            
            // 限制显示的记录数量，避免结果过长
            int maxRecords = Math.min(rows.size(), 20);
            for (int i = 0; i < maxRecords; i++) {
                Row row = rows.get(i);
                result.append("记录 ").append(i + 1).append(":\n");
                for (String key : row.keySet()) {
                    result.append("  ").append(key).append(": ").append(row.get(key)).append("\n");
                }
                result.append("\n");
            }
            
            if (rows.size() > maxRecords) {
                result.append("... 还有 ").append(rows.size() - maxRecords).append(" 条记录未显示\n");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("执行数据库查询失败", e);
            return "数据库查询执行失败: " + e.getMessage();
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null || !parameters.containsKey("sql")) {
            return false;
        }
        
        String sql = (String) parameters.get("sql");
        if (StrUtil.isBlank(sql)) {
            return false;
        }
        
        // 基本安全检查
        String trimmedSql = sql.trim().toLowerCase();
        return trimmedSql.startsWith("select") && !containsDangerousOperations(trimmedSql);
    }
    
    /**
     * 检查SQL是否包含危险操作
     */
    private boolean containsDangerousOperations(String sql) {
        String[] dangerousKeywords = {
            "insert", "update", "delete", "drop", "create", "alter", 
            "truncate", "exec", "execute", "sp_", "xp_", "grant", 
            "revoke", "commit", "rollback", "savepoint"
        };
        
        for (String keyword : dangerousKeywords) {
            if (sql.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 查询用户表：
           {"sql": "SELECT * FROM sys_user LIMIT 10"}
        
        2. 查询特定条件的数据：
           {"sql": "SELECT user_name, nick_name FROM sys_user WHERE status = '0'"}
        
        3. 统计查询：
           {"sql": "SELECT COUNT(*) as total FROM sys_user"}
        """;
    }
}