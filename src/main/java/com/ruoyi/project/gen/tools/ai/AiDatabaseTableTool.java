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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.datasource.DynamicDataSourceContextHolder;
import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;
import com.ruoyi.project.gen.service.IAsyncTaskService;
import com.ruoyi.project.gen.service.IGenTableColumnService;
import com.ruoyi.project.gen.service.IGenTableService;
import com.ruoyi.project.gen.tools.request.BatchSaveGenTableRequest;
import com.ruoyi.project.gen.tools.request.BatchUpdateGenTableColumnRequest;
import com.ruoyi.project.gen.tools.request.BatchUpdateGenTableRequest;
import com.ruoyi.project.gen.tools.request.SaveGenTableRequest;
import com.ruoyi.project.gen.tools.request.UpdateGenTableColumnRequest;
import com.ruoyi.project.gen.tools.request.UpdateGenTableRequest;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

/**
 * AI专用数据库表操作工具
 * 提供给AI模型调用的工具，使用Request类避免Long类型溢出问题
 */
@Service
public class AiDatabaseTableTool {
    private static final Logger logger = LoggerFactory.getLogger(AiDatabaseTableTool.class);

    @Autowired
    private IGenTableService genTableService;

    @Autowired
    private IGenTableColumnService genTableColumnService;

