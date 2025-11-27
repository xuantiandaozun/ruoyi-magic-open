-- 为飞书用户表添加密钥关联字段
ALTER TABLE `feishu_users` 
ADD COLUMN `key_id` BIGINT NULL COMMENT '关联的密钥ID' AFTER `updated_at`,
ADD COLUMN `key_name` VARCHAR(100) NULL COMMENT '关联的密钥名称' AFTER `key_id`,
ADD INDEX `idx_key_id` (`key_id`);

-- 为飞书文档表添加密钥ID字段（已有key_name字段）
ALTER TABLE `feishu_doc` 
ADD COLUMN `key_id` BIGINT NULL COMMENT '关联的密钥ID' AFTER `feishu_modified_time`,
ADD INDEX `idx_key_id` (`key_id`);

-- 添加外键约束（可选，如果需要强制约束的话）
-- ALTER TABLE `feishu_users` 
-- ADD CONSTRAINT `fk_feishu_users_key_id` FOREIGN KEY (`key_id`) REFERENCES `sys_secret_key` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- ALTER TABLE `feishu_doc` 
-- ADD CONSTRAINT `fk_feishu_doc_key_id` FOREIGN KEY (`key_id`) REFERENCES `sys_secret_key` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
