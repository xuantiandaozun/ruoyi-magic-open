package com.ruoyi.project.gen.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 代码生成业务字段表 gen_table_column
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("gen_table_column")
public class GenTableColumn extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 编号 */
    @Id(keyType = KeyType.Auto)
    private Long columnId;

    /** 归属表编号 */
    private Long tableId;

    /** 列名称 */
    private String columnName;

    /** 列描述 */
    private String columnComment;

    /** 列类型 */
    private String columnType;

    /** JAVA类型 */
    private String javaType;

    /** JAVA字段名 */
    @NotBlank(message = "Java属性不能为空")
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

    /** 表字典名称 */
    private String tableDictName;

    /** 表字典显示字段 */
    private String tableDictLabelField;

    /** 表字典值字段 */
    private String tableDictValueField;

    /** 表字典查询条件(JSON格式) */
    private String tableDictCondition;

    public boolean isPk()
    {
        return isPk(this.isPk);
    }

    public boolean isPk(String isPk)
    {
        return isPk != null && StrUtil.equals("1", isPk);
    }

    public boolean isIncrement()
    {
        return isIncrement(this.isIncrement);
    }

    public boolean isIncrement(String isIncrement)
    {
        return isIncrement != null && StrUtil.equals("1", isIncrement);
    }

    public boolean isRequired()
    {
        return isRequired(this.isRequired);
    }

    public boolean isRequired(String isRequired)
    {
        return isRequired != null && StrUtil.equals("1", isRequired);
    }

    public boolean isInsert()
    {
        return isInsert(this.isInsert);
    }

    public boolean isInsert(String isInsert)
    {
        return isInsert != null && StrUtil.equals("1", isInsert);
    }

    public boolean isEdit()
    {
        return isEdit(this.isEdit);
    }

    public boolean isEdit(String isEdit)
    {
        return isEdit != null && StrUtil.equals("1", isEdit);
    }

    public boolean isList()
    {
        return isList(this.isList);
    }

    public boolean isList(String isList)
    {
        return isList != null && StrUtil.equals("1", isList);
    }

    public boolean isQuery()
    {
        return isQuery(this.isQuery);
    }

    public boolean isQuery(String isQuery)
    {
        return isQuery != null && StrUtil.equals("1", isQuery);
    }

    public boolean isSuperColumn()
    {
        return isSuperColumn(this.javaField);
    }

    public static boolean isSuperColumn(String javaField)
    {
        return StrUtil.equalsAnyIgnoreCase(javaField,
                // BaseEntity
                "createBy", "createTime", "updateBy", "updateTime", "remark",
                // TreeEntity
                "parentName", "parentId", "orderNum", "ancestors");
    }

    public boolean isUsableColumn()
    {
        return isUsableColumn(javaField);
    }

    public static boolean isUsableColumn(String javaField)
    {
        // isSuperColumn()中的名单用于避免生成多余代码
        return StrUtil.equalsAnyIgnoreCase(javaField, "parentId", "orderNum", "remark");
    }

    public String readConverterExp()
    {
        String remarks = StrUtil.subBetween(this.columnComment, "（", "）");
        StringBuffer sb = new StringBuffer();
        if (StrUtil.isNotEmpty(remarks))
        {
            for (String value : remarks.split(" "))
            {
                if (StrUtil.isNotEmpty(value))
                {
                    Object startStr = value.subSequence(0, 1);
                    String endStr = value.substring(1);
                    sb.append("").append(startStr).append("=").append(endStr).append(",");
                }
            }
            return sb.deleteCharAt(sb.length() - 1).toString();
        }
        else
        {
            return this.columnComment;
        }
    }

    /**
     * 是否配置了表字典
     */
    public boolean hasTableDict()
    {
        return StrUtil.isNotBlank(this.tableDictName);
    }

    /**
     * 获取表字典显示字段（模板兼容方法）
     */
    public String getTableDictText()
    {
        return this.tableDictLabelField;
    }

    /**
     * 获取表字典值字段（模板兼容方法）
     */
    public String getTableDictValue()
    {
        return this.tableDictValueField;
    }
}