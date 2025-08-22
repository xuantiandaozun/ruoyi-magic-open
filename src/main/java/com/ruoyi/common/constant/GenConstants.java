package com.ruoyi.common.constant;

/**
 * 代码生成通用常量
 * 
 * @author ruoyi
 */
public class GenConstants
{
    /** 单表（增删改查） */
    public static final String TPL_CRUD = "crud";

    /** 树表（增删改查） */
    public static final String TPL_TREE = "tree";

    /** 主子表（增删改查） */
    public static final String TPL_SUB = "sub";

    /** 树编码字段 */
    public static final String TREE_CODE = "treeCode";

    /** 树父编码字段 */
    public static final String TREE_PARENT_CODE = "treeParentCode";

    /** 树名称字段 */
    public static final String TREE_NAME = "treeName";

    /** 上级菜单ID字段 */
    public static final String PARENT_MENU_ID = "parentMenuId";

    /** 上级菜单名称字段 */
    public static final String PARENT_MENU_NAME = "parentMenuName";

    /** 是否自动去除表前缀 */
    public static final boolean AUTO_REMOVE_PRE = true;

    /** 表前缀(类名不会包含表前缀) */
    public static final String TABLE_PREFIX = "sys_";

    /** 默认包名 */
    public static final String PACKAGE_NAME = "com.ruoyi.project.system";

    /** 默认作者 */
    public static final String AUTHOR = "ruoyi";

    /** 数据库字符串类型 */
    public static final String[] COLUMNTYPE_STR = { "char", "varchar", "nvarchar", "varchar2" };

    /** 数据库文本类型 */
    public static final String[] COLUMNTYPE_TEXT = { "tinytext", "text", "mediumtext", "longtext" };

    /** 数据库时间类型 */
    public static final String[] COLUMNTYPE_TIME = { "datetime", "time", "date", "timestamp" };

    /** 数据库数字类型 */
    public static final String[] COLUMNTYPE_NUMBER = { "tinyint", "smallint", "mediumint", "int", "number", "integer",
            "bit", "bigint", "float", "double", "decimal" };

    /** 页面不需要编辑字段 */
    public static final String[] COLUMNNAME_NOT_EDIT = { "id", "create_by", "create_time", "del_flag" };

    /** 页面不需要显示的列表字段 */
    public static final String[] COLUMNNAME_NOT_LIST = { "id", "create_by", "create_time", "del_flag", "update_by",
            "update_time" };

    /** 页面不需要查询字段 */
    public static final String[] COLUMNNAME_NOT_QUERY = { "id", "create_by", "create_time", "del_flag", "update_by",
            "update_time", "remark" };

    /** 外键字段后缀模式 */
    public static final String[] FOREIGN_KEY_SUFFIXES = { "_id", "Id" };

    /** 系统维护字段 */
    public static final String[] SYSTEM_FIELDS = { "create_by", "create_time", "update_by", "update_time", 
            "createBy", "createTime", "updateBy", "updateTime" };

    /** 删除标志字段 */
    public static final String[] DELETE_FLAG_FIELDS = { "del_flag", "delFlag", "deleted", "is_deleted", "isDeleted" };

    /** 状态类字段后缀 */
    public static final String[] STATUS_SUFFIXES = { "status", "state", "flag" };

    /** 类型类字段后缀 */
    public static final String[] TYPE_SUFFIXES = { "type", "sex", "gender", "level", "grade", "category" };

    /** 图片类字段后缀 */
    public static final String[] IMAGE_SUFFIXES = { "image", "img", "avatar", "photo", "pic" };

    /** 文件类字段后缀 */
    public static final String[] FILE_SUFFIXES = { "file", "attachment", "document" };

    /** 富文本类字段后缀 */
    public static final String[] RICH_TEXT_SUFFIXES = { "content", "detail", "html", "rich" };

    /** 排序类字段后缀 */
    public static final String[] SORT_SUFFIXES = { "sort", "order", "seq" };

    /** 模糊查询字段后缀 */
    public static final String[] LIKE_QUERY_SUFFIXES = { "name", "title", "desc", "description" };

    /** 数值范围查询字段后缀 */
    public static final String[] RANGE_QUERY_SUFFIXES = { "amount", "price", "cost", "fee", "count", "num", "quantity" };

    /** 时间范围查询字段后缀 */
    public static final String[] TIME_RANGE_SUFFIXES = { "time", "date", "datetime" };

    /** Entity基类字段 */
    public static final String[] BASE_ENTITY = { "createBy", "createTime", "updateBy", "updateTime", "remark" };

    /** Tree基类字段 */
    public static final String[] TREE_ENTITY = { "parentName", "parentId", "orderNum", "ancestors", "children" };

    /** 文本框 */
    public static final String HTML_INPUT = "input";

    /** 文本域 */
    public static final String HTML_TEXTAREA = "textarea";

    /** 下拉框 */
    public static final String HTML_SELECT = "select";

    /** 单选框 */
    public static final String HTML_RADIO = "radio";

    /** 复选框 */
    public static final String HTML_CHECKBOX = "checkbox";

    /** 日期控件 */
    public static final String HTML_DATETIME = "datetime";

    /** 图片上传控件 */
    public static final String HTML_IMAGE_UPLOAD = "imageUpload";

    /** 文件上传控件 */
    public static final String HTML_FILE_UPLOAD = "fileUpload";

    /** 富文本控件 */
    public static final String HTML_EDITOR = "editor";

    /** 字符串类型 */
    public static final String TYPE_STRING = "String";

    /** 整型 */
    public static final String TYPE_INTEGER = "Integer";

    /** 长整型 */
    public static final String TYPE_LONG = "Long";

    /** 浮点型 */
    public static final String TYPE_DOUBLE = "Double";

    /** 高精度计算类型 */
    public static final String TYPE_BIGDECIMAL = "BigDecimal";

    /** 时间类型 */
    public static final String TYPE_DATE = "Date";

    /** 模糊查询 */
    public static final String QUERY_LIKE = "LIKE";

    /** 相等查询 */
    public static final String QUERY_EQ = "EQ";

    /** 需要 */
    public static final String REQUIRE = "1";
}
