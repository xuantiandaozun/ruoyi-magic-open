-- ----------------------------
-- 飞书文档表(简化版)
-- ----------------------------
DROP TABLE IF EXISTS `feishu_doc`;
CREATE TABLE `feishu_doc` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `token` varchar(100) NOT NULL COMMENT '文档token',
  `name` varchar(255) NOT NULL COMMENT '文档名称',
  `type` varchar(50) NOT NULL COMMENT '文档类型(doc/sheet/bitable/mindnote/file/folder)',
  `url` varchar(500) DEFAULT NULL COMMENT '文档访问URL',
  `owner_id` varchar(100) DEFAULT NULL COMMENT '拥有者ID',
  `parent_token` varchar(100) DEFAULT NULL COMMENT '父文件夹token',
  `is_folder` tinyint(1) DEFAULT '0' COMMENT '是否为文件夹(0-否,1-是)',
  `content` longtext COMMENT '文档内容(缓存)',
  `feishu_created_time` varchar(20) DEFAULT NULL COMMENT '飞书创建时间(时间戳)',
  `feishu_modified_time` varchar(20) DEFAULT NULL COMMENT '飞书修改时间(时间戳)',
  `key_name` varchar(100) DEFAULT NULL COMMENT '关联的密钥名称',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_key` (`token`, `key_name`),
  KEY `idx_name` (`name`),
  KEY `idx_type` (`type`),
  KEY `idx_parent_token` (`parent_token`),
  KEY `idx_key_name` (`key_name`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书文档信息表';

-- ----------------------------
-- 初始化菜单权限
-- ----------------------------

-- 飞书文档管理主菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('飞书文档管理', 2000, 6, 'feishu-doc', 'system/feishu-doc/index', 1, 0, 'C', '0', '0', 'system:feishu:doc:list', 'documentation', 'admin', sysdate(), '', null, '飞书文档管理菜单');

-- 获取飞书文档管理菜单ID
SET @feishu_doc_menu_id = LAST_INSERT_ID();

-- 查询飞书文档列表
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('查询飞书文档', @feishu_doc_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'system:feishu:doc:query', '#', 'admin', sysdate(), '', null, '');

-- 读取文档内容
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('读取文档内容', @feishu_doc_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'system:feishu:doc:read', '#', 'admin', sysdate(), '', null, '');

-- 创建飞书文档
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('创建飞书文档', @feishu_doc_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'system:feishu:doc:create', '#', 'admin', sysdate(), '', null, '');

-- 更新文档内容
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('更新文档内容', @feishu_doc_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'system:feishu:doc:update', '#', 'admin', sysdate(), '', null, '');

-- 飞书文档配置
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('飞书文档配置', @feishu_doc_menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'system:feishu:doc:config', '#', 'admin', sysdate(), '', null, '');

-- ----------------------------
-- 示例数据（可选）
-- ----------------------------
-- INSERT INTO `feishu_doc` VALUES (1, 'JpGKfHMhHlnhrwdNCm4cUA5XnKb', '知识问答', 'folder', 'nodcn9f4APuT0ArsFIeL3PsD3js', 'https://sa31e3tf7x1.feishu.cn/drive/folder/JpGKfHMhHlnhrwdNCm4cUA5XnKb', 'ou_40edd7e6f94000df614ed88b297d70a5', NULL, NULL, 1, 'normal', NULL, NULL, NULL, '1749980058', '1749980058', 'synced', NULL, sysdate(), 'feishu', 'admin', sysdate(), 'admin', sysdate(), '示例文件夹', '0');
-- INSERT INTO `feishu_doc` VALUES (2, 'LNlEde9Ktog3CgxzP8acDvfgnAx', '', 'docx', 'nodcn9f4APuT0ArsFIeL3PsD3js', 'https://sa31e3tf7x1.feishu.cn/docx/LNlEde9Ktog3CgxzP8acDvfgnAx', 'ou_40edd7e6f94000df614ed88b297d70a5', NULL, NULL, 0, 'normal', NULL, NULL, NULL, '1751088721', '1751174873', 'synced', NULL, sysdate(), 'feishu', 'admin', sysdate(), 'admin', sysdate(), '示例文档', '0');