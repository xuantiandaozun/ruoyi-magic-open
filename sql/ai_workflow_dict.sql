-- ----------------------------
-- AI工作流相关字典数据
-- ----------------------------

-- 添加工作流类型字典类型
INSERT INTO `sys_dict_type` VALUES (100, 'AI工作流类型', 'ai_workflow_type', '0', 'admin', NOW(), '', NULL, 'AI工作流类型列表', '0');

-- 添加工具类型字典类型
INSERT INTO `sys_dict_type` VALUES (101, 'AI工具类型', 'ai_tool_type', '0', 'admin', NOW(), '', NULL, 'AI工具类型列表', '0');

-- 添加工作流类型字典数据
INSERT INTO `sys_dict_data` VALUES (1000, 1, '顺序工作流', 'sequential', 'ai_workflow_type', '', '', 'Y', '0', 'admin', NOW(), '', NULL, '传统的顺序执行工作流', '0');
INSERT INTO `sys_dict_data` VALUES (1001, 2, 'LangChain4j Agent', 'langchain4j_agent', 'ai_workflow_type', '', '', 'Y', '0', 'admin', NOW(), '', NULL, '基于LangChain4j的智能Agent工作流', '0');
INSERT INTO `sys_dict_data` VALUES (1002, 3, '条件工作流', 'conditional', 'ai_workflow_type', '', '', 'Y', '0', 'admin', NOW(), '', NULL, '基于条件的分支工作流', '0');
INSERT INTO `sys_dict_data` VALUES (1003, 4, '循环工作流', 'loop', 'ai_workflow_type', '', '', 'Y', '0', 'admin', NOW(), '', NULL, '循环执行的工作流', '0');

-- 添加工具类型字典数据
INSERT INTO `sys_dict_data` VALUES (1010, 1, 'GitHub趋势', 'github_trending', 'ai_tool_type', '', '', 'Y', '0', 'admin', NOW(), '', NULL, 'GitHub趋势查询工具', '0');
INSERT INTO `sys_dict_data` VALUES (1011, 2, '数据库查询', 'database_query', 'ai_tool_type', '', '', 'Y', '0', 'admin', NOW(), '', NULL, '数据库查询工具', '0');
INSERT INTO `sys_dict_data` VALUES (1012, 3, '文件操作', 'file_operation', 'ai_tool_type', '', '', 'Y', '0', 'admin', NOW(), '', NULL, '文件操作工具', '0');
INSERT INTO `sys_dict_data` VALUES (1013, 4, '网络请求', 'http_request', 'ai_tool_type', '', '', 'Y', '0', 'admin', NOW(), '', NULL, 'HTTP网络请求工具', '0');