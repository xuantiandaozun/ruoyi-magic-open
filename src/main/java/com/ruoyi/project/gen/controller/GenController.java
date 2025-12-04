package com.ruoyi.project.gen.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;
import com.ruoyi.project.gen.domain.request.BatchGenCodeRequest;
import com.ruoyi.project.gen.domain.vo.AiCreateTableResponse;
import com.ruoyi.project.gen.domain.vo.CreateImportTableRequest;
import com.ruoyi.project.gen.service.IAsyncTaskService;
import com.ruoyi.project.gen.service.IGenTableColumnService;
import com.ruoyi.project.gen.service.IGenTableService;
import com.ruoyi.project.gen.tools.request.BatchUpdateGenTableRequest;
import com.ruoyi.project.gen.util.GenUtils;
import com.ruoyi.project.gen.util.VelocityUtils;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.service.ISysDataSourceService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码生成 操作处理
 *
 * @author ruoyi
 */
@Tag(name = "代码生成")
@RestController
@Slf4j
@RequestMapping("/tool/gen")
public class GenController extends BaseController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IGenTableService genTableService;

    @Autowired
    private IGenTableColumnService genTableColumnService;



    @Autowired
    private ISysDataSourceService sysDataSourceService;


    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired
    private IAsyncTaskService asyncTaskService;

    /**
     * 查询代码生成列表
     */
    @Operation(summary = "查询代码生成列表")
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/list")
    public TableDataInfo genList(GenTable genTable) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(genTable);

        // 使用 MyBatisFlex 的分页方法
        Page<GenTable> page = genTableService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 查询代码生成业务
     */
    @Operation(summary = "查询代码生成业务")
    @SaCheckPermission("tool:gen:query")
    @GetMapping(value = "/{tableId}")
    public AjaxResult getInfo(@PathVariable Long tableId) {
        GenTable table = genTableService.selectGenTableById(tableId);
        List<GenTable> tables = genTableService.selectGenTableAll();
        List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("info", table);
        map.put("rows", list);
        map.put("tables", tables);
        return success(map);
    }

    /**
     * 查询数据库列表
     */
    @Operation(summary = "查询数据库列表")
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/db/list")
    public TableDataInfo dataList(GenTable genTable) {
        Page<GenTable> list = genTableService.selectDbTableList(genTable);
        return getDataTable(list);
    }

    /**
     * 根据指定数据源查询数据库列表
     */
    @Operation(summary = "根据指定数据源查询数据库列表")
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/db/list/dataSource/{dataSourceName}")
    public TableDataInfo dataListByDataSource(GenTable genTable, @PathVariable String dataSourceName) {
        Page<GenTable> list;
        if ("master".equalsIgnoreCase(dataSourceName)) {
            list = genTableService.selectDbTableList(genTable);
        } else {
            list = genTableService.selectDbTableListByDataSource(genTable, dataSourceName);
        }
        return getDataTable(list);
    }

    /**
     * 查询数据表字段列表
     */
    @Operation(summary = "查询数据表字段列表")
    @SaCheckPermission("tool:gen:list")
    @GetMapping(value = "/column/{tableId}")
    public TableDataInfo columnList(Long tableId) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where("table_id", tableId)
                .orderBy("sort asc");

        // 使用 MyBatisFlex 的分页方法
        Page<GenTableColumn> page = genTableColumnService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 修改单个表字段
     */
    @Operation(summary = "修改单个表字段")
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @PutMapping("/column")
    public AjaxResult editColumn(@Validated @RequestBody GenTableColumn genTableColumn) {
        genTableColumnService.updateGenTableColumn(genTableColumn);
        return success();
    }

    /**
     * 导入表结构（保存）
     */
    @Operation(summary = "导入表结构（保存）")
    @SaCheckPermission("tool:gen:import")
    @Log(title = "代码生成", businessType = BusinessType.IMPORT)
    @PostMapping("/importTable")
    public AjaxResult importTableSave(String tables) {
        String[] tableNames = StrUtil.splitToArray(tables, ',');
        // 查询表信息
        List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames);

        // 设置主数据源标记
        tableList.forEach(table -> table.setDataSource("MASTER"));
        genTableService.importGenTable(tableList, SecurityUtils.getUsername());
        return success();
    }

    /**
     * 根据指定数据源导入表结构（保存）
     */
    @Operation(summary = "根据指定数据源导入表结构（保存）")
    @SaCheckPermission("tool:gen:import")
    @Log(title = "代码生成", businessType = BusinessType.IMPORT)
    @PostMapping("/importTable/dataSource/{dataSourceName}")
    public AjaxResult importTableSaveFromDataSource(String tables, @PathVariable String dataSourceName) {
        String[] tableNames = StrUtil.splitToArray(tables, ',');
        List<GenTable> tableList;
        // 统一数据源名称格式，主数据源使用MASTER
        String normalizedDataSourceName = "master".equalsIgnoreCase(dataSourceName) ? "MASTER" : dataSourceName;

        if ("master".equalsIgnoreCase(dataSourceName)) {
            // 查询表信息
            tableList = genTableService.selectDbTableListByNames(tableNames);
        } else {
            // 查询表信息
            tableList = genTableService.selectDbTableListByNamesAndDataSource(tableNames, dataSourceName);
        }
        // 设置数据源标记，使用标准化的数据源名称
        tableList.forEach(table -> table.setDataSource(normalizedDataSourceName));
        genTableService.importGenTable(tableList, SecurityUtils.getUsername());
        return success();
    }

    /**
     * 修改保存代码生成业务
     */
    @Operation(summary = "修改保存代码生成业务")
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult editSave(@Validated @RequestBody GenTable genTable) {
        genTableService.validateEdit(genTable);
        genTableService.updateGenTable(genTable);
        return success();
    }

    /**
     * 批量修改代码生成业务（排除生成业务名和生成功能名）
     */
    @Operation(summary = "批量修改代码生成业务")
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @PutMapping("/batchUpdate")
    public AjaxResult batchUpdate(@Validated @RequestBody BatchUpdateGenTableRequest request) {
        genTableService.batchUpdateGenTableExcludeBusinessAndFunction(request);
        return success();
    }

    /**
     * 删除代码生成
     */
    @Operation(summary = "删除代码生成")
    @SaCheckPermission("tool:gen:remove")
    @Log(title = "代码生成", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tableIds}")
    public AjaxResult remove(@PathVariable Long[] tableIds) {
        genTableService.deleteGenTableByIds(tableIds);
        return success();
    }

    /**
     * 预览代码
     */
    @Operation(summary = "预览代码")
    @SaCheckPermission("tool:gen:preview")
    @GetMapping("/preview/{tableId}")
    public AjaxResult preview(@PathVariable("tableId") Long tableId) throws IOException {
        Map<String, String> dataMap = genTableService.previewCode(tableId);
        return success(dataMap);
    }

    /**
     * 生成代码（下载方式）
     */
    @Operation(summary = "生成代码（下载方式）")
    @SaCheckPermission("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/download/{tableName}")
    public void download(HttpServletResponse response, @PathVariable("tableName") String tableName) throws IOException {
        byte[] data = genTableService.downloadCode(tableName);
        String fileName = tableName + "_code.zip";
        genCode(response, data, fileName);
    }

    /**
     * 批量生成代码（下载方式）- 支持tableId数组和生成类型
     */
    @Operation(summary = "批量生成代码（下载方式）- 支持tableId数组和生成类型")
    @SaCheckPermission("tool:gen:code")
    @Log(title = "批量代码生成", businessType = BusinessType.GENCODE)
    @PostMapping("/batchDownload")
    public void batchDownload(HttpServletResponse response, @Validated @RequestBody BatchGenCodeRequest request) throws IOException {
        byte[] data = genTableService.downloadCodeByIds(request.getTableIds(), request.getGenType());
        
        // 动态生成文件名
        String fileName = generateZipFileName(request.getTableIds());
        genCode(response, data, fileName);
    }

    /**
     * 生成代码（自定义路径）
     */
    @Operation(summary = "生成代码（自定义路径）")
    @SaCheckPermission("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/genCode/{tableName}")
    public AjaxResult genCode(@PathVariable("tableName") String tableName) {
        genTableService.generatorCode(tableName);
        return success();
    }

    /**
     * 批量生成代码（自定义路径）- 支持tableId数组和生成类型
     */
    @Operation(summary = "批量生成代码（自定义路径）- 支持tableId数组和生成类型")
    @SaCheckPermission("tool:gen:code")
    @Log(title = "批量代码生成", businessType = BusinessType.GENCODE)
    @PostMapping("/batchGenCode")
    public AjaxResult batchGenCode(@Validated @RequestBody BatchGenCodeRequest request) {
        genTableService.generatorCodeByIds(request.getTableIds(), request.getGenType());
        return success();
    }

    /**
     * 同步数据库
     * 将当前表结构信息同步到数据库中，如果表不存在则创建，如果表存在则更新结构
     * 自动根据表的数据源配置选择同步方法
     */
    @Operation(summary = "同步数据库")
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @GetMapping("/synchDb/{tableName}")
    public AjaxResult synchDb(@PathVariable("tableName") String tableName) {
        genTableService.synchDb(tableName);
        return success();
    }

    /**
     * 从数据库同步指定的表到GenTable和GenTableColumn
     * 根据指定的表名从数据库中获取表结构信息，并同步到代码生成的元数据表中
     */
    @Operation(summary = "从数据库同步指定表")
    @SaCheckPermission("tool:gen:import")
    @Log(title = "代码生成", businessType = BusinessType.IMPORT)
    @PostMapping("/syncTableFromDb")
    public AjaxResult syncTableFromDb(@RequestBody Map<String, Object> request) {
        String tableName = (String) request.get("tableName");
        String dataSourceName = (String) request.get("dataSourceName");
        
        if (StrUtil.isBlank(tableName)) {
            return error("表名不能为空");
        }
        
        try {
            // 验证表名格式，防止SQL注入
            if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
                return error("表名格式不正确，只允许字母、数字和下划线");
            }
            
            // 检查表是否已经存在于GenTable中
             GenTable existingTable = genTableService.selectGenTableByName(tableName);
             if (existingTable != null) {  
                 // 重新导入表字段信息到GenTableColumn，以同步新增的字段
                 List<GenTable> tableList;
                 if (StrUtil.isBlank(dataSourceName) || "master".equalsIgnoreCase(dataSourceName)) {
                     // 从主数据源获取最新的表字段信息
                     tableList = genTableService.selectDbTableListByNames(new String[]{tableName});
                 } else {
                     // 从指定数据源获取最新的表字段信息
                     tableList = genTableService.selectDbTableListByNamesAndDataSource(new String[]{tableName}, dataSourceName);
                 }
                 
                 if (!tableList.isEmpty()) {
                     GenTable latestTableInfo = tableList.get(0);
                     // 保留原有的表配置信息，只更新字段信息
                     latestTableInfo.setTableId(existingTable.getTableId());
                     latestTableInfo.setClassName(existingTable.getClassName());
                     latestTableInfo.setTplCategory(existingTable.getTplCategory());
                     latestTableInfo.setPackageName(existingTable.getPackageName());
                     latestTableInfo.setModuleName(existingTable.getModuleName());
                     latestTableInfo.setBusinessName(existingTable.getBusinessName());
                     latestTableInfo.setFunctionName(existingTable.getFunctionName());
                     latestTableInfo.setFunctionAuthor(existingTable.getFunctionAuthor());
                     latestTableInfo.setGenType(existingTable.getGenType());
                     latestTableInfo.setGenPath(existingTable.getGenPath());
                     latestTableInfo.setVuePath(existingTable.getVuePath());
                     latestTableInfo.setOptions(existingTable.getOptions());
                     latestTableInfo.setRemark(existingTable.getRemark());
                     
                     // 先获取旧的字段配置信息
                     List<GenTableColumn> oldColumns = genTableColumnService.selectGenTableColumnListByTableId(existingTable.getTableId());
                     Map<String, GenTableColumn> oldColumnMap = new HashMap<>();
                     if (!oldColumns.isEmpty()) {
                         oldColumns.forEach(col -> oldColumnMap.put(col.getColumnName(), col));
                     }
                     
                     // 从数据库获取最新的字段信息
                     List<GenTableColumn> newColumns;
                     if (StrUtil.isNotEmpty(latestTableInfo.getDataSource()) && !StrUtil.equals(latestTableInfo.getDataSource(), "MASTER")) {
                         // 从指定数据源获取字段信息
                         newColumns = genTableColumnService.selectDbTableColumnsByNameAndDataSource(tableName, latestTableInfo.getDataSource());
                     } else {
                         // 从主数据源获取字段信息
                         newColumns = genTableColumnService.selectDbTableColumnsByName(tableName);
                     }
                     
                     // 处理新字段：根据columnName匹配旧字段配置
                     if (!newColumns.isEmpty()) {
                         newColumns.forEach(column -> {
                             column.setTableId(existingTable.getTableId());
                             GenTableColumn oldColumn = oldColumnMap.get(column.getColumnName());
                             if (oldColumn != null) {
                                 // 有相同columnName的，把旧的设置赋值到新的上，但不设置ID
                                 column.setJavaField(oldColumn.getJavaField());
                                 column.setJavaType(oldColumn.getJavaType());
                                 column.setQueryType(oldColumn.getQueryType());
                                 column.setHtmlType(oldColumn.getHtmlType());
                                 column.setDictType(oldColumn.getDictType());
                                 column.setIsInsert(oldColumn.getIsInsert());
                                 column.setIsEdit(oldColumn.getIsEdit());
                                 column.setIsList(oldColumn.getIsList());
                                 column.setIsQuery(oldColumn.getIsQuery());
                                 column.setIsRequired(oldColumn.getIsRequired());
                                 column.setSort(oldColumn.getSort());
                                 column.setCreateBy(oldColumn.getCreateBy());
                                 column.setUpdateBy(oldColumn.getUpdateBy());
                             } else {
                                 // 没有相同的就初始化
                                 GenUtils.initColumnField(column, existingTable);
                             }
                         });
                         
                         // 删除旧的字段
                         if (!oldColumns.isEmpty()) {
                             genTableColumnService.deleteGenTableColumns(oldColumns);
                         }
                         
                         // 保存新的字段
                         genTableColumnService.saveBatch(newColumns);
                     }
                 }
                 
                 return success("表 '" + tableName + "' 已存在，已更新其表结构和字段信息");
             }
            
            List<GenTable> tableList;
            String normalizedDataSourceName;
            
            // 根据数据源获取表信息
            if (StrUtil.isBlank(dataSourceName) || "master".equalsIgnoreCase(dataSourceName)) {
                // 从主数据源获取表信息
                tableList = genTableService.selectDbTableListByNames(new String[]{tableName});
                normalizedDataSourceName = "MASTER";
            } else {
                // 从指定数据源获取表信息
                tableList = genTableService.selectDbTableListByNamesAndDataSource(new String[]{tableName}, dataSourceName);
                normalizedDataSourceName = dataSourceName;
            }
            
            if (tableList.isEmpty()) {
                return error("在" + (StrUtil.isBlank(dataSourceName) ? "主数据源" : "数据源 '" + dataSourceName + "'") + "中未找到表 '" + tableName + "'");
            }
            
            // 设置数据源标记
            tableList.forEach(table -> table.setDataSource(normalizedDataSourceName));
            
            // 导入表结构到GenTable和GenTableColumn
            genTableService.importGenTable(tableList, SecurityUtils.getUsername());
            
            return success("成功从数据库同步表 '" + tableName + "' 到代码生成配置中");
            
        } catch (Exception e) {
            logger.error("从数据库同步表失败: tableName={}, dataSourceName={}", tableName, dataSourceName, e);
            return error("同步表失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成代码
     */
    @Operation(summary = "批量生成代码")
    @SaCheckPermission("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/batchGenCode")
    public void batchGenCode(HttpServletResponse response, String tables) throws IOException {
        String[] tableNames = StrUtil.splitToArray(tables, ',');
        byte[] data = genTableService.downloadCode(tableNames);
        String fileName = "batch_code.zip";
        genCode(response, data, fileName);
    }

    /**
     * 创建数据库表
     */
    @Operation(summary = "创建数据库表")
    @SaCheckPermission("tool:gen:create")
    @Log(title = "代码生成", businessType = BusinessType.INSERT)
    @PostMapping("/createTable")
    public AjaxResult createTable(@Validated @RequestBody String sql) {
        genTableService.createTable(sql);
        return success();
    }

    /**
     * 生成zip文件
     */
    private void genCode(HttpServletResponse response, byte[] data, String fileName) throws IOException {
        response.reset();
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
        
        // 支持中文文件名，使用RFC 5987标准编码
        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8");
        response.setHeader("Content-Disposition", 
            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);
        
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IoUtil.write(response.getOutputStream(), true, data);
    }

    /**
     * 根据表ID生成动态zip文件名
     */
    private String generateZipFileName(List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return "ruoyi_code.zip";
        }
        
        if (tableIds.size() == 1) {
            // 单个表，使用表名
            GenTable table = genTableService.getById(tableIds.get(0));
            if (table != null && StrUtil.isNotEmpty(table.getTableName())) {
                return table.getTableName() + "_code.zip";
            }
        } else {
            // 多个表，使用表名组合
            List<String> tableNames = new ArrayList<>();
            for (Long tableId : tableIds) {
                GenTable table = genTableService.getById(tableId);
                if (table != null && StrUtil.isNotEmpty(table.getTableName())) {
                    tableNames.add(table.getTableName());
                }
            }
            
            if (!tableNames.isEmpty()) {
                if (tableNames.size() <= 3) {
                    // 3个或更少的表，组合表名
                    return String.join("_", tableNames) + "_code.zip";
                } else {
                    // 超过3个表，使用数量标识
                    return "batch_" + tableNames.size() + "_tables_code.zip";
                }
            }
        }
        
        return "ruoyi_code.zip";
    }

    /**
     * 创建导入表
     */
    @Operation(summary = "导入建表")
    @SaCheckPermission("tool:gen:create")
    @Log(title = "导入建表", businessType = BusinessType.INSERT)
    @PostMapping("/createImportTable")
    public AjaxResult createImportTable(@Validated @RequestBody CreateImportTableRequest request) {
        try {
            // 获取表信息和列信息
            GenTable table = request.getInfo();
            List<GenTableColumn> columns = request.getRows();

            // 检查表ID是否存在，不存在则生成新ID
            if (table.getTableId() == null) {
                table.setTableId(IdUtil.getSnowflakeNextId());
            }

            // 检查数据源是否设置，未设置则默认为MASTER
            if (StrUtil.isBlank(table.getDataSource())) {
                table.setDataSource("MASTER");
            }

            // 为每个列设置表ID，检查列ID是否存在，不存在则生成新ID
            if (columns != null) {
                for (GenTableColumn column : columns) {
                    column.setTableId(table.getTableId());
                    if (column.getColumnId() == null) {
                        column.setColumnId(IdUtil.getSnowflakeNextId());
                    }
                }
            }

            // 设置表的列
            table.setColumns(columns);

            // 保存表信息
            genTableService.insertGenTable(table);

            // 构造返回结果
            AiCreateTableResponse result = new AiCreateTableResponse();
            result.setInfo(table);
            result.setRows(columns);

            return success(result);
        } catch (Exception e) {
            logger.error("导入建表过程中发生错误", e);
            throw new ServiceException("导入建表失败：" + e.getMessage());
        }
    }

    /**
     * 执行权限SQL
     */
    @Operation(summary = "执行权限SQL")
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "执行权限SQL", businessType = BusinessType.UPDATE)
    @PostMapping("/execPermissionSql/{tableId}")
    public AjaxResult execPermissionSql(@PathVariable("tableId") Long tableId) {
        // 查询表信息
        GenTable table = genTableService.getById(tableId);
        if (table == null) {
            return error("表信息不存在");
        }

        // 使用预览代码方法获取SQL，该方法内部会设置主子表信息和主键列信息
        Map<String, String> previewMap = genTableService.previewCode(tableId);
        String sql = previewMap.get("vm/sql/sql.vm");
        if (StrUtil.isEmpty(sql)) {
            return error("生成权限SQL失败");
        }

        // 检查菜单是否已存在
        String functionName = table.getFunctionName();
        String permissionPrefix = VelocityUtils.getPermissionPrefix(table.getModuleName(), table.getBusinessName());

        // 将 QueryWrapper 转换为 SQL 和参数
        String countSql = "SELECT COUNT(*) FROM sys_menu WHERE menu_name = ? AND perms = ?";
        Long count = Db.selectCount(countSql, functionName, permissionPrefix + ":list");
        if (count > 0) {
            return error("菜单'" + functionName + "'已存在，无需重复执行权限SQL");
        }

        // 执行SQL
        boolean result = genTableService.createTable(sql);
        if (!result) {
            return error("执行权限SQL失败");
        }

        return success("执行权限SQL成功");
    }

    /**
     * 获取可用的数据源列表
     */
    @Operation(summary = "获取可用的数据源列表")
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/dataSources")
    public AjaxResult getDataSources() {
        List<SysDataSource> dataSources = sysDataSourceService.selectSysDataSourceList(new SysDataSource());

        List<Map<String, Object>> dataSourceList = new ArrayList<>();

        // 添加默认主数据源
        Map<String, Object> mainDataSource = new HashMap<>();
        mainDataSource.put("name", "MASTER");
        mainDataSource.put("databaseName", "主数据源");
        mainDataSource.put("description", "默认主数据源");
        dataSourceList.add(mainDataSource);

        // 添加其他数据源
        for (SysDataSource dataSource : dataSources) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", dataSource.getName());
            map.put("databaseName", dataSource.getDatabaseName());
            map.put("description", dataSource.getDescription());
            dataSourceList.add(map);
        }

        return success(dataSourceList);
    }

    /**
     * 获取目录列表
     */
    @Operation(summary = "获取目录列表")
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/directories")
    public AjaxResult getDirectories(String path) {
        try {
            // 如果没有指定路径，使用当前工作目录
            if (StrUtil.isEmpty(path)) {
                path = System.getProperty("user.dir");
            }

            File directory = new File(path);

            // 检查路径是否存在且为目录
            if (!directory.exists()) {
                return error("路径不存在: " + path);
            }

            if (!directory.isDirectory()) {
                return error("不是有效的目录: " + path);
            }

            List<Map<String, Object>> result = new ArrayList<>();

            // 添加上级目录（如果不是根目录）
            File parentDir = directory.getParentFile();
            if (parentDir != null) {
                Map<String, Object> parentInfo = new HashMap<>();
                parentInfo.put("name", "..");
                parentInfo.put("path", parentDir.getAbsolutePath());
                parentInfo.put("type", "parent");
                parentInfo.put("isDirectory", true);
                result.add(parentInfo);
            }

            // 获取当前目录下的所有文件和文件夹
            File[] files = directory.listFiles();
            if (files != null) {
                // 先添加目录，再添加文件
                Arrays.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) {
                        return -1;
                    } else if (!f1.isDirectory() && f2.isDirectory()) {
                        return 1;
                    } else {
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    }
                });

                for (File file : files) {
                    // 跳过隐藏文件和系统文件
                    if (file.isHidden() || file.getName().startsWith(".")) {
                        continue;
                    }

                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileInfo.put("isDirectory", file.isDirectory());
                    fileInfo.put("type", file.isDirectory() ? "directory" : "file");

                    if (file.isDirectory()) {
                        fileInfo.put("canRead", file.canRead());
                        fileInfo.put("canWrite", file.canWrite());
                    }

                    result.add(fileInfo);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("currentPath", directory.getAbsolutePath());
            response.put("directories", result);

            return success(response);

        } catch (Exception e) {
            logger.error("获取目录列表失败", e);
            return error("获取目录列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取常用路径
     */
    @Operation(summary = "获取常用路径")
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/commonPaths")
    public AjaxResult getCommonPaths() {
        try {
            List<Map<String, Object>> paths = new ArrayList<>();

            // 当前工作目录
            String currentDir = System.getProperty("user.dir");
            Map<String, Object> current = new HashMap<>();
            current.put("name", "当前项目");
            current.put("path", currentDir);
            current.put("description", "当前运行项目的根目录");
            paths.add(current);

            // 当前项目的src目录
            File srcDir = new File(currentDir, "src");
            if (srcDir.exists() && srcDir.isDirectory()) {
                Map<String, Object> src = new HashMap<>();
                src.put("name", "当前项目源码");
                src.put("path", srcDir.getAbsolutePath());
                src.put("description", "当前项目的源码目录");
                paths.add(src);
            }

            // 用户主目录
            String userHome = System.getProperty("user.home");
            Map<String, Object> home = new HashMap<>();
            home.put("name", "用户主目录");
            home.put("path", userHome);
            home.put("description", "当前用户的主目录");
            paths.add(home);

            // 桌面目录
            File desktop = new File(userHome, "Desktop");
            if (desktop.exists() && desktop.isDirectory()) {
                Map<String, Object> desktopPath = new HashMap<>();
                desktopPath.put("name", "桌面");
                desktopPath.put("path", desktop.getAbsolutePath());
                desktopPath.put("description", "用户桌面目录");
                paths.add(desktopPath);
            }

            // 文档目录
            File documents = new File(userHome, "Documents");
            if (documents.exists() && documents.isDirectory()) {
                Map<String, Object> documentsPath = new HashMap<>();
                documentsPath.put("name", "文档");
                documentsPath.put("path", documents.getAbsolutePath());
                documentsPath.put("description", "用户文档目录");
                paths.add(documentsPath);
            }

            return success(paths);

        } catch (Exception e) {
            logger.error("获取常用路径失败", e);
            return error("获取常用路径失败: " + e.getMessage());
        }
    }

    /**
     * 验证路径是否有效
     */
    @Operation(summary = "验证路径是否有效")
    @SaCheckPermission("tool:gen:list")
    @PostMapping("/validatePath")
    public AjaxResult validatePath(@RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");
            if (StrUtil.isEmpty(path)) {
                return error("路径不能为空");
            }

            File dir = new File(path);
            if (!dir.exists()) {
                return error("路径不存在");
            }

            if (!dir.isDirectory()) {
                return error("不是有效的目录");
            }

            if (!dir.canRead()) {
                return error("没有读取权限");
            }

            if (!dir.canWrite()) {
                return error("没有写入权限");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("absolutePath", dir.getAbsolutePath());
            result.put("canRead", dir.canRead());
            result.put("canWrite", dir.canWrite());

            return success(result);

        } catch (Exception e) {
            logger.error("验证路径失败", e);
            return error("验证路径失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用的表字典列表
     */
    @Operation(summary = "获取可用的表字典列表")
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/tableDicts")
    public AjaxResult getAvailableTableDicts() {
        try {
            // 获取所有表信息，排除系统表
            GenTable queryTable = new GenTable();
            Page<GenTable> tableList = genTableService.selectDbTableList(queryTable);

            // 过滤掉系统表和代码生成相关表
            List<Map<String, Object>> availableTables = new ArrayList<>();
            for (GenTable table : tableList.getRecords()) {
                String tableName = table.getTableName();
                // 排除系统表和代码生成表
                if (!tableName.startsWith("sys_") &&
                        !tableName.startsWith("gen_") &&
                        !tableName.startsWith("qrtz_")) {
                    Map<String, Object> tableInfo = new HashMap<>();
                    tableInfo.put("tableName", tableName);
                    tableInfo.put("tableComment", table.getTableComment());
                    availableTables.add(tableInfo);
                }
            }

            return success("获取表字典列表成功", availableTables);
        } catch (Exception e) {
            logger.error("获取可用表字典列表失败", e);
            return error("获取表字典列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取表字典字段列表
     */
    @Operation(summary = "获取表字典字段列表")
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/tableDicts/{tableName}/columns")
    public AjaxResult getTableDictColumns(@PathVariable String tableName) {
        try {
            // 验证表名格式，防止SQL注入
            if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
                return error("表名格式不正确");
            }

            // 获取表的字段信息
            List<GenTableColumn> columns = genTableColumnService.selectDbTableColumnsByName(tableName);

            List<Map<String, Object>> columnList = new ArrayList<>();
            for (GenTableColumn column : columns) {
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("columnName", column.getColumnName());
                columnInfo.put("columnComment", column.getColumnComment());
                columnInfo.put("columnType", column.getColumnType());
                columnInfo.put("javaType", column.getJavaType());
                columnList.add(columnInfo);
            }

            return success("获取字段列表成功", columnList);
        } catch (Exception e) {
            logger.error("获取表字典字段列表失败: tableName={}", tableName, e);
            return error("获取字段列表失败: " + e.getMessage());
        }
    }

    /**
     * 验证表字典配置
     */
    @Operation(summary = "验证表字典配置")
    @SaCheckPermission("tool:gen:edit")
    @PostMapping("/validateTableDict")
    public AjaxResult validateTableDictConfig(@RequestBody Map<String, String> request) {
        try {
            String tableName = request.get("tableName");
            String labelField = request.get("labelField");
            String valueField = request.get("valueField");

            // 验证表名
            if (StrUtil.isBlank(tableName)) {
                return error("表名不能为空");
            }

            // 验证表名格式，防止SQL注入
            if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
                return error("表名格式不正确，只允许字母、数字和下划线");
            }

            // 检查是否为系统表
            if (tableName.startsWith("sys_") || tableName.startsWith("gen_") || tableName.startsWith("qrtz_")) {
                return error("不允许使用系统表作为字典表");
            }

            // 验证表是否存在
            try {
                List<GenTableColumn> columns = genTableColumnService.selectDbTableColumnsByName(tableName);
                if (columns.isEmpty()) {
                    return error("指定的表 '" + tableName + "' 不存在");
                }

                // 验证显示字段
                if (StrUtil.isNotBlank(labelField)) {
                    // 验证字段名格式
                    if (!labelField.matches("^[a-zA-Z0-9_]+$")) {
                        return error("显示字段名格式不正确");
                    }

                    GenTableColumn labelColumn = columns.stream()
                            .filter(col -> col.getColumnName().equals(labelField))
                            .findFirst()
                            .orElse(null);

                    if (labelColumn == null) {
                        return error("显示字段 '" + labelField + "' 在表中不存在");
                    }

                    // 验证显示字段类型是否适合作为显示文本
                    String columnType = labelColumn.getColumnType().toLowerCase();
                    if (!columnType.contains("varchar") && !columnType.contains("char") &&
                            !columnType.contains("text") && !columnType.contains("string")) {
                        logger.warn("显示字段 '{}' 的类型为 '{}'，可能不适合作为显示文本", labelField, columnType);
                    }
                }

                // 验证值字段
                if (StrUtil.isNotBlank(valueField)) {
                    // 验证字段名格式
                    if (!valueField.matches("^[a-zA-Z0-9_]+$")) {
                        return error("值字段名格式不正确");
                    }

                    GenTableColumn valueColumn = columns.stream()
                            .filter(col -> col.getColumnName().equals(valueField))
                            .findFirst()
                            .orElse(null);

                    if (valueColumn == null) {
                        return error("值字段 '" + valueField + "' 在表中不存在");
                    }
                }

                // 检查循环依赖（简单检查，避免表A引用表B，表B又引用表A）
                String currentTableName = request.get("currentTableName");
                if (isCircularReference(tableName, currentTableName)) {
                    return error("检测到循环引用，表字典配置可能导致循环依赖");
                }

                Map<String, Object> result = new HashMap<>();
                result.put("valid", true);
                result.put("message", "表字典配置验证通过");
                result.put("columnCount", columns.size());

                return success(result);

            } catch (Exception e) {
                return error("验证表字典配置时发生错误: " + e.getMessage());
            }

        } catch (Exception e) {
            logger.error("验证表字典配置失败", e);
            return error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 检查循环引用
     */
    private boolean isCircularReference(String dictTableName, String currentTableName) {
        try {
            if (StrUtil.isBlank(currentTableName) || StrUtil.isBlank(dictTableName)) {
                return false;
            }

            // 简单的循环引用检查：如果字典表名和当前表名相同，则可能存在循环引用
            if (dictTableName.equals(currentTableName)) {
                return true;
            }

            // 可以在这里添加更复杂的循环引用检测逻辑
            // 例如检查dictTableName是否已经引用了currentTableName

            return false;
        } catch (Exception e) {
            logger.warn("检查循环引用时发生错误", e);
            return false;
        }
    }
}