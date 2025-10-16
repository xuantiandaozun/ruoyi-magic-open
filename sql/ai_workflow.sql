-- ----------------------------
-- AI工作流配置表
-- ----------------------------
DROP TABLE IF EXISTS `ai_workflow`;
CREATE TABLE `ai_workflow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
  `workflow_name` varchar(100) NOT NULL COMMENT '工作流名称',
  `workflow_description` varchar(500) DEFAULT NULL COMMENT '工作流描述',
  `workflow_type` varchar(50) DEFAULT 'sequential' COMMENT '工作流类型（sequential=顺序执行）',
  `workflow_version` varchar(20) DEFAULT '1.0' COMMENT '工作流版本',
  `enabled` char(1) DEFAULT '1' COMMENT '启用状态（0=禁用 1=启用）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0=正常 1=停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `config_json` text COMMENT '配置JSON（扩展配置参数）',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_name` (`workflow_name`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI工作流配置表';

-- ----------------------------
-- AI工作流步骤表
-- ----------------------------
DROP TABLE IF EXISTS `ai_workflow_step`;
CREATE TABLE `ai_workflow_step` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  `workflow_id` bigint(20) NOT NULL COMMENT '工作流ID',
  `step_name` varchar(100) NOT NULL COMMENT '步骤名称',
  `step_description` varchar(500) DEFAULT NULL COMMENT '步骤描述',
  `step_order` int(11) NOT NULL COMMENT '步骤顺序',
  `model_config_id` bigint(20) DEFAULT NULL COMMENT 'AI模型配置ID',
  `system_prompt` text COMMENT '系统提示词',
  `input_variable` varchar(100) DEFAULT NULL COMMENT '输入变量名',
  `output_variable` varchar(100) DEFAULT NULL COMMENT '输出变量名',
  `enabled` char(1) DEFAULT '1' COMMENT '启用状态（0=禁用 1=启用）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0=正常 1=停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `config_json` text COMMENT '配置JSON（扩展配置参数）',
  `tool_type` varchar(50) DEFAULT NULL COMMENT '工具类型（如github_trending、database_query等）',
  `tool_parameters` text COMMENT '工具参数JSON（存储工具执行所需的参数）',
  `tool_enabled` char(1) DEFAULT 'N' COMMENT '是否启用工具（Y=启用工具 N=不启用工具，默认为N）',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_step_order` (`step_order`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_tool_type` (`tool_type`),
  KEY `idx_tool_enabled` (`tool_enabled`),
  CONSTRAINT `fk_workflow_step_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI工作流步骤表';

-- ----------------------------
-- AI工作流执行记录表
-- ----------------------------
DROP TABLE IF EXISTS `ai_workflow_execution`;
CREATE TABLE `ai_workflow_execution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
  `workflow_id` bigint(20) NOT NULL COMMENT '工作流ID',
  `status` varchar(20) DEFAULT 'running' COMMENT '执行状态（running=运行中, completed=已完成, failed=失败）',
  `input_data` text COMMENT '输入数据（JSON格式）',
  `output_data` text COMMENT '输出数据（JSON格式）',
  `error_message` text COMMENT '错误信息',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_workflow_execution_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI工作流执行记录表';

-- ----------------------------
-- 初始化数据
-- ----------------------------
INSERT INTO `ai_workflow` VALUES 
(1, '示例工作流', '这是一个简单的示例工作流，包含两个步骤', 'sequential', '1.0', '1', '0', '0', 'admin', NOW(), 'admin', NOW(), '示例工作流，用于演示基本功能', 1, '{}');

INSERT INTO `ai_workflow_step` VALUES 
(1, 1, '文本分析', '分析输入文本的情感', 1, 1, '你是一个文本分析专家，请分析以下文本的情感倾向', 'input_text', 'sentiment_result', '1', '0', '0', 'admin', NOW(), 'admin', NOW(), '第一步：文本情感分析', '{}', NULL, NULL, 'N'),
(2, 1, '总结生成', '根据分析结果生成总结', 2, 1, '请根据情感分析结果，生成一个简洁的总结', 'sentiment_result', 'final_summary', '1', '0', '0', 'admin', NOW(), 'admin', NOW(), '第二步：生成总结报告', '{}', NULL, NULL, 'N'),
(3, 2, 'GitHub趋势分析', '查询今天GitHub上榜的仓库并进行分析', 1, 1, '你是一个技术趋势分析专家，请分析GitHub上榜仓库的技术趋势和特点', 'final_summary', 'github_analysis', '1', '0', '0', 'admin', NOW(), 'admin', NOW(), '第一步：GitHub趋势分析', '{}', 'github_trending', '{"language": "all", "limit": 10}', 'Y');