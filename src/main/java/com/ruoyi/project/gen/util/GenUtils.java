package com.ruoyi.project.gen.util;

import java.util.Arrays;
import org.apache.commons.lang3.RegExUtils;
import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;
import cn.hutool.core.util.StrUtil;

/**
 * 代码生成器 工具类
 * 
 * @author ruoyi
 */
public class GenUtils
{
    /**
     * 初始化表信息
     */
    public static void initTable(GenTable genTable, String operName)
    {
        genTable.setClassName(convertClassName(genTable.getTableName()));
        genTable.setPackageName(GenConstants.PACKAGE_NAME);
        genTable.setModuleName(getModuleName(GenConstants.PACKAGE_NAME));
        genTable.setBusinessName(getBusinessName(genTable.getTableName()));
        genTable.setFunctionName(replaceText(genTable.getTableComment()));
        genTable.setFunctionAuthor(GenConstants.AUTHOR);
        genTable.setCreateBy(operName);
    }

    /**
     * 初始化列属性字段（优化版本）
     */
    public static void initColumnField(GenTableColumn column, GenTable table)
    {
        String dataType = getDbType(column.getColumnType());
        String columnName = column.getColumnName();
        column.setTableId(table.getTableId());
        column.setCreateBy(table.getCreateBy());
        // 设置java字段名
        column.setJavaField(StrUtil.toCamelCase(columnName));
        // 设置默认类型
        column.setJavaType(GenConstants.TYPE_STRING);
        column.setQueryType(GenConstants.QUERY_EQ);

        // 设置Java类型和HTML类型
        setJavaTypeAndHtmlType(column, dataType);
        
        // 智能设置字段属性
        setSmartFieldProperties(column, columnName);
        
        // 设置HTML控件类型和查询类型
        setHtmlTypeAndQueryType(column, columnName);
    }
    
