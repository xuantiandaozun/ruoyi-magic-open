package com.ruoyi.project.gen.tools.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.datasource.DynamicDataSourceContextHolder;

import cn.hutool.core.util.StrUtil;

/**
 * 数据操作工具
 * 提供对数据库表数据的增删改查操作
 */
@Service
public class DataOperationTool {
    private static final Logger logger = LoggerFactory.getLogger(DataOperationTool.class);

    /**
     * 向指定数据源的表中添加数据
     */
    @Tool(name = "addDataToTable", description = "向指定数据源的表中添加数据")
    public Map<String, Object> addDataToTable(String dataSourceName, String tableName, java.util.Map<String, Object> data) {
        try {
            logger.info("addDataToTable向数据源[{}]的表[{}]添加数据", dataSourceName, tableName);

            // 判断不能是主数据源
            if (StrUtil.isEmpty(dataSourceName) || StrUtil.equals(dataSourceName, "master")) {
                throw new ServiceException("不能对主数据源进行数据操作");
            }

            // 切换到指定数据源
            DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);

            try {
                // 构建插入SQL
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("INSERT INTO ").append(tableName).append(" (");
                
                List<String> columns = new ArrayList<>();
                List<Object> values = new ArrayList<>();
                
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    columns.add(entry.getKey());
                    values.add(entry.getValue());
                }
                
                sqlBuilder.append(String.join(", ", columns));
                sqlBuilder.append(") VALUES (");
                sqlBuilder.append(String.join(", ", columns.stream().map(c -> "?").toArray(String[]::new)));
                sqlBuilder.append(")");

                int affectedRows = Db.updateBySql(sqlBuilder.toString(), values.toArray());
                
                Map<String, Object> result = new HashMap<>();
                result.put("dataSourceName", dataSourceName);
                result.put("tableName", tableName);
                result.put("affectedRows", affectedRows);
                result.put("success", affectedRows > 0);
                result.put("data", data);
                result.put("message", affectedRows > 0 ? "数据添加成功" : "数据添加失败");
                
                return result;
            } finally {
                // 操作完成后清理数据源上下文
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        } catch (Exception e) {
            logger.error("向表中添加数据失败", e);
            throw new ServiceException("向表中添加数据失败：" + e.getMessage());
        }
    }

    /**
     * 修改指定数据源的表中的数据
     */
    @Tool(name = "updateDataInTable", description = "修改指定数据源的表中的数据")
    public Map<String, Object> updateDataInTable(String dataSourceName, String tableName, java.util.Map<String, Object> data,
            java.util.Map<String, Object> whereCondition) {
        try {
            logger.info("updateDataInTable修改数据源[{}]的表[{}]中的数据", dataSourceName, tableName);

            // 判断不能是主数据源
            if (StrUtil.isEmpty(dataSourceName) || StrUtil.equals(dataSourceName, "master")) {
                throw new ServiceException("不能对主数据源进行数据操作");
            }

            // 切换到指定数据源
            DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);

            try {
                // 构建更新SQL
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("UPDATE ").append(tableName).append(" SET ");
                
                List<Object> params = new ArrayList<>();
                
                // 设置更新字段
                List<String> setClauses = new ArrayList<>();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    setClauses.add(entry.getKey() + " = ?");
                    params.add(entry.getValue());
                }
                sqlBuilder.append(String.join(", ", setClauses));
                
                // 添加WHERE条件
                if (whereCondition != null && !whereCondition.isEmpty()) {
                    sqlBuilder.append(" WHERE ");
                    List<String> whereClauses = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : whereCondition.entrySet()) {
                        whereClauses.add(entry.getKey() + " = ?");
                        params.add(entry.getValue());
                    }
                    sqlBuilder.append(String.join(" AND ", whereClauses));
                }

                int affectedRows = Db.updateBySql(sqlBuilder.toString(), params.toArray());
                
                Map<String, Object> result = new HashMap<>();
                result.put("dataSourceName", dataSourceName);
                result.put("tableName", tableName);
                result.put("affectedRows", affectedRows);
                result.put("success", affectedRows > 0);
                result.put("data", data);
                result.put("whereCondition", whereCondition);
                result.put("message", affectedRows > 0 ? "数据修改成功" : "数据修改失败或没有匹配的记录");
                
                return result;
            } finally {
                // 操作完成后清理数据源上下文
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        } catch (Exception e) {
            logger.error("修改表中数据失败", e);
            throw new ServiceException("修改表中数据失败：" + e.getMessage());
        }
    }

    /**
     * 查询指定数据源的表中的数据
     */
    @Tool(name = "queryDataFromTable", description = "查询指定数据源的表中的数据")
    public Map<String, Object> queryDataFromTable(String dataSourceName, String tableName,
            java.util.Map<String, Object> whereCondition, Integer limit) {
        try {
            logger.info("queryDataFromTable查询数据源[{}]的表[{}]中的数据", dataSourceName, tableName);

            // 判断不能是主数据源
            if (StrUtil.isEmpty(dataSourceName) || StrUtil.equals(dataSourceName, "master")) {
                throw new ServiceException("不能对主数据源进行数据操作");
            }

            // 限制最大返回500条记录
            if (limit == null || limit > 500) {
                limit = 500;
            }

            // 切换到指定数据源
            DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);

            try {
                // 构建查询SQL
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("SELECT * FROM ").append(tableName);
                
                List<Object> params = new ArrayList<>();
                
                // 添加WHERE条件
                if (whereCondition != null && !whereCondition.isEmpty()) {
                    sqlBuilder.append(" WHERE ");
                    List<String> whereClauses = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : whereCondition.entrySet()) {
                        whereClauses.add(entry.getKey() + " = ?");
                        params.add(entry.getValue());
                    }
                    sqlBuilder.append(String.join(" AND ", whereClauses));
                }
                
                // 添加LIMIT
                sqlBuilder.append(" LIMIT ?");
                params.add(limit);

                List<Row> rows = Db.selectListBySql(sqlBuilder.toString(), params.toArray());
                
                // 转换为Map列表，字段名转为驼峰命名
                List<Map<String, Object>> dataList = new ArrayList<>();
                if (rows != null) {
                    for (Row row : rows) {
                        Map<String, Object> rowMap = new HashMap<>();
                        for (String key : row.keySet()) {
                            String camelKey = toCamelCase(key);
                            rowMap.put(camelKey, row.get(key));
                        }
                        dataList.add(rowMap);
                    }
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("dataSourceName", dataSourceName);
                result.put("tableName", tableName);
                result.put("limit", limit);
                result.put("whereCondition", whereCondition);
                result.put("totalCount", dataList.size());
                result.put("data", dataList);
                result.put("message", "查询数据成功");
                
                return result;
            } finally {
                // 操作完成后清理数据源上下文
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        } catch (Exception e) {
            logger.error("查询表中数据失败", e);
            throw new ServiceException("查询表中数据失败：" + e.getMessage());
        }
    }

    /**
     * 将下划线命名转换为驼峰命名
     */
    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }
}