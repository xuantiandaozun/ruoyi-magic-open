-- ----------------------------
-- 域名证书监控表
-- ----------------------------
DROP TABLE IF EXISTS `domain_cert_monitor`;
CREATE TABLE `domain_cert_monitor`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '域名',
  `port` int NULL DEFAULT 443 COMMENT '端口号，默认443',
  `issuer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证书颁发者',
  `subject` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证书主体',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '证书过期时间',
  `days_remaining` int NULL DEFAULT NULL COMMENT '剩余天数',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0-正常 1-即将过期 2-已过期 3-检测失败）',
  `last_check_time` datetime NULL DEFAULT NULL COMMENT '最后检测时间',
  `last_notify_time` datetime NULL DEFAULT NULL COMMENT '最后通知时间',
  `notify_enabled` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否开启通知（0-关闭 1-开启）',
  `notify_days` int NULL DEFAULT 3 COMMENT '提前多少天通知',
  `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息（检测失败时记录）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_domain_port`(`domain` ASC, `port` ASC) USING BTREE,
  INDEX `idx_expire_time`(`expire_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_notify_enabled`(`notify_enabled` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '域名证书监控表' ROW_FORMAT = DYNAMIC;
