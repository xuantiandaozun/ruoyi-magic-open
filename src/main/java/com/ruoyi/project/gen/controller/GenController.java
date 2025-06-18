package com.ruoyi.project.gen.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.springframework.ai.chat.client.ChatClient;
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
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.gen.domain.AsyncTaskInfo;
import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;
import com.ruoyi.project.gen.domain.vo.AiCreateTableResponse;
import com.ruoyi.project.gen.domain.vo.AiDirectTableRequest;
import com.ruoyi.project.gen.domain.vo.CreateImportTableRequest;
import com.ruoyi.project.gen.service.IAsyncTaskService;
import com.ruoyi.project.gen.service.IGenTableColumnService;
import com.ruoyi.project.gen.service.IGenTableService;
import com.ruoyi.project.gen.tools.DatabaseTableTool;
import com.ruoyi.project.gen.util.VelocityUtils;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.service.ISysDataSourceService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.micrometer.observation.Observation;
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
    private ChatClient chatClient;

    @Autowired
    private ISysDataSourceService sysDataSourceService;

    @Autowired
    private DatabaseTableTool databaseTableTool;

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
        tableList.forEach(table -> table.setDataSource("master"));
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
        if ("master".equalsIgnoreCase(dataSourceName)) {
            // 查询表信息
            tableList = genTableService.selectDbTableListByNames(tableNames);
        } else {
            // 查询表信息
            tableList = genTableService.selectDbTableListByNamesAndDataSource(tableNames, dataSourceName);
        }
        // 设置数据源标记
        tableList.forEach(table -> table.setDataSource(dataSourceName));
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
        genCode(response, data);
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
     * 批量生成代码
     */
    @Operation(summary = "批量生成代码")
    @SaCheckPermission("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/batchGenCode")
    public void batchGenCode(HttpServletResponse response, String tables) throws IOException {
        String[] tableNames = StrUtil.splitToArray(tables, ',');
        byte[] data = genTableService.downloadCode(tableNames);
        genCode(response, data);
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
     * 智能直接建表
     */
    @Operation(summary = "智能直接建表")
    @SaCheckPermission("tool:gen:create")
    @Log(title = "智能直接建表", businessType = BusinessType.INSERT)
    @PostMapping("/aiDirectTable")
    public AjaxResult aiDirectTable(@Validated @RequestBody AiDirectTableRequest request) {
        try {
            logger.info("开始智能建表，请求参数: {}", objectMapper.writeValueAsString(request));

            String prompt = String.format("""
                    你是一个专业的数据库建模专家，请根据以下用户需求设计并创建数据库表结构，可能包含多个表及外键关联。

                    ---

                    ### 用户需求
                    %s

                    ---

                    ### 设计要求

                    1. 每张表需归属于模块：%s，包名：%s，数据源：%s
                    2. 请你根据用户需求分析需要创建哪些表，并进行合理的命名
                    3. 每张表都需要设置功能名称（functionName），功能名称应该简洁明了地描述该表的业务用途
                    4. 表字段必须包括用户业务字段 + 系统基础字段：
                       - 系统基础字段：id（主键，自动生成）、create_by、create_time、update_by、update_time、remark
                       - 这些基础字段请在字段定义中 **完整添加**（例如类型、注释等）
                    5. 每张表的字段请包含字段名、类型、是否主键、是否必填、备注、是否自增等必要信息
                    6. 可设计表之间的外键关联（如一对多、多对多）

                    ---

                    ### 可用工具

                    - saveGenTable(table, dataSource)：用于保存表定义（不包含字段），返回包含 tableId 的表对象
                      注意：调用时请确保设置table的以下必要属性：
                      * tableName: 表名（如：sys_user）
                      * tableComment: 表注释（如：用户信息表）
                      * className: 实体类名称，首字母大写（如：SysUser）
                      * functionName: 功能名称（重要！如"用户管理"、"订单管理"等）
                      * functionAuthor: 作者（如：ruoyi）
                      * moduleName: 模块名称（如：%s）
                      * packageName: 包名（如：%s）
                      * businessName: 业务名称，通常是表名去掉前缀（如：user）
                      * tplCategory: 模板类型，默认使用"crud"
                      * genType: 生成方式，默认使用"0"（zip压缩包）
                      * dataSource: 数据源名称（如：%s）

                    - saveGenTableColumns(tableId, columns, tableName)：用于保存字段并创建数据库表，请确保字段中包含系统基础字段

                    ---

                    ### 你的任务

                    1. 解析用户需求，决定需要创建几张表
                    2. 对于每一张表，执行以下操作：
                       - 根据表的业务用途确定合适的功能名称（functionName）
                       - 设计合适的实体类名（className）和业务名（businessName）
                       - 使用 saveGenTable 保存表定义信息（包含所有必要字段），获取 tableId
                       - 设计字段（业务字段 + 系统字段）
                       - 使用 saveGenTableColumns 创建表并保存字段信息

                    ### 字段命名规范和设置示例
                    - 表名：使用下划线命名（如：sys_user、order_info）
                    - 实体类名：使用大驼峰命名（如：SysUser、OrderInfo）
                    - 业务名：通常是表名去掉模块前缀（如：user、orderInfo）
                    - 功能名：简洁的中文描述（如：用户管理、订单管理）
                    - 作者：统一使用"ruoyi"

                    ### 功能名称示例
                    - 用户表 -> "用户管理"
                    - 订单表 -> "订单管理"
                    - 商品表 -> "商品管理"
                    - 分类表 -> "分类管理"
                    - 角色表 -> "角色管理"
                    - 权限表 -> "权限管理"

                    请你只调用工具完成上述流程，无需返回表结构、字段定义或解释说明，仅返回执行结果。
                    """,
                    request.getRequirement(), request.getModuleName(), request.getPackageName(),
                    request.getDataSource());

            // 创建观察
            Observation observation = Observation.start("ai.table.creation", observationRegistry);

            // 使用AI工具调用直接创建表并同步到数据库
            String result = observation.observe(() -> {
                logger.info("开始调用 AI...");
                String aiResult = chatClient.prompt()
                        .user(prompt)
                        .tools(databaseTableTool)
                        .call()
                        .content();
                logger.info("AI 返回结果: {}", aiResult);
                return aiResult;
            });

            return success("智能建表结果：" + result);
        } catch (Exception e) {
            logger.error("智能直接建表过程中发生错误", e);
            throw new ServiceException("智能直接建表失败：" + e.getMessage());
        }
    }
    

    
    /**
     * 异步智能直接建表
     */
    @Operation(summary = "异步智能直接建表")
    @SaCheckPermission("tool:gen:create")
    @Log(title = "异步智能直接建表", businessType = BusinessType.INSERT)
    @PostMapping("/aiDirectTableAsync")
    public AjaxResult aiDirectTableAsync(@Validated @RequestBody AiDirectTableRequest request) {
        try {
            // 生成任务ID
            String taskId = IdUtil.fastSimpleUUID();
            String username = SecurityUtils.getUsername();
            String description = "智能建表: " + request.getRequirement();
            
            // 创建并保存任务信息
            AsyncTaskInfo taskInfo = AsyncTaskInfo.createPendingTask(taskId, "AI_DIRECT_TABLE", description, username);
            asyncTaskService.saveTask(taskInfo);
            
            // 异步执行任务
            AsyncManager.me().execute(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // 更新任务状态为执行中
                        asyncTaskService.updateTaskStatus(taskId, "RUNNING");
                        
                        String prompt = String.format("""
                                你是一个专业的数据库建模专家，请根据以下用户需求设计并创建数据库表结构，可能包含多个表及外键关联。

                                ---

                                ### 用户需求
                                %s

                                ---

                                ### 设计要求

                                1. 每张表需归属于模块：%s，包名：%s，数据源：%s
                                2. 请你根据用户需求分析需要创建哪些表，并进行合理的命名
                                3. 每张表都需要设置功能名称（functionName），功能名称应该简洁明了地描述该表的业务用途
                                4. 表字段必须包括用户业务字段 + 系统基础字段：
                                   - 系统基础字段：id（主键，自动生成）、create_by、create_time、update_by、update_time、remark
                                   - 这些基础字段请在字段定义中 **完整添加**（例如类型、注释等）
                                5. 每张表的字段请包含字段名、类型、是否主键、是否必填、备注、是否自增等必要信息
                                6. 可设计表之间的外键关联（如一对多、多对多）

                                ---

                                ### 可用工具

                                - saveGenTable(table, dataSource)：用于保存表定义（不包含字段），返回包含 tableId 的表对象
                                  注意：调用时请确保设置table的以下必要属性：
                                  * tableName: 表名（如：sys_user）
                                  * tableComment: 表注释（如：用户信息表）
                                  * className: 实体类名称，首字母大写（如：SysUser）
                                  * functionName: 功能名称（重要！如"用户管理"、"订单管理"等）
                                  * functionAuthor: 作者（如：ruoyi）
                                  * moduleName: 模块名称（如：%s）
                                  * packageName: 包名（如：%s）
                                  * businessName: 业务名称，通常是表名去掉前缀（如：user）
                                  * tplCategory: 模板类型，默认使用"crud"
                                  * genType: 生成方式，默认使用"0"（zip压缩包）
                                  * dataSource: 数据源名称（如：%s）

                                - saveGenTableColumns(tableId, columns, tableName)：用于保存字段并创建数据库表，请确保字段中包含系统基础字段

                                ---

                                ### 你的任务

                                1. 解析用户需求，决定需要创建几张表
                                2. 对于每一张表，执行以下操作：
                                   - 根据表的业务用途确定合适的功能名称（functionName）
                                   - 设计合适的实体类名（className）和业务名（businessName）
                                   - 使用 saveGenTable 保存表定义信息（包含所有必要字段），获取 tableId
                                   - 设计字段（业务字段 + 系统字段）
                                   - 使用 saveGenTableColumns 创建表并保存字段信息

                                ### 字段命名规范和设置示例
                                - 表名：使用下划线命名（如：sys_user、order_info）
                                - 实体类名：使用大驼峰命名（如：SysUser、OrderInfo）
                                - 业务名：通常是表名去掉模块前缀（如：user、orderInfo）
                                - 功能名：简洁的中文描述（如：用户管理、订单管理）
                                - 作者：统一使用"ruoyi"

                                ### 功能名称示例
                                - 用户表 -> "用户管理"
                                - 订单表 -> "订单管理"
                                - 商品表 -> "商品管理"
                                - 分类表 -> "分类管理"
                                - 角色表 -> "角色管理"
                                - 权限表 -> "权限管理"

                                请你只调用工具完成上述流程，无需返回表结构、字段定义或解释说明，仅返回执行结果。
                                """,
                                request.getRequirement(), request.getModuleName(), request.getPackageName(),
                                request.getDataSource());
                        
                        // 创建观察
                        Observation observation = Observation.start("ai.table.creation.async", observationRegistry);
                        
                        // 使用AI工具调用直接创建表并同步到数据库
                        String result = observation.observe(() -> {
                            logger.info("开始异步调用 AI...");
                            String aiResult = chatClient.prompt()
                                    .user(prompt)
                                    .tools(databaseTableTool)
                                    .call()
                                    .content();
                            logger.info("AI 返回结果: {}", aiResult);
                            return aiResult;
                        });
                        
                        // 更新任务结果
                        asyncTaskService.updateTaskResult(taskId, result);
                    } catch (Exception e) {
                        logger.error("异步智能直接建表过程中发生错误", e);
                        asyncTaskService.updateTaskError(taskId, e.getMessage());
                    }
                }
            });
            
            return success("任务已提交" ,taskId);
        } catch (Exception e) {
            logger.error("提交异步智能直接建表任务失败", e);
            throw new ServiceException("提交异步智能直接建表任务失败：" + e.getMessage());
        }
    }

    /**
     * 生成zip文件
     */
    private void genCode(HttpServletResponse response, byte[] data) throws IOException {
        response.reset();
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Content-Disposition", "attachment; filename=\"ruoyi.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IoUtil.write(response.getOutputStream(), true, data);
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

            // 检查数据源是否设置，未设置则默认为master
            if (StrUtil.isBlank(table.getDataSource())) {
                table.setDataSource("master");
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
        mainDataSource.put("name", "master");
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
}