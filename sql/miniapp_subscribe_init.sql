CREATE TABLE IF NOT EXISTS `mini_subscribe_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mini_app_id` bigint NOT NULL,
  `app_code` varchar(64) NOT NULL,
  `scene_code` varchar(64) NOT NULL,
  `template_id` varchar(128) NOT NULL,
  `template_no` varchar(32) DEFAULT NULL,
  `title` varchar(128) DEFAULT NULL,
  `page_path` varchar(255) DEFAULT NULL,
  `field_config_json` text NOT NULL,
  `enabled` char(1) NOT NULL DEFAULT 'Y',
  `sort_order` int NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mini_subscribe_template` (`mini_app_id`, `scene_code`, `template_id`),
  KEY `idx_mini_subscribe_app_scene` (`app_code`, `scene_code`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序订阅消息模板配置';

INSERT INTO mini_subscribe_template (
  mini_app_id,
  app_code,
  scene_code,
  template_id,
  template_no,
  title,
  page_path,
  field_config_json,
  enabled,
  sort_order,
  remark
)
SELECT
  id,
  app_code,
  'translate_task_complete',
  'NVo6dV1rom6hzukeb-Mb1CmF5v-VgNf3pNVtfQJHatg',
  '28117',
  '任务完成通知',
  'pages/tasks/index',
  '[{"fieldName":"thing1","valueExpr":"${document.originalName}","defaultValue":"文档翻译任务","maxLength":20},{"fieldName":"thing2","valueExpr":"文档翻译完成，目标语言${task.targetLanguage}","maxLength":20}]',
  'Y',
  0,
  '后台文档翻译任务完成推送'
FROM mini_app
WHERE app_code = 'yizhou-doc-translate'
  AND del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM mini_subscribe_template t
    WHERE t.app_code = 'yizhou-doc-translate'
      AND t.scene_code = 'translate_task_complete'
      AND t.del_flag = '0'
  );
