/*
 Navicat Premium Dump SQL

 Source Server         : 个人阿里云数据库
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : rm-bp1k7j3y96w020tz1eo.mysql.rds.aliyuncs.com:3306
 Source Schema         : ry-vue

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 16/10/2025 11:24:05
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 120 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2024-12-27 10:06:44', '', NULL, '初始化密码 123456', '0');
INSERT INTO `sys_config` VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-light', 'Y', 'admin', '2024-12-27 10:06:44', '', '2024-12-28 16:43:36', '深色主题theme-dark，浅色主题theme-light', '0');
INSERT INTO `sys_config` VALUES (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'false', 'Y', 'admin', '2024-12-27 10:06:44', '', '2024-12-28 16:43:17', '是否开启验证码功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', '2024-12-27 10:06:44', '', NULL, '是否开启注册用户功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y', 'admin', '2024-12-27 10:06:44', '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）', '0');
INSERT INTO `sys_config` VALUES (100, 'AI生图数量阈值', 'ai.image.generation.limit', '300', 'Y', '', NULL, '', NULL, '超过此数量时只生成提示词不生成图片', '0');
INSERT INTO `sys_config` VALUES (101, 'AI默认服务类型', 'ai.default.type', 'DOUBAO', 'Y', 'admin', '2025-10-09 16:45:51', 'admin', '2025-10-09 16:45:51', 'AI默认使用的服务类型（DOUBAO、OPENAI、DEEPSEEK、QIANWEN、GLM、OPEROUTER）', '0');
INSERT INTO `sys_config` VALUES (102, 'AI服务总开关', 'ai.service.enabled', 'true', 'Y', 'admin', '2025-10-09 16:45:51', 'admin', '2025-10-09 16:45:51', '是否启用AI服务功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (103, 'AI聊天功能开关', 'ai.chat.enabled', 'true', 'Y', 'admin', '2025-10-09 16:45:51', 'admin', '2025-10-09 16:45:51', '是否启用AI聊天功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (104, 'AI聊天最大历史记录数', 'ai.chat.maxHistory', '10', 'Y', 'admin', '2025-10-09 16:45:51', 'admin', '2025-10-09 16:45:51', 'AI聊天保留的最大历史记录数量', '0');
INSERT INTO `sys_config` VALUES (105, 'AI聊天单次最大Token数', 'ai.chat.maxTokens', '4096', 'Y', 'admin', '2025-10-09 16:45:51', 'admin', '2025-10-09 16:45:51', 'AI聊天单次请求的最大Token数量', '0');
INSERT INTO `sys_config` VALUES (106, 'AI图像生成功能开关', 'ai.image.enabled', 'true', 'Y', 'admin', '2025-10-09 16:45:51', 'admin', '2025-10-09 16:45:51', '是否启用AI图像生成功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (107, 'AI图像识别功能开关', 'ai.vision.enabled', 'true', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', '是否启用AI图像识别功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (108, 'AI向量化功能开关', 'ai.embedding.enabled', 'true', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', '是否启用AI向量化功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (109, 'AI内容安全检查', 'ai.security.contentCheck', 'true', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', '是否启用AI内容安全检查（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (110, 'AI请求频率限制', 'ai.security.rateLimit', '100', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', 'AI服务每分钟最大请求次数', '0');
INSERT INTO `sys_config` VALUES (111, 'AI模型自动切换', 'ai.model.autoSwitch', 'true', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', '当前模型不可用时是否自动切换到其他可用模型（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (112, 'AI模型健康检查间隔', 'ai.model.healthCheckInterval', '300', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', 'AI模型健康检查间隔时间（秒）', '0');
INSERT INTO `sys_config` VALUES (113, 'AI请求日志记录', 'ai.log.requestEnabled', 'true', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', '是否记录AI请求日志（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (114, 'AI错误日志记录', 'ai.log.errorEnabled', 'true', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', '是否记录AI错误日志（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (115, 'AI响应缓存开关', 'ai.cache.enabled', 'true', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', '是否启用AI响应缓存（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (116, 'AI缓存过期时间', 'ai.cache.expireTime', '3600', 'Y', 'admin', '2025-10-09 16:45:52', 'admin', '2025-10-09 16:45:52', 'AI响应缓存过期时间（秒）', '0');
INSERT INTO `sys_config` VALUES (117, 'AI数据库访问开关', 'ai.database.enabled', 'true', 'Y', 'admin', '2025-10-10 17:47:47', 'admin', '2025-10-10 17:47:47', '是否启用AI数据库访问功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES (118, 'AI可访问的数据库表', 'ai.database.allowed_tables', 'sys_user,sys_role,sys_menu,sys_dept,sys_config,github_trending', 'Y', 'admin', '2025-10-10 17:47:47', 'admin', '2025-10-10 17:47:47', 'AI可以查询的数据库表列表，用逗号分隔', '0');
INSERT INTO `sys_config` VALUES (119, 'AI数据库查询限制', 'ai.database.query_limit', '100', 'Y', 'admin', '2025-10-10 17:47:47', 'admin', '2025-10-10 17:47:47', 'AI数据库查询结果数量限制（默认100，最大1000）', '0');

SET FOREIGN_KEY_CHECKS = 1;
