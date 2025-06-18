package com.ruoyi.project.gen.tools;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;
import com.ruoyi.project.gen.service.IAsyncTaskService;
import com.ruoyi.project.gen.service.IGenTableColumnService;
import com.ruoyi.project.gen.service.IGenTableService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 数据库表操作工具
 * 提供给AI模型调用的工具，用于创建和管理数据库表
 */
@Component
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
                genTableColumnService.insertGenTableColumn(column);
            }

            // 更新任务进度
            if (StrUtil.isNotBlank(taskId)) {
                asyncTaskService.updateTaskProgress(taskId, 80);
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
}