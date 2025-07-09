package com.ruoyi.project.gen.tools;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.datasource.DynamicDataSourceContextHolder;
import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;
import com.ruoyi.project.gen.service.IAsyncTaskService;
import com.ruoyi.project.gen.service.IGenTableColumnService;
import com.ruoyi.project.gen.service.IGenTableService;
import com.ruoyi.project.system.domain.SysConfig;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.domain.SysDictData;
import com.ruoyi.project.system.domain.SysDictType;
import com.ruoyi.project.system.domain.SysMenu;
import com.ruoyi.project.system.service.ISysConfigService;
import com.ruoyi.project.system.service.ISysDataSourceService;
import com.ruoyi.project.system.service.ISysDictDataService;
import com.ruoyi.project.system.service.ISysDictTypeService;
import com.ruoyi.project.system.service.ISysMenuService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.core.io.FileUtil;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 数据库表操作工具
 * 提供给AI模型调用的工具，用于创建和管理数据库表
 */
@Service
public class DatabaseTableTool {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseTableTool.class);

    @Autowired
    private IGenTableService genTableService;

    @Autowired
    private IGenTableColumnService genTableColumnService;

    @Autowired
    private IAsyncTaskService asyncTaskService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ISysDataSourceService sysDataSourceService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private ISysDictTypeService sysDictTypeService;

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private ISysMenuService sysMenuService;

    /**
     * 保存表定义信息和字段信息到系统（不创建实际表）
     * 
     * @param table      表信息
     * @param columns    列信息列表
     * @param dataSource 数据源名称
     * @param taskId     任务ID
     * @return 保存结果的字符串描述
     */
    @Tool(name = "saveGenTable", description = "保存表定义信息和字段信息到系统，不创建实际表，返回包含tableId的字符串结果")
    public String saveGenTable(GenTable table, List<Object> columns, String dataSource, String taskId) {
        try {
            logger.info("saveGenTable保存表定义信息: {}, taskId: {}", table, taskId);

            // 更新任务进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 20);
                
                // 更新任务extraInfo，记录当前正在创建的表信息
                String extraInfo = String.format("{\"currentAction\":\"创建表定义\",\"tableName\":\"%s\",\"tableComment\":\"%s\"}", 
                        table.getTableName(), table.getTableComment());
                asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
            }

            table.setTableId(IdUtil.getSnowflakeNextId());

            // 设置数据源
            if (StrUtil.isBlank(dataSource)) {
                dataSource = "master";
            }
            table.setDataSource(dataSource);

            // 初始化表信息
            table.setCreateBy("admin");

            // 保存表基本信息
            genTableService.save(table);

            // 更新任务进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 40);
            }

            // 保存字段信息
            if (columns != null && !columns.isEmpty()) {
                // 更新任务进度
                if (StrUtil.isNotBlank(taskId)) {
                    asyncTaskService.updateTaskProgress(taskId, 60);
                    
                    // 更新任务extraInfo，记录当前正在创建表和字段的信息
                    String extraInfo = String.format("{\"currentAction\":\"创建表结构\",\"tableName\":\"%s\",\"fieldCount\":%d}", 
                            table.getTableName(), columns.size());
                    asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
                }

                // 把 List<Object> 转成 List<GenTableColumn>
                List<GenTableColumn> genTableColumns = new ArrayList<>();
                for (Object obj : columns) {
                    GenTableColumn column = objectMapper.convertValue(obj, GenTableColumn.class);
                    column.setTableId(table.getTableId());
                    column.setColumnId(IdUtil.getSnowflakeNextId());
                    genTableColumns.add(column);
                }

                // 保存表的列信息
                for (GenTableColumn column : genTableColumns) {
                    // 更新任务extraInfo，记录当前正在保存的字段信息
                    if (StrUtil.isNotBlank(taskId)) {
                        String extraInfo = String.format("{\"currentAction\":\"保存字段\",\"tableName\":\"%s\",\"columnName\":\"%s\",\"columnComment\":\"%s\"}", 
                                table.getTableName(), column.getColumnName(), column.getColumnComment());
                        asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
                    }
                    genTableColumnService.insertGenTableColumn(column);
                }
            }

            String result = "表定义[" + table.getTableName() + "]保存成功，tableId=" + table.getTableId().toString() + 
                    "，共包含" + (columns != null ? columns.size() : 0) + "个字段。注意：实际表尚未创建，需要调用syncTableToDatabase方法同步到数据库。";
            
            // 更新任务完成状态
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskResult(taskId, result);
            }

            return result;
        } catch (Exception e) {
            logger.error("保存表定义失败", e);
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskError(taskId, "保存表定义失败：" + e.getMessage());
            }
            throw new ServiceException("保存表定义失败：" + e.getMessage());
        }
    }

    /**
     * 根据tableId同步表到数据库
     * 
     * @param tableId 表ID
     * @param taskId  任务ID
     * @return 操作结果
     */
    @Tool(name = "syncTableToDatabase", description = "根据tableId将表定义同步到数据库，创建实际的表")
    public String syncTableToDatabase(String tableId, String taskId) {
        try {
            logger.info("syncTableToDatabase同步表到数据库: tableId={}, taskId={}", tableId, taskId);

            // 获取表信息
            GenTable table = genTableService.selectGenTableById(Long.valueOf(tableId));
            if (table == null) {
                throw new ServiceException("表定义不存在，tableId=" + tableId);
            }

            // 更新任务进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 80);
                
                // 更新任务extraInfo，记录当前正在同步表到数据库
                String extraInfo = String.format("{\"currentAction\":\"同步表到数据库\",\"tableName\":\"%s\"}", table.getTableName());
                asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
            }

            // 创建实际表
            genTableService.synchDb(table.getTableName());

            String result = "表[" + table.getTableName() + "]同步到数据库成功，tableId=" + tableId + "。";
            
            // 更新任务完成状态
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskResult(taskId, result);
            }

            return result;
        } catch (Exception e) {
            logger.error("同步表到数据库失败", e);
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskError(taskId, "同步表到数据库失败：" + e.getMessage());
            }
            throw new ServiceException("同步表到数据库失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取GenTable数据
     * 
     * @param id 表ID
     * @return GenTable对象的字符串描述
     */
    @Tool(name = "getGenTableById", description = "根据ID获取表定义信息")
    public String getGenTableById(String id) {
        try {
            logger.info("getGenTableById获取表信息: {}", id);
            GenTable table = genTableService.selectGenTableById(Long.valueOf(id));
            if (table == null) {
                return "表定义不存在，tableId=" + id;
            }
            return String.format("表信息: tableId=%s, tableName=%s, tableComment=%s, dataSource=%s, createBy=%s, createTime=%s",
                    table.getTableId().toString(), table.getTableName(), table.getTableComment(), 
                    table.getDataSource(), table.getCreateBy(), table.getCreateTime());
        } catch (Exception e) {
            logger.error("获取表信息失败", e);
            throw new ServiceException("获取表信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据tableId获取GenTableColumn列表
     * 
     * @param tableId 表ID
     * @return GenTableColumn列表的字符串描述
     */
    @Tool(name = "getGenTableColumnsByTableId", description = "根据表ID获取表字段列表")
    public String getGenTableColumnsByTableId(String tableId) {
        try {
            logger.info("getGenTableColumnsByTableId获取表字段列表: {}", tableId);
            List<GenTableColumn> columns = genTableColumnService.selectGenTableColumnListByTableId(Long.valueOf(tableId));
            if (columns == null || columns.isEmpty()) {
                return "表字段列表为空，tableId=" + tableId;
            }
            StringBuilder result = new StringBuilder();
            result.append("表字段列表(共").append(columns.size()).append("个字段):\n");
            for (GenTableColumn column : columns) {
                result.append(String.format("- columnId=%s, columnName=%s, columnComment=%s, columnType=%s, javaType=%s, isPk=%s, isRequired=%s\n",
                        column.getColumnId().toString(), column.getColumnName(), column.getColumnComment(),
                        column.getColumnType(), column.getJavaType(), column.getIsPk(), column.getIsRequired()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("获取表字段列表失败", e);
            throw new ServiceException("获取表字段列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID修改GenTable
     * 
     * @param genTable 表信息
     * @return 操作结果
     */
    @Tool(name = "updateGenTable", description = "根据ID修改表定义信息")
    public String updateGenTable(GenTable genTable) {
        try {
            logger.info("updateGenTable修改表信息: {}", genTable);
            genTableService.updateGenTable(genTable);
            return "表[" + genTable.getTableName() + "]修改成功";
        } catch (Exception e) {
            logger.error("修改表信息失败", e);
            throw new ServiceException("修改表信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID修改GenTableColumn
     * 
     * @param genTableColumn 表字段信息
     * @param taskId 任务ID，用于更新任务的extraInfo
     * @param tableName 表名，用于构建extraInfo
     * @return 操作结果
     */
    @Tool(name = "updateGenTableColumn", description = "根据ID修改表字段信息")
    public String updateGenTableColumn(GenTableColumn genTableColumn, String taskId, String tableName) {
        try {
            logger.info("updateGenTableColumn修改表字段信息: {}, taskId: {}", genTableColumn, taskId);
            
            // 如果提供了任务ID，更新任务的extraInfo
            if (StrUtil.isNotBlank(taskId)) {
                String extraInfo = String.format("{\"tableName\":\"%s\",\"columnName\":\"%s\",\"columnComment\":\"%s\"}", 
                        tableName, 
                        genTableColumn.getColumnName(),
                        genTableColumn.getColumnComment());
                
                asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
                logger.info("更新任务扩展信息: 正在优化字段 {}", genTableColumn.getColumnName());
            }
            
            boolean result = genTableColumnService.updateGenTableColumn(genTableColumn);
            return result ? "字段[" + genTableColumn.getColumnName() + "]修改成功" : "字段修改失败";
        } catch (Exception e) {
            logger.error("修改表字段信息失败", e);
            throw new ServiceException("修改表字段信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取指定数据源的所有表
     * 
     * @param dataSourceName 数据源名称
     * @param tableName 表名（可选，用于模糊查询）
     * @param tableComment 表注释（可选，用于模糊查询）
     * @param pageNum 页码
     * @param pageSize 每页记录数
     * @return 表列表的字符串描述
     */
    @Tool(name = "getTablesFromDataSource", description = "获取指定数据源的所有表")
    public String getTablesFromDataSource(String dataSourceName, String tableName, String tableComment, Integer pageNum, Integer pageSize) {
        try {
            logger.info("getTablesFromDataSource获取数据源[{}]的表列表", dataSourceName);
            GenTable genTable = new GenTable();
            if (StrUtil.isNotBlank(tableName)) {
                genTable.setTableName(tableName);
            }
            if (StrUtil.isNotBlank(tableComment)) {
                genTable.setTableComment(tableComment);
            }
            Page<GenTable> page = genTableService.selectDbTableListByDataSource(genTable, dataSourceName);
            if (page == null || page.getRecords().isEmpty()) {
                return "数据源[" + dataSourceName + "]中没有找到匹配的表";
            }
            StringBuilder result = new StringBuilder();
            result.append("数据源[").append(dataSourceName).append("]表列表(共").append(page.getTotalRow()).append("条记录):\n");
            for (GenTable table : page.getRecords()) {
                result.append(String.format("- tableName=%s, tableComment=%s, createTime=%s\n",
                        table.getTableName(), table.getTableComment(), table.getCreateTime()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("获取数据源表列表失败", e);
            throw new ServiceException("获取数据源表列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取指定数据源的表结构
     * 
     * @param tableName 表名
     * @param dataSourceName 数据源名称
     * @return 表字段列表的字符串描述
     */
    @Tool(name = "getTableStructureFromDataSource", description = "获取指定数据源的表结构")
    public String getTableStructureFromDataSource(String tableName, String dataSourceName) {
        try {
            logger.info("getTableStructureFromDataSource获取数据源[{}]表[{}]的结构", dataSourceName, tableName);
            
            // 如果不是主数据源，则切换数据源
            if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master")) {
                DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            }
            
            try {
                // 获取数据源信息以获取数据库名称
                SysDataSource sysDataSource = sysDataSourceService.selectSysDataSourceByName(dataSourceName);
                if (sysDataSource == null || StrUtil.isEmpty(sysDataSource.getDatabaseName())) {
                    throw new ServiceException("数据源不存在或数据库名称未配置");
                }
                
                List<GenTableColumn> columns = genTableColumnService.selectDbTableColumnsByNameAndDataSource(tableName, sysDataSource.getDatabaseName());
                if (columns == null || columns.isEmpty()) {
                    return "数据源[" + dataSourceName + "]中表[" + tableName + "]的结构为空";
                }
                StringBuilder result = new StringBuilder();
                result.append("数据源[").append(dataSourceName).append("]表[").append(tableName).append("]结构(共").append(columns.size()).append("个字段):\n");
                for (GenTableColumn column : columns) {
                    result.append(String.format("- columnName=%s, columnComment=%s, columnType=%s, isNullable=%s, isPk=%s\n",
                            column.getColumnName(), column.getColumnComment(), column.getColumnType(),
                            column.getIsRequired(), column.getIsPk()));
                }
                return result.toString();
            } finally {
                // 如果不是主数据源，操作完成后清理数据源上下文
                if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master")) {
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
     * 
     * @param tableName 表名
     * @return 表结构信息的字符串描述
     */
    @Tool(name = "getTableStructureByName", description = "直接查询数据库表结构")
    public String getTableStructureByName(String tableName) {
        try {
            logger.info("getTableStructureByName查询表结构: {}", tableName);
            String sql = "SELECT column_name, column_type, column_comment, " +
                    "(CASE WHEN (is_nullable = 'no' AND column_key != 'PRI') THEN '1' ELSE '0' END) AS is_required, " +
                    "(CASE WHEN column_key = 'PRI' THEN '1' ELSE '0' END) AS is_pk, " +
                    "ordinal_position AS sort, " +
                    "(CASE WHEN extra = 'auto_increment' THEN '1' ELSE '0' END) AS is_increment " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = (SELECT DATABASE()) AND table_name = ? " +
                    "ORDER BY ordinal_position";
            
            List<Row> rows = Db.selectListBySql(sql, tableName);
            if (rows == null || rows.isEmpty()) {
                return "表[" + tableName + "]不存在或结构为空";
            }
            StringBuilder result = new StringBuilder();
            result.append("表[").append(tableName).append("]结构(共").append(rows.size()).append("个字段):\n");
            for (Row row : rows) {
                result.append(String.format("- columnName=%s, columnType=%s, columnComment=%s, isRequired=%s, isPk=%s, sort=%s, isIncrement=%s\n",
                        row.getString("column_name"), row.getString("column_type"), row.getString("column_comment"),
                        row.getString("is_required"), row.getString("is_pk"), row.getString("sort"), row.getString("is_increment")));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("查询表结构失败", e);
            throw new ServiceException("查询表结构失败：" + e.getMessage());
        }
    }
    
    /**
     * 直接查询指定数据源的所有表
     * 
     * @param dataSourceName 数据源名称
     * @param tableName 表名（可选，用于模糊查询）
     * @param tableComment 表注释（可选，用于模糊查询）
     * @return 表列表的字符串描述
     */
    @Tool(name = "getAllTablesFromDataSource", description = "直接查询指定数据源的所有表")
    public String getAllTablesFromDataSource(String dataSourceName, String tableName, String tableComment) {
        try {
            logger.info("getAllTablesFromDataSource查询数据源[{}]的所有表", dataSourceName);
            
            // 如果不是主数据源，则切换数据源
            if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master")) {
                DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            }
            
            try {
                // 构建SQL查询
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("SELECT table_name, table_comment, create_time, update_time ")
                        .append("FROM information_schema.tables ")
                        .append("WHERE table_schema = (SELECT DATABASE()) ");
                
                // 添加表名过滤条件
                if (StrUtil.isNotBlank(tableName)) {
                    sqlBuilder.append("AND table_name LIKE CONCAT('%', ?, '%') ");
                }
                
                // 添加表注释过滤条件
                if (StrUtil.isNotBlank(tableComment)) {
                    sqlBuilder.append("AND table_comment LIKE CONCAT('%', ?, '%') ");
                }
                
                sqlBuilder.append("ORDER BY create_time DESC");
                
                // 执行查询
                List<Row> rows;
                if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(tableComment)) {
                    rows = Db.selectListBySql(sqlBuilder.toString(), tableName, tableComment);
                } else if (StrUtil.isNotBlank(tableName)) {
                    rows = Db.selectListBySql(sqlBuilder.toString(), tableName);
                } else if (StrUtil.isNotBlank(tableComment)) {
                    rows = Db.selectListBySql(sqlBuilder.toString(), tableComment);
                } else {
                    rows = Db.selectListBySql(sqlBuilder.toString());
                }
                
                if (rows == null || rows.isEmpty()) {
                    return "数据源[" + dataSourceName + "]中没有找到匹配的表";
                }
                StringBuilder result = new StringBuilder();
                result.append("数据源[").append(dataSourceName).append("]表列表(共").append(rows.size()).append("条记录):\n");
                for (Row row : rows) {
                    result.append(String.format("- tableName=%s, tableComment=%s, createTime=%s, updateTime=%s\n",
                            row.getString("table_name"), row.getString("table_comment"), 
                            row.getString("create_time"), row.getString("update_time")));
                }
                return result.toString();
            } finally {
                // 如果不是主数据源，操作完成后清理数据源上下文
                if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master")) {
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
     * 
     * @return 数据源列表的字符串描述
     */
    @Tool(name = "getAllDataSources", description = "获取所有数据源列表")
    public String getAllDataSources() {
        try {
            logger.info("getAllDataSources获取所有数据源列表");
            List<SysDataSource> dataSources = sysDataSourceService.selectSysDataSourceList(new SysDataSource());
            if (dataSources == null || dataSources.isEmpty()) {
                return "没有找到任何数据源";
            }
            StringBuilder result = new StringBuilder();
            result.append("数据源列表(共").append(dataSources.size()).append("个数据源):\n");
            for (SysDataSource dataSource : dataSources) {
                result.append(String.format("- dataSourceId=%s, name=%s, databaseName=%s, url=%s, status=%s\n",
                        dataSource.getDataSourceId().toString(), dataSource.getName(), dataSource.getDatabaseName(),
                        dataSource.getUrl(), dataSource.getStatus()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("获取数据源列表失败", e);
            throw new ServiceException("获取数据源列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据数据源名称获取数据源信息
     * 
     * @param dataSourceName 数据源名称
     * @return 数据源信息的字符串描述
     */
    @Tool(name = "getDataSourceByName", description = "根据数据源名称获取数据源信息")
    public String getDataSourceByName(String dataSourceName) {
        try {
            logger.info("getDataSourceByName获取数据源信息: {}", dataSourceName);
            SysDataSource dataSource = sysDataSourceService.selectSysDataSourceByName(dataSourceName);
            if (dataSource == null) {
                return "数据源[" + dataSourceName + "]不存在";
            }
            return String.format("数据源信息: dataSourceId=%s, name=%s, databaseName=%s, url=%s, username=%s, status=%s",
                    dataSource.getDataSourceId().toString(), dataSource.getName(), dataSource.getDatabaseName(),
                    dataSource.getUrl(), dataSource.getUsername(), dataSource.getStatus());
        } catch (Exception e) {
            logger.error("获取数据源信息失败", e);
            throw new ServiceException("获取数据源信息失败：" + e.getMessage());
        }
    }

    /**
     * 根据数据源获取指定表信息（限制返回数量不超过500条）
     * 
     * @param dataSourceName 数据源名称
     * @param tableName 表名（可选，用于模糊查询）
     * @param tableComment 表注释（可选，用于模糊查询）
     * @param pageNum 页码，默认1
     * @param pageSize 每页记录数，最大500
     * @return 表信息列表的字符串描述
     */
    @Tool(name = "getTableInfoFromDataSource", description = "根据数据源获取指定表信息，限制返回数量不超过500条")
    public String getTableInfoFromDataSource(String dataSourceName, String tableName, String tableComment, Integer pageNum, Integer pageSize) {
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
            if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master")) {
                DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            }
            
            try {
                // 构建SQL查询
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("SELECT table_name, table_comment, create_time, update_time, table_rows, data_length ")
                        .append("FROM information_schema.tables ")
                        .append("WHERE table_schema = (SELECT DATABASE()) ");
                
                // 添加表名过滤条件
                if (StrUtil.isNotBlank(tableName)) {
                    sqlBuilder.append("AND table_name LIKE CONCAT('%', ?, '%') ");
                }
                
                // 添加表注释过滤条件
                if (StrUtil.isNotBlank(tableComment)) {
                    sqlBuilder.append("AND table_comment LIKE CONCAT('%', ?, '%') ");
                }
                
                sqlBuilder.append("ORDER BY create_time DESC ")
                        .append("LIMIT ?, ?");
                
                // 计算偏移量
                int offset = (pageNum - 1) * pageSize;
                
                // 执行查询
                List<Row> rows;
                if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(tableComment)) {
                    rows = Db.selectListBySql(sqlBuilder.toString(), tableName, tableComment, offset, pageSize);
                } else if (StrUtil.isNotBlank(tableName)) {
                    rows = Db.selectListBySql(sqlBuilder.toString(), tableName, offset, pageSize);
                } else if (StrUtil.isNotBlank(tableComment)) {
                    rows = Db.selectListBySql(sqlBuilder.toString(), tableComment, offset, pageSize);
                } else {
                    rows = Db.selectListBySql(sqlBuilder.toString(), offset, pageSize);
                }
                
                if (rows == null || rows.isEmpty()) {
                    return "数据源[" + dataSourceName + "]中没有找到匹配的表信息";
                }
                StringBuilder result = new StringBuilder();
                result.append("数据源[").append(dataSourceName).append("]表信息(共").append(rows.size()).append("条记录):\n");
                for (Row row : rows) {
                    result.append(String.format("- tableName=%s, tableComment=%s, createTime=%s, updateTime=%s, tableRows=%s, dataLength=%s\n",
                            row.getString("table_name"), row.getString("table_comment"), 
                            row.getString("create_time"), row.getString("update_time"),
                            row.getString("table_rows"), row.getString("data_length")));
                }
                return result.toString();
            } finally {
                // 如果不是主数据源，操作完成后清理数据源上下文
                if (StrUtil.isNotEmpty(dataSourceName) && !StrUtil.equals(dataSourceName, "master")) {
                    DynamicDataSourceContextHolder.clearDataSourceType();
                }
            }
        } catch (Exception e) {
            logger.error("获取表信息失败", e);
            throw new ServiceException("获取表信息失败：" + e.getMessage());
        }
    }

    // ==================== 字典表操作方法 ====================

    /**
     * 查询字典类型列表
     * 
     * @param dictType 字典类型查询条件
     * @return 字典类型列表的字符串描述
     */
    @Tool(name = "getDictTypeList", description = "查询字典类型列表")
    public String getDictTypeList(SysDictType dictType) {
        try {
            logger.info("getDictTypeList查询字典类型列表");
            List<SysDictType> dictTypes = sysDictTypeService.selectDictTypeList(dictType);
            if (dictTypes == null || dictTypes.isEmpty()) {
                return "没有找到任何字典类型";
            }
            StringBuilder result = new StringBuilder();
            result.append("字典类型列表(共").append(dictTypes.size()).append("个类型):\n");
            for (SysDictType dt : dictTypes) {
                result.append(String.format("- dictId=%s, dictName=%s, dictType=%s, status=%s\n",
                        dt.getDictId().toString(), dt.getDictName(), dt.getDictType(), dt.getStatus()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("查询字典类型列表失败", e);
            throw new ServiceException("查询字典类型列表失败：" + e.getMessage());
        }
    }

    /**
     * 新增字典类型
     * 
     * @param dictType 字典类型信息
     * @return 操作结果
     */
    @Tool(name = "addDictType", description = "新增字典类型")
    public String addDictType(SysDictType dictType) {
        try {
            logger.info("addDictType新增字典类型: {}", dictType);
            boolean save = sysDictTypeService.save(dictType);
            return save ? "字典类型[" + dictType.getDictName() + "]新增成功" : "字典类型新增失败";
        } catch (Exception e) {
            logger.error("新增字典类型失败", e);
            throw new ServiceException("新增字典类型失败：" + e.getMessage());
        }
    }

    /**
     * 修改字典类型
     * 
     * @param dictType 字典类型信息
     * @return 操作结果
     */
    @Tool(name = "updateDictType", description = "修改字典类型")
    public String updateDictType(SysDictType dictType) {
        try {
            logger.info("updateDictType修改字典类型: {}", dictType);
            boolean result = sysDictTypeService.updateById(dictType);
            return result ? "字典类型[" + dictType.getDictName() + "]修改成功" : "字典类型修改失败";
        } catch (Exception e) {
            logger.error("修改字典类型失败", e);
            throw new ServiceException("修改字典类型失败：" + e.getMessage());
        }
    }

    /**
     * 查询字典数据列表
     * 
     * @param dictData 字典数据查询条件
     * @return 字典数据列表的字符串描述
     */
    @Tool(name = "getDictDataList", description = "查询字典数据列表")
    public String getDictDataList(SysDictData dictData) {
        try {
            logger.info("getDictDataList查询字典数据列表");
            List<SysDictData> dictDataList = sysDictDataService.selectDictDataList(dictData);
            if (dictDataList == null || dictDataList.isEmpty()) {
                return "没有找到任何字典数据";
            }
            StringBuilder result = new StringBuilder();
            result.append("字典数据列表(共").append(dictDataList.size()).append("条数据):\n");
            for (SysDictData dd : dictDataList) {
                result.append(String.format("- dictCode=%s, dictLabel=%s, dictValue=%s, dictType=%s, status=%s\n",
                        dd.getDictCode().toString(), dd.getDictLabel(), dd.getDictValue(), dd.getDictType(), dd.getStatus()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("查询字典数据列表失败", e);
            throw new ServiceException("查询字典数据列表失败：" + e.getMessage());
        }
    }

    /**
     * 新增字典数据
     * 
     * @param dictData 字典数据信息
     * @return 操作结果
     */
    @Tool(name = "addDictData", description = "新增字典数据")
    public String addDictData(SysDictData dictData) {
        try {
            logger.info("addDictData新增字典数据: {}", dictData);
            boolean result = sysDictDataService.save(dictData);
            return result ? "字典数据[" + dictData.getDictLabel() + "]新增成功" : "字典数据新增失败";
        } catch (Exception e) {
            logger.error("新增字典数据失败", e);
            throw new ServiceException("新增字典数据失败：" + e.getMessage());
        }
    }

    /**
     * 修改字典数据
     * 
     * @param dictData 字典数据信息
     * @return 操作结果
     */
    @Tool(name = "updateDictData", description = "修改字典数据")
    public String updateDictData(SysDictData dictData) {
        try {
            logger.info("updateDictData修改字典数据: {}", dictData);
            boolean result = sysDictDataService.updateById(dictData);
            return result ? "字典数据[" + dictData.getDictLabel() + "]修改成功" : "字典数据修改失败";
        } catch (Exception e) {
            logger.error("修改字典数据失败", e);
            throw new ServiceException("修改字典数据失败：" + e.getMessage());
        }
    }

    // ==================== 系统参数表操作方法 ====================

    /**
     * 查询系统参数列表
     * 
     * @param config 系统参数查询条件
     * @return 系统参数列表的字符串描述
     */
    @Tool(name = "getConfigList", description = "查询系统参数列表")
    public String getConfigList(SysConfig config) {
        try {
            logger.info("getConfigList查询系统参数列表");
            List<SysConfig> configList = sysConfigService.selectConfigList(config);
            if (configList == null || configList.isEmpty()) {
                return "没有找到任何系统参数";
            }
            StringBuilder result = new StringBuilder();
            result.append("系统参数列表(共").append(configList.size()).append("个参数):\n");
            for (SysConfig cfg : configList) {
                result.append(String.format("- configId=%s, configName=%s, configKey=%s, configValue=%s, configType=%s\n",
                        cfg.getConfigId().toString(), cfg.getConfigName(), cfg.getConfigKey(), cfg.getConfigValue(), cfg.getConfigType()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("查询系统参数列表失败", e);
            throw new ServiceException("查询系统参数列表失败：" + e.getMessage());
        }
    }

    /**
     * 新增系统参数
     * 
     * @param config 系统参数信息
     * @return 操作结果
     */
    @Tool(name = "addConfig", description = "新增系统参数")
    public String addConfig(SysConfig config) {
        try {
            logger.info("addConfig新增系统参数: {}", config);
            boolean result = sysConfigService.save(config);
            return result ? "系统参数[" + config.getConfigName() + "]新增成功" : "系统参数新增失败";
        } catch (Exception e) {
            logger.error("新增系统参数失败", e);
            throw new ServiceException("新增系统参数失败：" + e.getMessage());
        }
    }

    /**
     * 修改系统参数
     * 
     * @param config 系统参数信息
     * @return 操作结果
     */
    @Tool(name = "updateConfig", description = "修改系统参数")
    public String updateConfig(SysConfig config) {
        try {
            logger.info("updateConfig修改系统参数: {}", config);
            boolean result = sysConfigService.updateById(config);
            return result ? "系统参数[" + config.getConfigName() + "]修改成功" : "系统参数修改失败";
        } catch (Exception e) {
            logger.error("修改系统参数失败", e);
            throw new ServiceException("修改系统参数失败：" + e.getMessage());
        }
    }

    /**
     * 根据参数键名查询参数值
     * 
     * @param configKey 参数键名
     * @return 参数值
     */
    @Tool(name = "getConfigByKey", description = "根据参数键名查询参数值")
    public String getConfigByKey(String configKey) {
        try {
            logger.info("getConfigByKey查询参数值: {}", configKey);
            return sysConfigService.selectConfigByKey(configKey);
        } catch (Exception e) {
            logger.error("查询参数值失败", e);
            throw new ServiceException("查询参数值失败：" + e.getMessage());
        }
    }

    // ==================== 主数据源菜单表查询方法 ====================

    /**
     * 查询菜单列表
     * 
     * @param menu 菜单查询条件
     * @param userId 用户ID（可选，用于权限过滤）
     * @return 菜单列表的字符串描述
     */
    @Tool(name = "getMenuList", description = "查询主数据源的菜单列表")
    public String getMenuList(SysMenu menu, Long userId) {
        try {
            logger.info("getMenuList查询菜单列表, userId: {}", userId);
            List<SysMenu> menuList;
            if (userId != null) {
                menuList = sysMenuService.selectMenuList(menu, userId);
            } else {
                menuList = sysMenuService.selectMenuList(menu, null);
            }
            if (menuList == null || menuList.isEmpty()) {
                return "没有找到任何菜单";
            }
            StringBuilder result = new StringBuilder();
            result.append("菜单列表(共").append(menuList.size()).append("个菜单):\n");
            for (SysMenu m : menuList) {
                result.append(String.format("- menuId=%s, menuName=%s, parentId=%s, orderNum=%s, path=%s, menuType=%s, visible=%s, status=%s\n",
                        m.getMenuId().toString(), m.getMenuName(), m.getParentId().toString(), m.getOrderNum().toString(), 
                        m.getPath(), m.getMenuType(), m.getVisible(), m.getStatus()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("查询菜单列表失败", e);
            throw new ServiceException("查询菜单列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询菜单树
     * 
     * @param userId 用户ID
     * @return 菜单树列表的字符串描述
     */
    @Tool(name = "getMenuTreeByUserId", description = "根据用户ID查询菜单树")
    public String getMenuTreeByUserId(Long userId) {
        try {
            logger.info("getMenuTreeByUserId查询用户菜单树: {}", userId);
            List<SysMenu> menuList = sysMenuService.selectMenuList(userId);
            if (menuList == null || menuList.isEmpty()) {
                return "用户[" + userId + "]没有任何菜单权限";
            }
            StringBuilder result = new StringBuilder();
            result.append("用户[").append(userId).append("]菜单树(共").append(menuList.size()).append("个菜单):\n");
            for (SysMenu m : menuList) {
                result.append(String.format("- menuId=%s, menuName=%s, parentId=%s, orderNum=%s, path=%s, menuType=%s, visible=%s, status=%s\n",
                        m.getMenuId().toString(), m.getMenuName(), m.getParentId().toString(), m.getOrderNum().toString(), 
                        m.getPath(), m.getMenuType(), m.getVisible(), m.getStatus()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("查询用户菜单树失败", e);
            throw new ServiceException("查询用户菜单树失败：" + e.getMessage());
        }
    }

    /**
     * 根据菜单ID查询菜单信息
     * 
     * @param menuId 菜单ID
     * @return 菜单信息的字符串描述
     */
    @Tool(name = "getMenuById", description = "根据菜单ID查询菜单信息")
    public String getMenuById(Long menuId) {
        try {
            logger.info("getMenuById查询菜单信息: {}", menuId);
            SysMenu menu = sysMenuService.getById(menuId);
            if (menu == null) {
                return "菜单[" + menuId + "]不存在";
            }
            return String.format("菜单信息: menuId=%s, menuName=%s, parentId=%s, orderNum=%s, path=%s, component=%s, menuType=%s, visible=%s, status=%s, perms=%s, icon=%s, remark=%s",
                    menu.getMenuId().toString(), menu.getMenuName(), menu.getParentId().toString(), menu.getOrderNum().toString(),
                    menu.getPath(), menu.getComponent(), menu.getMenuType(), menu.getVisible(), menu.getStatus(),
                    menu.getPerms(), menu.getIcon(), menu.getRemark());
        } catch (Exception e) {
            logger.error("查询菜单信息失败", e);
            throw new ServiceException("查询菜单信息失败：" + e.getMessage());
        }
    }

    // ==================== 其他数据源的数据操作方法 ====================

    /**
     * 向指定数据源的表中添加数据
     * 
     * @param dataSourceName 数据源名称
     * @param tableName 表名
     * @param data 数据（Map格式，key为字段名，value为字段值）
     * @return 操作结果
     */
    @Tool(name = "addDataToTable", description = "向指定数据源的表中添加数据")
    public String addDataToTable(String dataSourceName, String tableName, java.util.Map<String, Object> data) {
        try {
            logger.info("addDataToTable向数据源[{}]的表[{}]添加数据", dataSourceName, tableName);
            
            // 判断不能是主数据源
            if (StrUtil.isEmpty(dataSourceName) || StrUtil.equals(dataSourceName, "master")) {
                throw new ServiceException("不能对主数据源进行数据操作");
            }
            
            // 切换到指定数据源
            DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            
            try {
                // 使用Row方式插入数据
                Row row = new Row();
                for (java.util.Map.Entry<String, Object> entry : data.entrySet()) {
                    row.set(entry.getKey(), entry.getValue());
                }
                
                // 执行插入
                int result = Db.insert(tableName, row);
                return result > 0 ? "数据添加成功" : "数据添加失败";
            } finally {
                // 操作完成后清理数据源上下文
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        } catch (Exception e) {
            logger.error("添加数据失败", e);
            throw new ServiceException("添加数据失败：" + e.getMessage());
        }
    }

    /**
     * 修改指定数据源的表中的数据
     * 
     * @param dataSourceName 数据源名称
     * @param tableName 表名
     * @param data 要修改的数据（Map格式，key为字段名，value为字段值）
     * @param whereCondition 修改条件（Map格式，key为字段名，value为字段值）
     * @return 操作结果
     */
    @Tool(name = "updateDataInTable", description = "修改指定数据源的表中的数据")
    public String updateDataInTable(String dataSourceName, String tableName, java.util.Map<String, Object> data, java.util.Map<String, Object> whereCondition) {
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
                boolean first = true;
                for (java.util.Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!first) {
                        sqlBuilder.append(", ");
                    }
                    sqlBuilder.append(entry.getKey()).append(" = ?");
                    params.add(entry.getValue());
                    first = false;
                }
                
                // 添加WHERE条件
                if (whereCondition != null && !whereCondition.isEmpty()) {
                    sqlBuilder.append(" WHERE ");
                    first = true;
                    for (java.util.Map.Entry<String, Object> entry : whereCondition.entrySet()) {
                        if (!first) {
                            sqlBuilder.append(" AND ");
                        }
                        sqlBuilder.append(entry.getKey()).append(" = ?");
                        params.add(entry.getValue());
                        first = false;
                    }
                }
                
                // 执行更新
                int result = Db.updateBySql(sqlBuilder.toString(), params.toArray());
                return result > 0 ? "数据修改成功，影响" + result + "行" : "数据修改失败";
            } finally {
                // 操作完成后清理数据源上下文
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        } catch (Exception e) {
            logger.error("修改数据失败", e);
            throw new ServiceException("修改数据失败：" + e.getMessage());
        }
    }

    /**
     * 查询指定数据源的表中的数据
     * 
     * @param dataSourceName 数据源名称
     * @param tableName 表名
     * @param whereCondition 查询条件（Map格式，key为字段名，value为字段值）
     * @param limit 限制返回记录数，最大500
     * @return 查询结果的字符串描述
     */
    @Tool(name = "queryDataFromTable", description = "查询指定数据源的表中的数据")
    public String queryDataFromTable(String dataSourceName, String tableName, java.util.Map<String, Object> whereCondition, Integer limit) {
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
                    boolean first = true;
                    for (java.util.Map.Entry<String, Object> entry : whereCondition.entrySet()) {
                        if (!first) {
                            sqlBuilder.append(" AND ");
                        }
                        sqlBuilder.append(entry.getKey()).append(" = ?");
                        params.add(entry.getValue());
                        first = false;
                    }
                }
                
                sqlBuilder.append(" LIMIT ?");
                params.add(limit);
                
                // 执行查询
                List<Row> rows = Db.selectListBySql(sqlBuilder.toString(), params.toArray());
                
                if (rows == null || rows.isEmpty()) {
                    return "表[" + tableName + "]中没有找到匹配的数据";
                }
                
                StringBuilder result = new StringBuilder();
                result.append("表[").append(tableName).append("]查询结果(共").append(rows.size()).append("条记录):\n");
                for (Row row : rows) {
                    result.append("- ").append(row.toString()).append("\n");
                }
                return result.toString();
            } finally {
                // 操作完成后清理数据源上下文
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        } catch (Exception e) {
            logger.error("查询数据失败", e);
            throw new ServiceException("查询数据失败：" + e.getMessage());
        }
    }

    // ==================== MD文件读取工具 ====================

    /**
     * 读取OSS上的MD文件内容并进行适量压缩
     * 
     * @param ossUrl MD文件的OSS地址
     * @return 压缩后的MD文件内容
     */
    @Tool(name = "readMarkdownFromOss", description = "读取OSS上的MD文件内容并进行适量压缩，确保AI能够正确理解")
    public String readMarkdownFromOss(String ossUrl) {
        try {
            logger.info("readMarkdownFromOss读取MD文件: {}", ossUrl);
            
            // 验证URL格式
            if (StrUtil.isBlank(ossUrl)) {
                throw new ServiceException("MD文件地址不能为空");
            }
            
            if (!ossUrl.toLowerCase().startsWith("http")) {
                throw new ServiceException("MD文件地址必须是有效的HTTP/HTTPS URL");
            }
            
            // 创建临时文件
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = "temp_md_" + System.currentTimeMillis() + ".md";
            File tempFile = new File(tempDir, fileName);
            
            try {
                // 使用hutool的HttpUtil下载文件到临时目录
                HttpUtil.downloadFile(ossUrl, tempFile);
                
                // 读取下载的文件内容
                String content = FileUtil.readString(tempFile, StandardCharsets.UTF_8);
                
                if (StrUtil.isBlank(content)) {
                    return "文件内容为空";
                }
                
                // 对内容进行适量压缩，保证AI理解的准确性
                String compressedContent = compressMarkdownContent(content);
                
                logger.info("MD文件读取成功，原始长度: {}, 压缩后长度: {}", content.length(), compressedContent.length());
                return compressedContent;
                
            } finally {
                // 清理临时文件
                if (tempFile.exists()) {
                    FileUtil.del(tempFile);
                    logger.debug("临时文件已删除: {}", tempFile.getAbsolutePath());
                }
            }
            
        } catch (Exception e) {
            logger.error("读取MD文件失败: {}", ossUrl, e);
            throw new ServiceException("读取MD文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 压缩Markdown内容，保留关键信息，确保AI能够正确理解
     * 
     * @param content 原始MD内容
     * @return 压缩后的内容
     */
    private String compressMarkdownContent(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        
        StringBuilder compressed = new StringBuilder();
        String[] lines = content.split("\\n");
        
        boolean inCodeBlock = false;
        String codeBlockType = "";
        StringBuilder codeContent = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 处理代码块
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // 代码块结束
                    inCodeBlock = false;
                    // 保留代码块，但适当压缩
                    compressed.append("```").append(codeBlockType).append("\\n");
                    String compressedCode = compressCodeBlock(codeContent.toString());
                    compressed.append(compressedCode);
                    compressed.append("\\n```\\n");
                    codeContent.setLength(0);
                    codeBlockType = "";
                } else {
                    // 代码块开始
                    inCodeBlock = true;
                    codeBlockType = trimmedLine.substring(3).trim();
                }
                continue;
            }
            
            if (inCodeBlock) {
                // 在代码块内
                codeContent.append(line).append("\\n");
                continue;
            }
            
            // 跳过空行（但保留一些结构）
            if (trimmedLine.isEmpty()) {
                // 只在前一行不是空行时添加空行
                if (compressed.length() > 0 && !compressed.toString().endsWith("\\n\\n")) {
                    compressed.append("\\n");
                }
                continue;
            }
            
            // 保留标题
            if (trimmedLine.startsWith("#")) {
                compressed.append(line).append("\\n");
                continue;
            }
            
            // 保留列表项
            if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*") || 
                trimmedLine.startsWith("+") || trimmedLine.matches("^\\d+\\.")) {
                compressed.append(line).append("\\n");
                continue;
            }
            
            // 保留表格
            if (trimmedLine.contains("|")) {
                compressed.append(line).append("\\n");
                continue;
            }
            
            // 保留引用
            if (trimmedLine.startsWith(">")) {
                compressed.append(line).append("\\n");
                continue;
            }
            
            // 普通段落 - 移除多余空格，但保留基本格式
            String cleanedLine = line.replaceAll("\\s+", " ").trim();
            if (cleanedLine.length() > 0) {
                compressed.append(cleanedLine).append("\\n");
            }
        }
        
        // 最终清理：移除多余的空行
        String result = compressed.toString();
        result = result.replaceAll("\\n{3,}", "\\n\\n"); // 最多保留两个连续换行
        
        return result.trim();
    }
    
    /**
     * 压缩代码块内容
     * 
     * @param codeContent 代码内容
     * @return 压缩后的代码内容
     */
    private String compressCodeBlock(String codeContent) {
        if (StrUtil.isBlank(codeContent)) {
            return codeContent;
        }
        
        String[] lines = codeContent.split("\\n");
        StringBuilder compressed = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 跳过空行和注释行（但保留一些重要注释）
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // 保留重要的注释（包含关键词的）
            if (trimmedLine.startsWith("//") || trimmedLine.startsWith("#") || 
                trimmedLine.startsWith("/*") || trimmedLine.startsWith("*")) {
                if (containsImportantKeywords(trimmedLine)) {
                    compressed.append(line).append("\\n");
                }
                continue;
            }
            
            // 保留所有非注释代码行
            compressed.append(line).append("\\n");
        }
        
        return compressed.toString();
    }
    
    /**
     * 检查注释是否包含重要关键词
     * 
     * @param comment 注释内容
     * @return 是否包含重要关键词
     */
    private boolean containsImportantKeywords(String comment) {
        String lowerComment = comment.toLowerCase();
        String[] keywords = {"todo", "fixme", "note", "important", "warning", "注意", "重要", "说明", "参数", "返回", "异常"};
        
        for (String keyword : keywords) {
            if (lowerComment.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}