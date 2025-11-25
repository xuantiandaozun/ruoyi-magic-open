package com.ruoyi.project.ai.tool.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
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
    
    // SQL安全检查的正则表达式
    // 使用单词边界\b确保只匹配完整的SQL关键词，避免误判字段名或表名中包含这些词的情况
    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
        "(?i).*\\b(drop|delete|truncate|alter|create\\s+table|create\\s+database|create\\s+index|create\\s+view|insert|update|grant|revoke|exec|execute|xp_|sp_)\\b.*"
    );
    
    @Override
    public String getToolName() {
        return "database_query";
    }
    
    @Override
    public String getToolDescription() {
        String description = "执行数据库查询并返回结果。";
        try {
            // 获取AI可访问的表列表
            Row configRow = Db.selectOneBySql("SELECT config_value FROM sys_config WHERE config_key = ?", "ai.database.allowed_tables");
            String allowedTables = configRow != null ? configRow.getString("config_value") : null;
            if (StrUtil.isNotBlank(allowedTables)) {
                description += "允许查询的表包括：" + allowedTables + "。";
            }
        } catch (Exception e) {
            // 忽略错误，使用默认描述
        }
        return description;
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("sql", "要执行的SQL查询语句，必须是SELECT语句")
                .required("sql")
                .build())
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        String sql = (String) parameters.get("sql");
        if (StrUtil.isBlank(sql)) {
            return "SQL语句不能为空";
        }

        // 安全检查
        if (!isSafeSql(sql)) {
            return "安全检查失败：只允许执行SELECT查询语句";
        }

        // 限制查询结果数量
        int limit = 100;

        // 检查SQL是否已包含LIMIT子句
        String normalizedSql = sql.trim().toLowerCase();
        if (!normalizedSql.contains("limit")) {
            sql = sql.trim();
            if (!sql.endsWith(";")) {
                sql += " LIMIT " + limit;
            } else {
                sql = sql.substring(0, sql.length() - 1) + " LIMIT " + limit + ";";
            }
        }

        // 执行查询
        List<Row> rows = Db.selectListBySql(sql);
        
        if (rows.isEmpty()) {
            return ToolExecutionResult.empty("query", "数据库查询结果为空");
        }

        // 构建结果JSON
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("limit", limit);
        result.put("data", rows);

        return ToolExecutionResult.querySuccess(JSONUtil.toJsonStr(result), "成功执行数据库查询");
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return false;
        }
        
        // 检查必需的sql参数
        if (!parameters.containsKey("sql") || StrUtil.isBlank((String) parameters.get("sql"))) {
            return false;
        }
        
        // 检查SQL安全性
        String sql = (String) parameters.get("sql");
        return isSafeSql(sql);
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 查询部门信息：
           {"sql": "SELECT `dept_id`, `dept_name`, `parent_id` FROM `sys_dept` WHERE `del_flag` = '0' LIMIT 10"}
        
        2. 查询用户信息：
           {"sql": "SELECT `user_name`, `nick_name`, `email` FROM `sys_user` WHERE `status` = '0' LIMIT 5"}
        
        3. 查询菜单权限：
           {"sql": "SELECT `menu_name`, `path`, `perms` FROM `sys_menu` WHERE `del_flag` = '0' ORDER BY `order_num` LIMIT 20"}
        """;
    }
    
    /**
     * 检查SQL是否安全（只允许SELECT语句）
     */
    private boolean isSafeSql(String sql) {
        if (StrUtil.isBlank(sql)) {
            return false;
        }

        String normalizedSql = sql.trim().toLowerCase();
        
        // 检查是否以SELECT开头
        if (!normalizedSql.startsWith("select")) {
            return false;
        }

        // 检查是否包含危险操作
        if (DANGEROUS_SQL_PATTERN.matcher(normalizedSql).matches()) {
            return false;
        }

        return true;
    }
}