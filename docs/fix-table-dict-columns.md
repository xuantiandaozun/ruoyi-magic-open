# 修复代码生成器表字典字段缺失问题

## 问题描述

在删除代码生成器记录时出现以下错误：

```
org.springframework.jdbc.BadSqlGrammarException: 
### Error querying database.  Cause: java.sql.SQLSyntaxErrorException: Unknown column 'table_dict_name' in 'field list'
```

## 问题原因

数据库表 `gen_table_column` 中缺少以下字段：
- `table_dict_name` - 表字典名称
- `table_dict_label_field` - 表字典显示字段
- `table_dict_value_field` - 表字典值字段
- `table_dict_condition` - 表字典查询条件

但是实体类 `GenTableColumn.java` 中已经定义了这些字段，导致 MyBatis-Flex 查询时出现字段不匹配的问题。

## 解决方案

### 方案一：执行 SQL 脚本（推荐）

执行 `sql/add_table_dict_columns.sql` 文件中的 SQL 语句：

```sql
-- 为 gen_table_column 表添加表字典相关字段
USE ry_vue;

-- 添加表字典名称字段
ALTER TABLE `gen_table_column` ADD COLUMN `table_dict_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '表字典名称' AFTER `column_default`;

-- 添加表字典显示字段
ALTER TABLE `gen_table_column` ADD COLUMN `table_dict_label_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '表字典显示字段' AFTER `table_dict_name`;

-- 添加表字典值字段
ALTER TABLE `gen_table_column` ADD COLUMN `table_dict_value_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '表字典值字段' AFTER `table_dict_label_field`;

-- 添加表字典查询条件字段
ALTER TABLE `gen_table_column` ADD COLUMN `table_dict_condition` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '表字典查询条件(JSON格式)' AFTER `table_dict_value_field`;
```

### 方案二：重新创建数据库

如果是新环境，可以直接使用更新后的 `sql/ry-vue.sql` 文件重新创建数据库，该文件已经包含了完整的表结构。

## 验证修复

执行以下 SQL 验证字段是否添加成功：

```sql
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'ry_vue' 
AND TABLE_NAME = 'gen_table_column' 
AND COLUMN_NAME IN ('table_dict_name', 'table_dict_label_field', 'table_dict_value_field', 'table_dict_condition')
ORDER BY ORDINAL_POSITION;
```

## 功能说明

这些字段用于支持代码生成器的表字典功能：
- **table_dict_name**: 指定关联的字典表名称
- **table_dict_label_field**: 字典表中用作显示文本的字段名
- **table_dict_value_field**: 字典表中用作值的字段名
- **table_dict_condition**: 字典查询的附加条件（JSON格式）

修复后，代码生成器将能够正常删除记录，并且表字典功能也能正常使用。