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

 Date: 16/10/2025 13:44:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 333 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '性别男', '0');
INSERT INTO `sys_dict_data` VALUES (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '性别女', '0');
INSERT INTO `sys_dict_data` VALUES (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '性别未知', '0');
INSERT INTO `sys_dict_data` VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '显示菜单', '0');
INSERT INTO `sys_dict_data` VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '隐藏菜单', '0');
INSERT INTO `sys_dict_data` VALUES (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '正常状态', '0');
INSERT INTO `sys_dict_data` VALUES (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '停用状态', '0');
INSERT INTO `sys_dict_data` VALUES (8, 1, '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '正常状态', '0');
INSERT INTO `sys_dict_data` VALUES (9, 2, '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '停用状态', '0');
INSERT INTO `sys_dict_data` VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '默认分组', '0');
INSERT INTO `sys_dict_data` VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '系统分组', '0');
INSERT INTO `sys_dict_data` VALUES (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '系统默认是', '0');
INSERT INTO `sys_dict_data` VALUES (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '系统默认否', '0');
INSERT INTO `sys_dict_data` VALUES (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '通知', '0');
INSERT INTO `sys_dict_data` VALUES (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '公告', '0');
INSERT INTO `sys_dict_data` VALUES (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '正常状态', '0');
INSERT INTO `sys_dict_data` VALUES (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '关闭状态', '0');
INSERT INTO `sys_dict_data` VALUES (18, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '其他操作', '0');
INSERT INTO `sys_dict_data` VALUES (19, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '新增操作', '0');
INSERT INTO `sys_dict_data` VALUES (20, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '修改操作', '0');
INSERT INTO `sys_dict_data` VALUES (21, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '删除操作', '0');
INSERT INTO `sys_dict_data` VALUES (22, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '授权操作', '0');
INSERT INTO `sys_dict_data` VALUES (23, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '导出操作', '0');
INSERT INTO `sys_dict_data` VALUES (24, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '导入操作', '0');
INSERT INTO `sys_dict_data` VALUES (25, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '强退操作', '0');
INSERT INTO `sys_dict_data` VALUES (26, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '生成操作', '0');
INSERT INTO `sys_dict_data` VALUES (27, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '清空操作', '0');
INSERT INTO `sys_dict_data` VALUES (28, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '正常状态', '0');
INSERT INTO `sys_dict_data` VALUES (29, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '停用状态', '0');
INSERT INTO `sys_dict_data` VALUES (100, 1, '本地存储', 'local', 'sys_storage_type', '', '', 'N', '0', 'admin', '2025-07-11 11:49:36', '', '2025-07-11 11:49:36', '本地文件存储', '0');
INSERT INTO `sys_dict_data` VALUES (101, 2, '阿里云OSS', 'aliyun', 'sys_storage_type', '', '', 'Y', '0', 'admin', '2025-07-11 11:49:41', '', '2025-07-11 11:49:41', '阿里云对象存储', '0');
INSERT INTO `sys_dict_data` VALUES (102, 3, '腾讯云COS', 'tencent', 'sys_storage_type', '', '', 'N', '0', 'admin', '2025-07-11 11:49:49', '', '2025-07-11 11:49:49', '腾讯云对象存储', '0');
INSERT INTO `sys_dict_data` VALUES (103, 4, '亚马逊S3', 'amazon', 'sys_storage_type', '', '', 'N', '0', 'admin', '2025-07-11 11:49:57', '', '2025-07-11 11:49:57', '亚马逊云存储', '0');
INSERT INTO `sys_dict_data` VALUES (104, 5, '微软Azure', 'azure', 'sys_storage_type', '', '', 'N', '0', 'admin', '2025-07-11 11:50:03', '', '2025-07-11 11:50:03', '微软Azure云存储', '0');
INSERT INTO `sys_dict_data` VALUES (105, 1, '图片', 'image', 'sys_file_type', '', 'primary', 'N', '0', 'admin', '2025-07-11 11:50:10', '', '2025-07-11 11:50:10', '图片文件', '0');
INSERT INTO `sys_dict_data` VALUES (106, 2, '文档', 'document', 'sys_file_type', '', 'success', 'N', '0', 'admin', '2025-07-11 11:50:16', '', '2025-07-11 11:50:16', '文档文件', '0');
INSERT INTO `sys_dict_data` VALUES (107, 3, '视频', 'video', 'sys_file_type', '', 'warning', 'N', '0', 'admin', '2025-07-11 11:50:21', '', '2025-07-11 11:50:21', '视频文件', '0');
INSERT INTO `sys_dict_data` VALUES (108, 4, '音频', 'audio', 'sys_file_type', '', 'info', 'N', '0', 'admin', '2025-07-11 11:50:28', '', '2025-07-11 11:50:28', '音频文件', '0');
INSERT INTO `sys_dict_data` VALUES (109, 5, '压缩包', 'archive', 'sys_file_type', '', 'default', 'N', '0', 'admin', '2025-07-11 11:50:34', '', '2025-07-11 11:50:34', '压缩包文件', '0');
INSERT INTO `sys_dict_data` VALUES (110, 6, '其他', 'other', 'sys_file_type', '', 'default', 'N', '0', 'admin', '2025-07-11 11:50:46', '', '2025-07-11 11:50:46', '其他类型文件', '0');
INSERT INTO `sys_dict_data` VALUES (111, 1, '成功', '1', 'sys_success_fail', '', 'success', 'N', '0', 'admin', '2025-07-11 11:50:53', '', '2025-07-11 11:50:53', '成功状态', '0');
INSERT INTO `sys_dict_data` VALUES (112, 2, '失败', '0', 'sys_success_fail', '', 'danger', 'N', '0', 'admin', '2025-07-11 11:50:58', '', '2025-07-11 11:50:58', '失败状态', '0');
INSERT INTO `sys_dict_data` VALUES (113, 1, '云厂商', '1', 'sys_provider_type', '', 'primary', 'N', '0', 'admin', '2025-07-11 16:05:49', '', '2025-07-11 16:05:49', '云计算服务厂商', '0');
INSERT INTO `sys_dict_data` VALUES (114, 2, '应用厂商', '2', 'sys_provider_type', '', 'info', 'N', '0', 'admin', '2025-07-11 16:05:58', '', '2025-07-11 16:05:58', '应用软件服务厂商', '0');
INSERT INTO `sys_dict_data` VALUES (115, 1, 'AccessKey/SecretKey', 'AK_SK', 'sys_key_type', '', 'primary', 'Y', '0', 'admin', '2025-07-11 16:06:15', '', '2025-07-11 16:06:15', '云厂商常用的AccessKey/SecretKey密钥对', '0');
INSERT INTO `sys_dict_data` VALUES (116, 2, '应用密钥', 'APP_SECRET', 'sys_key_type', '', 'info', 'N', '0', 'admin', '2025-07-11 16:06:24', '', '2025-07-11 16:06:24', '应用软件的AppId/AppSecret密钥', '0');
INSERT INTO `sys_dict_data` VALUES (117, 3, 'API密钥', 'API_KEY', 'sys_key_type', '', 'success', 'N', '0', 'admin', '2025-07-11 16:06:33', '', '2025-07-11 16:06:33', '简单的API密钥认证', '0');
INSERT INTO `sys_dict_data` VALUES (118, 4, '访问令牌', 'TOKEN', 'sys_key_type', '', 'warning', 'N', '0', 'admin', '2025-07-11 16:06:41', '', '2025-07-11 16:06:41', '传统的Token令牌认证', '0');
INSERT INTO `sys_dict_data` VALUES (119, 1, '个人', '1', 'sys_scope_type', '', 'primary', 'Y', '0', 'admin', '2025-07-11 16:06:58', '', '2025-07-11 16:06:58', '个人使用的密钥', '0');
INSERT INTO `sys_dict_data` VALUES (120, 2, '公司', '2', 'sys_scope_type', '', 'success', 'N', '0', 'admin', '2025-07-11 16:07:06', '', '2025-07-11 16:07:06', '公司使用的密钥', '0');
INSERT INTO `sys_dict_data` VALUES (121, 3, '客户', '3', 'sys_scope_type', '', 'info', 'N', '0', 'admin', '2025-07-11 16:07:14', '', '2025-07-11 16:07:14', '客户使用的密钥', '0');
INSERT INTO `sys_dict_data` VALUES (122, 1, 'MySQL', 'MySQL', 'rds_engine_type', '', 'primary', 'N', '0', 'admin', '2025-07-11 17:22:48', '', '2025-07-11 17:22:48', 'MySQL数据库', '0');
INSERT INTO `sys_dict_data` VALUES (123, 2, 'PostgreSQL', 'PostgreSQL', 'rds_engine_type', '', 'info', 'N', '0', 'admin', '2025-07-11 17:22:48', '', '2025-07-11 17:22:48', 'PostgreSQL数据库', '0');
INSERT INTO `sys_dict_data` VALUES (124, 3, 'SQL Server', 'SQLServer', 'rds_engine_type', '', 'warning', 'N', '0', 'admin', '2025-07-11 17:22:48', '', '2025-07-11 17:22:48', 'SQL Server数据库', '0');
INSERT INTO `sys_dict_data` VALUES (125, 4, 'MariaDB', 'MariaDB', 'rds_engine_type', '', 'success', 'N', '0', 'admin', '2025-07-11 17:22:49', '', '2025-07-11 17:22:49', 'MariaDB数据库', '0');
INSERT INTO `sys_dict_data` VALUES (126, 1, '运行中', 'Running', 'rds_instance_status', '', 'success', 'N', '0', 'admin', '2025-07-11 17:23:08', '', '2025-07-11 17:23:08', '实例正常运行', '0');
INSERT INTO `sys_dict_data` VALUES (127, 2, '创建中', 'Creating', 'rds_instance_status', '', 'info', 'N', '0', 'admin', '2025-07-11 17:23:08', '', '2025-07-11 17:23:08', '实例创建中', '0');
INSERT INTO `sys_dict_data` VALUES (128, 3, '重启中', 'Rebooting', 'rds_instance_status', '', 'warning', 'N', '0', 'admin', '2025-07-11 17:23:08', '', '2025-07-11 17:23:08', '实例重启中', '0');
INSERT INTO `sys_dict_data` VALUES (129, 4, '删除中', 'Deleting', 'rds_instance_status', '', 'danger', 'N', '0', 'admin', '2025-07-11 17:23:08', '', '2025-07-11 17:23:08', '实例删除中', '0');
INSERT INTO `sys_dict_data` VALUES (130, 5, '已锁定', 'Locked', 'rds_instance_status', '', 'danger', 'N', '0', 'admin', '2025-07-11 17:23:09', '', '2025-07-11 17:23:09', '实例已被锁定', '0');
INSERT INTO `sys_dict_data` VALUES (131, 1, '主实例', 'Primary', 'rds_instance_type', '', 'primary', 'N', '0', 'admin', '2025-07-11 17:23:23', '', '2025-07-11 17:23:23', '主实例', '0');
INSERT INTO `sys_dict_data` VALUES (132, 2, '只读实例', 'ReadOnly', 'rds_instance_type', '', 'info', 'N', '0', 'admin', '2025-07-11 17:23:23', '', '2025-07-11 17:23:23', '只读实例', '0');
INSERT INTO `sys_dict_data` VALUES (133, 3, '灾备实例', 'Guard', 'rds_instance_type', '', 'warning', 'N', '0', 'admin', '2025-07-11 17:23:23', '', '2025-07-11 17:23:23', '灾备实例', '0');
INSERT INTO `sys_dict_data` VALUES (134, 4, '临时实例', 'Temp', 'rds_instance_type', '', 'success', 'N', '0', 'admin', '2025-07-11 17:23:23', '', '2025-07-11 17:23:23', '临时实例', '0');
INSERT INTO `sys_dict_data` VALUES (135, 1, '按量付费', 'Postpaid', 'rds_pay_type', '', 'primary', 'N', '0', 'admin', '2025-07-11 17:23:35', '', '2025-07-11 17:23:35', '按使用量付费', '0');
INSERT INTO `sys_dict_data` VALUES (136, 2, '包年包月', 'Prepaid', 'rds_pay_type', '', 'success', 'N', '0', 'admin', '2025-07-11 17:23:35', '', '2025-07-11 17:23:35', '包年包月付费', '0');
INSERT INTO `sys_dict_data` VALUES (137, 1, '阿里云', 'aliyun', 'sys_provider_brand', '', '', 'N', '0', 'admin', '2025-07-11 17:31:43', 'admin', '2025-07-11 17:31:43', '阿里云计算', '0');
INSERT INTO `sys_dict_data` VALUES (138, 2, '腾讯云', 'tencent', 'sys_provider_brand', '', '', 'N', '0', 'admin', '2025-07-11 17:31:51', 'admin', '2025-07-11 17:31:51', '腾讯云计算', '0');
INSERT INTO `sys_dict_data` VALUES (139, 3, '华为云', 'huawei', 'sys_provider_brand', '', '', 'N', '0', 'admin', '2025-07-11 17:31:57', 'admin', '2025-07-11 17:31:57', '华为云计算', '0');
INSERT INTO `sys_dict_data` VALUES (140, 4, '百度云', 'baidu', 'sys_provider_brand', '', '', 'N', '0', 'admin', '2025-07-11 17:32:06', 'admin', '2025-07-11 17:32:06', '百度智能云', '0');
INSERT INTO `sys_dict_data` VALUES (141, 5, '京东云', 'jdcloud', 'sys_provider_brand', '', '', 'N', '0', 'admin', '2025-07-11 17:32:16', 'admin', '2025-07-11 17:32:16', '京东智联云', '0');
INSERT INTO `sys_dict_data` VALUES (142, 6, 'AWS', 'aws', 'sys_provider_brand', '', '', 'N', '0', 'admin', '2025-07-11 17:32:25', 'admin', '2025-07-11 17:32:25', 'Amazon Web Services', '0');
INSERT INTO `sys_dict_data` VALUES (143, 7, 'Azure', 'azure', 'sys_provider_brand', '', '', 'N', '0', 'admin', '2025-07-11 17:32:31', 'admin', '2025-07-11 17:32:31', 'Microsoft Azure', '0');
INSERT INTO `sys_dict_data` VALUES (144, 1, '郑州（联通云）', 'cn-zhengzhou-jva', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:47', 'admin', '2025-07-30 14:14:47', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (145, 2, '乌兰察布专属云HDG', 'cn-wulanchabu-acdr-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:47', 'admin', '2025-07-30 14:14:47', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (146, 3, '华东1（杭州）', 'cn-hangzhou', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:48', 'admin', '2025-07-30 14:14:48', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (163, 4, '华东2（上海）', 'cn-shanghai', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:49', 'admin', '2025-07-30 14:14:49', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (182, 5, '华东3（南通）', 'cn-nantong', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:51', 'admin', '2025-07-30 14:14:51', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (184, 6, '华东5（南京-本地地域）', 'cn-nanjing', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:51', 'admin', '2025-07-30 14:14:51', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (185, 7, '华东6（福州-本地地域）', 'cn-fuzhou', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:52', 'admin', '2025-07-30 14:14:52', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (186, 8, '华北1（青岛）', 'cn-qingdao', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:52', 'admin', '2025-07-30 14:14:52', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (189, 9, '华北2（北京）', 'cn-beijing', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:52', 'admin', '2025-07-30 14:14:52', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (208, 10, '华北3（张家口）', 'cn-zhangjiakou', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:54', 'admin', '2025-07-30 14:14:54', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (215, 11, '华北5（呼和浩特）', 'cn-huhehaote', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:55', 'admin', '2025-07-30 14:14:55', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (218, 12, '华北6（乌兰察布）', 'cn-wulanchabu', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:55', 'admin', '2025-07-30 14:14:55', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (221, 13, '华南1（深圳）', 'cn-shenzhen', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:55', 'admin', '2025-07-30 14:14:55', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (240, 14, '华南2（河源）', 'cn-heyuan', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:57', 'admin', '2025-07-30 14:14:57', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (244, 15, '华南3（广州）', 'cn-guangzhou', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:57', 'admin', '2025-07-30 14:14:57', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (246, 16, '华中1（武汉-本地地域）', 'cn-wuhan-lr', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:58', 'admin', '2025-07-30 14:14:58', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (247, 17, '西南1（成都）', 'cn-chengdu', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:58', 'admin', '2025-07-30 14:14:58', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (251, 18, '中国香港', 'cn-hongkong', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:58', 'admin', '2025-07-30 14:14:58', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (255, 19, '新加坡', 'ap-southeast-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:58', 'admin', '2025-07-30 14:14:58', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (265, 20, '韩国（首尔）', 'ap-northeast-2', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:14:59', 'admin', '2025-07-30 14:14:59', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (267, 21, '马来西亚（吉隆坡）', 'ap-southeast-3', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:00', 'admin', '2025-07-30 14:15:00', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (271, 22, '印度尼西亚（雅加达）', 'ap-southeast-5', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:00', 'admin', '2025-07-30 14:15:00', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (275, 23, '日本（东京）', 'ap-northeast-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:00', 'admin', '2025-07-30 14:15:00', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (279, 24, '德国（法兰克福）', 'eu-central-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:01', 'admin', '2025-07-30 14:15:01', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (283, 25, '英国（伦敦）', 'eu-west-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:01', 'admin', '2025-07-30 14:15:01', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (286, 26, '美国（硅谷）', 'us-west-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:01', 'admin', '2025-07-30 14:15:01', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (289, 27, '美国（弗吉尼亚）', 'us-east-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:02', 'admin', '2025-07-30 14:15:02', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (292, 28, '阿联酋（迪拜）', 'me-east-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:02', 'admin', '2025-07-30 14:15:02', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (294, 29, '菲律宾（马尼拉）', 'ap-southeast-6', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:02', 'admin', '2025-07-30 14:15:02', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (295, 30, '墨西哥', 'na-south-1', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:02', 'admin', '2025-07-30 14:15:02', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (296, 31, '泰国（曼谷）', 'ap-southeast-7', 'aliyun_region', NULL, NULL, 'N', '0', 'admin', '2025-07-30 14:15:02', 'admin', '2025-07-30 14:15:02', NULL, '0');
INSERT INTO `sys_dict_data` VALUES (306, 1, '顺序执行', 'sequential', 'ai_workflow_type', '', 'primary', 'Y', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '按步骤顺序执行工作流', '0');
INSERT INTO `sys_dict_data` VALUES (307, 2, '循环执行', 'loop', 'ai_workflow_type', '', 'info', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '循环执行工作流步骤', '0');
INSERT INTO `sys_dict_data` VALUES (308, 3, '条件执行', 'conditional', 'ai_workflow_type', '', 'warning', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '根据条件执行不同分支', '0');
INSERT INTO `sys_dict_data` VALUES (309, 4, '并行执行', 'parallel', 'ai_workflow_type', '', 'success', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '并行执行多个步骤', '0');
INSERT INTO `sys_dict_data` VALUES (310, 1, '等待中', 'pending', 'ai_workflow_execution_status', '', 'info', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '工作流等待执行', '0');
INSERT INTO `sys_dict_data` VALUES (311, 2, '运行中', 'running', 'ai_workflow_execution_status', '', 'primary', 'Y', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '工作流正在执行', '0');
INSERT INTO `sys_dict_data` VALUES (312, 3, '已完成', 'completed', 'ai_workflow_execution_status', '', 'success', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '工作流执行完成', '0');
INSERT INTO `sys_dict_data` VALUES (313, 4, '执行失败', 'failed', 'ai_workflow_execution_status', '', 'danger', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '工作流执行失败', '0');
INSERT INTO `sys_dict_data` VALUES (314, 5, '已取消', 'cancelled', 'ai_workflow_execution_status', '', 'warning', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '工作流执行被取消', '0');
INSERT INTO `sys_dict_data` VALUES (315, 6, '已暂停', 'paused', 'ai_workflow_execution_status', '', 'info', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '工作流执行暂停', '0');
INSERT INTO `sys_dict_data` VALUES (316, 1, '正常', '0', 'ai_workflow_step_status', '', 'primary', 'Y', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '步骤状态正常', '0');
INSERT INTO `sys_dict_data` VALUES (317, 2, '停用', '1', 'ai_workflow_step_status', '', 'danger', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', '步骤状态停用', '0');
INSERT INTO `sys_dict_data` VALUES (318, 1, 'GitHub趋势', 'github_trending', 'ai_workflow_tool_type', '', 'primary', 'N', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', 'GitHub趋势分析工具', '0');
INSERT INTO `sys_dict_data` VALUES (319, 2, '数据库查询', 'database_query', 'ai_workflow_tool_type', '', 'info', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '数据库查询工具', '0');
INSERT INTO `sys_dict_data` VALUES (320, 3, '文件操作', 'file_operation', 'ai_workflow_tool_type', '', 'success', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '文件读写操作工具', '0');
INSERT INTO `sys_dict_data` VALUES (321, 4, 'HTTP请求', 'http_request', 'ai_workflow_tool_type', '', 'warning', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', 'HTTP API请求工具', '0');
INSERT INTO `sys_dict_data` VALUES (322, 5, '邮件发送', 'email_sender', 'ai_workflow_tool_type', '', 'danger', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '邮件发送工具', '0');
INSERT INTO `sys_dict_data` VALUES (323, 6, '文本处理', 'text_processor', 'ai_workflow_tool_type', '', 'info', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '文本处理工具', '0');
INSERT INTO `sys_dict_data` VALUES (324, 7, '图像处理', 'image_processor', 'ai_workflow_tool_type', '', 'primary', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '图像处理工具', '0');
INSERT INTO `sys_dict_data` VALUES (325, 8, '数据分析', 'data_analyzer', 'ai_workflow_tool_type', '', 'success', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '数据分析工具', '0');
INSERT INTO `sys_dict_data` VALUES (326, 1, '启用', '1', 'ai_workflow_enabled_status', '', 'primary', 'Y', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '工作流启用状态', '0');
INSERT INTO `sys_dict_data` VALUES (327, 2, '禁用', '0', 'ai_workflow_enabled_status', '', 'danger', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '工作流禁用状态', '0');
INSERT INTO `sys_dict_data` VALUES (328, 1, '字符串', 'string', 'ai_workflow_variable_type', '', 'primary', 'Y', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '字符串类型变量', '0');
INSERT INTO `sys_dict_data` VALUES (329, 2, '数字', 'number', 'ai_workflow_variable_type', '', 'info', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '数字类型变量', '0');
INSERT INTO `sys_dict_data` VALUES (330, 3, '布尔值', 'boolean', 'ai_workflow_variable_type', '', 'success', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '布尔类型变量', '0');
INSERT INTO `sys_dict_data` VALUES (331, 4, 'JSON对象', 'json', 'ai_workflow_variable_type', '', 'warning', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', 'JSON对象类型变量', '0');
INSERT INTO `sys_dict_data` VALUES (332, 5, '数组', 'array', 'ai_workflow_variable_type', '', 'danger', 'N', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', '数组类型变量', '0');

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 118 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', '2024-12-27 10:06:44', '', '2024-12-28 16:42:53', '用户性别列表', '0');
INSERT INTO `sys_dict_type` VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '菜单状态列表', '0');
INSERT INTO `sys_dict_type` VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '系统开关列表', '0');
INSERT INTO `sys_dict_type` VALUES (4, '任务状态', 'sys_job_status', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '任务状态列表', '0');
INSERT INTO `sys_dict_type` VALUES (5, '任务分组', 'sys_job_group', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '任务分组列表', '0');
INSERT INTO `sys_dict_type` VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '系统是否列表', '0');
INSERT INTO `sys_dict_type` VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '通知类型列表', '0');
INSERT INTO `sys_dict_type` VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '通知状态列表', '0');
INSERT INTO `sys_dict_type` VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '操作类型列表', '0');
INSERT INTO `sys_dict_type` VALUES (10, '系统状态', 'sys_common_status', '0', 'admin', '2024-12-27 10:06:44', '', NULL, '登录状态列表', '0');
INSERT INTO `sys_dict_type` VALUES (100, '存储类型', 'sys_storage_type', '0', 'admin', '2025-07-11 11:30:02', '', '2025-07-11 11:30:02', '文件存储类型列表', '0');
INSERT INTO `sys_dict_type` VALUES (101, '文件类型', 'sys_file_type', '0', 'admin', '2025-07-11 11:30:19', '', '2025-07-11 11:30:19', '文件类型列表', '0');
INSERT INTO `sys_dict_type` VALUES (102, '成功失败', 'sys_success_fail', '0', 'admin', '2025-07-11 11:30:30', '', '2025-07-11 11:30:30', '成功失败状态列表', '0');
INSERT INTO `sys_dict_type` VALUES (103, '厂商类型', 'sys_provider_type', '0', 'admin', '2025-07-11 16:05:40', '', '2025-07-11 16:05:40', '云厂商和应用厂商分类', '0');
INSERT INTO `sys_dict_type` VALUES (104, '密钥类型', 'sys_key_type', '0', 'admin', '2025-07-11 16:06:07', '', '2025-07-11 16:06:07', '不同类型的密钥分类', '0');
INSERT INTO `sys_dict_type` VALUES (105, '使用范围', 'sys_scope_type', '0', 'admin', '2025-07-11 16:06:49', '', '2025-07-11 16:06:49', '密钥的使用范围分类', '0');
INSERT INTO `sys_dict_type` VALUES (106, 'RDS数据库类型', 'rds_engine_type', '0', 'admin', '2025-07-11 17:22:48', '', '2025-07-11 17:22:48', 'RDS支持的数据库引擎类型', '0');
INSERT INTO `sys_dict_type` VALUES (107, 'RDS实例状态', 'rds_instance_status', '0', 'admin', '2025-07-11 17:23:08', '', '2025-07-11 17:23:08', 'RDS实例的运行状态', '0');
INSERT INTO `sys_dict_type` VALUES (108, 'RDS实例类型', 'rds_instance_type', '0', 'admin', '2025-07-11 17:23:23', '', '2025-07-11 17:23:23', 'RDS实例的类型分类', '0');
INSERT INTO `sys_dict_type` VALUES (109, 'RDS付费类型', 'rds_pay_type', '0', 'admin', '2025-07-11 17:23:35', '', '2025-07-11 17:23:35', 'RDS实例的计费模式', '0');
INSERT INTO `sys_dict_type` VALUES (110, '厂商品牌', 'sys_provider_brand', '0', 'admin', '2025-07-11 17:31:33', 'admin', '2025-07-11 17:31:33', '用于标识不同的云厂商品牌', '0');
INSERT INTO `sys_dict_type` VALUES (111, '阿里云地域', 'aliyun_region', '0', 'admin', '2025-07-30 13:40:11', 'admin', '2025-07-30 13:40:11', NULL, '0');
INSERT INTO `sys_dict_type` VALUES (112, '工作流类型', 'ai_workflow_type', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', 'AI工作流执行类型列表', '0');
INSERT INTO `sys_dict_type` VALUES (113, '工作流执行状态', 'ai_workflow_execution_status', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', 'AI工作流执行状态列表', '0');
INSERT INTO `sys_dict_type` VALUES (114, '工作流步骤状态', 'ai_workflow_step_status', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', 'AI工作流步骤状态列表', '0');
INSERT INTO `sys_dict_type` VALUES (115, '工作流工具类型', 'ai_workflow_tool_type', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', 'AI工作流支持的工具类型列表', '0');
INSERT INTO `sys_dict_type` VALUES (116, '工作流启用状态', 'ai_workflow_enabled_status', '0', 'admin', '2025-10-16 11:55:07', 'admin', '2025-10-16 11:55:07', 'AI工作流启用状态列表', '0');
INSERT INTO `sys_dict_type` VALUES (117, '工作流变量类型', 'ai_workflow_variable_type', '0', 'admin', '2025-10-16 11:55:08', 'admin', '2025-10-16 11:55:08', 'AI工作流变量类型列表', '0');

SET FOREIGN_KEY_CHECKS = 1;
