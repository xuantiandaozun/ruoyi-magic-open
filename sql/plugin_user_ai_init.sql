-- ============================================================
-- 插件用户体系 & AI 免费配额初始化脚本
-- 执行前请确认数据库连接正确
-- ============================================================

-- ------------------------------------------------------------
-- 1. sys_oauth_account 表补全字段（若尚未有 avatar / raw_json）
--    MyBatis-Flex 会自动建表，此处仅作备注，正常情况无需执行
-- ------------------------------------------------------------
-- ALTER TABLE sys_oauth_account ADD COLUMN avatar varchar(500) DEFAULT NULL COMMENT '头像URL';
-- ALTER TABLE sys_oauth_account ADD COLUMN raw_json text DEFAULT NULL COMMENT '第三方原始 JSON';


-- ------------------------------------------------------------
-- 2. 插件免费用户配额规则
--    quota_code: plugin_free_daily
--    product_type: plugin
--    user_tier: free
--    quota_period: daily
--    request_limit: 20（每日 20 次）
--    token_limit: 100000（每日 10w tokens）
-- ------------------------------------------------------------
INSERT INTO ai_usage_quota (
    quota_code, user_id, user_tier, product_type,
    quota_period, request_limit, token_limit, image_limit,
    concurrent_limit, cost_limit, reset_time, enabled, del_flag,
    create_by, update_by, create_time, update_time
)
SELECT
    'plugin_free_daily', NULL, 'free', 'plugin',
    'daily', 20, 100000, 0,
    2, NULL, '00:05:00', 'Y', '0',
    'system', 'system', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_usage_quota
    WHERE quota_code = 'plugin_free_daily' AND del_flag = '0'
);


-- ------------------------------------------------------------
-- 3. OpenRouter 免费模型配置示例
--    实际 api_key 请替换为你的 OpenRouter API Key
--    model 名称来自 OpenRouter 免费模型列表（:free 后缀）
--    OpenRouterFreeModelSyncTask 会自动同步并更新 free_available 字段
-- ------------------------------------------------------------

-- 主力免费模型：deepseek/deepseek-chat:free
INSERT INTO ai_model_config (
    provider, capability, model, api_key_ref,
    endpoint, context_window, max_output_tokens,
    supports_stream, supports_vision, free_available,
    tool_call_delay, enabled, is_default, status, del_flag,
    create_by, update_by, create_time, update_time
)
SELECT
    'openrouter', 'chat', 'deepseek/deepseek-chat:free', 'OPENROUTER_API_KEY',
    'https://openrouter.ai/api/v1', 65536, 4096,
    'Y', 'N', 1,
    0, 'Y', 'N', '0', '0',
    'system', 'system', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_model_config
    WHERE model = 'deepseek/deepseek-chat:free' AND del_flag = '0'
);

-- 备用免费模型：meta-llama/llama-3.3-8b-instruct:free
INSERT INTO ai_model_config (
    provider, capability, model, api_key_ref,
    endpoint, context_window, max_output_tokens,
    supports_stream, supports_vision, free_available,
    tool_call_delay, enabled, is_default, status, del_flag,
    create_by, update_by, create_time, update_time
)
SELECT
    'openrouter', 'chat', 'meta-llama/llama-3.3-8b-instruct:free', 'OPENROUTER_API_KEY',
    'https://openrouter.ai/api/v1', 131072, 4096,
    'Y', 'N', 1,
    0, 'Y', 'N', '0', '0',
    'system', 'system', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_model_config
    WHERE model = 'meta-llama/llama-3.3-8b-instruct:free' AND del_flag = '0'
);


-- ------------------------------------------------------------
-- 4. 插件 free 用户 AI 模型路由
--    product_type: plugin
--    scene_code: chat
--    user_tier: free
--    primary_model_config_id → deepseek-chat:free
--    fallback_model_config_id → llama:free
-- ------------------------------------------------------------
INSERT INTO ai_model_route (
    route_code, product_type, scene_code, user_tier,
    primary_model_config_id, fallback_model_config_id,
    rag_enabled, enabled, del_flag,
    create_by, update_by, create_time, update_time
)
SELECT
    'plugin_free_chat', 'plugin', 'chat', 'free',
    (SELECT id FROM ai_model_config WHERE model = 'deepseek/deepseek-chat:free' AND del_flag = '0' LIMIT 1),
    (SELECT id FROM ai_model_config WHERE model = 'meta-llama/llama-3.3-8b-instruct:free' AND del_flag = '0' LIMIT 1),
    'N', 'Y', '0',
    'system', 'system', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_model_route
    WHERE route_code = 'plugin_free_chat' AND del_flag = '0'
);


-- ------------------------------------------------------------
-- 5. （可选）ai_model_policy 插件 free 用户策略
--    关联到 deepseek:free 模型配置，控制每次请求的 context/output 上限
-- ------------------------------------------------------------
INSERT INTO ai_model_policy (
    model_config_id, product_type, user_tier, priority,
    daily_request_limit, daily_token_limit, daily_image_limit,
    max_context_tokens, max_output_tokens,
    del_flag, create_by, update_by, create_time, update_time
)
SELECT
    (SELECT id FROM ai_model_config WHERE model = 'deepseek/deepseek-chat:free' AND del_flag = '0' LIMIT 1),
    'plugin', 'free', 1,
    20, 100000, 0,
    8192, 2048,
    '0', 'system', 'system', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_model_policy
    WHERE product_type = 'plugin' AND user_tier = 'free' AND del_flag = '0'
);
