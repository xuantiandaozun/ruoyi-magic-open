package com.ruoyi.project.gen.tools.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 更新表字段请求类
 * 用于AI调用时避免Long类型数据溢出问题
 * 
 * @author ruoyi
 */
@Data
@Accessors(chain = true)
public class UpdateGenTableColumnRequest {
    
    /** 字段ID（字符串格式，避免Long溢出） */
    private String columnId;
    
    /** 归属表编号（字符串格式，避免Long溢出） */
    private String tableId;
    
    /** 列名称 */
    private String columnName;
    
    /** 列描述 */
    private String columnComment;
    
    /** 列类型 */
    private String columnType;
    
    /** JAVA类型 */
    private String javaType;
    
    /** JAVA字段名 */
    private String javaField;
    
    /** 是否主键（1是） */
    private String isPk;
    
    /** 是否自增（1是） */
    private String isIncrement;
    
    /** 是否必填（1是） */
    private String isRequired;
    
    /** 是否为插入字段（1是） */
    private String isInsert;
    
    /** 是否编辑字段（1是） */
    private String isEdit;
    
    /** 是否列表字段（1是） */
    private String isList;
    
    /** 是否查询字段（1是） */
    private String isQuery;
    
    /** 查询方式（EQ等于、NE不等于、GT大于、LT小于、LIKE模糊、BETWEEN范围） */
    private String queryType;
    
    /** 显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件） */
    private String htmlType;
    
    /** 字典类型 */
    private String dictType;
    
    /** 排序 */
    private Integer sort;
    
    /** 默认值 */
    private String columnDefault;
    
    /** 任务ID */
    private String taskId;
    
    /** 表名 */
    private String tableName;
}