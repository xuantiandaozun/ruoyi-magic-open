-- ----------------------------
-- AI相关系统参数配置
-- ----------------------------

-- AI默认类型配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI默认服务类型', 'ai.default.type', 'DOUBAO', 'Y', 'admin', NOW(), 'admin', NOW(), 'AI默认使用的服务类型（DOUBAO、OPENAI、DEEPSEEK、QIANWEN、GLM、OPEROUTER）', '0');

-- AI服务开关配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI服务总开关', 'ai.service.enabled', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否启用AI服务功能（true开启，false关闭）', '0');

-- AI聊天配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI聊天功能开关', 'ai.chat.enabled', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否启用AI聊天功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI聊天最大历史记录数', 'ai.chat.maxHistory', '10', 'Y', 'admin', NOW(), 'admin', NOW(), 'AI聊天保留的最大历史记录数量', '0');
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI聊天单次最大Token数', 'ai.chat.maxTokens', '4096', 'Y', 'admin', NOW(), 'admin', NOW(), 'AI聊天单次请求的最大Token数量', '0');

-- AI图像功能配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI图像生成功能开关', 'ai.image.enabled', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否启用AI图像生成功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI图像识别功能开关', 'ai.vision.enabled', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否启用AI图像识别功能（true开启，false关闭）', '0');

-- AI向量化配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI向量化功能开关', 'ai.embedding.enabled', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否启用AI向量化功能（true开启，false关闭）', '0');

-- AI安全配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI内容安全检查', 'ai.security.contentCheck', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否启用AI内容安全检查（true开启，false关闭）', '0');
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI请求频率限制', 'ai.security.rateLimit', '100', 'Y', 'admin', NOW(), 'admin', NOW(), 'AI服务每分钟最大请求次数', '0');

-- AI模型切换配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI模型自动切换', 'ai.model.autoSwitch', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '当前模型不可用时是否自动切换到其他可用模型（true开启，false关闭）', '0');
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI模型健康检查间隔', 'ai.model.healthCheckInterval', '300', 'Y', 'admin', NOW(), 'admin', NOW(), 'AI模型健康检查间隔时间（秒）', '0');

-- AI日志配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI请求日志记录', 'ai.log.requestEnabled', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否记录AI请求日志（true开启，false关闭）', '0');
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI错误日志记录', 'ai.log.errorEnabled', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否记录AI错误日志（true开启，false关闭）', '0');

-- AI缓存配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI响应缓存开关', 'ai.cache.enabled', 'true', 'Y', 'admin', NOW(), 'admin', NOW(), '是否启用AI响应缓存（true开启，false关闭）', '0');
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES ('AI缓存过期时间', 'ai.cache.expireTime', '3600', 'Y', 'admin', NOW(), 'admin', NOW(), 'AI响应缓存过期时间（秒）', '0');