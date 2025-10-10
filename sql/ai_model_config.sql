-- ----------------------------
-- Table structure for ai_model_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_model_config`;
CREATE TABLE `ai_model_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `provider` varchar(50) NOT NULL COMMENT '厂商类型（doubao、openai、deepseek、qianwen、glm、operouter等）',
  `capability` varchar(50) NOT NULL COMMENT '模型类型（chat、vision、embedding、image等）',
  `model` varchar(100) NOT NULL COMMENT '模型名称/ID',
  `api_key` varchar(500) DEFAULT NULL COMMENT 'API密钥',
  `endpoint` varchar(500) DEFAULT NULL COMMENT 'API端点',
  `extra_params` text COMMENT '额外参数（JSON）',
  `enabled` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用（Y是 N否）',
  `is_default` char(1) NOT NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_provider_capability` (`provider`, `capability`),
  KEY `idx_enabled_status` (`enabled`, `status`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';