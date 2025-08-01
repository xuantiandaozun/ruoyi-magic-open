-- ----------------------------
-- Table structure for sys_secret_key
-- ----------------------------
DROP TABLE IF EXISTS `sys_secret_key`;
CREATE TABLE `sys_secret_key` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '厂商类型（1云厂商 2应用厂商）',
  `provider_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '厂商名称',
  `provider_brand` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '厂商品牌',
  `key_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '密钥类型',
  `key_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '密钥名称/别名',
  `access_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '访问密钥',
  `secret_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '密钥',
  `scope_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '使用范围',
  `scope_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '范围名称',
  `region` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '地域',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_provider` (`provider_name`,`provider_brand`) USING BTREE,
  KEY `idx_status` (`status`,`del_flag`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='密钥管理表' ROW_FORMAT=Dynamic;

-- ----------------------------
-- Records of sys_secret_key (示例数据)
-- ----------------------------
-- 飞书应用密钥示例（请替换为实际的应用ID和密钥）
INSERT INTO `sys_secret_key` VALUES 
(1, '2', '飞书', 'feishu', 'app_secret', '飞书应用密钥', 'YOUR_APP_ID', 'YOUR_APP_SECRET', 'global', '全局', NULL, NULL, '0', '0', 'admin', NOW(), 'admin', NOW(), '飞书应用密钥配置，用于发送消息');

-- 可以添加更多厂商的密钥配置
-- 阿里云示例
-- INSERT INTO `sys_secret_key` VALUES 
-- (2, '1', '阿里云', 'aliyun', 'access_key', '阿里云访问密钥', 'YOUR_ACCESS_KEY_ID', 'YOUR_ACCESS_KEY_SECRET', 'global', '全局', 'cn-hangzhou', NULL, '0', '0', 'admin', NOW(), 'admin', NOW(), '阿里云访问密钥');

-- 腾讯云示例
-- INSERT INTO `sys_secret_key` VALUES 
-- (3, '1', '腾讯云', 'tencent', 'secret_key', '腾讯云密钥', 'YOUR_SECRET_ID', 'YOUR_SECRET_KEY', 'global', '全局', 'ap-guangzhou', NULL, '0', '0', 'admin', NOW(), 'admin', NOW(), '腾讯云访问密钥');