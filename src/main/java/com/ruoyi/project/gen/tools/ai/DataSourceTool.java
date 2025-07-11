package com.ruoyi.project.gen.tools.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.datasource.DynamicDataSourceContextHolder;
import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.service.IGenTableColumnService;
import com.ruoyi.project.gen.service.IGenTableService;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.service.ISysDataSourceService;

import cn.hutool.core.util.StrUtil;

/**
 * 数据源操作工具
 * 提供数据源和表结构相关的操作
 */
@Service
public class DataSourceTool {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceTool.class);

    @Autowired
    private IGenTableService genTableService;

    @Autowired
    private IGenTableColumnService genTableColumnService;

    @Autowired
    private ISysDataSourceService sysDataSourceService;

    /**
     * 获取指定数据源的所有表
     */
    @Tool(name = "getTablesFromDataSource", description = "获取指定数据源的所有表")
    public Map<String, Object> getTablesFromDataSource(String dataSourceName, String tableName, String tableComment, Integer pageNum,
            Integer pageSize) {
        try {
            logger.info("getTablesFromDataSource查询数据源[{}]的所有表", dataSourceName);

            // 如果不是主数据源，则切换数据源
            if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master") && !StrUtil.equals(dataSourceName, "MASTER")) {
                DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            }

            try {
                // 构建SQL查询
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("SELECT table_name, table_comment, create_time, update_time ")
                        .append("FROM information_schema.tables ")
                        .append("WHERE table_schema = (SELECT DATABASE()) ");

                List<Object> params = new ArrayList<>();
                if (StrUtil.isNotBlank(tableName)) {
                    sqlBuilder.append("AND table_name LIKE ? ");
                    params.add("%" + tableName + "%");
                }
                if (StrUtil.isNotBlank(tableComment)) {
                    sqlBuilder.append("AND table_comment LIKE ? ");
                    params.add("%" + tableComment + "%");
                }
                sqlBuilder.append("ORDER BY table_name");

                // 计算总记录数
                String countSql = "SELECT COUNT(*) as total FROM (" + sqlBuilder.toString() + ") t";
                Row countRow = Db.selectOneBySql(countSql, params.toArray());
                Map<String, Object> countMap = countRow != null ? countRow.toCamelKeysMap() : null;
                long total = countMap != null ? Long.parseLong(countMap.get("total").toString()) : 0;

                // 添加分页
                if (pageNum != null && pageSize != null) {
                    sqlBuilder.append(" LIMIT ?, ?");
                    params.add((pageNum - 1) * pageSize);
                    params.add(pageSize);
                }

                List<Row> rows = Db.selectListBySql(sqlBuilder.toString(), params.toArray());
                
                Map<String, Object> result = new HashMap<>();
                result.put("dataSourceName", dataSourceName);
                result.put("totalCount", total);
                result.put("pageNum", pageNum);
                result.put("pageSize", pageSize);
                
                if (rows == null || rows.isEmpty()) {
                    result.put("message", "数据源中没有找到匹配的表");
                    result.put("tables", new ArrayList<>());
                    return result;
                }
                
                List<Map<String, Object>> tables = new ArrayList<>();
                for (Row row : rows) {
                    Map<String, Object> rowMap = row.toCamelKeysMap();
                    Map<String, Object> table = new HashMap<>();
                    table.put("tableName", rowMap.get("tableName"));
                    table.put("tableComment", rowMap.get("tableComment"));
                    table.put("createTime", rowMap.get("createTime"));
                    table.put("updateTime", rowMap.get("updateTime"));
                    tables.add(table);
                }
                result.put("tables", tables);
                result.put("message", "获取表列表成功");
                
                return result;
            } finally {
                // 如果不是主数据源，操作完成后清理数据源上下文
                if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master") && !StrUtil.equals(dataSourceName, "MASTER")) {
                    DynamicDataSourceContextHolder.clearDataSourceType();
                }
            }
        } catch (Exception e) {
            logger.error("获取数据源表列表失败", e);
            throw new ServiceException("获取数据源表列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定数据源的表结构
     */
    @Tool(name = "getTableStructureFromDataSource", description = "获取指定数据源的表结构")
    public Map<String, Object> getTableStructureFromDataSource(String tableName, String dataSourceName) {
        try {
            logger.info("getTableStructureFromDataSource获取数据源[{}]表[{}]的结构", dataSourceName, tableName);

            // 如果不是主数据源，则切换数据源
            if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master") && !StrUtil.equals(dataSourceName, "MASTER")) {
                DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            }

            try {
                // 直接查询数据库表结构
                String sql = "SELECT column_name, column_type, column_comment, " +
                        "(CASE WHEN (is_nullable = 'no' AND column_key != 'PRI') THEN '1' ELSE '0' END) AS is_required, " +
                        "(CASE WHEN column_key = 'PRI' THEN '1' ELSE '0' END) AS is_pk, " +
                        "ordinal_position AS sort, " +
                        "column_default AS column_default " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = (SELECT DATABASE()) AND table_name = ? " +
                        "ORDER BY ordinal_position";

                List<Row> rows = Db.selectListBySql(sql, tableName);
                
                Map<String, Object> result = new HashMap<>();
                result.put("dataSourceName", dataSourceName);
                result.put("tableName", tableName);
                result.put("columnCount", rows != null ? rows.size() : 0);
                
                if (rows == null || rows.isEmpty()) {
                    result.put("message", "表不存在或表结构为空");
                    result.put("columns", new ArrayList<>());
                    return result;
                }
                
                List<Map<String, Object>> columns = new ArrayList<>();
                for (Row row : rows) {
                    Map<String, Object> rowMap = row.toCamelKeysMap();
                    Map<String, Object> column = new HashMap<>();
                    column.put("columnName", rowMap.get("columnName"));
                    column.put("columnType", rowMap.get("columnType"));
                    column.put("columnComment", rowMap.get("columnComment"));
                    column.put("isRequired", rowMap.get("isRequired"));
                    column.put("isPk", rowMap.get("isPk"));
                    column.put("sort", rowMap.get("sort"));
                    column.put("columnDefault", rowMap.get("columnDefault"));
                    columns.add(column);
                }
                result.put("columns", columns);
                result.put("message", "获取表结构成功");
                
                return result;
            } finally {
                // 如果不是主数据源，操作完成后清理数据源上下文
                if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master") && !StrUtil.equals(dataSourceName, "MASTER")) {
                    DynamicDataSourceContextHolder.clearDataSourceType();
                }
            }
        } catch (Exception e) {
            logger.error("获取表结构失败", e);
            throw new ServiceException("获取表结构失败：" + e.getMessage());
        }
    }

    /**
     * 直接查询数据库表结构
     */
    @Tool(name = "getTableStructureByName", description = "直接查询数据库表结构")
    public Map<String, Object> getTableStructureByName(String tableName) {
        try {
            logger.info("getTableStructureByName查询表结构: {}", tableName);
            String sql = "SELECT column_name, column_type, column_comment, " +
                    "(CASE WHEN (is_nullable = 'no' AND column_key != 'PRI') THEN '1' ELSE '0' END) AS is_required, " +
                    "(CASE WHEN column_key = 'PRI' THEN '1' ELSE '0' END) AS is_pk, " +
                    "ordinal_position AS sort, " +
                    "column_default AS column_default " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = (SELECT DATABASE()) AND table_name = ? " +
                    "ORDER BY ordinal_position";

            List<Row> rows = Db.selectListBySql(sql, tableName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("tableName", tableName);
            result.put("columnCount", rows != null ? rows.size() : 0);
            
            if (rows == null || rows.isEmpty()) {
                result.put("message", "表不存在或表结构为空");
                result.put("columns", new ArrayList<>());
                return result;
            }
            
            List<Map<String, Object>> columns = new ArrayList<>();
            for (Row row : rows) {
                Map<String, Object> rowMap = row.toCamelKeysMap();
                Map<String, Object> column = new HashMap<>();
                column.put("columnName", rowMap.get("columnName"));
                column.put("columnType", rowMap.get("columnType"));
                column.put("columnComment", rowMap.get("columnComment"));
                column.put("isRequired", rowMap.get("isRequired"));
                column.put("isPk", rowMap.get("isPk"));
                column.put("sort", rowMap.get("sort"));
                column.put("columnDefault", rowMap.get("columnDefault"));
                columns.add(column);
            }
            result.put("columns", columns);
            result.put("message", "获取表结构成功");
            
            return result;
        } catch (Exception e) {
            logger.error("查询表结构失败", e);
            throw new ServiceException("查询表结构失败：" + e.getMessage());
        }
    }

    /**
     * 直接查询指定数据源的所有表
     */
    @Tool(name = "getAllTablesFromDataSource", description = "直接查询指定数据源的所有表")
    public Map<String, Object> getAllTablesFromDataSource(String dataSourceName, String tableName, String tableComment) {
        try {
            logger.info("getAllTablesFromDataSource查询数据源[{}]的所有表", dataSourceName);

            // 如果不是主数据源，则切换数据源
            if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master") && !StrUtil.equals(dataSourceName, "MASTER")) {
                DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            }

            try {
                // 构建SQL查询
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("SELECT table_name, table_comment, create_time, update_time ")
                        .append("FROM information_schema.tables ")
                        .append("WHERE table_schema = (SELECT DATABASE()) ");

                List<Object> params = new ArrayList<>();
                if (StrUtil.isNotBlank(tableName)) {
                    sqlBuilder.append("AND table_name LIKE ? ");
                    params.add("%" + tableName + "%");
                }
                if (StrUtil.isNotBlank(tableComment)) {
                    sqlBuilder.append("AND table_comment LIKE ? ");
                    params.add("%" + tableComment + "%");
                }
                sqlBuilder.append("ORDER BY table_name");

                List<Row> rows = Db.selectListBySql(sqlBuilder.toString(), params.toArray());
                
                Map<String, Object> result = new HashMap<>();
                result.put("dataSourceName", dataSourceName);
                result.put("totalCount", rows != null ? rows.size() : 0);
                
                if (rows == null || rows.isEmpty()) {
                    result.put("message", "数据源中没有找到匹配的表");
                    result.put("tables", new ArrayList<>());
                    return result;
                }
                
                List<Map<String, Object>> tables = new ArrayList<>();
                for (Row row : rows) {
                    Map<String, Object> rowMap = row.toCamelKeysMap();
                    Map<String, Object> table = new HashMap<>();
                    table.put("tableName", rowMap.get("tableName"));
                    table.put("tableComment", rowMap.get("tableComment"));
                    table.put("createTime", rowMap.get("createTime"));
                    table.put("updateTime", rowMap.get("updateTime"));
                    tables.add(table);
                }
                result.put("tables", tables);
                result.put("message", "获取表列表成功");
                
                return result;
            } finally {
                // 如果不是主数据源，操作完成后清理数据源上下文
                if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master") && !StrUtil.equals(dataSourceName, "MASTER")) {
                    DynamicDataSourceContextHolder.clearDataSourceType();
                }
            }
        } catch (Exception e) {
            logger.error("查询数据源表列表失败", e);
            throw new ServiceException("查询数据源表列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有数据源列表
     */
    @Tool(name = "getAllDataSources", description = "获取所有数据源列表")
    public Map<String, Object> getAllDataSources() {
        try {
            logger.info("getAllDataSources获取所有数据源列表");
            List<SysDataSource> dataSources = sysDataSourceService.selectSysDataSourceList(new SysDataSource());

            Map<String, Object> result = new HashMap<>();
            List<String> dataSourceNames = new ArrayList<>();
            
            // 手动添加主数据源
            dataSourceNames.add("MASTER");

            // 添加其他数据源名称
            if (dataSources != null && !dataSources.isEmpty()) {
                for (SysDataSource dataSource : dataSources) {
                    dataSourceNames.add(dataSource.getName());
                }
            }

            result.put("totalCount", dataSourceNames.size());
            result.put("dataSourceNames", dataSourceNames);
            result.put("dataSources", dataSources);
            result.put("message", "获取数据源列表成功");
            
            return result;
        } catch (Exception e) {
            logger.error("获取数据源列表失败", e);
            throw new ServiceException("获取数据源列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据数据源获取指定表信息，支持分页查询
     */
    @Tool(name = "getTableInfoFromDataSource", description = "根据数据源获取指定表信息，支持分页查询")
    public Map<String, Object> getTableInfoFromDataSource(String dataSourceName, String tableName, String tableComment,
            Integer pageNum, Integer pageSize) {
        try {
            logger.info("getTableInfoFromDataSource获取数据源[{}]的表信息", dataSourceName);

            // 限制每页最大500条记录
            if (pageSize == null || pageSize > 500) {
                pageSize = 500;
            }
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }

            // 如果不是主数据源，则切换数据源
            if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master") && !StrUtil.equals(dataSourceName, "MASTER")) {
                DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            }

            try {
                // 构建查询条件
                QueryWrapper queryWrapper = QueryWrapper.create()
                        .select()
                        .from("gen_table");
                
                // 添加表名条件
                if (StrUtil.isNotBlank(tableName)) {
                    queryWrapper.and(new QueryColumn("table_name").like(tableName));
                }
                
                // 添加表注释条件
                if (StrUtil.isNotBlank(tableComment)) {
                    queryWrapper.and(new QueryColumn("table_comment").like(tableComment));
                }
                
                // 添加排序
                queryWrapper.orderBy(new QueryColumn("table_name").asc());

                // 创建分页对象
                Page<GenTable> pageObj = Page.of(pageNum, pageSize);
                
                // 执行分页查询
                Page<GenTable> page = genTableService.page(pageObj, queryWrapper);
            
            Map<String, Object> result = new HashMap<>();
            result.put("dataSourceName", dataSourceName);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalCount", page != null ? page.getTotalRow() : 0);
            result.put("totalPage", page != null ? page.getTotalPage() : 0);
            
            if (page == null || page.getRecords().isEmpty()) {
                result.put("message", "数据源中没有找到匹配的表");
                result.put("tables", new ArrayList<>());
                return result;
            }
            
            List<Map<String, Object>> tables = new ArrayList<>();
            for (GenTable table : page.getRecords()) {
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("tableName", table.getTableName());
                tableInfo.put("tableComment", table.getTableComment());
                tableInfo.put("createTime", table.getCreateTime());
                tableInfo.put("updateTime", table.getUpdateTime());
                tables.add(tableInfo);
            }
            result.put("tables", tables);
            result.put("message", "获取表信息成功");
            
                return result;
            } finally {
                // 如果不是主数据源，操作完成后清理数据源上下文
                if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master") && !StrUtil.equals(dataSourceName, "MASTER")) {
                    DynamicDataSourceContextHolder.clearDataSourceType();
                }
            }
        } catch (Exception e) {
            logger.error("获取表信息失败", e);
            throw new ServiceException("获取表信息失败：" + e.getMessage());
        }
    }
}