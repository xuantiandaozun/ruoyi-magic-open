-- ----------------------------
-- AI工作流测试数据
-- ----------------------------

-- 插入测试工作流
INSERT INTO `ai_workflow` (`id`, `workflow_name`, `workflow_description`, `workflow_type`, `workflow_version`, `enabled`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `user_id`, `config_json`) VALUES
(1, 'GitHub趋势分析工作流', '使用LangChain4j Agent分析GitHub热门项目趋势', 'langchain4j_agent', '1.0', '1', '0', '0', 'admin', NOW(), '', NULL, '测试LangChain4j Agent工作流', 1, '{"description": "GitHub趋势分析工作流测试"}'),
(2, '传统顺序工作流', '传统的顺序执行工作流示例', 'sequential', '1.0', '1', '0', '0', 'admin', NOW(), '', NULL, '测试传统工作流', 1, '{"description": "传统顺序工作流测试"}'),
(3, '多步骤Agent工作流', '包含多个Agent步骤的复杂工作流', 'langchain4j_agent', '1.0', '1', '0', '0', 'admin', NOW(), '', NULL, '测试多步骤Agent工作流', 1, '{"description": "多步骤Agent工作流测试"}');

-- 插入测试工作流步骤

-- GitHub趋势分析工作流步骤（单步骤LangChain4j Agent）
INSERT INTO `ai_workflow_step` (`id`, `workflow_id`, `step_name`, `step_description`, `step_order`, `model_config_id`, `system_prompt`, `input_variable`, `output_variable`, `enabled`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `config_json`, `tool_type`, `tool_parameters`, `tool_enabled`) VALUES
(1, 1, 'GitHub趋势查询', '查询GitHub热门项目趋势', 1, 1, '你是一个GitHub趋势分析专家。请根据用户的要求查询GitHub上的热门项目，并提供详细的分析报告。', 'userInput', 'githubTrendingResult', '1', '0', '0', 'admin', NOW(), '', NULL, 'GitHub趋势查询步骤', '{"tools": ["github_trending"]}', 'github_trending', '{"language": "java", "period": "daily"}', 'Y');

-- 传统顺序工作流步骤
INSERT INTO `ai_workflow_step` (`id`, `workflow_id`, `step_name`, `step_description`, `step_order`, `model_config_id`, `system_prompt`, `input_variable`, `output_variable`, `enabled`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `config_json`, `tool_type`, `tool_parameters`, `tool_enabled`) VALUES
(2, 2, '内容分析', '分析用户输入的内容', 1, 1, '你是一个内容分析专家。请分析用户提供的内容，提取关键信息。', 'userInput', 'analysisResult', '1', '0', '0', 'admin', NOW(), '', NULL, '内容分析步骤', '{}', NULL, NULL, 'N'),
(3, 2, '结果总结', '总结分析结果', 2, 1, '你是一个总结专家。请根据前面的分析结果，生成简洁明了的总结报告。', 'analysisResult', 'summaryResult', '1', '0', '0', 'admin', NOW(), '', NULL, '结果总结步骤', '{}', NULL, NULL, 'N');

-- 多步骤Agent工作流步骤
INSERT INTO `ai_workflow_step` (`id`, `workflow_id`, `step_name`, `step_description`, `step_order`, `model_config_id`, `system_prompt`, `input_variable`, `output_variable`, `enabled`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `config_json`, `tool_type`, `tool_parameters`, `tool_enabled`) VALUES
(4, 3, '需求分析Agent', '分析用户需求', 1, 1, '你是一个需求分析专家。请仔细分析用户的需求，识别关键要素和目标。', 'userInput', 'requirementAnalysis', '1', '0', '0', 'admin', NOW(), '', NULL, '需求分析Agent步骤', '{}', NULL, NULL, 'N'),
(5, 3, '技术调研Agent', '进行技术调研', 2, 1, '你是一个技术调研专家。请根据需求分析结果，调研相关的技术方案和工具。', 'requirementAnalysis', 'techResearch', '1', '0', '0', 'admin', NOW(), '', NULL, '技术调研Agent步骤', '{"tools": ["github_trending"]}', 'github_trending', '{}', 'Y'),
(6, 3, '方案设计Agent', '设计解决方案', 3, 1, '你是一个方案设计专家。请根据需求分析和技术调研结果，设计完整的解决方案。', 'techResearch', 'solutionDesign', '1', '0', '0', 'admin', NOW(), '', NULL, '方案设计Agent步骤', '{}', NULL, NULL, 'N');

-- 插入测试AI模型配置（如果不存在）
INSERT IGNORE INTO `ai_model_config` (`id`, `config_name`, `provider`, `model`, `api_key`, `endpoint`, `enabled`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(1, '测试OpenAI模型', 'openai', 'gpt-3.5-turbo', 'test-api-key', 'https://api.openai.com/v1', 'Y', '0', '0', 'admin', NOW(), '', NULL, '测试用的OpenAI模型配置');