    /**
     * 优化列表显示字段数量，确保最多显示7个字段
     */
    public static void optimizeListColumns(java.util.List<GenTableColumn> columns) {
        if (columns == null || columns.isEmpty()) {
            return;
        }
        
        // 统计当前设置为列表显示的字段数量
        long listColumnCount = columns.stream()
                .filter(column -> "1".equals(column.getIsList()))
                .count();
        
        // 如果超过7个字段，需要优化
        if (listColumnCount > 7) {
            // 按优先级排序字段
            java.util.List<GenTableColumn> sortedColumns = columns.stream()
                    .filter(column -> "1".equals(column.getIsList()))
                    .sorted((c1, c2) -> {
                        // 优先级排序：创建时间 > 业务主要字段 > 其他字段
                        int priority1 = getListDisplayPriority(c1.getColumnName());
                        int priority2 = getListDisplayPriority(c2.getColumnName());
                        return Integer.compare(priority2, priority1); // 降序排列
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            // 保留前7个字段，其余设置为不显示
            for (int i = 7; i < sortedColumns.size(); i++) {
                sortedColumns.get(i).setIsList("0");
            }
        }
    }
    
    /**
     * 优化查询字段数量，确保最多设置4个查询条件
     */
    public static void optimizeQueryColumns(java.util.List<GenTableColumn> columns) {
        if (columns == null || columns.isEmpty()) {
            return;
        }
        
        // 统计当前设置为查询的字段数量
        long queryColumnCount = columns.stream()
                .filter(column -> "1".equals(column.getIsQuery()))
                .count();
        
        // 如果超过4个查询字段，需要优化
        if (queryColumnCount > 4) {
            // 按优先级排序字段
            java.util.List<GenTableColumn> sortedColumns = columns.stream()
                    .filter(column -> "1".equals(column.getIsQuery()))
                    .sorted((c1, c2) -> {
                        // 优先级排序：主要查询字段 > 其他字段
                        int priority1 = getQueryPriority(c1.getColumnName());
                        int priority2 = getQueryPriority(c2.getColumnName());
                        return Integer.compare(priority2, priority1); // 降序排列
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            // 保留前4个字段，其余设置为不查询
            for (int i = 4; i < sortedColumns.size(); i++) {
                sortedColumns.get(i).setIsQuery("0");
            }
        }
    }
    
    /**
     * 获取字段在列表中的显示优先级
     * 数值越大优先级越高
     */
    private static int getListDisplayPriority(String columnName) {
        // 创建时间最高优先级
        if (StrUtil.equalsAnyIgnoreCase(columnName, "create_time", "createTime", "created_at", "createdAt", "gmt_create", "gmtCreate")) {
            return 100;
        }
        
        // 名称字段高优先级
        if (StrUtil.endWithIgnoreCase(columnName, "name") || StrUtil.endWithIgnoreCase(columnName, "title")) {
            return 90;
        }
        
        // 状态字段
        if (StrUtil.endWithIgnoreCase(columnName, "status") || StrUtil.endWithIgnoreCase(columnName, "state")) {
            return 80;
        }
        
        // 类型字段
        if (StrUtil.endWithIgnoreCase(columnName, "type") || StrUtil.endWithIgnoreCase(columnName, "category")) {
            return 70;
        }
        
        // 排序字段
        if (StrUtil.endWithIgnoreCase(columnName, "sort") || StrUtil.endWithIgnoreCase(columnName, "order")) {
            return 60;
        }
        
        // 其他业务字段
        return 50;
    }
    
    /**
     * 获取字段在查询中的优先级
     * 数值越大优先级越高
     */
    private static int getQueryPriority(String columnName) {
        // 名称字段最高优先级
        if (StrUtil.endWithIgnoreCase(columnName, "name") || StrUtil.endWithIgnoreCase(columnName, "title")) {
            return 100;
        }
        
        // 状态字段高优先级
        if (StrUtil.endWithIgnoreCase(columnName, "status") || StrUtil.endWithIgnoreCase(columnName, "state")) {
            return 90;
        }
        
        // 类型字段
        if (StrUtil.endWithIgnoreCase(columnName, "type") || StrUtil.endWithIgnoreCase(columnName, "category")) {
            return 80;
        }
        
        // 时间字段
        if (StrUtil.equalsAnyIgnoreCase(columnName, "create_time", "createTime", "created_at", "createdAt", "gmt_create", "gmtCreate")) {
            return 70;
        }
        
        // 编码字段
        if (StrUtil.endWithIgnoreCase(columnName, "code") || StrUtil.endWithIgnoreCase(columnName, "no")) {
            return 60;
        }
        
        // 其他业务字段
        return 50;
    }
    
    /**
     * 设置Java类型和基础HTML类型
     */
    private static void setJavaTypeAndHtmlType(GenTableColumn column, String dataType) {
        if (arraysContains(GenConstants.COLUMNTYPE_STR, dataType) || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType))
        {
            // 字符串长度超过500设置为文本域
            Integer columnLength = getColumnLength(column.getColumnType());
            String htmlType = columnLength >= 500 || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType) ? GenConstants.HTML_TEXTAREA : GenConstants.HTML_INPUT;
            column.setHtmlType(htmlType);
        }
        else if (arraysContains(GenConstants.COLUMNTYPE_TIME, dataType))
        {
            column.setJavaType(GenConstants.TYPE_DATE);
            column.setHtmlType(GenConstants.HTML_DATETIME);
        }
        else if (arraysContains(GenConstants.COLUMNTYPE_NUMBER, dataType))
        {
            column.setHtmlType(GenConstants.HTML_INPUT);

            // 如果是浮点型 统一用BigDecimal
            String[] str = StrUtil.splitToArray(StrUtil.sub(column.getColumnType(), 0, column.getColumnType().indexOf("(")), " ");
            if (str != null && str.length == 2 && StrUtil.equals(str[1], "decimal"))
            {
                column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
            }
            // 如果是整形
            else if (str != null && str.length == 1 && (StrUtil.equals(str[0], "bigint") || StrUtil.equals(str[0], "int")))
            {
                column.setJavaType(GenConstants.TYPE_LONG);
            }
            // 如果是整形
            else if (str != null && str.length == 1 && StrUtil.equals(str[0], "integer"))
            {
                column.setJavaType(GenConstants.TYPE_INTEGER);
            }
            // 如果是长整形
            else if (str != null && str.length == 1 && StrUtil.equals(str[0], "tinyint"))
            {
                column.setJavaType(GenConstants.TYPE_INTEGER);
            }
        }
    }
    
    /**
     * 智能设置字段属性（插入、编辑、列表、查询）
     */
    private static void setSmartFieldProperties(GenTableColumn column, String columnName) {
        // 默认所有字段都需要插入
        column.setIsInsert(GenConstants.REQUIRE);
        
        // 主键字段特殊处理
        if (column.isPk()) {
            // 主键字段：不编辑、不在列表显示、不查询
            column.setIsEdit("0");
            column.setIsList("0");
            column.setIsQuery("0");
            // 如果是自增主键，不需要插入
            if ("1".equals(column.getIsIncrement())) {
                column.setIsInsert("0");
            }
            return;
        }
        
        // ID字段（非主键但名称为id或以_id结尾）特殊处理
        if (isIdField(columnName)) {
            // ID字段通常是外键，设置为下拉框，不在列表显示，可以查询
            column.setHtmlType(GenConstants.HTML_SELECT);
            column.setIsEdit(GenConstants.REQUIRE);
            column.setIsList("0"); // ID字段不在列表显示
            column.setIsQuery(GenConstants.REQUIRE);
            return;
        }
        
        // 创建时间字段 - 默认显示在列表中
        if (StrUtil.equalsAnyIgnoreCase(columnName, "create_time", "createTime", "created_at", "createdAt", "gmt_create", "gmtCreate")) {
            column.setIsInsert("0"); // 创建时间不可插入
            column.setIsEdit("0");   // 创建时间不可编辑
            column.setIsList("1");   // 创建时间默认在列表显示
            column.setIsQuery("1");  // 创建时间可以查询
            return;
        }
        
        // 修改时间字段 - 默认不显示在列表中
        if (StrUtil.equalsAnyIgnoreCase(columnName, "update_time", "updateTime", "updated_at", "updatedAt", "gmt_modified", "gmtModified", "modify_time", "modifyTime")) {
            column.setIsInsert("0"); // 修改时间不可插入
            column.setIsEdit("0");   // 修改时间不可编辑
            column.setIsList("0");   // 修改时间默认不在列表显示
            column.setIsQuery("1");  // 修改时间可以查询
            return;
        }
        
        // 创建人字段 - 默认不显示在列表中
        if (StrUtil.equalsAnyIgnoreCase(columnName, "create_by", "createBy", "created_by", "createdBy", "creator", "create_user", "createUser")) {
            column.setIsInsert("0"); // 创建人不可插入
            column.setIsEdit("0");   // 创建人不可编辑
            column.setIsList("0");   // 创建人默认不在列表显示
            column.setIsQuery("1");  // 创建人可以查询
            return;
        }
        
        // 修改人字段 - 默认不显示在列表中
        if (StrUtil.equalsAnyIgnoreCase(columnName, "update_by", "updateBy", "updated_by", "updatedBy", "modifier", "update_user", "updateUser", "modify_by", "modifyBy")) {
            column.setIsInsert("0"); // 修改人不可插入
            column.setIsEdit("0");   // 修改人不可编辑
            column.setIsList("0");   // 修改人默认不在列表显示
            column.setIsQuery("1");  // 修改人可以查询
            return;
        }
        
        // 备注字段 - 默认不显示在列表中
        if (StrUtil.equalsAnyIgnoreCase(columnName, "remark", "remarks", "note", "notes", "comment", "comments", "description", "desc")) {
            column.setIsInsert(GenConstants.REQUIRE);
            column.setIsEdit(GenConstants.REQUIRE);
            column.setIsList("0");   // 备注字段默认不在列表显示
            column.setIsQuery("0");  // 备注字段通常不查询
            return;
        }
        
        // 删除标志字段
        if (isDeleteFlag(columnName)) {
            column.setIsInsert("0");
            column.setIsEdit("0");
            column.setIsList("0");
            column.setIsQuery("0");
            return;
        }
        
        // 普通业务字段
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_EDIT, columnName)) {
            column.setIsEdit(GenConstants.REQUIRE);
        }
        
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_LIST, columnName)) {
            column.setIsList(GenConstants.REQUIRE);
        }
        
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_QUERY, columnName)) {
            column.setIsQuery(GenConstants.REQUIRE);
        }
    }
    
    /**
     * 设置HTML控件类型和查询类型
     */
    private static void setHtmlTypeAndQueryType(GenTableColumn column, String columnName) {
        // 设置查询类型
        setQueryType(column, columnName);
        
        // 设置HTML控件类型
        setHtmlControlType(column, columnName);
        
        // 特殊字段处理
        handleSpecialFields(column, columnName);
    }
    
    /**
     * 设置查询类型
     */
    private static void setQueryType(GenTableColumn column, String columnName) {
        // 模糊查询字段
        if (endsWithAny(columnName, GenConstants.LIKE_QUERY_SUFFIXES)) {
            column.setQueryType(GenConstants.QUERY_LIKE);
        }
        // 数值范围查询字段
        else if (endsWithAny(columnName, GenConstants.RANGE_QUERY_SUFFIXES)) {
            column.setQueryType("BETWEEN");
        }
        // 时间范围查询字段
        else if (endsWithAny(columnName, GenConstants.TIME_RANGE_SUFFIXES)) {
            column.setQueryType("BETWEEN");
        }
    }
    
    /**
     * 设置HTML控件类型
     */
    private static void setHtmlControlType(GenTableColumn column, String columnName) {
        // 状态字段设置单选框
        if (endsWithAny(columnName, GenConstants.STATUS_SUFFIXES)) {
            column.setHtmlType(GenConstants.HTML_RADIO);
        }
        // 类型、性别、级别等字段设置下拉框
        else if (endsWithAny(columnName, GenConstants.TYPE_SUFFIXES)) {
            column.setHtmlType(GenConstants.HTML_SELECT);
        }
        // 图片字段设置图片上传控件
        else if (endsWithAny(columnName, GenConstants.IMAGE_SUFFIXES)) {
            column.setHtmlType(GenConstants.HTML_IMAGE_UPLOAD);
        }
        // 文件字段设置文件上传控件
        else if (endsWithAny(columnName, GenConstants.FILE_SUFFIXES)) {
            column.setHtmlType(GenConstants.HTML_FILE_UPLOAD);
        }
        // 内容字段设置富文本控件
        else if (endsWithAny(columnName, GenConstants.RICH_TEXT_SUFFIXES)) {
            column.setHtmlType(GenConstants.HTML_EDITOR);
        }
    }
    
    /**
     * 处理特殊字段
     */
    private static void handleSpecialFields(GenTableColumn column, String columnName) {
        // 排序字段通常不需要查询
        if (endsWithAny(columnName, GenConstants.SORT_SUFFIXES)) {
            column.setIsQuery("0");
        }
        
        // 密码字段特殊处理
        if (StrUtil.containsAnyIgnoreCase(columnName, "password", "pwd", "pass")) {
            column.setHtmlType(GenConstants.HTML_INPUT);
            column.setIsList("0"); // 密码字段不在列表显示
            column.setIsQuery("0"); // 密码字段不查询
        }
        
        // 邮箱字段
        if (StrUtil.containsAnyIgnoreCase(columnName, "email", "mail")) {
            column.setQueryType(GenConstants.QUERY_LIKE);
        }
        
        // 电话号码字段
        if (StrUtil.containsAnyIgnoreCase(columnName, "phone", "mobile", "tel")) {
            column.setQueryType(GenConstants.QUERY_LIKE);
        }
        
        // URL字段
        if (StrUtil.containsAnyIgnoreCase(columnName, "url", "link", "href")) {
            column.setHtmlType(GenConstants.HTML_INPUT);
            column.setIsQuery("0");
        }
    }
    
    /**
     * 判断字段名是否以指定后缀结尾（忽略大小写）
     */
    private static boolean endsWithAny(String columnName, String[] suffixes) {
        for (String suffix : suffixes) {
            if (StrUtil.endWithIgnoreCase(columnName, suffix)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断是否为ID字段（外键字段）
     */
    private static boolean isIdField(String columnName) {
        return StrUtil.equalsIgnoreCase(columnName, "id") ||
               StrUtil.endWithIgnoreCase(columnName, "_id") ||
               StrUtil.endWithIgnoreCase(columnName, "Id");
    }
    
    /**
     * 判断是否为系统维护字段
     */
    private static boolean isSystemField(String columnName) {
        return StrUtil.equalsAnyIgnoreCase(columnName, 
            "create_by", "create_time", "update_by", "update_time", 
            "createBy", "createTime", "updateBy", "updateTime");
    }
    
    /**
     * 判断是否为删除标志字段
     */
    private static boolean isDeleteFlag(String columnName) {
        return StrUtil.equalsAnyIgnoreCase(columnName, 
            "del_flag", "delFlag", "deleted", "is_deleted", "isDeleted");
    }

    /**
     * 校验数组是否包含指定值
     * 
     * @param arr 数组
     * @param targetValue 值
     * @return 是否包含
     */
    public static boolean arraysContains(String[] arr, String targetValue)
    {
        return Arrays.asList(arr).contains(targetValue);
    }

    /**
     * 获取模块名
     * 
     * @param packageName 包名
     * @return 模块名
     */
    public static String getModuleName(String packageName)
    {
        int lastIndex = packageName.lastIndexOf(".");
        int nameLength = packageName.length();
        return StrUtil.sub(packageName, lastIndex + 1, nameLength);
    }

    /**
     * 获取业务名
     * 
     * @param tableName 表名
     * @return 业务名
     */
    public static String getBusinessName(String tableName)
    {
        int lastIndex = tableName.lastIndexOf("_");
        int nameLength = tableName.length();
        return StrUtil.sub(tableName, lastIndex + 1, nameLength);
    }

    /**
     * 表名转换成Java类名
     * 
     * @param tableName 表名称
     * @return 类名
     */
    public static String convertClassName(String tableName)
    {
        boolean autoRemovePre = GenConstants.AUTO_REMOVE_PRE;
        String tablePrefix = GenConstants.TABLE_PREFIX;
        if (autoRemovePre && StrUtil.isNotEmpty(tablePrefix))
        {
            String[] searchList = StrUtil.splitToArray(tablePrefix, ",");
            tableName = replaceFirst(tableName, searchList);
        }
        return StrUtil.upperFirst(StrUtil.toCamelCase(tableName));
    }

    /**
     * 批量替换前缀
     * 
     * @param replacementm 替换值
     * @param searchList 替换列表
     * @return
     */
    public static String replaceFirst(String replacementm, String[] searchList)
    {
        String text = replacementm;
        for (String searchString : searchList)
        {
            if (StrUtil.startWith(text, searchString))
            {
                text = StrUtil.replaceFirst(text, searchString, "");
                break;
            }
        }
        return text;
    }

    /**
     * 关键字替换
     * 
     * @param text 需要被替换的名字
     * @return 替换后的名字
     */
    public static String replaceText(String text)
    {
        return RegExUtils.replaceAll(text, "(?:表|若依)", "");
    }

    /**
     * 获取数据库类型字段
     * 
     * @param columnType 列类型
     * @return 截取后的列类型
     */
    public static String getDbType(String columnType)
    {
        if (StrUtil.indexOf(columnType, '(') > 0)
        {
            return StrUtil.subBefore(columnType, "(", false);
        }
        else
        {
            return columnType;
        }
    }

    /**
     * 获取字段长度
     * 
     * @param columnType 列类型
     * @return 截取后的列类型
     */
    public static Integer getColumnLength(String columnType)
    {
        if (StrUtil.indexOf(columnType, '(') > 0)
        {
            String length = StrUtil.subBetween(columnType, "(", ")");
            return Integer.valueOf(length);
        }
        else
        {
            return 0;
        }
    }
}
