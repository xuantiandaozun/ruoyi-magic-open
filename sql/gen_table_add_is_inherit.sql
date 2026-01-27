-- 代码生成器优化 - 添加继承控制字段
-- 根据数据库表设计规范，支持表备注中的 [不继承] 和 [继承] 标记

-- 为 gen_table 表添加 is_inherit 字段
ALTER TABLE `gen_table` ADD COLUMN `is_inherit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '1' COMMENT '是否继承基类（1继承 0不继承）' AFTER `data_source`;