    @Autowired
    private IAsyncTaskService asyncTaskService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 保存表定义信息和字段信息到系统（不创建实际表）
     * 使用请求类避免Long类型溢出问题
     * 
     * @param request 保存表请求对象
     * @return 保存结果的字符串描述
     */
    @Tool(name = "saveGenTable", description = "将代码生成器的表定义（GenTable）和字段定义（GenTableColumn）保存到系统数据库，这只是元数据记录，并不会在数据库中创建实际的物理表。需要调用 syncTableToDatabase 方法才能将表结构同步到数据库中")
    public String saveGenTable(SaveGenTableRequest request) {
        try {
            logger.info("saveGenTable保存表定义信息: {}", request);
            
            // 创建GenTable对象
            GenTable table = new GenTable();
            table.setTableName(request.getTableName());
            table.setTableComment(request.getTableComment());
            table.setSubTableName(request.getSubTableName());
            table.setSubTableFkName(request.getSubTableFkName());
            table.setClassName(request.getClassName());
            table.setTplCategory(request.getTplCategory());
            table.setTplWebType(request.getTplWebType());
            table.setPackageName(request.getPackageName());
            table.setModuleName(request.getModuleName());
            table.setBusinessName(request.getBusinessName());
            table.setFunctionName(request.getFunctionName());
            table.setFunctionAuthor(request.getFunctionAuthor());
            table.setGenType(request.getGenType());
            table.setGenPath(request.getGenPath());
            table.setOptions(request.getOptions());
            
            // 获取其他参数
            String dataSource = request.getDataSource();
            String taskId = request.getTaskId();
            List<Object> columns = request.getColumns();
            
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
                dataSource = DynamicDataSourceContextHolder.MASTER;
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
            if (StrUtil.isNotBlank(request.getTaskId())) {
                asyncTaskService.updateTaskError(request.getTaskId(), "保存表定义失败：" + e.getMessage());
            }
            throw new ServiceException("保存表定义失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量保存表定义信息和字段信息到系统（不创建实际表）
     * 
     * @param request 批量保存表请求对象
     * @return 保存结果的字符串描述
     */
    @Tool(name = "batchSaveGenTable", description = "批量将代码生成器的表定义（GenTable）和字段定义（GenTableColumn）保存到系统数据库，这只是元数据记录，并不会在数据库中创建实际的物理表。需要调用 syncTableToDatabase 方法才能将表结构同步到数据库中")
    public String batchSaveGenTable(BatchSaveGenTableRequest request) {
        try {
            logger.info("batchSaveGenTable批量保存表定义信息: {}", request);
            
            if (request.getTables() == null || request.getTables().isEmpty()) {
                throw new ServiceException("批量保存表定义失败：表信息列表为空");
            }
            
            String taskId = request.getTaskId();
            List<String> results = new ArrayList<>();
            int totalTables = request.getTables().size();
            int currentTable = 0;
            
            // 更新任务初始进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 10);
                String extraInfo = String.format("{\"currentAction\":\"开始批量创建表定义\",\"totalTables\":%d}", totalTables);
                asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
            }
            
            for (SaveGenTableRequest tableRequest : request.getTables()) {
                currentTable++;
                
                // 设置任务ID，以便在单表处理中更新任务状态
                tableRequest.setTaskId(taskId);
                
                try {
                    // 更新任务进度
                    if (StrUtil.isNotBlank(taskId)) {
                        int progress = 10 + (currentTable * 90 / totalTables);
                        asyncTaskService.updateTaskProgress(taskId, progress);
                        
                        String extraInfo = String.format("{\"currentAction\":\"创建表定义\",\"currentTable\":%d,\"totalTables\":%d,\"tableName\":\"%s\"}", 
                                currentTable, totalTables, tableRequest.getTableName());
                        asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
                    }
                    
                    // 创建GenTable对象
                    GenTable table = new GenTable();
                    table.setTableName(tableRequest.getTableName());
                    table.setTableComment(tableRequest.getTableComment());
                    table.setSubTableName(tableRequest.getSubTableName());
                    table.setSubTableFkName(tableRequest.getSubTableFkName());
                    table.setClassName(tableRequest.getClassName());
                    table.setTplCategory(tableRequest.getTplCategory());
                    table.setTplWebType(tableRequest.getTplWebType());
                    table.setPackageName(tableRequest.getPackageName());
                    table.setModuleName(tableRequest.getModuleName());
                    table.setBusinessName(tableRequest.getBusinessName());
                    table.setFunctionName(tableRequest.getFunctionName());
                    table.setFunctionAuthor(tableRequest.getFunctionAuthor());
                    table.setGenType(tableRequest.getGenType());
                    table.setGenPath(tableRequest.getGenPath());
                    table.setOptions(tableRequest.getOptions());
                    
                    // 获取其他参数
                    String dataSource = tableRequest.getDataSource();
                    List<Object> columns = tableRequest.getColumns();
                    
                    table.setTableId(IdUtil.getSnowflakeNextId());

                    // 设置数据源
                    if (StrUtil.isBlank(dataSource)) {
                        dataSource = DynamicDataSourceContextHolder.MASTER;
                    }
                    table.setDataSource(dataSource);

                    // 初始化表信息
                    table.setCreateBy("admin");

                    // 保存表基本信息
                    genTableService.save(table);

                    // 保存字段信息
                    if (columns != null && !columns.isEmpty()) {
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
                            genTableColumnService.insertGenTableColumn(column);
                        }
                    }

                    String result = "表定义[" + table.getTableName() + "]保存成功，tableId=" + table.getTableId().toString() + 
                            "，共包含" + (columns != null ? columns.size() : 0) + "个字段";
                    results.add(result);
                    
                } catch (Exception e) {
                    logger.error("保存表定义失败: " + tableRequest.getTableName(), e);
                    results.add("表定义[" + tableRequest.getTableName() + "]保存失败：" + e.getMessage());
                }
            }
            
            StringBuilder finalResult = new StringBuilder();
            finalResult.append("批量保存表定义完成，共处理").append(totalTables).append("个表：\n");
            for (String result : results) {
                finalResult.append(result).append("\n");
            }
            finalResult.append("注意：实际表尚未创建，需要调用syncTableToDatabase方法同步到数据库。");
            
            // 更新任务完成状态
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskResult(taskId, finalResult.toString());
            }

            return finalResult.toString();
        } catch (Exception e) {
            logger.error("批量保存表定义失败", e);
            if (StrUtil.isNotBlank(request.getTaskId())) {
                asyncTaskService.updateTaskError(request.getTaskId(), "批量保存表定义失败：" + e.getMessage());
            }
            throw new ServiceException("批量保存表定义失败：" + e.getMessage());
        }
    }
    
  
    /**
     * 根据tableId同步表到数据库
     * 
     * @param tableId 表ID（字符串格式）
     * @param taskId  任务ID
     * @return 操作结果
     */
    @Tool(name = "syncTableToDatabase", description = "根据 tableId 将之前保存的表定义（GenTable）同步到数据库中，创建或更新实际的物理表结构。这是一个 DDL 操作。")
    public String syncTableToDatabase(String tableId, String taskId) {
        try {
            logger.info("syncTableToDatabase同步表到数据库: tableId={}, taskId={}", tableId, taskId);

            // 字符串转Long
            Long tableIdLong;
            try {
                tableIdLong = Long.parseLong(tableId);
            } catch (NumberFormatException e) {
                logger.error("表ID格式错误: {}", tableId);
                throw new ServiceException("表ID格式错误：" + tableId);
            }

            // 获取表信息
            GenTable table = genTableService.selectGenTableById(tableIdLong);
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
     * 根据ID获取表定义信息
     * 
     * @param tableId 表ID（字符串格式）
     * @return 表信息的字符串描述
     */
    @Tool(name = "getGenTableById", description = "根据 tableId 查询代码生成器的表定义（GenTable）的元数据。这只是查询元数据，不涉及物理数据库表。")
    public String getGenTableById(String tableId) {
        try {
            logger.info("getGenTableById获取表信息: {}", tableId);
            
            // 字符串转Long
            Long tableIdLong;
            try {
                tableIdLong = Long.parseLong(tableId);
            } catch (NumberFormatException e) {
                logger.error("表ID格式错误: {}", tableId);
                throw new ServiceException("表ID格式错误：" + tableId);
            }
            
            GenTable table = genTableService.selectGenTableById(tableIdLong);
            if (table == null) {
                return "表定义不存在，tableId=" + tableId;
            }
            return String.format("表信息: tableId=%s, tableName=%s, tableComment=%s, className=%s, packageName=%s, moduleName=%s, businessName=%s, functionName=%s, functionAuthor=%s, dataSource=%s",
                    table.getTableId(), table.getTableName(), table.getTableComment(), table.getClassName(),
                    table.getPackageName(), table.getModuleName(), table.getBusinessName(), table.getFunctionName(),
                    table.getFunctionAuthor(), table.getDataSource());
        } catch (Exception e) {
            logger.error("获取表信息失败", e);
            throw new ServiceException("获取表信息失败：" + e.getMessage());
        }
    }

    /**
     * 根据表ID获取表字段列表
     * 
     * @param tableId 表ID（字符串格式）
     * @return 字段列表的字符串描述
     */
    @Tool(name = "getGenTableColumnsByTableId", description = "根据 tableId 查询代码生成器的表字段定义（GenTableColumn）的元数据列表。这只是查询元数据，不涉及物理数据库表。")
    public String getGenTableColumnsByTableId(String tableId) {
        try {
            logger.info("getGenTableColumnsByTableId获取表字段列表: {}", tableId);
            
            // 字符串转Long
            Long tableIdLong;
            try {
                tableIdLong = Long.parseLong(tableId);
            } catch (NumberFormatException e) {
                logger.error("表ID格式错误: {}", tableId);
                throw new ServiceException("表ID格式错误：" + tableId);
            }
            
            List<GenTableColumn> columns = genTableColumnService.selectGenTableColumnListByTableId(tableIdLong);
            if (columns == null || columns.isEmpty()) {
                return "表字段列表为空，tableId=" + tableId;
            }
            StringBuilder result = new StringBuilder();
            result.append("表字段列表(共").append(columns.size()).append("个字段):\n");
            for (GenTableColumn column : columns) {
                result.append(String.format("- columnId=%s, columnName=%s, columnComment=%s, columnType=%s, javaType=%s, javaField=%s, isPk=%s, isRequired=%s, isInsert=%s, isEdit=%s, isList=%s, isQuery=%s, queryType=%s, htmlType=%s, dictType=%s, sort=%s\n",
                        column.getColumnId(), column.getColumnName(), column.getColumnComment(), column.getColumnType(),
                        column.getJavaType(), column.getJavaField(), column.getIsPk(), column.getIsRequired(),
                        column.getIsInsert(), column.getIsEdit(), column.getIsList(), column.getIsQuery(),
                        column.getQueryType(), column.getHtmlType(), column.getDictType(), column.getSort()));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("获取表字段列表失败", e);
            throw new ServiceException("获取表字段列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID修改表定义信息
     * 
     * @param request 更新表请求对象
     * @return 操作结果
     */
    @Tool(name = "updateGenTable", description = "根据 tableId 更新代码生成器的表定义（GenTable）的元数据。这只是修改元数据，不涉及物理数据库表。如果需要同步到数据库，请调用 syncTableToDatabase 方法。")
    public String updateGenTable(UpdateGenTableRequest request) {
        try {
            logger.info("updateGenTable修改表信息: {}", request);
            
            // 字符串转Long
            Long tableIdLong;
            try {
                tableIdLong = Long.parseLong(request.getTableId());
            } catch (NumberFormatException e) {
                logger.error("表ID格式错误: {}", request.getTableId());
                throw new ServiceException("表ID格式错误：" + request.getTableId());
            }
            
            // 获取现有表信息
            GenTable genTable = genTableService.selectGenTableById(tableIdLong);
            if (genTable == null) {
                throw new ServiceException("表定义不存在，tableId=" + request.getTableId());
            }
            
            // 更新表信息（只更新非空字段）
            if (StrUtil.isNotBlank(request.getTableName())) {
                genTable.setTableName(request.getTableName());
            }
            if (StrUtil.isNotBlank(request.getTableComment())) {
                genTable.setTableComment(request.getTableComment());
            }
            if (StrUtil.isNotBlank(request.getSubTableName())) {
                genTable.setSubTableName(request.getSubTableName());
            }
            if (StrUtil.isNotBlank(request.getSubTableFkName())) {
                genTable.setSubTableFkName(request.getSubTableFkName());
            }
            if (StrUtil.isNotBlank(request.getClassName())) {
                genTable.setClassName(request.getClassName());
            }
            if (StrUtil.isNotBlank(request.getTplCategory())) {
                genTable.setTplCategory(request.getTplCategory());
            }
            if (StrUtil.isNotBlank(request.getPackageName())) {
                genTable.setPackageName(request.getPackageName());
            }
            if (StrUtil.isNotBlank(request.getModuleName())) {
                genTable.setModuleName(request.getModuleName());
            }
            if (StrUtil.isNotBlank(request.getBusinessName())) {
                genTable.setBusinessName(request.getBusinessName());
            }
            if (StrUtil.isNotBlank(request.getFunctionName())) {
                genTable.setFunctionName(request.getFunctionName());
            }
            if (StrUtil.isNotBlank(request.getFunctionAuthor())) {
                genTable.setFunctionAuthor(request.getFunctionAuthor());
            }
            if (StrUtil.isNotBlank(request.getGenType())) {
                genTable.setGenType(request.getGenType());
            }
            if (StrUtil.isNotBlank(request.getGenPath())) {
                genTable.setGenPath(request.getGenPath());
            }
            if (StrUtil.isNotBlank(request.getOptions())) {
                genTable.setOptions(request.getOptions());
            }
            if (StrUtil.isNotBlank(request.getDataSource())) {
                genTable.setDataSource(request.getDataSource());
            }
            
            // 如果提供了任务ID，更新任务的extraInfo
            if (StrUtil.isNotBlank(request.getTaskId())) {
                String extraInfo = String.format("{\"tableName\":\"%s\",\"tableComment\":\"%s\"}", 
                        genTable.getTableName(), genTable.getTableComment());
                asyncTaskService.updateTaskExtraInfo(request.getTaskId(), extraInfo);
                logger.info("更新任务扩展信息: 正在优化表 {}", genTable.getTableName());
            }
            
            boolean result = genTableService.updateById(genTable);
            return result ? "表[" + genTable.getTableName() + "]修改成功" : "表修改失败";
        } catch (Exception e) {
            logger.error("修改表信息失败", e);
            throw new ServiceException("修改表信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量修改表定义信息
     * 
     * @param request 批量更新表请求对象
     * @return 操作结果
     */
    @Tool(name = "batchUpdateGenTable", description = "根据 tableId 列表，批量更新代码生成器的表定义（GenTable）的元数据。这只是修改元数据，不涉及物理数据库表。如果需要同步到数据库，请调用 syncTableToDatabase 方法。")
    public String batchUpdateGenTable(BatchUpdateGenTableRequest request) {
        try {
            logger.info("batchUpdateGenTable批量修改表信息: {}", request);
            
            if (request.getTables() == null || request.getTables().isEmpty()) {
                throw new ServiceException("批量修改表定义失败：表信息列表为空");
            }
            
            String taskId = request.getTaskId();
            List<String> results = new ArrayList<>();
            int totalTables = request.getTables().size();
            int currentTable = 0;
            
            // 更新任务初始进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 10);
                String extraInfo = String.format("{\"currentAction\":\"开始批量修改表定义\",\"totalTables\":%d}", totalTables);
                asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
            }
            
            for (UpdateGenTableRequest tableRequest : request.getTables()) {
                currentTable++;
                
                // 设置任务ID，以便在单表处理中更新任务状态
                tableRequest.setTaskId(taskId);
                
                try {
                    // 更新任务进度
                    if (StrUtil.isNotBlank(taskId)) {
                        int progress = 10 + (currentTable * 90 / totalTables);
                        asyncTaskService.updateTaskProgress(taskId, progress);
                        
                        String extraInfo = String.format("{\"currentAction\":\"修改表定义\",\"currentTable\":%d,\"totalTables\":%d,\"tableId\":\"%s\"}", 
                                currentTable, totalTables, tableRequest.getTableId());
                        asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
                    }
                    
                    // 字符串转Long
                    Long tableIdLong;
                    try {
                        tableIdLong = Long.parseLong(tableRequest.getTableId());
                    } catch (NumberFormatException e) {
                        logger.error("表ID格式错误: {}", tableRequest.getTableId());
                        results.add("表ID[" + tableRequest.getTableId() + "]格式错误");
                        continue;
                    }
                    
                    // 获取现有表信息
                    GenTable genTable = genTableService.selectGenTableById(tableIdLong);
                    if (genTable == null) {
                        results.add("表定义不存在，tableId=" + tableRequest.getTableId());
                        continue;
                    }
                    
                    // 更新表信息（只更新非空字段）
                    if (StrUtil.isNotBlank(tableRequest.getTableName())) {
                        genTable.setTableName(tableRequest.getTableName());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getTableComment())) {
                        genTable.setTableComment(tableRequest.getTableComment());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getSubTableName())) {
                        genTable.setSubTableName(tableRequest.getSubTableName());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getSubTableFkName())) {
                        genTable.setSubTableFkName(tableRequest.getSubTableFkName());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getClassName())) {
                        genTable.setClassName(tableRequest.getClassName());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getTplCategory())) {
                        genTable.setTplCategory(tableRequest.getTplCategory());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getPackageName())) {
                        genTable.setPackageName(tableRequest.getPackageName());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getModuleName())) {
                        genTable.setModuleName(tableRequest.getModuleName());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getBusinessName())) {
                        genTable.setBusinessName(tableRequest.getBusinessName());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getFunctionName())) {
                        genTable.setFunctionName(tableRequest.getFunctionName());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getFunctionAuthor())) {
                        genTable.setFunctionAuthor(tableRequest.getFunctionAuthor());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getGenType())) {
                        genTable.setGenType(tableRequest.getGenType());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getGenPath())) {
                        genTable.setGenPath(tableRequest.getGenPath());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getOptions())) {
                        genTable.setOptions(tableRequest.getOptions());
                    }
                    if (StrUtil.isNotBlank(tableRequest.getDataSource())) {
                        genTable.setDataSource(tableRequest.getDataSource());
                    }
                    
                    boolean result = genTableService.updateById(genTable);
                    results.add(result ? "表[" + genTable.getTableName() + "]修改成功" : "表[" + genTable.getTableName() + "]修改失败");
                    
                } catch (Exception e) {
                    logger.error("修改表信息失败: " + tableRequest.getTableId(), e);
                    results.add("表ID[" + tableRequest.getTableId() + "]修改失败：" + e.getMessage());
                }
            }
            
            StringBuilder finalResult = new StringBuilder();
            finalResult.append("批量修改表定义完成，共处理").append(totalTables).append("个表：\n");
            for (String result : results) {
                finalResult.append(result).append("\n");
            }
            
            // 更新任务完成状态
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskResult(taskId, finalResult.toString());
            }

            return finalResult.toString();
        } catch (Exception e) {
            logger.error("批量修改表信息失败", e);
            if (StrUtil.isNotBlank(request.getTaskId())) {
                asyncTaskService.updateTaskError(request.getTaskId(), "批量修改表信息失败：" + e.getMessage());
            }
            throw new ServiceException("批量修改表信息失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID修改表字段信息
     * 
     * @param request 更新字段请求对象
     * @return 操作结果
     */
    @Tool(name = "updateGenTableColumn", description = "根据 columnId 更新代码生成器的表字段定义（GenTableColumn）的元数据。这只是修改元数据，不涉及物理数据库表。如果需要同步到数据库，请调用 syncTableToDatabase 方法。")
    public Map<String, Object> updateGenTableColumn(UpdateGenTableColumnRequest request) {
        try {
            logger.info("updateGenTableColumn修改表字段信息: {}", request);
            
            // 字符串转Long
            Long columnIdLong;
            try {
                columnIdLong = Long.parseLong(request.getColumnId());
            } catch (NumberFormatException e) {
                logger.error("字段ID格式错误: {}", request.getColumnId());
                throw new ServiceException("字段ID格式错误：" + request.getColumnId());
            }
            
            // 获取现有字段信息
            GenTableColumn genTableColumn = genTableColumnService.selectGenTableColumnById(columnIdLong);
            if (genTableColumn == null) {
                throw new ServiceException("字段定义不存在，columnId=" + request.getColumnId());
            }
            
            // 更新字段信息（只更新非空字段）
            if (StrUtil.isNotBlank(request.getTableId())) {
                try {
                    genTableColumn.setTableId(Long.parseLong(request.getTableId()));
                } catch (NumberFormatException e) {
                    logger.error("表ID格式错误: {}", request.getTableId());
                    throw new ServiceException("表ID格式错误：" + request.getTableId());
                }
            }
            if (StrUtil.isNotBlank(request.getColumnName())) {
                genTableColumn.setColumnName(request.getColumnName());
            }
            if (StrUtil.isNotBlank(request.getColumnComment())) {
                genTableColumn.setColumnComment(request.getColumnComment());
            }
            if (StrUtil.isNotBlank(request.getColumnType())) {
                genTableColumn.setColumnType(request.getColumnType());
            }
            if (StrUtil.isNotBlank(request.getJavaType())) {
                genTableColumn.setJavaType(request.getJavaType());
            }
            if (StrUtil.isNotBlank(request.getJavaField())) {
                genTableColumn.setJavaField(request.getJavaField());
            }
            if (StrUtil.isNotBlank(request.getIsPk())) {
                genTableColumn.setIsPk(request.getIsPk());
            }
            if (StrUtil.isNotBlank(request.getIsIncrement())) {
                genTableColumn.setIsIncrement(request.getIsIncrement());
            }
            if (StrUtil.isNotBlank(request.getIsRequired())) {
                genTableColumn.setIsRequired(request.getIsRequired());
            }
            if (StrUtil.isNotBlank(request.getIsInsert())) {
                genTableColumn.setIsInsert(request.getIsInsert());
            }
            if (StrUtil.isNotBlank(request.getIsEdit())) {
                genTableColumn.setIsEdit(request.getIsEdit());
            }
            if (StrUtil.isNotBlank(request.getIsList())) {
                genTableColumn.setIsList(request.getIsList());
            }
            if (StrUtil.isNotBlank(request.getIsQuery())) {
                genTableColumn.setIsQuery(request.getIsQuery());
            }
            if (StrUtil.isNotBlank(request.getQueryType())) {
                genTableColumn.setQueryType(request.getQueryType());
            }
            if (StrUtil.isNotBlank(request.getHtmlType())) {
                genTableColumn.setHtmlType(request.getHtmlType());
            }
            if (StrUtil.isNotBlank(request.getDictType())) {
                genTableColumn.setDictType(request.getDictType());
            }
            if (request.getSort() != null) {
                genTableColumn.setSort(request.getSort());
            }
            if (StrUtil.isNotBlank(request.getColumnDefault())) {
                genTableColumn.setColumnDefault(request.getColumnDefault());
            }
            
            // 如果提供了任务ID，更新任务的extraInfo
            if (StrUtil.isNotBlank(request.getTaskId())) {
                String extraInfo = String.format("{\"tableName\":\"%s\",\"columnName\":\"%s\",\"columnComment\":\"%s\"}", 
                        request.getTableName(), 
                        genTableColumn.getColumnName(),
                        genTableColumn.getColumnComment());
                
                asyncTaskService.updateTaskExtraInfo(request.getTaskId(), extraInfo);
                logger.info("更新任务扩展信息: 正在优化字段 {}", genTableColumn.getColumnName());
            }
            
            boolean result = genTableColumnService.updateGenTableColumn(genTableColumn);
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("success", result);
            resultMap.put("columnId", request.getColumnId());
            resultMap.put("columnName", genTableColumn.getColumnName());
            resultMap.put("columnComment", genTableColumn.getColumnComment());
            resultMap.put("tableId", genTableColumn.getTableId());
            resultMap.put("taskId", request.getTaskId());
            resultMap.put("message", result ? "字段[" + genTableColumn.getColumnName() + "]修改成功" : "字段修改失败");
            
            return resultMap;
        } catch (Exception e) {
            logger.error("修改表字段信息失败", e);
            throw new ServiceException("修改表字段信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量修改表字段信息
     * 
     * @param request 批量更新字段请求对象
     * @return 操作结果
     */
    @Tool(name = "batchUpdateGenTableColumn", description = "根据 columnId 列表，批量更新代码生成器的表字段定义（GenTableColumn）的元数据。这只是修改元数据，不涉及物理数据库表。如果需要同步到数据库，请调用 syncTableToDatabase 方法。")
    public Map<String, Object> batchUpdateGenTableColumn(BatchUpdateGenTableColumnRequest request) {
        try {
            logger.info("batchUpdateGenTableColumn批量修改表字段信息: {}", request);
            
            if (request.getColumns() == null || request.getColumns().isEmpty()) {
                throw new ServiceException("批量修改表字段失败：字段信息列表为空");
            }
            
            String taskId = request.getTaskId();
            List<String> results = new ArrayList<>();
            int totalColumns = request.getColumns().size();
            int currentColumn = 0;
            
            // 更新任务初始进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 10);
                String extraInfo = String.format("{\"currentAction\":\"开始批量修改表字段\",\"totalColumns\":%d}", totalColumns);
                asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
            }
            
            for (UpdateGenTableColumnRequest columnRequest : request.getColumns()) {
                currentColumn++;
                
                // 设置任务ID，以便在单字段处理中更新任务状态
                columnRequest.setTaskId(taskId);
                
                try {
                    // 更新任务进度
                    if (StrUtil.isNotBlank(taskId)) {
                        int progress = 10 + (currentColumn * 90 / totalColumns);
                        asyncTaskService.updateTaskProgress(taskId, progress);
                        
                        String extraInfo = String.format("{\"currentAction\":\"修改表字段\",\"currentColumn\":%d,\"totalColumns\":%d,\"columnId\":\"%s\"}", 
                                currentColumn, totalColumns, columnRequest.getColumnId());
                        asyncTaskService.updateTaskExtraInfo(taskId, extraInfo);
                    }
                    
                    // 字符串转Long
                    Long columnIdLong;
                    try {
                        columnIdLong = Long.parseLong(columnRequest.getColumnId());
                    } catch (NumberFormatException e) {
                        logger.error("字段ID格式错误: {}", columnRequest.getColumnId());
                        results.add("字段ID[" + columnRequest.getColumnId() + "]格式错误");
                        continue;
                    }
                    
                    // 获取现有字段信息
                    GenTableColumn genTableColumn = genTableColumnService.selectGenTableColumnById(columnIdLong);
                    if (genTableColumn == null) {
                        results.add("字段定义不存在，columnId=" + columnRequest.getColumnId());
                        continue;
                    }
                    
                    // 更新字段信息（只更新非空字段）
                    if (StrUtil.isNotBlank(columnRequest.getTableId())) {
                        try {
                            genTableColumn.setTableId(Long.parseLong(columnRequest.getTableId()));
                        } catch (NumberFormatException e) {
                            logger.error("表ID格式错误: {}", columnRequest.getTableId());
                            results.add("字段[" + genTableColumn.getColumnName() + "]的表ID格式错误");
                            continue;
                        }
                    }
                    if (StrUtil.isNotBlank(columnRequest.getColumnName())) {
                        genTableColumn.setColumnName(columnRequest.getColumnName());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getColumnComment())) {
                        genTableColumn.setColumnComment(columnRequest.getColumnComment());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getColumnType())) {
                        genTableColumn.setColumnType(columnRequest.getColumnType());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getJavaType())) {
                        genTableColumn.setJavaType(columnRequest.getJavaType());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getJavaField())) {
                        genTableColumn.setJavaField(columnRequest.getJavaField());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getIsPk())) {
                        genTableColumn.setIsPk(columnRequest.getIsPk());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getIsIncrement())) {
                        genTableColumn.setIsIncrement(columnRequest.getIsIncrement());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getIsRequired())) {
                        genTableColumn.setIsRequired(columnRequest.getIsRequired());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getIsInsert())) {
                        genTableColumn.setIsInsert(columnRequest.getIsInsert());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getIsEdit())) {
                        genTableColumn.setIsEdit(columnRequest.getIsEdit());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getIsList())) {
                        genTableColumn.setIsList(columnRequest.getIsList());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getIsQuery())) {
                        genTableColumn.setIsQuery(columnRequest.getIsQuery());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getQueryType())) {
                        genTableColumn.setQueryType(columnRequest.getQueryType());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getHtmlType())) {
                        genTableColumn.setHtmlType(columnRequest.getHtmlType());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getDictType())) {
                        genTableColumn.setDictType(columnRequest.getDictType());
                    }
                    if (columnRequest.getSort() != null) {
                        genTableColumn.setSort(columnRequest.getSort());
                    }
                    if (StrUtil.isNotBlank(columnRequest.getColumnDefault())) {
                        genTableColumn.setColumnDefault(columnRequest.getColumnDefault());
                    }
                    
                    boolean result = genTableColumnService.updateGenTableColumn(genTableColumn);
                    results.add(result ? "字段[" + genTableColumn.getColumnName() + "]修改成功" : "字段[" + genTableColumn.getColumnName() + "]修改失败");
                    
                } catch (Exception e) {
                    logger.error("修改表字段信息失败: " + columnRequest.getColumnId(), e);
                    results.add("字段ID[" + columnRequest.getColumnId() + "]修改失败：" + e.getMessage());
                }
            }
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("taskId", taskId);
            resultMap.put("totalColumns", totalColumns);
            resultMap.put("processedColumns", currentColumn);
            resultMap.put("results", results);
            
            StringBuilder finalResult = new StringBuilder();
            finalResult.append("批量修改表字段完成，共处理").append(totalColumns).append("个字段：\n");
            for (String result : results) {
                finalResult.append(result).append("\n");
            }
            resultMap.put("message", finalResult.toString());
            
            // 更新任务完成状态
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskResult(taskId, finalResult.toString());
            }

            return resultMap;
        } catch (Exception e) {
            logger.error("批量修改表字段信息失败", e);
            if (StrUtil.isNotBlank(request.getTaskId())) {
                asyncTaskService.updateTaskError(request.getTaskId(), "批量修改表字段信息失败：" + e.getMessage());
            }
            throw new ServiceException("批量修改表字段信息失败：" + e.getMessage());
        }
    }



    /**
     * 分页查询代码生成器的表定义
     * 
     * @param tableName 表名称
     * @param tableComment 表描述
     * @param pageNum 页码
     * @param pageSize 每页记录数
     * @return 查询结果
     */
    @Tool(name = "getGenTableList", description = "分页查询代码生成器的表定义列表")
    public Map<String, Object> getGenTableList(String tableName, String tableComment, Integer pageNum, Integer pageSize) {
        try {
            logger.info("getGenTableList分页查询表定义列表, tableName: {}, tableComment: {}", tableName, tableComment);
            
            // 限制每页最大500条记录
            if (pageSize == null || pageSize > 500) {
                pageSize = 500;
            }
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            
            // 构建查询条件
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .select()
                    .from("gen_table");
            
            // 添加表名称条件
            if (StrUtil.isNotBlank(tableName)) {
                queryWrapper.and(new QueryColumn("table_name").like(tableName));
            }
            
            // 添加表描述条件
            if (StrUtil.isNotBlank(tableComment)) {
                queryWrapper.and(new QueryColumn("table_comment").like(tableComment));
            }
            
            // 添加排序
            queryWrapper.orderBy(new QueryColumn("create_time").desc());

            // 创建分页对象
            Page<GenTable> pageObj = Page.of(pageNum, pageSize);
            
            // 执行分页查询
            Page<GenTable> page = genTableService.page(pageObj, queryWrapper);
            
            Map<String, Object> result = new HashMap<>();
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalCount", page != null ? page.getTotalRow() : 0);
            result.put("totalPage", page != null ? page.getTotalPage() : 0);
            
            if (page == null || page.getRecords().isEmpty()) {
                result.put("message", "没有找到匹配的表定义");
                result.put("tableList", new ArrayList<>());
                return result;
            }
            
            List<GenTable> tableList = new ArrayList<>(page.getRecords());
            result.put("tableList", tableList);
            result.put("message", "查询表定义列表成功");
            
            return result;
        } catch (Exception e) {
            logger.error("查询表定义列表失败", e);
            throw new ServiceException("查询表定义列表失败：" + e.getMessage());
        }
    }
}