-- ----------------------------
-- 修改AI工作流步骤表，添加工具配置字段
-- ----------------------------

-- 添加工具类型字段
ALTER TABLE `ai_workflow_step` 
ADD COLUMN `tool_type` varchar(50) DEFAULT NULL COMMENT '工具类型（如github_trending、database_query等）';

-- 添加工具参数字段
ALTER TABLE `ai_workflow_step` 
ADD COLUMN `tool_parameters` text COMMENT '工具参数JSON（存储工具执行所需的参数）';

-- 添加工具启用状态字段
ALTER TABLE `ai_workflow_step` 
ADD COLUMN `tool_enabled` char(1) DEFAULT 'N' COMMENT '是否启用工具（Y=启用工具 N=不启用工具，默认为N）';

-- 添加索引
ALTER TABLE `ai_workflow_step` 
ADD INDEX `idx_tool_type` (`tool_type`);

ALTER TABLE `ai_workflow_step` 
ADD INDEX `idx_tool_enabled` (`tool_enabled`);

