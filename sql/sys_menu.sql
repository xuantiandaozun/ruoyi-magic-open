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

 Date: 05/01/2026 17:22:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '菜单名称',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '路由名称',
  `is_frame` int NULL DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 2, 'system', NULL, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', '2024-12-27 10:06:44', 'admin', '2025-08-05 16:48:42', '系统管理目录', '0');
INSERT INTO `sys_menu` VALUES (2, '系统监控', 0, 2, 'monitor', NULL, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', '2024-12-27 10:06:44', '', NULL, '系统监控目录', '0');
INSERT INTO `sys_menu` VALUES (3, '系统工具', 0, 3, 'tool', NULL, '', '', 1, 0, 'M', '0', '0', '', 'tool', 'admin', '2024-12-27 10:06:44', '', NULL, '系统工具目录', '0');
INSERT INTO `sys_menu` VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', '2024-12-27 10:06:44', '', NULL, '用户管理菜单', '0');
INSERT INTO `sys_menu` VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', '2024-12-27 10:06:44', '', NULL, '角色管理菜单', '0');
INSERT INTO `sys_menu` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', '2024-12-27 10:06:44', '', NULL, '菜单管理菜单', '0');
INSERT INTO `sys_menu` VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', '2024-12-27 10:06:44', '', NULL, '部门管理菜单', '0');
INSERT INTO `sys_menu` VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', '2024-12-27 10:06:44', '', NULL, '岗位管理菜单', '0');
INSERT INTO `sys_menu` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', '2024-12-27 10:06:44', '', NULL, '字典管理菜单', '0');
INSERT INTO `sys_menu` VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', '2024-12-27 10:06:44', '', NULL, '参数设置菜单', '0');
INSERT INTO `sys_menu` VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 'admin', '2024-12-27 10:06:44', '', NULL, '通知公告菜单', '0');
INSERT INTO `sys_menu` VALUES (108, '日志管理', 1, 9, 'log', '', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', '2024-12-27 10:06:44', '', NULL, '日志管理菜单', '0');
INSERT INTO `sys_menu` VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', '2024-12-27 10:06:44', '', NULL, '在线用户菜单', '0');
INSERT INTO `sys_menu` VALUES (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job', 'admin', '2024-12-27 10:06:44', '', NULL, '定时任务菜单', '0');
INSERT INTO `sys_menu` VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list', 'druid', 'admin', '2024-12-27 10:06:44', '', NULL, '数据监控菜单', '1');
INSERT INTO `sys_menu` VALUES (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', '', 1, 0, 'C', '0', '0', 'monitor:server:list', 'server', 'admin', '2024-12-27 10:06:44', '', NULL, '服务监控菜单', '0');
INSERT INTO `sys_menu` VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 'admin', '2024-12-27 10:06:44', '', NULL, '缓存监控菜单', '0');
INSERT INTO `sys_menu` VALUES (114, '缓存列表', 2, 6, 'cacheList', 'monitor/cache/list', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis-list', 'admin', '2024-12-27 10:06:44', '', NULL, '缓存列表菜单', '0');
INSERT INTO `sys_menu` VALUES (115, '表单构建', 3, 1, 'build', 'tool/build/index', '', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 'admin', '2024-12-27 10:06:44', '', NULL, '表单构建菜单', '0');
INSERT INTO `sys_menu` VALUES (116, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin', '2024-12-27 10:06:44', '', NULL, '代码生成菜单', '0');
INSERT INTO `sys_menu` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', '2024-12-27 10:06:44', '', NULL, '操作日志菜单', '0');
INSERT INTO `sys_menu` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', '2024-12-27 10:06:44', '', NULL, '登录日志菜单', '0');
INSERT INTO `sys_menu` VALUES (1000, '用户查询', 100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1001, '用户新增', 100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1002, '用户修改', 100, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1003, '用户删除', 100, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1004, '用户导出', 100, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1005, '用户导入', 100, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1006, '重置密码', 100, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1007, '角色查询', 101, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1008, '角色新增', 101, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1009, '角色修改', 101, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1010, '角色删除', 101, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1011, '角色导出', 101, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1012, '菜单查询', 102, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1013, '菜单新增', 102, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1014, '菜单修改', 102, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1015, '菜单删除', 102, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1016, '部门查询', 103, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1017, '部门新增', 103, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1018, '部门修改', 103, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1019, '部门删除', 103, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1020, '岗位查询', 104, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1021, '岗位新增', 104, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1022, '岗位修改', 104, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1023, '岗位删除', 104, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1024, '岗位导出', 104, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1025, '字典查询', 105, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1026, '字典新增', 105, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1027, '字典修改', 105, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1028, '字典删除', 105, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1029, '字典导出', 105, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1030, '参数查询', 106, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1031, '参数新增', 106, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1032, '参数修改', 106, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1033, '参数删除', 106, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1034, '参数导出', 106, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1035, '公告查询', 107, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1036, '公告新增', 107, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1037, '公告修改', 107, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1038, '公告删除', 107, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1039, '操作查询', 500, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1040, '操作删除', 500, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1041, '日志导出', 500, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1042, '登录查询', 501, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1043, '登录删除', 501, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1044, '日志导出', 501, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1045, '账户解锁', 501, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1046, '在线查询', 109, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1047, '批量强退', 109, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1048, '单条强退', 109, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1049, '任务查询', 110, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1050, '任务新增', 110, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1051, '任务修改', 110, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1052, '任务删除', 110, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1053, '状态修改', 110, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1054, '任务导出', 110, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1055, '生成查询', 116, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1056, '生成修改', 116, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1057, '生成删除', 116, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1058, '导入代码', 116, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1059, '预览代码', 116, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (1060, '生成代码', 116, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 'admin', '2024-12-27 10:06:44', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2002, '数据源管理', 3, 10, 'datasource', 'tool/datasource/index', NULL, '', 1, 0, 'C', '0', '0', 'system:datasource:list', 'database', 'admin', '2025-05-19 17:30:04', '', '2025-05-19 17:34:33', '数据源管理菜单', '0');
INSERT INTO `sys_menu` VALUES (2003, '数据源查询', 2002, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:datasource:query', '#', 'admin', '2025-05-19 17:30:05', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2004, '数据源新增', 2002, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:datasource:add', '#', 'admin', '2025-05-19 17:30:05', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2005, '数据源修改', 2002, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:datasource:edit', '#', 'admin', '2025-05-19 17:30:05', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2006, '数据源删除', 2002, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:datasource:remove', '#', 'admin', '2025-05-19 17:30:05', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2007, '数据源导出', 2002, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:datasource:export', '#', 'admin', '2025-05-19 17:30:05', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2008, '测试连接', 2002, 6, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:datasource:test', '#', 'admin', '2025-05-19 17:30:05', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2009, '刷新数据源', 2002, 7, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:datasource:refresh', '#', 'admin', '2025-05-19 17:30:05', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2017, 'Github', 0, 1, 'github', NULL, NULL, '', 1, 0, 'M', '0', '0', NULL, 'list', 'admin', '2025-07-03 10:43:01', 'admin', '2025-07-03 10:43:01', '', '0');
INSERT INTO `sys_menu` VALUES (2018, 'github流行榜单', 2017, 1, 'trending', 'github/trending/index', NULL, '', 1, 0, 'C', '0', '0', 'system:trending:list', '#', 'admin', '2025-07-03 10:47:25', 'admin', '2025-07-03 11:51:03', 'github流行榜单菜单', '1');
INSERT INTO `sys_menu` VALUES (2019, 'github流行榜单查询', 2018, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:trending:query', '#', 'admin', '2025-07-03 10:47:25', '', NULL, '', '1');
INSERT INTO `sys_menu` VALUES (2020, 'github流行榜单新增', 2018, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:trending:add', '#', 'admin', '2025-07-03 10:47:25', '', NULL, '', '1');
INSERT INTO `sys_menu` VALUES (2021, 'github流行榜单修改', 2018, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:trending:edit', '#', 'admin', '2025-07-03 10:47:25', '', NULL, '', '1');
INSERT INTO `sys_menu` VALUES (2022, 'github流行榜单删除', 2018, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:trending:remove', '#', 'admin', '2025-07-03 10:47:25', '', NULL, '', '1');
INSERT INTO `sys_menu` VALUES (2023, 'github流行榜单导出', 2018, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:trending:export', '#', 'admin', '2025-07-03 10:47:25', '', NULL, '', '1');
INSERT INTO `sys_menu` VALUES (2024, 'github流行榜单', 2017, 1, 'trending', 'github/trending/index', NULL, '', 1, 0, 'C', '0', '0', 'github:trending:list', '#', 'admin', '2025-07-03 13:54:52', '', NULL, 'github流行榜单菜单', '0');
INSERT INTO `sys_menu` VALUES (2025, 'github流行榜单查询', 2024, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'github:trending:query', '#', 'admin', '2025-07-03 13:54:52', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2026, 'github流行榜单新增', 2024, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'github:trending:add', '#', 'admin', '2025-07-03 13:54:52', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2027, 'github流行榜单修改', 2024, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'github:trending:edit', '#', 'admin', '2025-07-03 13:54:52', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2028, 'github流行榜单删除', 2024, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'github:trending:remove', '#', 'admin', '2025-07-03 13:54:52', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2029, 'github流行榜单导出', 2024, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'github:trending:export', '#', 'admin', '2025-07-03 13:54:53', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2030, '存储配置', 1, 1, 'storageConfig', 'system/storageConfig/index', NULL, '', 1, 0, 'C', '0', '0', 'system:storageConfig:list', 'database', 'admin', '2025-07-11 11:33:08', 'admin', '2025-07-11 12:02:54', '存储配置菜单', '0');
INSERT INTO `sys_menu` VALUES (2031, '存储配置查询', 2030, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:storageConfig:query', '#', 'admin', '2025-07-11 11:33:08', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2032, '存储配置新增', 2030, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:storageConfig:add', '#', 'admin', '2025-07-11 11:33:08', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2033, '存储配置修改', 2030, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:storageConfig:edit', '#', 'admin', '2025-07-11 11:33:08', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2034, '存储配置删除', 2030, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:storageConfig:remove', '#', 'admin', '2025-07-11 11:33:08', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2035, '存储配置导出', 2030, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:storageConfig:export', '#', 'admin', '2025-07-11 11:33:08', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2036, '文件上传记录', 1, 1, 'record', 'system/record/index', NULL, '', 1, 0, 'C', '0', '0', 'system:record:list', 'database', 'admin', '2025-07-11 12:01:16', 'admin', '2025-07-11 12:03:00', '文件上传记录菜单', '0');
INSERT INTO `sys_menu` VALUES (2037, '文件上传记录查询', 2036, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:record:query', '#', 'admin', '2025-07-11 12:01:16', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2038, '文件上传记录新增', 2036, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:record:add', '#', 'admin', '2025-07-11 12:01:16', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2039, '文件上传记录修改', 2036, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:record:edit', '#', 'admin', '2025-07-11 12:01:16', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2040, '文件上传记录删除', 2036, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:record:remove', '#', 'admin', '2025-07-11 12:01:17', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2041, '文件上传记录导出', 2036, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:record:export', '#', 'admin', '2025-07-11 12:01:17', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2042, '密钥管理', 1, 1, 'secretKey', 'system/secretKey/index', NULL, '', 1, 0, 'C', '0', '0', 'system:secretKey:list', 'example', 'admin', '2025-07-11 16:08:03', 'admin', '2025-07-30 11:45:20', '密钥管理菜单', '0');
INSERT INTO `sys_menu` VALUES (2043, '密钥管理查询', 2042, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:secretKey:query', '#', 'admin', '2025-07-11 16:08:03', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2044, '密钥管理新增', 2042, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:secretKey:add', '#', 'admin', '2025-07-11 16:08:03', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2045, '密钥管理修改', 2042, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:secretKey:edit', '#', 'admin', '2025-07-11 16:08:03', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2046, '密钥管理删除', 2042, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:secretKey:remove', '#', 'admin', '2025-07-11 16:08:03', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2047, '密钥管理导出', 2042, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:secretKey:export', '#', 'admin', '2025-07-11 16:08:03', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2048, '阿里云', 0, 1, 'al', NULL, NULL, '', 1, 0, 'M', '0', '0', NULL, 'database', 'admin', '2025-07-11 17:25:09', 'admin', '2025-07-30 11:45:11', '', '0');
INSERT INTO `sys_menu` VALUES (2049, 'RDS实例管理', 2048, 1, 'rdsInstance', 'system/rdsInstance/index', NULL, '', 1, 0, 'C', '0', '0', 'system:rdsInstance:list', '#', 'admin', '2025-07-11 17:49:54', '', NULL, 'RDS实例管理菜单', '0');
INSERT INTO `sys_menu` VALUES (2050, 'RDS实例管理查询', 2049, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:rdsInstance:query', '#', 'admin', '2025-07-11 17:49:54', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2051, 'RDS实例管理新增', 2049, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:rdsInstance:add', '#', 'admin', '2025-07-11 17:49:54', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2052, 'RDS实例管理修改', 2049, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:rdsInstance:edit', '#', 'admin', '2025-07-11 17:49:54', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2053, 'RDS实例管理删除', 2049, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:rdsInstance:remove', '#', 'admin', '2025-07-11 17:49:54', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2054, 'RDS实例管理导出', 2049, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:rdsInstance:export', '#', 'admin', '2025-07-11 17:49:54', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2055, '飞书', 0, 1, 'feishu', NULL, NULL, '', 1, 0, 'M', '0', '0', NULL, 'date-range', 'admin', '2025-07-31 16:42:53', 'admin', '2025-07-31 16:43:02', '', '0');
INSERT INTO `sys_menu` VALUES (2056, '飞书文档信息', 2055, 1, 'feishudoc', 'feishu/feishudoc/index', NULL, '', 1, 0, 'C', '0', '0', 'feishu:feishudoc:list', '#', 'admin', '2025-07-31 16:47:37', '', NULL, '飞书文档信息菜单', '0');
INSERT INTO `sys_menu` VALUES (2057, '飞书文档信息查询', 2056, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:query', '#', 'admin', '2025-07-31 16:47:37', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2058, '飞书文档信息新增', 2056, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:add', '#', 'admin', '2025-07-31 16:47:37', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2059, '飞书文档信息修改', 2056, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:edit', '#', 'admin', '2025-07-31 16:47:37', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2060, '飞书文档信息删除', 2056, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:remove', '#', 'admin', '2025-07-31 16:47:37', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2061, '飞书文档信息导出', 2056, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:export', '#', 'admin', '2025-07-31 16:47:37', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2062, '博客', 0, 1, 'blog', NULL, NULL, '', 1, 0, 'M', '0', '0', NULL, 'database', 'admin', '2025-08-05 16:48:28', 'admin', '2025-08-05 16:48:28', '', '0');
INSERT INTO `sys_menu` VALUES (2063, '文章列表', 2062, 1, 'blog', 'article/blog/index', NULL, '', 1, 0, 'C', '0', '0', 'article:blog:list', '#', 'admin', '2025-08-05 16:49:28', '', NULL, '文章列表菜单', '0');
INSERT INTO `sys_menu` VALUES (2064, '文章列表查询', 2063, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:blog:query', '#', 'admin', '2025-08-05 16:49:28', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2065, '文章列表新增', 2063, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:blog:add', '#', 'admin', '2025-08-05 16:49:28', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2066, '文章列表修改', 2063, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:blog:edit', '#', 'admin', '2025-08-05 16:49:28', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2067, '文章列表删除', 2063, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:blog:remove', '#', 'admin', '2025-08-05 16:49:28', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2068, '文章列表导出', 2063, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:blog:export', '#', 'admin', '2025-08-05 16:49:28', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2069, '英文博客', 2062, 1, 'enBlog', 'article/enBlog/index', NULL, '', 1, 0, 'C', '0', '0', 'article:enBlog:list', '#', 'admin', '2025-08-26 15:05:56', '', NULL, '英文博客菜单', '0');
INSERT INTO `sys_menu` VALUES (2070, '英文博客查询', 2069, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:enBlog:query', '#', 'admin', '2025-08-26 15:05:56', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2071, '英文博客新增', 2069, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:enBlog:add', '#', 'admin', '2025-08-26 15:05:56', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2072, '英文博客修改', 2069, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:enBlog:edit', '#', 'admin', '2025-08-26 15:05:56', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2073, '英文博客删除', 2069, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:enBlog:remove', '#', 'admin', '2025-08-26 15:05:56', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2074, '英文博客导出', 2069, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:enBlog:export', '#', 'admin', '2025-08-26 15:05:57', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2075, '自媒体文章', 2062, 1, 'socialArticle', 'article/socialArticle/index', NULL, '', 1, 0, 'C', '0', '0', 'article:socialArticle:list', '#', 'admin', '2025-09-02 16:42:26', '', NULL, '自媒体文章菜单', '0');
INSERT INTO `sys_menu` VALUES (2076, '自媒体文章查询', 2075, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:socialArticle:query', '#', 'admin', '2025-09-02 16:42:26', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2077, '自媒体文章新增', 2075, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:socialArticle:add', '#', 'admin', '2025-09-02 16:42:27', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2078, '自媒体文章修改', 2075, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:socialArticle:edit', '#', 'admin', '2025-09-02 16:42:27', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2079, '自媒体文章删除', 2075, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:socialArticle:remove', '#', 'admin', '2025-09-02 16:42:27', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2080, '自媒体文章导出', 2075, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'article:socialArticle:export', '#', 'admin', '2025-09-02 16:42:27', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2081, 'AI聊天', 1, 1, 'chat', 'ai/chat', NULL, 'chat', 1, 1, 'C', '0', '0', NULL, '#', 'admin', '2025-10-30 15:56:42', 'admin', '2025-10-30 15:56:55', '', '1');
INSERT INTO `sys_menu` VALUES (2082, '飞书用户', 2055, 1, 'feishuusers', 'feishu/users/index', NULL, 'feishuusers', 1, 0, 'C', '0', '0', 'feishu:users:list', '#', 'admin', '2025-11-27 12:14:23', 'admin', '2025-11-27 12:14:23', '', '0');
INSERT INTO `sys_menu` VALUES (2083, '飞书消息', 2056, 1, 'messageRecord', 'feishu/messageRecord/index', NULL, 'messageRecord', 1, 0, 'C', '0', '0', NULL, '#', 'admin', '2025-11-27 14:38:22', 'admin', '2025-11-27 14:38:22', '', '0');
INSERT INTO `sys_menu` VALUES (2084, '飞书消息', 2055, 1, 'messageRecord', 'feishu/messageRecord/index', NULL, 'messageRecord', 1, 0, 'C', '0', '0', NULL, '#', 'admin', '2025-11-27 14:39:35', 'admin', '2025-11-27 14:39:35', '', '0');
INSERT INTO `sys_menu` VALUES (2085, '域名监控', 2, 1, 'cert', 'monitor/cert/index', NULL, 'cert', 1, 0, 'C', '0', '0', NULL, '#', 'admin', '2025-12-04 11:55:40', 'admin', '2025-12-04 11:55:40', '', '0');
INSERT INTO `sys_menu` VALUES (2086, '素材管理', 2062, 1, 'socialMediaAsset', 'article/socialMediaAsset/index', NULL, 'socialMediaAsset', 1, 0, 'C', '0', '0', NULL, '#', 'admin', '2025-12-09 09:44:47', 'admin', '2025-12-09 09:44:47', '', '0');
INSERT INTO `sys_menu` VALUES (2087, '个人记账', 0, 6, 'bill', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'money', 'admin', '2025-12-15 09:42:34', '', NULL, '个人记账管理菜单', '0');
INSERT INTO `sys_menu` VALUES (2088, '账单记录', 2087, 1, 'record', 'bill/record/index', NULL, '', 1, 0, 'C', '0', '0', 'bill:record:list', 'money', 'admin', '2025-12-15 09:42:35', '', NULL, '账单记录菜单', '0');
INSERT INTO `sys_menu` VALUES (2089, '账单记录查询', 2088, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:record:list', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2090, '账单记录新增', 2088, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:record:add', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2091, '账单记录修改', 2088, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:record:edit', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2092, '账单记录删除', 2088, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:record:remove', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2093, '账单记录导出', 2088, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:record:export', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2094, '账单分类', 2087, 2, 'category', 'bill/category/index', NULL, '', 1, 0, 'C', '0', '0', 'bill:category:list', 'tree-table', 'admin', '2025-12-15 09:42:35', '', NULL, '账单分类菜单', '0');
INSERT INTO `sys_menu` VALUES (2095, '账单分类查询', 2094, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:category:list', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2096, '账单分类新增', 2094, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:category:add', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2097, '账单分类修改', 2094, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:category:edit', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2098, '账单分类删除', 2094, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:category:remove', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2099, '账单分类导出', 2094, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:category:export', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2100, '账户管理', 2087, 3, 'account', 'bill/account/index', NULL, '', 1, 0, 'C', '0', '0', 'bill:account:list', 'database', 'admin', '2025-12-15 09:42:35', 'admin', '2025-12-16 10:19:56', '账户管理菜单', '0');
INSERT INTO `sys_menu` VALUES (2101, '账户查询', 2100, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:account:list', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2102, '账户新增', 2100, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:account:add', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2103, '账户修改', 2100, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:account:edit', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2104, '账户删除', 2100, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:account:remove', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2105, '账户导出', 2100, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:account:export', '#', 'admin', '2025-12-15 09:42:35', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2106, '预算管理', 2087, 4, 'budget', 'bill/budget/index', NULL, '', 1, 0, 'C', '0', '0', 'bill:budget:list', 'chart', 'admin', '2025-12-15 09:42:36', '', NULL, '预算管理菜单', '0');
INSERT INTO `sys_menu` VALUES (2107, '预算查询', 2106, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:budget:list', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2108, '预算新增', 2106, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:budget:add', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2109, '预算修改', 2106, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:budget:edit', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2110, '预算删除', 2106, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:budget:remove', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2111, '预算导出', 2106, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:budget:export', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2112, '提醒管理', 2087, 5, 'reminder', 'bill/reminder/index', NULL, '', 1, 0, 'C', '0', '0', 'bill:reminder:list', 'example', 'admin', '2025-12-15 09:42:36', 'admin', '2025-12-16 10:20:01', '提醒管理菜单', '0');
INSERT INTO `sys_menu` VALUES (2113, '提醒查询', 2112, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:reminder:list', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2114, '提醒新增', 2112, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:reminder:add', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2115, '提醒修改', 2112, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:reminder:edit', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2116, '提醒删除', 2112, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:reminder:remove', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2117, '提醒导出', 2112, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:reminder:export', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2118, '家庭组管理', 2087, 6, 'family', 'bill/family/index', NULL, '', 1, 0, 'C', '0', '0', 'bill:family:list', 'peoples', 'admin', '2025-12-15 09:42:36', '', NULL, '家庭组管理菜单', '0');
INSERT INTO `sys_menu` VALUES (2119, '家庭组查询', 2118, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:family:list', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2120, '家庭组新增', 2118, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:family:add', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2121, '家庭组修改', 2118, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:family:edit', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2122, '家庭组删除', 2118, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:family:remove', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2123, '家庭组导出', 2118, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:family:export', '#', 'admin', '2025-12-15 09:42:36', '', NULL, '', '0');
INSERT INTO `sys_menu` VALUES (2124, '详情', 2094, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:category:query', '#', 'admin', '2025-12-16 10:20:39', 'admin', '2025-12-16 10:20:39', '', '0');
INSERT INTO `sys_menu` VALUES (2125, '详情', 2100, 0, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:account:query', '#', 'admin', '2025-12-16 10:33:11', 'admin', '2025-12-16 10:33:11', '', '0');
INSERT INTO `sys_menu` VALUES (2126, '个人账号查询', 2100, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:userProfile:query', '#', 'admin', '2025-12-16 10:36:32', 'admin', '2025-12-16 10:36:32', '', '0');
INSERT INTO `sys_menu` VALUES (2127, '详情', 2088, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:record:query', '#', 'admin', '2025-12-16 13:51:09', 'admin', '2025-12-16 13:51:09', '', '0');
INSERT INTO `sys_menu` VALUES (2128, '详情', 2118, 0, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'bill:family:query', '#', 'admin', '2025-12-16 17:22:48', 'admin', '2025-12-16 17:22:48', '', '0');

SET FOREIGN_KEY_CHECKS = 1;
