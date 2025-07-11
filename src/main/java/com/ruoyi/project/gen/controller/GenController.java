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
import com.ruoyi.project.gen.tools.ai.AiDatabaseTableTool;
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
    private AiDatabaseTableTool databaseTableTool;

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
                       - 对于非关联表，还需添加 del_flag（删除标志，0代表存在，2代表删除）
                       - 关联表（多对多关系的中间表）不需要添加 del_flag 字段
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
                                   - 对于非关联表，还需添加 del_flag（删除标志，0代表存在，2代表删除）
                                   - 关联表（多对多关系的中间表）不需要添加 del_flag 字段
                                   - 这些基础字段请在字段定义中 **完整添加**（例如类型、注释等）
                                5. 每张表的字段请包含字段名、类型、是否主键、是否必填、备注、是否自增等必要信息
                                6. 可设计表之间的外键关联（如一对多、多对多）

                                ---

                                ### 可用工具

                                - saveGenTable(table, dataSource, taskId)：用于保存表定义（不包含字段），返回包含 tableId 的表对象
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
                                  * taskId: 任务ID，请传入 "%s" 用于记录任务进度和当前操作的表信息

                                - saveGenTableColumns(tableId, columns, tableName, taskId)：用于保存字段并创建数据库表，请确保字段中包含系统基础字段，taskId请传入 "%s" 用于记录任务进度和当前操作的字段信息

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
                                request.getDataSource(), request.getModuleName(), request.getPackageName(),
                                request.getDataSource(), taskId, taskId);
                        
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
     * AI优化表字段
     */
    @Operation(summary = "AI优化表字段")
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "AI优化表字段", businessType = BusinessType.UPDATE)
    @PostMapping("/aiOptimizeColumns/{tableId}")
    public AjaxResult aiOptimizeColumns(@PathVariable Long tableId) {
        try {
            // 获取表信息
            GenTable table = genTableService.selectGenTableById(tableId);
            if (table == null) {
                return error("表信息不存在");
            }
            
            // 获取表字段列表
            List<GenTableColumn> columns = genTableColumnService.selectGenTableColumnListByTableId(tableId);
            if (columns == null || columns.isEmpty()) {
                return error("表字段不存在");
            }
            
            // 创建观察
            Observation observation = Observation.start("ai.column.optimization", observationRegistry);
            
            // 使用AI工具优化表字段
            String result = observation.observe(() -> {
                logger.info("开始调用AI优化表字段...");
                
                String prompt = String.format("""
                        你是一个专业的数据库设计专家，请根据以下表结构信息，优化表字段的配置。
                        
                        表名: %s
                        表注释: %s
                        
                        请分析每个字段的名称和注释，然后优化以下配置：
                        1. isPk: 是否为主键（1是，0否）
                        2. isIncrement: 是否自增（1是，0否）
                        3. isRequired: 是否必填（1是，0否）
                        4. isInsert: 是否为插入字段（1是，0否）
                        5. isEdit: 是否为编辑字段（1是，0否）
                        6. isList: 是否为列表字段（1是，0否）
                        7. isQuery: 是否为查询字段（1是，0否）
                        8. queryType: 查询方式（等于、不等于、大于、小于、范围等）
                        9. htmlType: 显示类型（文本框、文本域、下拉框、单选框、复选框、日期控件等）
                        10. dictType: 字典类型
                        
                        优化原则：
                        - 主键字段(通常是id)应设置isPk=1，如果是自增主键还应设置isIncrement=1
                        - 必要的业务字段应设置isRequired=1
                        - 系统维护字段（如create_time创建时间、update_time修改时间、create_by创建人、update_by修改人等）通常不需要手动插入和编辑(isInsert=0, isEdit=0)
                        - 查询字段(isQuery=1)总数不应超过4个，优先选择最常用的筛选条件
                        - 列表显示字段(isList=1)总数不应超过5个，优先显示最重要的业务字段
                        - 如果表的业务字段较少，可以将系统字段（如创建时间等）设为列表显示字段进行填充
                        - 如果表的业务字段较多，则系统字段（如创建时间等）不应显示在列表中
                        - 查询方式(queryType)应根据字段类型选择合适的方式：
                          * 精确匹配字段用EQ(等于)
                          * 模糊查询字段用LIKE(包含)
                          * 数值范围用BETWEEN(范围)
                          * 时间字段可用GT(大于)、LT(小于)或BETWEEN(范围)
                        - 显示类型(htmlType)应根据字段用途选择：
                          * 短文本用input
                          * 长文本用textarea
                          * 日期时间用datetime
                          * 数字用input
                          * 状态或类型等固定选项用select、radio或checkbox
                        - 对于有数据字典的字段，应设置正确的dictType
                        
                        字段列表：
                        %s
                        
                        请你只调用updateGenTableColumn工具来更新每个字段的配置，无需返回表结构或解释说明，仅返回执行结果。
                        注意：
                        1. 必须使用提供的字段ID(columnId)作为参数，不要使用虚构的ID
                        2. 只能修改isPk、isIncrement、isRequired、isInsert、isEdit、isList、isQuery、queryType、htmlType、dictType这几个字段，其他字段不能改
                        3. 每次调用updateGenTableColumn时，必须包含columnId字段
                        4. 必须严格遵循优化原则中的查询字段和列表字段数量限制
                        """, 
                        table.getTableName(), 
                        table.getTableComment(),
                        formatColumnsInfo(columns));
                
                String aiResult = chatClient.prompt()
                        .user(prompt)
                        .tools(databaseTableTool)
                        .call()
                        .content();
                
                logger.info("AI优化表字段返回结果: {}", aiResult);
                return aiResult;
            });
            
            return success("AI优化表字段成功", result);
        } catch (Exception e) {
            logger.error("AI优化表字段过程中发生错误", e);
            throw new ServiceException("AI优化表字段失败：" + e.getMessage());
        }
    }
    
    /**
     * 异步AI优化表字段
     */
    @Operation(summary = "异步AI优化表字段")
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "异步AI优化表字段", businessType = BusinessType.UPDATE)
    @PostMapping("/aiOptimizeColumnsAsync/{tableId}")
    public AjaxResult aiOptimizeColumnsAsync(@PathVariable Long tableId) {
        try {
            // 获取表信息
            GenTable table = genTableService.selectGenTableById(tableId);
            if (table == null) {
                return error("表信息不存在");
            }
            
            // 获取表字段列表
            List<GenTableColumn> columns = genTableColumnService.selectGenTableColumnListByTableId(tableId);
            if (columns == null || columns.isEmpty()) {
                return error("表字段不存在");
            }
            
            // 生成任务ID
            String taskId = IdUtil.fastSimpleUUID();
            String username = SecurityUtils.getUsername();
            String description = "AI优化表字段: " + table.getTableName();
            
            // 创建并保存任务信息
            AsyncTaskInfo taskInfo = AsyncTaskInfo.createPendingTask(taskId, "AI_OPTIMIZE_COLUMNS", description, username);
            asyncTaskService.saveTask(taskInfo);
            
            // 异步执行任务
            AsyncManager.me().execute(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // 更新任务状态为执行中
                        asyncTaskService.updateTaskStatus(taskId, "RUNNING");
                        
                        // 创建观察
                        Observation observation = Observation.start("ai.column.optimization.async", observationRegistry);
                        
                        // 使用AI工具优化表字段
                        String result = observation.observe(() -> {
                            logger.info("开始异步调用AI优化表字段...");
                            
                            String prompt = String.format("""
                                    你是一个专业的数据库设计专家，请根据以下表结构信息，优化表字段的配置。
                                    
                                    表名: %s
                                    表注释: %s
                                    
                                    请分析每个字段的名称和注释，然后优化以下配置：
                                    1. isPk: 是否为主键（1是，0否）
                                    2. isIncrement: 是否自增（1是，0否）
                                    3. isRequired: 是否必填（1是，0否）
                                    4. isInsert: 是否为插入字段（1是，0否）
                                    5. isEdit: 是否为编辑字段（1是，0否）
                                    6. isList: 是否为列表字段（1是，0否）
                                    7. isQuery: 是否为查询字段（1是，0否）
                                    8. queryType: 查询方式（等于、不等于、大于、小于、范围等）
                                    9. htmlType: 显示类型（文本框、文本域、下拉框、单选框、复选框、日期控件等）
                                    10. dictType: 字典类型
                                    
                                    优化原则：
                                    - 主键字段(通常是id)应设置isPk=1，如果是自增主键还应设置isIncrement=1
                                    - 必要的业务字段应设置isRequired=1
                                    - 系统维护字段（如create_time创建时间、update_time修改时间、create_by创建人、update_by修改人等）通常不需要手动插入和编辑(isInsert=0, isEdit=0)
                                    - 查询字段(isQuery=1)总数不应超过4个，优先选择最常用的筛选条件
                                    - 列表显示字段(isList=1)总数不应超过5个，优先显示最重要的业务字段
                                    - 如果表的业务字段较少，可以将系统字段（如创建时间等）设为列表显示字段进行填充
                                    - 如果表的业务字段较多，则系统字段（如创建时间等）不应显示在列表中
                                    - 查询方式(queryType)应根据字段类型选择合适的方式：
                                      * 精确匹配字段用EQ(等于)
                                      * 模糊查询字段用LIKE(包含)
                                      * 数值范围用BETWEEN(范围)
                                      * 时间字段可用GT(大于)、LT(小于)或BETWEEN(范围)
                                    - 显示类型(htmlType)应根据字段用途选择：
                                      * 短文本用input
                                      * 长文本用textarea
                                      * 日期时间用datetime
                                      * 数字用input
                                      * 状态或类型等固定选项用select、radio或checkbox
                                    - 对于有数据字典的字段，应设置正确的dictType
                                    
                                    字段列表：
                                    %s
                                    
                                    请你只调用updateGenTableColumn工具来更新每个字段的配置，无需返回表结构或解释说明，仅返回执行结果。
                                    注意：
                                    1. 必须使用提供的字段ID(columnId)作为参数，不要使用虚构的ID
                                    2. 只能修改isPk、isIncrement、isRequired、isInsert、isEdit、isList、isQuery、queryType、htmlType、dictType这几个字段，其他字段不能改
                                    3. 每次调用updateGenTableColumn时，必须包含columnId字段
                                    4. 必须严格遵循优化原则中的查询字段和列表字段数量限制
                                    5. 在每次调用updateGenTableColumn之前，请先更新任务的extraInfo，将当前正在处理的字段信息添加到任务中
                                    """, 
                                    table.getTableName(), 
                                    table.getTableComment(),
                                    formatColumnsInfo(columns));
                            
                            // 不再需要拦截器，直接使用修改后的updateGenTableColumn方法
                            // 该方法已经添加了taskId和tableName参数，会自动更新任务的extraInfo
                            
                            try {
                                String aiResult = chatClient.prompt()
                                        .user(prompt)
                                        .tools(databaseTableTool)
                                        .call()
                                        .content();
                                
                                logger.info("AI优化表字段返回结果: {}", aiResult);
                                return aiResult;
                            } finally {
                                // 不再需要移除拦截器
                            }
                        });
                        
                        // 更新任务结果
                        asyncTaskService.updateTaskResult(taskId, result);
                    } catch (Exception e) {
                        logger.error("异步AI优化表字段过程中发生错误", e);
                        asyncTaskService.updateTaskError(taskId, e.getMessage());
                    }
                }
            });
            
            return success("任务已提交", taskId);
        } catch (Exception e) {
            logger.error("提交异步AI优化表字段任务失败", e);
            throw new ServiceException("提交异步AI优化表字段任务失败：" + e.getMessage());
        }
    }
    
    /**
     * 格式化字段信息用于AI提示
     */
    private String formatColumnsInfo(List<GenTableColumn> columns) {
        StringBuilder sb = new StringBuilder();
        for (GenTableColumn column : columns) {
            sb.append(String.format("字段ID: %s, 字段名: %s, 注释: %s, 类型: %s, isPk: %s, isIncrement: %s, isRequired: %s, isInsert: %s, isEdit: %s, isList: %s, isQuery: %s, queryType: %s, htmlType: %s, dictType: %s\n",
                    column.getColumnId(),
                    column.getColumnName(),
                    column.getColumnComment(),
                    column.getColumnType(),
                    column.getIsPk(),
                    column.getIsIncrement(),
                    column.getIsRequired(),
                    column.getIsInsert(),
                    column.getIsEdit(),
                    column.getIsList(),
                    column.getIsQuery(),
                    column.getQueryType(),
                    column.getHtmlType(),
                    column.getDictType()));
        }
        return sb.toString();
    }
}