package com.ruoyi.project.gen.domain;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.framework.web.domain.BaseEntity;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 业务表 gen_table
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("gen_table")
public class GenTable extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 编号 */
    @Id(keyType = KeyType.Auto)
    private Long tableId;

    /** 表名称 */
    @NotBlank(message = "表名称不能为空")
    private String tableName;

    /** 表描述 */
    @NotBlank(message = "表描述不能为空")
    private String tableComment;

    /** 关联父表的表名 */
    private String subTableName;

    /** 本表关联父表的外键名 */
    private String subTableFkName;

    /** 实体类名称(首字母大写) */
    @NotBlank(message = "实体类名称不能为空")
    private String className;

    /** 使用的模板（crud单表操作 tree树表操作 sub主子表操作） */
    private String tplCategory;

    /** 前端类型（element-ui模版 element-plus模版） */
    private String tplWebType;

    /** 生成包路径 */
    @NotBlank(message = "生成包路径不能为空")
    private String packageName;

    /** 生成模块名 */
    @NotBlank(message = "生成模块名不能为空")
    private String moduleName;

    /** 生成业务名 */
    @NotBlank(message = "生成业务名不能为空")
    private String businessName;

    /** 生成功能名 */
    @NotBlank(message = "生成功能名不能为空")
    private String functionName;

    /** 生成作者 */
    @NotBlank(message = "作者不能为空")
    private String functionAuthor;

    /** 生成代码方式（0zip压缩包 1自定义路径） */
    private String genType;

    /** 生成路径（不填默认项目路径） */
    private String genPath;
    
    /** Vue代码生成路径 */
    private String vuePath;
    
    /** 数据源名称 */
    private String dataSource;

    /** 主键信息 */
    @Column(ignore = true)
    private GenTableColumn pkColumn;

    /** 子表信息 */
    @Column(ignore = true)
    private GenTable subTable;

    /** 表列信息 */
    @Valid
    @Column(ignore = true)
    private List<GenTableColumn> columns;

    /** 其它生成选项 */
    private String options;

    /** 树编码字段 */
    @Column(ignore = true)
    private String treeCode;

    /** 树父编码字段 */
    @Column(ignore = true)
    private String treeParentCode;

    /** 树名称字段 */
    @Column(ignore = true)
    private String treeName;

    /** 上级菜单ID字段 */
    @Column
    private Long parentMenuId;

    /** 上级菜单名称字段 */
    @Column(ignore = true)
    private String parentMenuName;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete=true)
    private String delFlag;

    public boolean isSub() {
        return isSub(this.tplCategory);
    }

    public static boolean isSub(String tplCategory) {
        return tplCategory != null && StrUtil.equals(GenConstants.TPL_SUB, tplCategory);
    }

    public boolean isTree() {
        return isTree(this.tplCategory);
    }

    public static boolean isTree(String tplCategory) {
        return tplCategory != null && StrUtil.equals(GenConstants.TPL_TREE, tplCategory);
    }

    public boolean isCrud() {
        return isCrud(this.tplCategory);
    }

    public static boolean isCrud(String tplCategory) {
        return tplCategory != null && StrUtil.equals(GenConstants.TPL_CRUD, tplCategory);
    }

    public boolean isSuperColumn(String javaField) {
        return isSuperColumn(this.tplCategory, javaField);
    }

    public static boolean isSuperColumn(String tplCategory, String javaField) {
        if (isTree(tplCategory)) {
            return StrUtil.equalsAnyIgnoreCase(javaField,
                    ArrayUtils.addAll(GenConstants.TREE_ENTITY, GenConstants.BASE_ENTITY));
        }
        return StrUtil.equalsAnyIgnoreCase(javaField, GenConstants.BASE_ENTITY);
    }
}