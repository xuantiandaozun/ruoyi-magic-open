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
     * 保存表定义信息到系统
     * 
     * @param table      表信息
     * @param dataSource 数据源名称
     * @param taskId     任务ID
     * @return 保存的表信息（包含生成的tableId）
     */
    @Tool(name = "saveGenTable", description = "保存表定义信息到系统，返回包含tableId的表对象")
    public GenTable saveGenTable(GenTable table, String dataSource, String taskId) {
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

            // 保存表基本信息(不包含列)
            genTableService.save(table);

            // 更新任务进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 40);
            }

            return table;
        } catch (Exception e) {
            logger.error("保存表定义失败", e);
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskError(taskId, "保存表定义失败：" + e.getMessage());
            }
            throw new ServiceException("保存表定义失败：" + e.getMessage());
        }
    }

    /**
     * 保存表字段信息到系统并创建数据库表
     * 
     * @param tableId   表ID
     * @param columns   列信息列表
     * @param tableName 表名
     * @param taskId    任务ID
     * @return 操作结果
     */
    @Tool(name = "saveGenTableColumns", description = "保存表字段信息并在数据库中创建实际的表")
    public String saveGenTableColumns(Long tableId, List<Object> columns, String tableName, String taskId) {
        try {
            logger.info("saveGenTableColumns创建表: {}, taskId: {}", tableName, taskId);

            // 更新任务进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 60);
                
                // 更新任务extraInfo，记录当前正在创建表和字段的信息
                String extraInfo = String.format("{\"currentAction\":\"创建表结构\",\"tableName\":\"%s\",\"fieldCount\":%d}", 
                        tableName, columns.size());
                asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
            }

            // 把 List<Object> 转成 List<GenTableColumn>
            List<GenTableColumn> genTableColumns = new ArrayList<>();
            for (Object obj : columns) {
                GenTableColumn column = objectMapper.convertValue(obj, GenTableColumn.class);
                column.setTableId(tableId);
                column.setColumnId(IdUtil.getSnowflakeNextId());
                genTableColumns.add(column);
            }

            // 保存表的列信息
            for (GenTableColumn column : genTableColumns) {
                // 更新任务extraInfo，记录当前正在保存的字段信息
                if (StrUtil.isNotBlank(taskId)) {
                    String extraInfo = String.format("{\"currentAction\":\"保存字段\",\"tableName\":\"%s\",\"columnName\":\"%s\",\"columnComment\":\"%s\"}", 
                            tableName, column.getColumnName(), column.getColumnComment());
                    asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
                }
                genTableColumnService.insertGenTableColumn(column);
            }

            // 更新任务进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 80);
                
                // 更新任务extraInfo，记录当前正在同步表到数据库
                String extraInfo = String.format("{\"currentAction\":\"同步表到数据库\",\"tableName\":\"%s\"}", tableName);
                asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
            }

            // 创建实际表
            genTableService.synchDb(tableName);

            String result = "表[" + tableName + "]创建成功，共包含" + genTableColumns.size() + "个字段。";
            
            // 更新任务完成状态
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskResult(taskId, result);
            }

            return result;
        } catch (Exception e) {
            logger.error("创建表失败", e);
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskError(taskId, "创建表失败：" + e.getMessage());
            }
            throw new ServiceException("创建表失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取GenTable数据
     * 
     * @param id 表ID
     * @return GenTable对象
     */
    @Tool(name = "getGenTableById", description = "根据ID获取表定义信息")
    public GenTable getGenTableById(Long id) {
        try {
            logger.info("getGenTableById获取表信息: {}", id);
            return genTableService.selectGenTableById(id);
        } catch (Exception e) {
            logger.error("获取表信息失败", e);
            throw new ServiceException("获取表信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据tableId获取GenTableColumn列表
     * 
     * @param tableId 表ID
     * @return GenTableColumn列表
     */
    @Tool(name = "getGenTableColumnsByTableId", description = "根据表ID获取表字段列表")
    public List<GenTableColumn> getGenTableColumnsByTableId(Long tableId) {
        try {
            logger.info("getGenTableColumnsByTableId获取表字段列表: {}", tableId);
            return genTableColumnService.selectGenTableColumnListByTableId(tableId);
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
     * @return 表列表（分页）
     */
    @Tool(name = "getTablesFromDataSource", description = "获取指定数据源的所有表")
    public Page<GenTable> getTablesFromDataSource(String dataSourceName, String tableName, String tableComment, Integer pageNum, Integer pageSize) {
        try {
            logger.info("getTablesFromDataSource获取数据源[{}]的表列表", dataSourceName);
            GenTable genTable = new GenTable();
            if (StrUtil.isNotBlank(tableName)) {
                genTable.setTableName(tableName);
            }
            if (StrUtil.isNotBlank(tableComment)) {
                genTable.setTableComment(tableComment);
            }
            return genTableService.selectDbTableListByDataSource(genTable, dataSourceName);
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
     * @return 表字段列表
     */
    @Tool(name = "getTableStructureFromDataSource", description = "获取指定数据源的表结构")
    public List<GenTableColumn> getTableStructureFromDataSource(String tableName, String dataSourceName) {
        try {
            logger.info("getTableStructureFromDataSource获取数据源[{}]表[{}]的结构", dataSourceName, tableName);
            return genTableColumnService.selectDbTableColumnsByNameAndDataSource(tableName, dataSourceName);
        } catch (Exception e) {
            logger.error("获取表结构失败", e);
            throw new ServiceException("获取表结构失败：" + e.getMessage());
        }
    }
    
    /**
     * 直接查询数据库表结构
     * 
     * @param tableName 表名
     * @return 表结构信息
     */
    @Tool(name = "getTableStructureByName", description = "直接查询数据库表结构")
    public List<Row> getTableStructureByName(String tableName) {
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
            
            return Db.selectListBySql(sql, tableName);
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
     * @return 表列表
     */
    @Tool(name = "getAllTablesFromDataSource", description = "直接查询指定数据源的所有表")
    public List<Row> getAllTablesFromDataSource(String dataSourceName, String tableName, String tableComment) {
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
                if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(tableComment)) {
                    return Db.selectListBySql(sqlBuilder.toString(), tableName, tableComment);
                } else if (StrUtil.isNotBlank(tableName)) {
                    return Db.selectListBySql(sqlBuilder.toString(), tableName);
                } else if (StrUtil.isNotBlank(tableComment)) {
                    return Db.selectListBySql(sqlBuilder.toString(), tableComment);
                } else {
                    return Db.selectListBySql(sqlBuilder.toString());
                }
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
     * @return 数据源列表
     */
    @Tool(name = "getAllDataSources", description = "获取所有数据源列表")
    public List<SysDataSource> getAllDataSources() {
        try {
            logger.info("getAllDataSources获取所有数据源列表");
            return sysDataSourceService.selectSysDataSourceList(new SysDataSource());
        } catch (Exception e) {
            logger.error("获取数据源列表失败", e);
            throw new ServiceException("获取数据源列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据数据源名称获取数据源信息
     * 
     * @param dataSourceName 数据源名称
     * @return 数据源信息
     */
    @Tool(name = "getDataSourceByName", description = "根据数据源名称获取数据源信息")
    public SysDataSource getDataSourceByName(String dataSourceName) {
        try {
            logger.info("getDataSourceByName获取数据源信息: {}", dataSourceName);
            return sysDataSourceService.selectSysDataSourceByName(dataSourceName);
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
     * @return 表信息列表
     */
    @Tool(name = "getTableInfoFromDataSource", description = "根据数据源获取指定表信息，限制返回数量不超过500条")
    public List<Row> getTableInfoFromDataSource(String dataSourceName, String tableName, String tableComment, Integer pageNum, Integer pageSize) {
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
                if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(tableComment)) {
                    return Db.selectListBySql(sqlBuilder.toString(), tableName, tableComment, offset, pageSize);
                } else if (StrUtil.isNotBlank(tableName)) {
                    return Db.selectListBySql(sqlBuilder.toString(), tableName, offset, pageSize);
                } else if (StrUtil.isNotBlank(tableComment)) {
                    return Db.selectListBySql(sqlBuilder.toString(), tableComment, offset, pageSize);
                } else {
                    return Db.selectListBySql(sqlBuilder.toString(), offset, pageSize);
                }
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
     * @return 字典类型列表
     */
    @Tool(name = "getDictTypeList", description = "查询字典类型列表")
    public List<SysDictType> getDictTypeList(SysDictType dictType) {
        try {
            logger.info("getDictTypeList查询字典类型列表");
            return sysDictTypeService.selectDictTypeList(dictType);
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
     * @return 字典数据列表
     */
    @Tool(name = "getDictDataList", description = "查询字典数据列表")
    public List<SysDictData> getDictDataList(SysDictData dictData) {
        try {
            logger.info("getDictDataList查询字典数据列表");
            return sysDictDataService.selectDictDataList(dictData);
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
     * @return 系统参数列表
     */
    @Tool(name = "getConfigList", description = "查询系统参数列表")
    public List<SysConfig> getConfigList(SysConfig config) {
        try {
            logger.info("getConfigList查询系统参数列表");
            return sysConfigService.selectConfigList(config);
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
     * @return 菜单列表
     */
    @Tool(name = "getMenuList", description = "查询主数据源的菜单列表")
    public List<SysMenu> getMenuList(SysMenu menu, Long userId) {
        try {
            logger.info("getMenuList查询菜单列表, userId: {}", userId);
            if (userId != null) {
                return sysMenuService.selectMenuList(menu, userId);
            } else {
                return sysMenuService.selectMenuList(menu, null);
            }
        } catch (Exception e) {
            logger.error("查询菜单列表失败", e);
            throw new ServiceException("查询菜单列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询菜单树
     * 
     * @param userId 用户ID
     * @return 菜单树列表
     */
    @Tool(name = "getMenuTreeByUserId", description = "根据用户ID查询菜单树")
    public List<SysMenu> getMenuTreeByUserId(Long userId) {
        try {
            logger.info("getMenuTreeByUserId查询用户菜单树: {}", userId);
            return sysMenuService.selectMenuList(userId);
        } catch (Exception e) {
            logger.error("查询用户菜单树失败", e);
            throw new ServiceException("查询用户菜单树失败：" + e.getMessage());
        }
    }

    /**
     * 根据菜单ID查询菜单信息
     * 
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    @Tool(name = "getMenuById", description = "根据菜单ID查询菜单信息")
    public SysMenu getMenuById(Long menuId) {
        try {
            logger.info("getMenuById查询菜单信息: {}", menuId);
            return sysMenuService.getById(menuId);
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
     * @return 查询结果
     */
    @Tool(name = "queryDataFromTable", description = "查询指定数据源的表中的数据")
    public List<Row> queryDataFromTable(String dataSourceName, String tableName, java.util.Map<String, Object> whereCondition, Integer limit) {
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
                return Db.selectListBySql(sqlBuilder.toString(), params.toArray());
            } finally {
                // 操作完成后清理数据源上下文
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        } catch (Exception e) {
            logger.error("查询数据失败", e);
            throw new ServiceException("查询数据失败：" + e.getMessage());
        }
    }
}