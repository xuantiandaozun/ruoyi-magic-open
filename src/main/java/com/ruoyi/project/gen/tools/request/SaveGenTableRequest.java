package com.ruoyi.project.gen.tools.request;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 保存表信息请求类
 * 用于AI调用时避免Long类型数据溢出问题
 * 
 * @author ruoyi
 */
@Data
@Accessors(chain = true)
public class SaveGenTableRequest {
    
    /** 表名称 */
    private String tableName;
    
    /** 表描述 */
    private String tableComment;
    
    /** 关联子表的表名 */
    private String subTableName;
    
    /** 子表关联的外键名 */
    private String subTableFkName;
    
    /** 实体类名称 */
    private String className;
    
    /** 使用的模板（crud单表操作 tree树表操作） */
    private String tplCategory;
    
    /** 前端类型（element-ui模版 element-plus模版） */
    private String tplWebType;
    
    /** 生成包路径 */
    private String packageName;
    
    /** 生成模块名 */
    private String moduleName;
    
    /** 生成业务名 */
    private String businessName;
    
    /** 生成功能名 */
    private String functionName;
    
    /** 生成功能作者 */
    private String functionAuthor;
    
    /** 生成代码方式（0zip压缩包 1自定义路径） */
    private String genType;
    
    /** 生成路径（不填默认项目路径） */
    private String genPath;
    
    /** 其它生成选项 */
    private String options;
    
    /** 数据源名称 */
    private String dataSource;
    
    /** 列信息列表 */
    private List<Object> columns;
    
    /** 任务ID */
    private String taskId;
}