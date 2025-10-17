/*
 修复工作流定时任务表结构脚本
 
 问题：ai_workflow_schedule表缺少user_id字段
 解决：添加user_id字段
 
 Date: 2025-01-17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 修复ai_workflow_schedule表结构
-- ========================================

-- 检查并添加user_id字段（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'ai_workflow_schedule' 
     AND COLUMN_NAME = 'user_id') = 0,
    'ALTER TABLE `ai_workflow_schedule` ADD COLUMN `user_id` bigint NULL DEFAULT NULL COMMENT ''创建者用户ID'' AFTER `del_flag`;',
    'SELECT ''user_id字段已存在，无需添加'' as message;'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加workflow_name字段（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'ai_workflow_schedule' 
     AND COLUMN_NAME = 'workflow_name') = 0,
    'ALTER TABLE `ai_workflow_schedule` ADD COLUMN `workflow_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT ''工作流名称（关联查询字段）'' AFTER `user_id`;',
    'SELECT ''workflow_name字段已存在，无需添加'' as message;'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加execution_count字段（如果不存在）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'ai_workflow_schedule' 
     AND COLUMN_NAME = 'execution_count') = 0,
    'ALTER TABLE `ai_workflow_schedule` ADD COLUMN `execution_count` int NULL DEFAULT 0 COMMENT ''执行次数统计'' AFTER `workflow_name`;',
    'SELECT ''execution_count字段已存在，无需添加'' as message;'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 执行完成提示
-- ========================================
SELECT '工作流定时任务表结构修复完成！' as message;