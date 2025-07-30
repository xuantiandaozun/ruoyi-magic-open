package com.ruoyi.project.common.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.common.domain.dto.DictQueryRequest;
import com.ruoyi.project.common.domain.vo.DictOption;
import com.ruoyi.project.common.service.IDictionaryService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 字典查询服务实现类
 * 
 * @author ruoyi
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements IDictionaryService {

    /**
     * 默认显示字段优先级
     */
    private static final List<String> DEFAULT_LABEL_FIELDS = Arrays.asList(
        "name", "title", "label", "text", "dict_label", "menu_name", "dept_name", "role_name", "user_name"
    );

    /**
     * 默认值字段优先级
     */
    private static final List<String> DEFAULT_VALUE_FIELDS = Arrays.asList(
        "id", "code", "value", "dict_value", "dict_code", "menu_id", "dept_id", "role_id", "user_id"
    );

    @Override
    public List<DictOption> getTableDict(String tableName, String labelField, String valueField, String status) {
        // 验证表是否存在
        if (!validateTableExists(tableName)) {
            throw new ServiceException("表 " + tableName + " 不存在");
        }

        // 获取表的所有列
        List<String> columns = getTableColumns(tableName);
        if (CollUtil.isEmpty(columns)) {
            throw new ServiceException("无法获取表 " + tableName + " 的列信息");
        }

        // 确定显示字段和值字段
        String actualLabelField = StrUtil.isNotBlank(labelField) ? labelField : getDefaultLabelField(tableName, columns);
        String actualValueField = StrUtil.isNotBlank(valueField) ? valueField : getDefaultValueField(tableName, columns);

        // 验证字段是否存在
        if (!columns.contains(actualLabelField)) {
            throw new ServiceException("字段 " + actualLabelField + " 在表 " + tableName + " 中不存在");
        }
        if (!columns.contains(actualValueField)) {
            throw new ServiceException("字段 " + actualValueField + " 在表 " + tableName + " 中不存在");
        }

        // 构建查询SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(actualLabelField).append(" as label, ")
           .append(actualValueField).append(" as value FROM ").append(tableName)
           .append(" WHERE 1=1");

        List<Object> params = new ArrayList<>();

        // 添加状态过滤
        if (columns.contains("del_flag")) {
            sql.append(" AND (del_flag = ? OR del_flag IS NULL)");
            params.add("0");
        }

        if (StrUtil.isNotBlank(status) && columns.contains("status")) {
            sql.append(" AND status = ?");
            params.add(status);
        } else if (columns.contains("status")) {
            sql.append(" AND (status = ? OR status IS NULL OR status = '')");
            params.add("0");
        }

        // 添加排序
        if (columns.contains("sort")) {
            sql.append(" ORDER BY sort ASC");
        } else if (columns.contains("order_num")) {
            sql.append(" ORDER BY order_num ASC");
        } else {
            sql.append(" ORDER BY ").append(actualValueField).append(" ASC");
        }

        try {
            List<Row> rows = Db.selectListBySql(sql.toString(), params.toArray());
            List<DictOption> options = new ArrayList<>();
            
            for (Row row : rows) {
                String label = row.getString("label");
                String value = row.getString("value");
                if (StrUtil.isNotBlank(label) && StrUtil.isNotBlank(value)) {
                    options.add(new DictOption(label, value));
                }
            }
            
            return options;
        } catch (Exception e) {
            log.error("查询表字典数据失败: tableName={}, error={}", tableName, e.getMessage(), e);
            throw new ServiceException("查询字典数据失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getCustomTableDict(String tableName, DictQueryRequest request) {
        // 验证表是否存在
        if (!validateTableExists(tableName)) {
            throw new ServiceException("表 " + tableName + " 不存在");
        }

        // 获取表的所有列
        List<String> columns = getTableColumns(tableName);
        if (CollUtil.isEmpty(columns)) {
            throw new ServiceException("无法获取表 " + tableName + " 的列信息");
        }

        // 构建查询SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName).append(" WHERE 1=1");

        List<Object> params = new ArrayList<>();

        // 添加基础过滤条件
        addBasicFilters(sql, params, columns, request.getStatus());

        // 添加自定义条件（简单等值查询，保持向后兼容）
        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getConditions().entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                
                if (columns.contains(fieldName) && fieldValue != null) {
                    sql.append(" AND ").append(fieldName).append(" = ?");
                    params.add(fieldValue);
                }
            }
        }

        // 添加高级查询条件
        if (request.getAdvancedConditions() != null && !request.getAdvancedConditions().isEmpty()) {
            for (DictQueryRequest.AdvancedCondition condition : request.getAdvancedConditions()) {
                if (StrUtil.isBlank(condition.getFieldName()) || StrUtil.isBlank(condition.getOperator())) {
                    continue;
                }
                
                String fieldName = condition.getFieldName();
                if (!columns.contains(fieldName)) {
                    continue;
                }
                
                String operator = condition.getOperator().toUpperCase();
                Object value = condition.getValue();
                Object value2 = condition.getValue2();
                
                switch (operator) {
                    case "=":
                        if (value != null) {
                            sql.append(" AND ").append(fieldName).append(" = ?");
                            params.add(value);
                        }
                        break;
                    case "!=":
                        if (value != null) {
                            sql.append(" AND ").append(fieldName).append(" != ?");
                            params.add(value);
                        }
                        break;
                    case ">":
                        if (value != null) {
                            sql.append(" AND ").append(fieldName).append(" > ?");
                            params.add(value);
                        }
                        break;
                    case "<":
                        if (value != null) {
                            sql.append(" AND ").append(fieldName).append(" < ?");
                            params.add(value);
                        }
                        break;
                    case ">=":
                        if (value != null) {
                            sql.append(" AND ").append(fieldName).append(" >= ?");
                            params.add(value);
                        }
                        break;
                    case "<=":
                        if (value != null) {
                            sql.append(" AND ").append(fieldName).append(" <= ?");
                            params.add(value);
                        }
                        break;
                    case "BETWEEN":
                        if (value != null && value2 != null) {
                            sql.append(" AND ").append(fieldName).append(" BETWEEN ? AND ?");
                            params.add(value);
                            params.add(value2);
                        }
                        break;
                    case "LIKE":
                        if (value != null) {
                            sql.append(" AND ").append(fieldName).append(" LIKE ?");
                            params.add(value);
                        }
                        break;
                    case "IN":
                        if (value instanceof List && !((List<?>) value).isEmpty()) {
                            List<?> values = (List<?>) value;
                            sql.append(" AND ").append(fieldName).append(" IN (");
                            for (int i = 0; i < values.size(); i++) {
                                if (i > 0) sql.append(", ");
                                sql.append("?");
                                params.add(values.get(i));
                            }
                            sql.append(")");
                        }
                        break;
                    case "NOT_IN":
                        if (value instanceof List && !((List<?>) value).isEmpty()) {
                            List<?> values = (List<?>) value;
                            sql.append(" AND ").append(fieldName).append(" NOT IN (");
                            for (int i = 0; i < values.size(); i++) {
                                if (i > 0) sql.append(", ");
                                sql.append("?");
                                params.add(values.get(i));
                            }
                            sql.append(")");
                        }
                        break;
                }
            }
        }

        // 添加排序
        if (StrUtil.isNotBlank(request.getOrderBy()) && columns.contains(request.getOrderBy())) {
            sql.append(" ORDER BY ").append(request.getOrderBy()).append(" ").append(request.getOrderDirection());
        } else {
            addDefaultOrder(sql, columns);
        }

        try {
            List<Row> rows = Db.selectListBySql(sql.toString(), params.toArray());
            List<Map<String, Object>> results = new ArrayList<>();
            
            for (Row row : rows) {
                results.add(row.toCamelKeysMap());
            }
            
            return results;
        } catch (Exception e) {
            log.error("查询自定义表字典数据失败: tableName={}, error={}", tableName, e.getMessage(), e);
            throw new ServiceException("查询字典数据失败: " + e.getMessage());
        }
    }

    @Override
    public Page<Map<String, Object>> getTableDictPage(String tableName, DictQueryRequest request) {
        // 验证表是否存在
        if (!validateTableExists(tableName)) {
            throw new ServiceException("表 " + tableName + " 不存在");
        }

        // 获取表的所有列
        List<String> columns = getTableColumns(tableName);
        if (CollUtil.isEmpty(columns)) {
            throw new ServiceException("无法获取表 " + tableName + " 的列信息");
        }

        // 设置默认分页参数
        int pageNum = request.getPageNum() != null && request.getPageNum() > 0 ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 10;

        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create().from(tableName);

        // 添加基础过滤条件
        addBasicFiltersToWrapper(queryWrapper, columns, request.getStatus());

        // 添加自定义条件（简单等值查询，保持向后兼容）
        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getConditions().entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                
                if (columns.contains(fieldName) && fieldValue != null) {
                    queryWrapper.and(fieldName + " = ?", fieldValue);
                }
            }
        }

        // 添加高级查询条件
        if (request.getAdvancedConditions() != null && !request.getAdvancedConditions().isEmpty()) {
            for (DictQueryRequest.AdvancedCondition condition : request.getAdvancedConditions()) {
                if (StrUtil.isBlank(condition.getFieldName()) || StrUtil.isBlank(condition.getOperator())) {
                    continue;
                }
                
                String fieldName = condition.getFieldName();
                if (!columns.contains(fieldName)) {
                    continue;
                }
                
                String operator = condition.getOperator().toUpperCase();
                Object value = condition.getValue();
                Object value2 = condition.getValue2();
                
                switch (operator) {
                    case "=":
                        if (value != null) {
                            queryWrapper.and(fieldName + " = ?", value);
                        }
                        break;
                    case "!=":
                        if (value != null) {
                            queryWrapper.and(fieldName + " != ?", value);
                        }
                        break;
                    case ">":
                        if (value != null) {
                            queryWrapper.and(fieldName + " > ?", value);
                        }
                        break;
                    case "<":
                        if (value != null) {
                            queryWrapper.and(fieldName + " < ?", value);
                        }
                        break;
                    case ">=":
                        if (value != null) {
                            queryWrapper.and(fieldName + " >= ?", value);
                        }
                        break;
                    case "<=":
                        if (value != null) {
                            queryWrapper.and(fieldName + " <= ?", value);
                        }
                        break;
                    case "BETWEEN":
                        if (value != null && value2 != null) {
                            queryWrapper.and(fieldName + " BETWEEN ? AND ?", value, value2);
                        }
                        break;
                    case "LIKE":
                        if (value != null) {
                            queryWrapper.and(fieldName + " LIKE ?", value);
                        }
                        break;
                    case "IN":
                        if (value instanceof List && !((List<?>) value).isEmpty()) {
                            List<?> values = (List<?>) value;
                            queryWrapper.and(fieldName + " IN (?)", values.toArray());
                        }
                        break;
                    case "NOT_IN":
                        if (value instanceof List && !((List<?>) value).isEmpty()) {
                            List<?> values = (List<?>) value;
                            queryWrapper.and(fieldName + " NOT IN (?)", values.toArray());
                        }
                        break;
                }
            }
        }

        // 添加排序
        if (StrUtil.isNotBlank(request.getOrderBy()) && columns.contains(request.getOrderBy())) {
            if ("DESC".equalsIgnoreCase(request.getOrderDirection())) {
                queryWrapper.orderBy(request.getOrderBy() + " DESC");
            } else {
                queryWrapper.orderBy(request.getOrderBy() + " ASC");
            }
        } else {
            addDefaultOrderToWrapper(queryWrapper, columns);
        }

        try {
            Page<Row> rowPage = Db.paginate(tableName, pageNum, pageSize, queryWrapper);
            
            // 转换Row为Map
            List<Map<String, Object>> records = new ArrayList<>();
            for (Row row : rowPage.getRecords()) {
                records.add(row.toCamelKeysMap());
            }
            
            // 创建新的Page对象
            Page<Map<String, Object>> resultPage = new Page<>();
            resultPage.setRecords(records);
            resultPage.setPageNumber(rowPage.getPageNumber());
            resultPage.setPageSize(rowPage.getPageSize());
            resultPage.setTotalRow(rowPage.getTotalRow());
            resultPage.setTotalPage(rowPage.getTotalPage());
            
            return resultPage;
        } catch (Exception e) {
            log.error("分页查询表字典数据失败: tableName={}, error={}", tableName, e.getMessage(), e);
            throw new ServiceException("查询字典数据失败: " + e.getMessage());
        }
    }

    @Override
    public boolean validateTableExists(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            return false;
        }

        // 验证表名格式，防止SQL注入
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return false;
        }

        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = DATABASE()";
            Long count = Db.selectCount(sql, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("验证表是否存在失败: tableName={}, error={}", tableName, e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getTableColumns(String tableName) {
        if (StrUtil.isBlank(tableName) || !validateTableExists(tableName)) {
            return new ArrayList<>();
        }

        try {
            String sql = "SELECT column_name FROM information_schema.columns WHERE table_name = ? AND table_schema = DATABASE()";
            List<Row> rows = Db.selectListBySql(sql, tableName);
            
            List<String> columns = new ArrayList<>();
            for (Row row : rows) {
                Map<String, Object> result = row.toCamelKeysMap();
                String columnName = (String) result.get("columnName");
                if (StrUtil.isNotBlank(columnName)) {
                    columns.add(columnName);
                }
            }
            
            return columns;
        } catch (Exception e) {
            log.error("获取表列信息失败: tableName={}, error={}", tableName, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public String getDefaultLabelField(String tableName, List<String> columns) {
        // 按优先级查找显示字段
        for (String field : DEFAULT_LABEL_FIELDS) {
            if (columns.contains(field)) {
                return field;
            }
        }

        // 如果没有找到默认字段，查找第一个varchar/text类型字段
        for (String column : columns) {
            if (isTextColumn(tableName, column)) {
                return column;
            }
        }

        // 如果都没有找到，返回第一个非主键字段
        for (String column : columns) {
            if (!"id".equals(column)) {
                return column;
            }
        }

        // 最后返回第一个字段
        return CollUtil.isNotEmpty(columns) ? columns.get(0) : "id";
    }

    @Override
    public String getDefaultValueField(String tableName, List<String> columns) {
        // 按优先级查找值字段
        for (String field : DEFAULT_VALUE_FIELDS) {
            if (columns.contains(field)) {
                return field;
            }
        }

        // 如果没有找到默认字段，查找第一个数字类型字段
        for (String column : columns) {
            if (isNumericColumn(tableName, column)) {
                return column;
            }
        }

        // 最后返回第一个字段
        return CollUtil.isNotEmpty(columns) ? columns.get(0) : "id";
    }

    /**
     * 添加基础过滤条件
     */
    private void addBasicFilters(StringBuilder sql, List<Object> params, List<String> columns, String status) {
        // 添加删除标志过滤
        if (columns.contains("del_flag")) {
            sql.append(" AND (del_flag = ? OR del_flag IS NULL)");
            params.add("0");
        }

        // 添加状态过滤
        if (StrUtil.isNotBlank(status) && columns.contains("status")) {
            sql.append(" AND status = ?");
            params.add(status);
        } else if (columns.contains("status")) {
            sql.append(" AND (status = ? OR status IS NULL OR status = '')");
            params.add("0");
        }
    }

    /**
     * 添加基础过滤条件到QueryWrapper
     */
    private void addBasicFiltersToWrapper(QueryWrapper queryWrapper, List<String> columns, String status) {
        // 添加删除标志过滤
        if (columns.contains("del_flag")) {
            queryWrapper.and("(del_flag = '0' OR del_flag IS NULL)");
        }

        // 添加状态过滤
        if (StrUtil.isNotBlank(status) && columns.contains("status")) {
            queryWrapper.and("status = ?", status);
        } else if (columns.contains("status")) {
            queryWrapper.and("(status = '0' OR status IS NULL OR status = '')");
        }
    }

    /**
     * 添加默认排序
     */
    private void addDefaultOrder(StringBuilder sql, List<String> columns) {
        if (columns.contains("sort")) {
            sql.append(" ORDER BY sort ASC");
        } else if (columns.contains("order_num")) {
            sql.append(" ORDER BY order_num ASC");
        } else if (columns.contains("id")) {
            sql.append(" ORDER BY id ASC");
        }
    }

    /**
     * 添加默认排序到QueryWrapper
     */
    private void addDefaultOrderToWrapper(QueryWrapper queryWrapper, List<String> columns) {
        if (columns.contains("sort")) {
            queryWrapper.orderBy("sort ASC");
        } else if (columns.contains("order_num")) {
            queryWrapper.orderBy("order_num ASC");
        } else if (columns.contains("id")) {
            queryWrapper.orderBy("id ASC");
        }
    }

    /**
     * 判断是否为文本类型字段
     */
    private boolean isTextColumn(String tableName, String columnName) {
        try {
            String sql = "SELECT data_type FROM information_schema.columns WHERE table_name = ? AND column_name = ? AND table_schema = DATABASE()";
            List<Row> rows = Db.selectListBySql(sql, tableName, columnName);
            
            if (CollUtil.isNotEmpty(rows)) {
                Map<String, Object> result = rows.get(0).toCamelKeysMap();
                String dataType = (String) result.get("dataType");
                if (StrUtil.isNotBlank(dataType)) {
                    dataType = dataType.toLowerCase();
                    return dataType.contains("varchar") || dataType.contains("text") || dataType.contains("char");
                }
            }
        } catch (Exception e) {
            log.debug("判断字段类型失败: tableName={}, columnName={}", tableName, columnName);
        }
        return false;
    }

    /**
     * 判断是否为数字类型字段
     */
    private boolean isNumericColumn(String tableName, String columnName) {
        try {
            String sql = "SELECT data_type FROM information_schema.columns WHERE table_name = ? AND column_name = ? AND table_schema = DATABASE()";
            List<Row> rows = Db.selectListBySql(sql, tableName, columnName);
            
            if (CollUtil.isNotEmpty(rows)) {
                Map<String, Object> result = rows.get(0).toCamelKeysMap();
                String dataType = (String) result.get("dataType");
                if (StrUtil.isNotBlank(dataType)) {
                    dataType = dataType.toLowerCase();
                    return dataType.contains("int") || dataType.contains("bigint") || dataType.contains("decimal") || 
                           dataType.contains("float") || dataType.contains("double");
                }
            }
        } catch (Exception e) {
            log.debug("判断字段类型失败: tableName={}, columnName={}", tableName, columnName);
        }
        return false;
    }
}