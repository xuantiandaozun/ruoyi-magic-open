-- 小账灵鹿 AI 记账小程序初始化
-- app_code: lingxi-ledger

INSERT INTO `mini_app` (
  `app_code`, `app_name`, `platform`, `app_id`, `app_secret`, `enabled`, `status`, `remark`
) VALUES (
  'lingxi-ledger',
  '小账灵鹿',
  'wechat_ma',
  'wxe4f1b4999f06e049',
  '506542529b9a573c4f701aaa6158c18b',
  'Y',
  '0',
  'AI 智能记账微信小程序'
) ON DUPLICATE KEY UPDATE
  `app_name` = VALUES(`app_name`),
  `app_secret` = VALUES(`app_secret`),
  `enabled` = VALUES(`enabled`),
  `remark` = VALUES(`remark`),
  `update_time` = CURRENT_TIMESTAMP;

-- bill_user_profile 增加小程序用户关联（若列已存在请跳过此语句）
ALTER TABLE `bill_user_profile`
  ADD COLUMN `mini_user_id` bigint DEFAULT NULL COMMENT '小程序用户ID(mini_user.id)' AFTER `user_id`;

CREATE INDEX `idx_bill_user_profile_mini_user` ON `bill_user_profile` (`mini_user_id`);
