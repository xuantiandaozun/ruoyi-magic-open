/*
 工作流定时任务功能数据库修改脚本
 
 功能说明：
 1. 为ai_workflow表添加定时调度相关字段
 2. 创建ai_workflow_schedule定时调度配置表
 3. 创建ai_workflow_schedule_log定时调度执行日志表
 4. 为ai_workflow_execution表添加调度相关字段
 5. 添加相关系统字典数据
 
 执行顺序：按照文件中的顺序依次执行
 
 Date: 2025-01-17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 1. 为ai_workflow表添加定时任务相关字段
-- ========================================

ALTER TABLE `ai_workflow` 
ADD COLUMN `schedule_enabled` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '定时调度启用状态（0=禁用 1=启用）' AFTER `enabled`,
ADD COLUMN `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'cron执行表达式' AFTER `schedule_enabled`,
ADD COLUMN `next_execution_time` datetime NULL DEFAULT NULL COMMENT '下次执行时间' AFTER `cron_expression`,
ADD COLUMN `last_execution_time` datetime NULL DEFAULT NULL COMMENT '上次执行时间' AFTER `next_execution_time`,
ADD COLUMN `execution_count` bigint NULL DEFAULT 0 COMMENT '执行次数统计' AFTER `last_execution_time`,
ADD COLUMN `max_execution_count` bigint NULL DEFAULT NULL COMMENT '最大执行次数（NULL表示无限制）' AFTER `execution_count`,
ADD COLUMN `schedule_start_time` datetime NULL DEFAULT NULL COMMENT '调度开始时间' AFTER `max_execution_count`,
ADD COLUMN `schedule_end_time` datetime NULL DEFAULT NULL COMMENT '调度结束时间' AFTER `schedule_start_time`,
ADD COLUMN `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）' AFTER `schedule_end_time`,
ADD COLUMN `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）' AFTER `misfire_policy`;

-- 添加索引
ALTER TABLE `ai_workflow` 
ADD INDEX `idx_schedule_enabled`(`schedule_enabled` ASC) USING BTREE,
ADD INDEX `idx_next_execution_time`(`next_execution_time` ASC) USING BTREE,
ADD INDEX `idx_cron_expression`(`cron_expression` ASC) USING BTREE;

-- ========================================
-- 2. 创建工作流定时任务配置表
-- ========================================

DROP TABLE IF EXISTS `ai_workflow_schedule`;
CREATE TABLE `ai_workflow_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调度配置ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `schedule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度任务名称',
  `schedule_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调度任务描述',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'cron执行表达式',
  `timezone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'Asia/Shanghai' COMMENT '时区设置',
  `enabled` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '启用状态（0=禁用 1=启用）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0=正常 1=暂停）',
  `input_data_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '输入数据模板（JSON格式）',
  `execution_timeout` int NULL DEFAULT 3600 COMMENT '执行超时时间（秒）',
  `retry_count` int NULL DEFAULT 0 COMMENT '失败重试次数',
  `retry_interval` int NULL DEFAULT 60 COMMENT '重试间隔（秒）',
  `max_execution_count` bigint NULL DEFAULT NULL COMMENT '最大执行次数（NULL表示无限制）',
  `execution_count` bigint NULL DEFAULT 0 COMMENT '已执行次数',
  `schedule_start_time` datetime NULL DEFAULT NULL COMMENT '调度开始时间',
  `schedule_end_time` datetime NULL DEFAULT NULL COMMENT '调度结束时间',
  `last_execution_time` datetime NULL DEFAULT NULL COMMENT '上次执行时间',
  `next_execution_time` datetime NULL DEFAULT NULL COMMENT '下次执行时间',
  `last_execution_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上次执行状态',
  `last_execution_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上次执行消息',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `priority` int NULL DEFAULT 5 COMMENT '优先级（1-10，数字越大优先级越高）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展配置JSON',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_workflow_schedule_name`(`workflow_id`, `schedule_name`) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_next_execution_time`(`next_execution_time` ASC) USING BTREE,
  INDEX `idx_cron_expression`(`cron_expression` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE,
  CONSTRAINT `fk_workflow_schedule_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI工作流定时调度配置表' ROW_FORMAT = Dynamic;

-- ========================================
-- 3. 创建工作流定时任务执行日志表
-- ========================================

DROP TABLE IF EXISTS `ai_workflow_schedule_log`;
CREATE TABLE `ai_workflow_schedule_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `schedule_id` bigint NOT NULL COMMENT '调度配置ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `execution_id` bigint NULL DEFAULT NULL COMMENT '工作流执行记录ID',
  `trigger_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'schedule' COMMENT '触发类型（schedule=定时触发 manual=手动触发 retry=重试触发）',
  `scheduled_time` datetime NOT NULL COMMENT '计划执行时间',
  `actual_start_time` datetime NULL DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_time` datetime NULL DEFAULT NULL COMMENT '实际结束时间',
  `execution_duration` bigint NULL DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'running' COMMENT '执行状态（running=运行中 completed=已完成 failed=失败 timeout=超时 cancelled=已取消）',
  `result_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '执行结果消息',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `input_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '输入数据（JSON格式）',
  `output_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '输出数据（JSON格式）',
  `retry_count` int NULL DEFAULT 0 COMMENT '重试次数',
  `max_retry_count` int NULL DEFAULT 0 COMMENT '最大重试次数',
  `server_info` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行服务器信息',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_schedule_id`(`schedule_id` ASC) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_execution_id`(`execution_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_scheduled_time`(`scheduled_time` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `fk_schedule_log_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `ai_workflow_schedule` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_schedule_log_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_schedule_log_execution` FOREIGN KEY (`execution_id`) REFERENCES `ai_workflow_execution` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI工作流定时调度执行日志表' ROW_FORMAT = Dynamic;

-- ========================================
-- 4. 为ai_workflow_execution表添加调度相关字段
-- ========================================

ALTER TABLE `ai_workflow_execution` 
ADD COLUMN `schedule_id` bigint NULL DEFAULT NULL COMMENT '调度配置ID（NULL表示手动执行）' AFTER `workflow_id`,
ADD COLUMN `trigger_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'manual' COMMENT '触发类型（manual=手动 schedule=定时 retry=重试）' AFTER `schedule_id`,
ADD COLUMN `scheduled_time` datetime NULL DEFAULT NULL COMMENT '计划执行时间' AFTER `trigger_type`,
ADD COLUMN `execution_duration` bigint NULL DEFAULT NULL COMMENT '执行耗时（毫秒）' AFTER `scheduled_time`;

-- 添加索引
ALTER TABLE `ai_workflow_execution` 
ADD INDEX `idx_schedule_id`(`schedule_id` ASC) USING BTREE,
ADD INDEX `idx_trigger_type`(`trigger_type` ASC) USING BTREE,
ADD INDEX `idx_scheduled_time`(`scheduled_time` ASC) USING BTREE;

-- 添加外键约束
ALTER TABLE `ai_workflow_execution` 
ADD CONSTRAINT `fk_execution_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `ai_workflow_schedule` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT;

-- ========================================
-- 5. 添加相关字典数据
-- ========================================

-- 添加工作流调度状态字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) 
VALUES ('工作流调度状态', 'ai_workflow_schedule_status', '0', 'admin', NOW(), '', NULL, '工作流调度状态列表', '0');

-- 添加工作流调度状态字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) 
VALUES 
(1, '正常', '0', 'ai_workflow_schedule_status', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态', '0'),
(2, '暂停', '1', 'ai_workflow_schedule_status', '', 'danger', 'N', '0', 'admin', NOW(), '', NULL, '暂停状态', '0');

-- 添加工作流调度触发类型字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) 
VALUES ('工作流调度触发类型', 'ai_workflow_trigger_type', '0', 'admin', NOW(), '', NULL, '工作流调度触发类型列表', '0');

-- 添加工作流调度触发类型字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) 
VALUES 
(1, '定时触发', 'schedule', 'ai_workflow_trigger_type', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '定时触发', '0'),
(2, '手动触发', 'manual', 'ai_workflow_trigger_type', '', 'info', 'N', '0', 'admin', NOW(), '', NULL, '手动触发', '0'),
(3, '重试触发', 'retry', 'ai_workflow_trigger_type', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '重试触发', '0');

-- 添加工作流调度执行状态字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) 
VALUES ('工作流调度执行状态', 'ai_workflow_execution_status', '0', 'admin', NOW(), '', NULL, '工作流调度执行状态列表', '0');

-- 添加工作流调度执行状态字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) 
VALUES 
(1, '运行中', 'running', 'ai_workflow_execution_status', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '运行中', '0'),
(2, '已完成', 'completed', 'ai_workflow_execution_status', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '已完成', '0'),
(3, '失败', 'failed', 'ai_workflow_execution_status', '', 'danger', 'N', '0', 'admin', NOW(), '', NULL, '失败', '0'),
(4, '超时', 'timeout', 'ai_workflow_execution_status', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '超时', '0'),
(5, '已取消', 'cancelled', 'ai_workflow_execution_status', '', 'info', 'N', '0', 'admin', NOW(), '', NULL, '已取消', '0');

-- 添加错误处理策略字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) 
VALUES ('工作流错误处理策略', 'ai_workflow_misfire_policy', '0', 'admin', NOW(), '', NULL, '工作流错误处理策略列表', '0');

-- 添加错误处理策略字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) 
VALUES 
(1, '立即执行', '1', 'ai_workflow_misfire_policy', '', 'primary', 'N', '0', 'admin', NOW(), '', NULL, '立即执行', '0'),
(2, '执行一次', '2', 'ai_workflow_misfire_policy', '', 'info', 'N', '0', 'admin', NOW(), '', NULL, '执行一次', '0'),
(3, '放弃执行', '3', 'ai_workflow_misfire_policy', '', 'warning', 'Y', '0', 'admin', NOW(), '', NULL, '放弃执行', '0');

-- ========================================
-- 6. 示例数据（可选）
-- ========================================

-- 插入示例调度配置
INSERT INTO `ai_workflow_schedule` (
  `workflow_id`, `schedule_name`, `schedule_description`, `cron_expression`, 
  `timezone`, `enabled`, `status`, `input_data_template`, `execution_timeout`, 
  `retry_count`, `retry_interval`, `priority`, `create_by`, `create_time`, `remark`
) VALUES (
  1, '每日GitHub推荐生成', '每天早上8点自动生成GitHub推荐排行榜', '0 0 8 * * ?', 
  'Asia/Shanghai', '1', '0', '{}', 3600, 
  2, 300, 5, 'admin', NOW(), '示例定时任务配置'
);

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 执行完成提示
-- ========================================
-- 工作流定时任务功能数据库修改完成！
-- 
-- 新增功能：
-- 1. 工作流支持定时调度执行
-- 2. 支持cron表达式配置
-- 3. 支持执行次数限制
-- 4. 支持失败重试机制
-- 5. 支持并发控制
-- 6. 完整的执行日志记录
-- 7. 灵活的调度配置管理
-- 
-- 下一步：
-- 1. 创建对应的实体类
-- 2. 实现服务层逻辑
-- 3. 添加控制器接口
-- 4. 集成Quartz调度器