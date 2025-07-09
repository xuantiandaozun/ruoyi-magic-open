package com.ruoyi.project.gen.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.RowUtil;
import com.ruoyi.framework.datasource.DataSourceUtils;
import com.ruoyi.framework.datasource.DynamicDataSourceContextHolder;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;
import com.ruoyi.project.gen.mapper.GenTableMapper;
import com.ruoyi.project.gen.service.IGenTableColumnService;
import com.ruoyi.project.gen.service.IGenTableService;
import com.ruoyi.project.gen.util.GenUtils;
import com.ruoyi.project.gen.util.VelocityInitializer;
import com.ruoyi.project.gen.util.VelocityUtils;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.service.ISysDataSourceService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 业务 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class GenTableServiceImpl extends ServiceImpl<GenTableMapper, GenTable> implements IGenTableService {

    private static final Logger log = LoggerFactory.getLogger(GenTableServiceImpl.class);

    @Autowired
    private IGenTableColumnService genTableColumnService;

    @Autowired
    private ISysDataSourceService sysDataSourceService;

    @Autowired
    private DataSourceUtils dataSourceUtils;

    /**
     * 查询业务列表
     * 
     * @param genTable 业务信息
     * @return 业务集合
     */
    @Override
    public List<GenTable> selectGenTableList(GenTable genTable) {
        QueryWrapper query = QueryWrapper.create()
                .from("gen_table")
                .where(new QueryColumn("table_name").like(genTable.getTableName(),
                        ObjectUtil.isNotEmpty(genTable.getTableName())))
                .and(new QueryColumn("table_comment").like(genTable.getTableComment(),
                        ObjectUtil.isNotEmpty(genTable.getTableComment())))
                .and(new QueryColumn("create_time").between(
                        genTable.getParams().get("beginTime"),
                        genTable.getParams().get("endTime"),
                        ObjectUtil.isNotNull(genTable.getParams().get("beginTime"))))
                .orderBy("create_time desc");
        return this.list(query);
    }

    /**
     * 查询数据库列表（分页）
     * 
     * @param genTable 业务信息
     * @return 数据库表集合
     */
    @Override
    public Page<GenTable> selectDbTableList(GenTable genTable) {
        // 获取分页参数
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 构建查询条件
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT table_name, table_comment, ")
                 // 使用CAST处理日期时间类型，将其转换为字符串
                 .append("create_time, ")
                 .append("update_time ")
                 .append("FROM information_schema.tables ")
                 .append("WHERE table_schema = (SELECT DATABASE()) ")
                 .append("AND table_name NOT LIKE 'qrtz\\_%' AND table_name NOT LIKE 'gen\\_%' ")
                 .append("AND table_name NOT IN (SELECT table_name FROM gen_table WHERE table_name IS NOT NULL) ");
        
        List<Object> params = new ArrayList<>();
        
        // 添加条件查询
        if (ObjectUtil.isNotEmpty(genTable.getTableName())) {
            sqlBuilder.append("AND table_name LIKE ? ");
            params.add("%" + genTable.getTableName() + "%");
        }
        
        if (ObjectUtil.isNotEmpty(genTable.getTableComment())) {
            sqlBuilder.append("AND table_comment LIKE ? ");
            params.add("%" + genTable.getTableComment() + "%");
        }
        
        // 添加排序
        sqlBuilder.append("ORDER BY create_time DESC ");
        
        // 先查询总记录数
        String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") t";
        Long total = (Long) Db.selectObject(countSql, params.toArray());
        
        // 添加分页条件
        sqlBuilder.append("LIMIT ?, ?");
        params.add((pageNum - 1) * pageSize);
        params.add(pageSize);
        
        // 执行查询并获取结果
        List<Row> rows = Db.selectListBySql(sqlBuilder.toString(), params.toArray());

        // 使用自定义RowUtil直接转为GenTable实体列表
        List<GenTable> list = RowUtil.toEntityList(rows, GenTable.class);

        // 手动构造 Page 对象
        Page<GenTable> page = new Page<>(pageNum, pageSize);
        page.setRecords(list);
        page.setTotalRow(total != null ? total : 0);
        
        return page;
    }
    
    /**
     * 根据指定数据源查询数据库列表（分页）
     * 
     * @param genTable       业务信息
     * @param dataSourceName 数据源名称
     * @return 数据库表集合
     */
    @Override
    public Page<GenTable> selectDbTableListByDataSource(GenTable genTable, String dataSourceName) {
        // 获取分页参数
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        // 获取已生成的表名列表
        final List<String> genTableNames = new ArrayList<>();
        try {
            genTableNames.addAll(this.selectTableNameList());
        } catch (Exception e) {
            log.warn("获取已生成表名列表失败，将不过滤已生成的表: {}", e.getMessage());
        }

        return dataSourceUtils.executeWithDataSource(dataSourceName, () -> {
            // 获取数据源信息
            SysDataSource sysDataSource = sysDataSourceService.selectSysDataSourceByName(dataSourceName);
            
            // 构建查询条件
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT table_name, table_comment, ")
                    .append("create_time, ")
                    .append("update_time ")
                    .append("FROM information_schema.tables ")
                    .append("WHERE table_schema = ? ")
                    .append("AND table_name NOT LIKE 'qrtz\\_%' AND table_name NOT LIKE 'gen\\_%' ");
            
            List<Object> params = new ArrayList<>();
            params.add(sysDataSource.getDatabaseName());
            
            // 如果成功获取到了已生成表名列表，则添加过滤条件
            if (CollUtil.isNotEmpty(genTableNames)) {
                sqlBuilder.append("AND table_name NOT IN (");
                for (int i = 0; i < genTableNames.size(); i++) {
                    sqlBuilder.append("?");
                    if (i < genTableNames.size() - 1) {
                        sqlBuilder.append(",");
                    }
                    params.add(genTableNames.get(i));
                }
                sqlBuilder.append(") ");
            }
            
            // 添加条件查询
            if (ObjectUtil.isNotEmpty(genTable.getTableName())) {
                sqlBuilder.append("AND table_name LIKE ? ");
                params.add("%" + genTable.getTableName() + "%");
            }
            
            if (ObjectUtil.isNotEmpty(genTable.getTableComment())) {
                sqlBuilder.append("AND table_comment LIKE ? ");
                params.add("%" + genTable.getTableComment() + "%");
            }
            
            // 添加排序
            sqlBuilder.append("ORDER BY create_time DESC ");
            
            // 先查询总记录数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") t";
            Long total = (Long) Db.selectObject(countSql, params.toArray());
            
            // 添加分页条件
            sqlBuilder.append("LIMIT ?, ?");
            params.add((pageNum - 1) * pageSize);
            params.add(pageSize);
            
            // 执行查询并获取结果
            List<Row> rows = Db.selectListBySql(sqlBuilder.toString(), params.toArray());
            List<GenTable> list = RowUtil.toEntityList(rows, GenTable.class);

            // 手动构造 Page 对象
            Page<GenTable> page = new Page<>(pageNum, pageSize);
            page.setRecords(list);
            page.setTotalRow(total != null ? total : 0);
            
            return page;
        });
    }

    /**
     * 查询数据库列表
     * 
     * @param tableNames 表名称组
     * @return 数据库表集合
     */
    @Override
    public List<GenTable> selectDbTableListByNames(String[] tableNames) {
        // 使用参数化查询避免SQL注入
        String sql = "SELECT table_name, table_comment, create_time, update_time " +
                    "FROM information_schema.tables " +
                    "WHERE table_schema = (SELECT DATABASE()) " +
                    "AND table_name IN (" + String.join(",", Collections.nCopies(tableNames.length, "?")) + ")";
        
        // 使用Db执行SQL并获取结果
        List<Row> rows = Db.selectListBySql(sql, (Object[]) tableNames);
        
        // 使用自定义RowUtil直接转为GenTable实体列表
        return RowUtil.toEntityList(rows, GenTable.class);
    }

    /**
     * 根据指定数据源查询数据库列表
     * 
     * @param tableNames     表名称组
     * @param dataSourceName 数据源名称
     * @return 数据库表集合
     */
    @Override
    public List<GenTable> selectDbTableListByNamesAndDataSource(String[] tableNames, String dataSourceName) {
        SysDataSource sysDataSource = sysDataSourceService.selectSysDataSourceByName(dataSourceName);
        if (sysDataSource == null || StrUtil.isEmpty(sysDataSource.getDatabaseName())) {
            throw new ServiceException("数据源不存在或数据库名称未配置");
        }

        return executeWithDataSource(dataSourceName, () -> {
            // 使用参数化查询避免SQL注入
            String sql = "SELECT table_name, table_comment, create_time, update_time " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = ? " +
                        "AND table_name NOT LIKE 'qrtz\\_%' AND table_name NOT LIKE 'gen\\_%' " +
                        "AND table_name IN (" + String.join(",", Collections.nCopies(tableNames.length, "?")) + ")";
            
            // 构建参数列表
            List<Object> params = new ArrayList<>();
            params.add(sysDataSource.getDatabaseName());
            params.addAll(Arrays.asList(tableNames));
            
            // 执行查询并获取结果
            List<Row> rows = Db.selectListBySql(sql, params.toArray());
            return RowUtil.toEntityList(rows, GenTable.class);
        });
    }

    /**
     * 查询所有表信息
     * 
     * @return 表信息集合
     */
    @Override
    public List<GenTable> selectGenTableAll() {
        return this.list();
    }

    /**
     * 查询业务信息
     * 
     * @param id 业务ID
     * @return 业务信息
     */
    @Override
    public GenTable selectGenTableById(Long id) {
        return this.getById(id);
    }

    /**
     * 修改业务
     * 
     * @param genTable 业务信息
     * @return 结果
     */
    @Override
    @Transactional
    public void updateGenTable(GenTable genTable) {
        this.updateById(genTable);

        // 查询原有表字段
        List<GenTableColumn> originalColumns = genTableColumnService.selectGenTableColumnListByTableId(genTable.getTableId());
        Map<Long, GenTableColumn> originalColumnMap = originalColumns.stream()
                .collect(Collectors.toMap(GenTableColumn::getColumnId, Function.identity(), (v1, v2) -> v1));

        // 批量处理列更新
        List<GenTableColumn> toUpdate = new ArrayList<>();
        List<GenTableColumn> toInsert = new ArrayList<>();

        for (GenTableColumn column : genTable.getColumns()) {
            if (column.getColumnId() != null && originalColumnMap.containsKey(column.getColumnId())) {
                toUpdate.add(column);
            } else {
                column.setTableId(genTable.getTableId());
                toInsert.add(column);
            }
        }

        // 批量更新和插入
        if (!toUpdate.isEmpty()) {
            for (GenTableColumn column : toUpdate) {
                genTableColumnService.updateById(column);
            }
        }
        if (!toInsert.isEmpty()) {
            for (GenTableColumn column : toInsert) {
                genTableColumnService.insertGenTableColumn(column);
            }
        }
    }

    /**
     * 删除业务信息
     * 
     * @param tableIds 需要删除的表数据ID
     */
    @Override
    public void deleteGenTableByIds(Long[] tableIds) {
        // 删除表字段数据
        for (Long tableId : tableIds) {
            List<GenTableColumn> genTableColumns = genTableColumnService.selectGenTableColumnListByTableId(tableId);
            if (!genTableColumns.isEmpty()) {
                genTableColumnService.deleteGenTableColumns(genTableColumns);
            }
        }
        // 删除表数据
        this.removeByIds(Arrays.asList(tableIds));
    }

    /**
     * 创建表
     *
     * @param sql 创建表语句
     * @return 结果
     */
    @Override
    public boolean createTable(String sql) {
        try {
            // 检查SQL是否包含分隔标记
            if (sql.contains("@SQL_STATEMENT_START") && sql.contains("@SQL_STATEMENT_END")) {
                // 按照标记拆分SQL语句
                String[] sqlParts = sql.split("-- @SQL_STATEMENT_START");
                
                // 第一部分是空的或只包含注释，跳过
                for (int i = 1; i < sqlParts.length; i++) {
                    String part = sqlParts[i];
                    // 提取SQL语句（从开始到END标记之前）
                    int endIndex = part.indexOf("-- @SQL_STATEMENT_END");
                    if (endIndex > 0) {
                        String singleSql = part.substring(0, endIndex).trim();
                        if (!singleSql.isEmpty()) {
                            // 执行单条SQL语句
                            Db.updateBySql(singleSql);
                        }
                    }
                }
            } else {
                // 如果没有分隔标记，按原来的方式执行
                Db.updateBySql(sql);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 插入业务表信息
     *
     * @param genTable 业务表信息
     * @return 结果
     */
    @Override
    public void insertGenTable(GenTable genTable) {
        // 保存业务表信息
        this.save(genTable);

        // 保存列信息
        List<GenTableColumn> columns = genTable.getColumns();
        if (CollUtil.isNotEmpty(columns)) {
            for (GenTableColumn column : columns) {
                genTableColumnService.save(column);
            }
        }
    }

    /**
     * 导入表结构
     *
     * @param tableList 导入表列表
     * @param operName  操作人员
     */
    @Override
    public void importGenTable(List<GenTable> tableList, String operName) {
        try {
            for (GenTable table : tableList) {
                // 初始化表信息
                GenUtils.initTable(table, operName);
                // 保存表信息
                this.save(table);
                
                // 获取表对应的列信息
                List<GenTableColumn> columns = getTableColumns(table);
                
                // 批量保存列信息
                if (CollUtil.isNotEmpty(columns)) {
                    // 初始化列属性字段
                    columns.forEach(column -> GenUtils.initColumnField(column, table));
                    genTableColumnService.saveBatch(columns);
                }
            }
        } catch (Exception e) {
            throw new ServiceException("导入失败：" + e.getMessage());
        }
    }

    /**
     * 获取表的列信息
     * 
     * @param table 表信息
     * @return 列信息列表
     */
    private List<GenTableColumn> getTableColumns(GenTable table) {
        String dataSource = table.getDataSource();
        if (StrUtil.isNotEmpty(dataSource) && !StrUtil.equals(dataSource, "master")) {
            return executeWithDataSource(dataSource, () -> {
                SysDataSource sysDataSource = sysDataSourceService.selectSysDataSourceByName(dataSource);
                if (sysDataSource != null && StrUtil.isNotEmpty(sysDataSource.getDatabaseName())) {
                    return genTableColumnService.selectDbTableColumnsByNameAndDataSource(
                            table.getTableName(), sysDataSource.getDatabaseName());
                }
                return new ArrayList<>();
            });
        } else {
            return genTableColumnService.selectDbTableColumnsByName(table.getTableName());
        }
    }

    /**
     * 预览代码
     * 
     * @param tableId 表编号
     * @return 预览数据列表
     */
    @Override
    public Map<String, String> previewCode(Long tableId) {
        Map<String, String> dataMap = new LinkedHashMap<>();
        // 查询表信息
        GenTable table = this.getById(tableId);
        // 查询表的列信息
        List<GenTableColumn> columns = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        table.setColumns(columns);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);
        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory(), table.getTplWebType());
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            dataMap.put(template, sw.toString());
        }
        return dataMap;
    }

    /**
     * 生成代码（下载方式）
     * 
     * @param tableName 表名称
     * @return 数据
     */
    @Override
    public byte[] downloadCode(String tableName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        generatorCode(tableName, zip);
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    /**
     * 批量生成代码（下载方式）
     *
     * @param tableNames 表数组
     * @return 数据
     */
    @Override
    public byte[] downloadCode(String[] tableNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        for (String tableName : tableNames) {
            generatorCode(tableName, zip);
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    /**
     * 生成代码（自定义路径）
     * 
     * @param tableName 表名称
     */
    @Override
    public void generatorCode(String tableName) {
        // 查询表信息
        GenTable table = this.selectGenTableByName(tableName);
        // 查询列信息
        List<GenTableColumn> columns = genTableColumnService.selectGenTableColumnListByTableId(table.getTableId());
        table.setColumns(columns);

        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory(), table.getTplWebType());
        for (String template : templates) {
            if (!StrUtil.containsAny(template, "mapper.xml.vm", "api.js.vm", "index.vue.vm", "index-tree.vue.vm")) {
                // 渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, Constants.UTF8);
                tpl.merge(context, sw);
                try {
                    String path = getGenPath(table, template);
                    FileUtils.writeStringToFile(new File(path), sw.toString(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new ServiceException("渲染模板失败，表名：" + table.getTableName());
                }
            }
        }
    }

    /**
     * 同步数据库
     * 
     * 实现功能：
     * 1. 如果表不存在，则创建表
     * 2. 如果表存在，则同步字段变更：
     * - 新增字段：在表中添加不存在的字段
     * - 修改字段：更新字段的类型、是否必填、注释等
     * - 删除字段：删除表中已经不需要的字段
     * 
     * @param tableName 表名称
     */
    @Override
    public void synchDb(String tableName) {
        // 获取生成表信息
        QueryWrapper query = new QueryWrapper();
        query.from("gen_table").where(new QueryColumn("table_name").eq(tableName));
        GenTable table = this.getOne(query);
        if (table == null) {
            throw new ServiceException("同步数据失败，表信息不存在");
        }
        
        // 检查数据源，如果不是主数据源，则调用synchDbWithDataSource方法
        String dataSource = table.getDataSource();
        if (StrUtil.isNotEmpty(dataSource) && !StrUtil.equals(dataSource, "master")) {
            synchDbWithDataSource(tableName, dataSource);
            return;
        }

        // 查询表列信息 - 确保使用主数据源查询元数据表
        List<GenTableColumn> tableColumns;
        try {
            // 临时切换回主数据源查询gen_table_column表
            DynamicDataSourceContextHolder.setDataSourceType("master");
            tableColumns = genTableColumnService.selectGenTableColumnListByTableId(table.getTableId());
        } finally {
            // 清理数据源上下文
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
        
        if (CollUtil.isEmpty(tableColumns)) {
            throw new ServiceException("同步数据失败，表字段不存在");
        }

        // 确保主键标记设置正确
        for (GenTableColumn column : tableColumns) {
            if (column.isPk()) {
                // 确保主键字段的 isPk 属性值为 "1"
                column.setIsPk("1");
            }
        }

        // 设置列信息
        table.setColumns(tableColumns);

        // 判断数据库中是否存在该表
        List<GenTableColumn> dbTableColumns = genTableColumnService.selectDbTableColumnsByName(tableName);
        boolean tableExists = !CollUtil.isEmpty(dbTableColumns);

        // 表不存在，创建表
        if (!tableExists) {
            String createTableSql = generateCreateTableSql(table, tableColumns);
            try {
                // CREATE TABLE 语句执行成功后通常返回0，因此不检查返回行数
                this.createTable(createTableSql);
                // 如果没有抛出异常，则认为执行成功
                return;
            } catch (Exception e) {
                log.error("同步数据失败，创建表失败: " + tableName, e);
                throw new ServiceException("同步数据失败，创建表失败：" + e.getMessage());
            }
        }

        // 表存在，检查字段差异并生成修改表的SQL
        Map<String, GenTableColumn> dbColumnMap = dbTableColumns.stream()
                .collect(Collectors.toMap(GenTableColumn::getColumnName, Function.identity()));

        // 生成表中的字段列表
        Map<String, GenTableColumn> genColumnMap = tableColumns.stream()
                .collect(Collectors.toMap(GenTableColumn::getColumnName, Function.identity()));

        // 需要新增的字段
        List<GenTableColumn> addColumns = tableColumns.stream()
                .filter(column -> !dbColumnMap.containsKey(column.getColumnName()))
                .collect(Collectors.toList());

        // 需要修改的字段
        List<GenTableColumn> modifyColumns = tableColumns.stream()
                .filter(column -> dbColumnMap.containsKey(column.getColumnName())
                        && !isSameColumnType(column, dbColumnMap.get(column.getColumnName())))
                .collect(Collectors.toList());

        // 需要删除的字段
        List<GenTableColumn> dropColumns = dbTableColumns.stream()
                .filter(column -> !genColumnMap.containsKey(column.getColumnName()))
                .collect(Collectors.toList());

        // 生成并执行修改表结构的SQL
        if (CollUtil.isNotEmpty(addColumns) || CollUtil.isNotEmpty(modifyColumns) || CollUtil.isNotEmpty(dropColumns)) {
            List<String> alterTableSqlList = generateAlterTableSqlList(table.getTableName(), addColumns, modifyColumns,
                    dropColumns, tableColumns);
            try {
                // 分别执行每个 ALTER TABLE 语句
                for (String sql : alterTableSqlList) {
                    this.createTable(sql);
                }
                // 如果没有抛出异常，则认为执行成功
            } catch (Exception e) {
                throw new ServiceException("同步数据失败，修改表结构失败：" + e.getMessage());
            }
        }
    }
    
    /**
     * 根据指定数据源同步数据库
     * 
     * @param tableName 表名称
     * @param dataSourceName 数据源名称
     */
    @Override
    public void synchDbWithDataSource(String tableName, String dataSourceName) {
        // 获取生成表信息 - 确保使用主数据源查询元数据表
        GenTable table;
        SysDataSource sysDataSource;
        try {
            // 临时切换到主数据源查询元数据表
            DynamicDataSourceContextHolder.setDataSourceType("master");
            table = this.selectGenTableByName(tableName);
            if (table == null) {
                throw new ServiceException("同步数据失败，表信息不存在");
            }
            
            // 获取数据源信息
            sysDataSource = sysDataSourceService.selectSysDataSourceByName(dataSourceName);
            if (sysDataSource == null || StrUtil.isEmpty(sysDataSource.getDatabaseName())) {
                throw new ServiceException("数据源不存在或数据库名称未配置");
            }
        } finally {
            // 清理数据源上下文
            DynamicDataSourceContextHolder.clearDataSourceType();
        }

        // 查询表列信息 - 确保使用主数据源查询元数据表
        List<GenTableColumn> tableColumns;
        try {
            // 临时切换回主数据源查询gen_table_column表
            DynamicDataSourceContextHolder.setDataSourceType("master");
            tableColumns = genTableColumnService.selectGenTableColumnListByTableId(table.getTableId());
        } finally {
            // 清理数据源上下文
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
        
        if (CollUtil.isEmpty(tableColumns)) {
            throw new ServiceException("同步数据失败，表字段不存在");
        }

        // 确保主键标记设置正确
        for (GenTableColumn column : tableColumns) {
            if (column.isPk()) {
                // 确保主键字段的 isPk 属性值为 "1"
                column.setIsPk("1");
            }
        }

        // 设置列信息
        table.setColumns(tableColumns);

        try {
            // 切换数据源
            DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            
            // 判断数据库中是否存在该表
            List<GenTableColumn> dbTableColumns;
            try {
                // 尝试查询指定数据源中的表字段信息
                dbTableColumns = genTableColumnService.selectDbTableColumnsByNameAndDataSource(tableName, sysDataSource.getDatabaseName());
            } catch (Exception e) {
                // 表可能不存在，设置为空列表
                dbTableColumns = new ArrayList<>();
                log.warn("查询数据源 {} 中表 {} 的字段信息失败: {}", dataSourceName, tableName, e.getMessage());
            }
            
            boolean tableExists = !CollUtil.isEmpty(dbTableColumns);

            // 表不存在，创建表
            if (!tableExists) {
                String createTableSql = generateCreateTableSql(table, tableColumns);
                try {
                    // CREATE TABLE 语句执行成功后通常返回0，因此不检查返回行数
                    this.createTable(createTableSql);
                    // 如果没有抛出异常，则认为执行成功
                    return;
                } catch (Exception e) {
                    log.error("同步数据失败，创建表失败: " + tableName, e);
                    throw new ServiceException("同步数据失败，创建表失败：" + e.getMessage());
                }
            }

            // 表存在，检查字段差异并生成修改表的SQL
            Map<String, GenTableColumn> dbColumnMap = dbTableColumns.stream()
                    .collect(Collectors.toMap(GenTableColumn::getColumnName, Function.identity()));

            // 生成表中的字段列表
            Map<String, GenTableColumn> genColumnMap = tableColumns.stream()
                    .collect(Collectors.toMap(GenTableColumn::getColumnName, Function.identity()));

            // 需要新增的字段
            List<GenTableColumn> addColumns = tableColumns.stream()
                    .filter(column -> !dbColumnMap.containsKey(column.getColumnName()))
                    .collect(Collectors.toList());

            // 需要修改的字段
            List<GenTableColumn> modifyColumns = tableColumns.stream()
                    .filter(column -> dbColumnMap.containsKey(column.getColumnName())
                            && !isSameColumnType(column, dbColumnMap.get(column.getColumnName())))
                    .collect(Collectors.toList());

            // 需要删除的字段
            List<GenTableColumn> dropColumns = dbTableColumns.stream()
                    .filter(column -> !genColumnMap.containsKey(column.getColumnName()))
                    .collect(Collectors.toList());

            // 生成并执行修改表结构的SQL
            if (CollUtil.isNotEmpty(addColumns) || CollUtil.isNotEmpty(modifyColumns) || CollUtil.isNotEmpty(dropColumns)) {
                List<String> alterTableSqlList = generateAlterTableSqlList(table.getTableName(), addColumns, modifyColumns,
                        dropColumns, tableColumns);
                try {
                    // 分别执行每个 ALTER TABLE 语句
                    for (String sql : alterTableSqlList) {
                        this.createTable(sql);
                    }
                    // 如果没有抛出异常，则认为执行成功
                } catch (Exception e) {
                    throw new ServiceException("同步数据失败，修改表结构失败：" + e.getMessage());
                }
            }
        } finally {
            // 操作完成后清理数据源上下文
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    /**
     * 获取当前数据库名称
     * 
     * @return 当前数据库名称
     */
    @Override
    public String selectCurrentDatabase() {
        String sql = "SELECT DATABASE()";
        return (String) Db.selectObject(sql);
    }
    
    /**
     * 获取所有已生成的表名列表
     * 
     * @return 已生成表名列表
     */
    @Override
    public List<String> selectTableNameList() {
        String sql = "SELECT table_name FROM gen_table WHERE table_name IS NOT NULL";
        List<Row> rows = Db.selectListBySql(sql);
        return rows.stream().map(row -> row.getString("table_name")).collect(Collectors.toList());
    }
    
    /**
     * 检查表是否有主键
     *
     * @param tableName 表名
     * @return 如果有主键返回1，否则返回0
     */
    @Override
    public int checkTablePrimaryKey(String tableName) {
        String sql = "SELECT COUNT(1) FROM information_schema.table_constraints " +
            "WHERE table_schema = (select database()) " +
            "AND table_name = ? " +
            "AND constraint_type = 'PRIMARY KEY'";
        
        Long result = (Long) Db.selectObject(sql, tableName);
        return result != null ? result.intValue() : 0;
    }
    
    /**
     * 查询表的主键列名
     * 
     * @param tableName 表名
     * @return 主键列名列表
     */
    @Override
    public List<String> getTablePrimaryKeyColumns(String tableName) {
        String sql = "SELECT column_name FROM information_schema.key_column_usage " +
            "WHERE table_schema = (select database()) " +
            "AND table_name = ? " +
            "AND constraint_name = 'PRIMARY'";
        
        List<Row> rows = Db.selectListBySql(sql, tableName);
        return rows.stream().map(row -> row.getString("column_name")).collect(Collectors.toList());
    }
    
    /**
     * 查询表名称业务信息
     * 
     * @param tableName 表名称
     * @return 业务信息
     */
    @Override
    public GenTable selectGenTableByName(String tableName) {
        QueryWrapper query = QueryWrapper.create()
            .from("gen_table")
            .where(new QueryColumn("table_name").eq(tableName));
        return this.getOne(query);
    }

    /**
     * 修改保存参数校验
     * 
     * @param genTable 业务信息
     */
    @Override
    public void validateEdit(GenTable genTable) {
        if (GenConstants.TPL_TREE.equals(genTable.getTplCategory())) {
            String options = genTable.getOptions();
            if (StrUtil.isEmpty(options) || !options.contains(GenConstants.TREE_CODE)
                    || !options.contains(GenConstants.TREE_PARENT_CODE) || !options.contains(GenConstants.TREE_NAME)) {
                throw new ServiceException("树表代码生成缺少必要参数：树编码、树父编码、树名称字段");
            }
        } else if (GenConstants.TPL_SUB.equals(genTable.getTplCategory())) {
            if (StrUtil.isEmpty(genTable.getSubTableName())) {
                throw new ServiceException("关联子表的表名不能为空");
            } else if (StrUtil.isEmpty(genTable.getSubTableFkName())) {
                throw new ServiceException("子表关联的外键名不能为空");
            }
        }
    }
    
    /**
     * 设置主键列信息
     * 
     * @param table 业务表信息
     */
    private void setPkColumn(GenTable table) {
        for (GenTableColumn column : table.getColumns()) {
            if (column.isPk()) {
                table.setPkColumn(column);
                break;
            }
        }
        // 表没有主键时，默认使用第一个字段作为主键
        if (table.getPkColumn() == null) {
            table.setPkColumn(table.getColumns().get(0));
        }
    }

    /**
     * 设置子表信息
     * 
     * @param table 业务表信息
     */
    private void setSubTable(GenTable table) {
        String subTableName = table.getSubTableName();
        if (StrUtil.isNotEmpty(subTableName)) {
            GenTable subTable = this.selectGenTableByName(subTableName);
            if (subTable != null) {
                table.setSubTable(subTable);
                subTable.setColumns(genTableColumnService.selectGenTableColumnListByTableId(subTable.getTableId()));
            }
        }
    }
    
    /**
     * 获取代码生成地址
     * 
     * @param table    业务表信息
     * @param template 模板文件路径
     * @return 生成地址
     */
    private String getGenPath(GenTable table, String template) {
        String genPath = table.getGenPath();
        if (StrUtil.isEmpty(genPath)) {
            String basePath = System.getProperty("user.dir") + File.separator;
            // 根据模板类型返回不同的路径
            if (template.contains(".java.vm")) {
                return basePath + "src" + File.separator + "main" + File.separator + "java" + File.separator;
            } else if (template.contains(".xml.vm")) {
                // 不生成XML文件
                return null;
            } else if (template.contains(".vue.vm") || template.contains(".js.vm")) {
                return basePath + "vue" + File.separator;
            } else if (template.contains(".sql.vm")) {
                return basePath;
            } else {
                return basePath + "src" + File.separator;
            }
        }
        
        return genPath + File.separator;
    }
    
    /**
     * 生成代码（下载方式）
     * 
     * @param tableName 表名称
     * @param zip       zip输出流
     */
    private void generatorCode(String tableName, ZipOutputStream zip) {
        // 查询表信息
        GenTable table = this.selectGenTableByName(tableName);
        // 查询列信息
        List<GenTableColumn> columns = genTableColumnService.selectGenTableColumnListByTableId(table.getTableId());
        table.setColumns(columns);

        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory(), table.getTplWebType());
        
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            try {
                // 添加到zip
                if (zip != null) {
                    String fileName = VelocityUtils.getFileName(template, table);
                    // 如果文件名为null，表示不需要生成该文件（如SQL文件）
                    if (fileName != null) {
                        zip.putNextEntry(new ZipEntry(fileName));
                        IOUtils.write(sw.toString(), zip, Constants.UTF8);
                        zip.flush();
                        zip.closeEntry();
                    }
                }
            } catch (IOException e) {
                log.error("渲染模板失败，表名：" + table.getTableName(), e);
            }
        }
    }
    
    /**
     * 生成创建表的SQL
     *
     * @param genTable 表定义
     * @param columns  表字段定义
     * @return 创建表的SQL语句
     */
    private String generateCreateTableSql(GenTable genTable, List<GenTableColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE `").append(genTable.getTableName()).append("` (\n");

        // 处理列定义
        for (int i = 0; i < columns.size(); i++) {
            GenTableColumn column = columns.get(i);
            sb.append("  `").append(column.getColumnName()).append("` ");
            sb.append(column.getColumnType());
            
            // 是否允许为NULL
            if ("1".equals(column.getIsRequired())) {
                sb.append(" NOT NULL");
            } else {
                sb.append(" NULL");
            }
            
            // 默认值
            if (StrUtil.isNotEmpty(column.getColumnDefault()) && !"undefined".equalsIgnoreCase(column.getColumnDefault())) {
                sb.append(" DEFAULT ").append(column.getColumnDefault());
            }
            
            // 注释
            if (StrUtil.isNotEmpty(column.getColumnComment())) {
                sb.append(" COMMENT '").append(column.getColumnComment().replace("'", "\\'")).append("'");
            }
            
            // 逗号分隔列
            if (i < columns.size() - 1) {
                sb.append(",\n");
            }
        }

        // 处理主键定义
        List<GenTableColumn> pkColumns = columns.stream().filter(GenTableColumn::isPk).collect(Collectors.toList());
        if (!pkColumns.isEmpty()) {
            sb.append(",\n  PRIMARY KEY (");
            for (int i = 0; i < pkColumns.size(); i++) {
                sb.append("`").append(pkColumns.get(i).getColumnName()).append("`");
                if (i < pkColumns.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")\n");
        } else {
            sb.append("\n");
        }
        
        sb.append(")");
        
        // 表注释
        if (StrUtil.isNotEmpty(genTable.getTableComment())) {
            sb.append(" COMMENT='").append(genTable.getTableComment().replace("'", "\\'")).append("'");
        }
        
        sb.append(";");
        return sb.toString();
    }
    
    /**
     * 生成修改表结构的SQL列表
     * 
     * @param tableName     表名
     * @param addColumns    需要添加的列
     * @param modifyColumns 需要修改的列
     * @param dropColumns   需要删除的列
     * @param tableColumns  所有列信息
     * @return SQL语句列表
     */
    private List<String> generateAlterTableSqlList(String tableName, List<GenTableColumn> addColumns,
                                                 List<GenTableColumn> modifyColumns, List<GenTableColumn> dropColumns,
                                                 List<GenTableColumn> tableColumns) {
        List<String> sqlList = new ArrayList<>();
        
        // 如果有添加和修改操作，先生成这部分的SQL
        List<String> alterAddModifySql = new ArrayList<>();
        
        // 添加列
        for (GenTableColumn column : addColumns) {
            StringBuilder alterSql = new StringBuilder();
            alterSql.append("ALTER TABLE `").append(tableName).append("` ADD COLUMN `")
                   .append(column.getColumnName()).append("` ").append(column.getColumnType());
            
            // 是否允许为NULL
            if ("1".equals(column.getIsRequired())) {
                alterSql.append(" NOT NULL");
            } else {
                alterSql.append(" NULL");
            }
            
            // 默认值
            if (StrUtil.isNotEmpty(column.getColumnDefault()) && !"undefined".equalsIgnoreCase(column.getColumnDefault())) {
                alterSql.append(" DEFAULT ").append(column.getColumnDefault());
            }
            
            // 列注释
            if (StrUtil.isNotEmpty(column.getColumnComment())) {
                alterSql.append(" COMMENT '").append(column.getColumnComment().replace("'", "\\'")).append("'");
            }
            
            alterAddModifySql.add(alterSql.toString());
        }
        
        // 修改列
        for (GenTableColumn column : modifyColumns) {
            StringBuilder alterSql = new StringBuilder();
            alterSql.append("ALTER TABLE `").append(tableName).append("` MODIFY COLUMN `")
                   .append(column.getColumnName()).append("` ").append(column.getColumnType());
            
            // 是否允许为NULL
            if ("1".equals(column.getIsRequired())) {
                alterSql.append(" NOT NULL");
            } else {
                alterSql.append(" NULL");
            }
            
            // 默认值
            if (StrUtil.isNotEmpty(column.getColumnDefault()) && !"undefined".equalsIgnoreCase(column.getColumnDefault())) {
                alterSql.append(" DEFAULT ").append(column.getColumnDefault());
            }
            
            // 列注释
            if (StrUtil.isNotEmpty(column.getColumnComment())) {
                alterSql.append(" COMMENT '").append(column.getColumnComment().replace("'", "\\'")).append("'");
            }
            
            alterAddModifySql.add(alterSql.toString());
        }
        
        // 如果有添加或修改的操作，生成一条ALTER语句包含所有的ADD和MODIFY操作
        if (!alterAddModifySql.isEmpty()) {
            sqlList.add(String.join("; ", alterAddModifySql) + ";");
        }
        
        // 删除列，每个DROP单独一条SQL语句
        for (GenTableColumn column : dropColumns) {
            String dropSql = "ALTER TABLE `" + tableName + "` DROP COLUMN `" + column.getColumnName() + "`;";
            sqlList.add(dropSql);
        }
        
        // 重新处理主键
        List<GenTableColumn> pkColumns = tableColumns.stream().filter(GenTableColumn::isPk).collect(Collectors.toList());
        if (!pkColumns.isEmpty()) {
            // 先删除原主键，再添加新主键
            String dropPkSql = "ALTER TABLE `" + tableName + "` DROP PRIMARY KEY";
            String addPkSql = "ALTER TABLE `" + tableName + "` ADD PRIMARY KEY (";
            
            for (int i = 0; i < pkColumns.size(); i++) {
                addPkSql += "`" + pkColumns.get(i).getColumnName() + "`";
                if (i < pkColumns.size() - 1) {
                    addPkSql += ", ";
                }
            }
            addPkSql += ")";
            
            // 添加到SQL列表
            sqlList.add(dropPkSql + ";");
            sqlList.add(addPkSql + ";");
        }
        
        return sqlList;
    }
    
    /**
     * 比较两个字段是否类型相同
     * 
     * @param genColumn 代码生成表字段
     * @param dbColumn  数据库表字段
     * @return 是否相同，true相同，false不同
     */
    private boolean isSameColumnType(GenTableColumn genColumn, GenTableColumn dbColumn) {
        if (genColumn == null || dbColumn == null) {
            return false;
        }
        
        // 比较字段类型
        if (!genColumn.getColumnType().equalsIgnoreCase(dbColumn.getColumnType())) {
            return false;
        }
        
        // 比较是否必填
        if (!StrUtil.equals(genColumn.getIsRequired(), dbColumn.getIsRequired())) {
            return false;
        }
        
        // 比较默认值
        if (!StrUtil.equals(genColumn.getColumnDefault(), dbColumn.getColumnDefault())) {
            return false;
        }
        
        // 比较注释
        if (!StrUtil.equals(genColumn.getColumnComment(), dbColumn.getColumnComment())) {
            return false;
        }
        
        return true;
    }

    /**
     * 在指定数据源上执行操作
     * 
     * @param dataSourceName 数据源名称
     * @param action 要执行的操作
     * @return 操作结果
     */
    private <T> T executeWithDataSource(String dataSourceName, Supplier<T> action) {
        try {
            DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
            return action.get();
        } finally {
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }
}
