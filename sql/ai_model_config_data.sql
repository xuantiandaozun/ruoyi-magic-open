-- ----------------------------
-- Records of ai_model_config
-- ----------------------------

-- 豆包AI配置
INSERT INTO `ai_model_config` VALUES 
(1, 'doubao', 'chat', 'ep-20241212105607-kcmvs', '', 'https://ark.cn-beijing.volces.com/api/v3', '{"temperature": 0.7, "maxTokens": 4096}', 'Y', 'Y', '0', '0', 'admin', NOW(), 'admin', NOW(), '豆包AI聊天模型'),
(2, 'doubao', 'vision', 'ep-20241212105607-kcmvs', '', 'https://ark.cn-beijing.volces.com/api/v3', '{"temperature": 0.7, "maxTokens": 4096}', 'Y', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), '豆包AI视觉模型'),
(3, 'doubao', 'embedding', 'ep-20241212105607-kcmvs', '', 'https://ark.cn-beijing.volces.com/api/v3', '{"temperature": 0.7, "maxTokens": 4096}', 'Y', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), '豆包AI向量化模型'),
(4, 'doubao', 'image', 'ep-20250818101908-mhzcm', '', 'https://ark.cn-beijing.volces.com/api/v3', '{"temperature": 0.7, "maxTokens": 4096}', 'Y', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), '豆包AI文生图模型');

-- OpenAI配置
INSERT INTO `ai_model_config` VALUES 
(5, 'openai', 'chat', 'gpt-3.5-turbo', '', 'https://api.openai.com/v1', '{"temperature": 0.7, "maxTokens": 4096, "organization": ""}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'OpenAI GPT-3.5 Turbo聊天模型'),
(6, 'openai', 'chat', 'gpt-4', '', 'https://api.openai.com/v1', '{"temperature": 0.7, "maxTokens": 4096, "organization": ""}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'OpenAI GPT-4聊天模型'),
(7, 'openai', 'vision', 'gpt-4-vision-preview', '', 'https://api.openai.com/v1', '{"temperature": 0.7, "maxTokens": 4096, "organization": ""}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'OpenAI GPT-4视觉模型'),
(8, 'openai', 'embedding', 'text-embedding-ada-002', '', 'https://api.openai.com/v1', '{"temperature": 0.7, "maxTokens": 4096, "organization": ""}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'OpenAI文本向量化模型'),
(9, 'openai', 'image', 'dall-e-3', '', 'https://api.openai.com/v1', '{"temperature": 0.7, "maxTokens": 4096, "organization": ""}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'OpenAI DALL-E 3图像生成模型');

-- DeepSeek配置
INSERT INTO `ai_model_config` VALUES 
(10, 'deepseek', 'chat', 'deepseek-chat', '', 'https://api.deepseek.com/v1', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'DeepSeek聊天模型'),
(11, 'deepseek', 'chat', 'deepseek-coder', '', 'https://api.deepseek.com/v1', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'DeepSeek代码模型');

-- 千问配置
INSERT INTO `ai_model_config` VALUES 
(12, 'qianwen', 'chat', 'qwen-turbo', '', 'https://dashscope.aliyuncs.com/api/v1', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), '千问Turbo聊天模型'),
(13, 'qianwen', 'chat', 'qwen-plus', '', 'https://dashscope.aliyuncs.com/api/v1', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), '千问Plus聊天模型'),
(14, 'qianwen', 'vision', 'qwen-vl-plus', '', 'https://dashscope.aliyuncs.com/api/v1', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), '千问视觉模型');

-- 智谱GLM配置
INSERT INTO `ai_model_config` VALUES 
(15, 'glm', 'chat', 'glm-4', '', 'https://open.bigmodel.cn/api/paas/v4', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), '智谱GLM-4聊天模型'),
(16, 'glm', 'vision', 'glm-4v', '', 'https://open.bigmodel.cn/api/paas/v4', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), '智谱GLM-4V视觉模型');

-- Kimi配置
INSERT INTO `ai_model_config` VALUES 
(19, 'kimi', 'chat', 'moonshot-v1-8k', '', 'https://api.moonshot.cn/v1', '{"temperature": 0.7, "maxTokens": 8192}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'Kimi 8K聊天模型'),
(20, 'kimi', 'chat', 'moonshot-v1-32k', '', 'https://api.moonshot.cn/v1', '{"temperature": 0.7, "maxTokens": 32768}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'Kimi 32K聊天模型'),
(21, 'kimi', 'chat', 'moonshot-v1-128k', '', 'https://api.moonshot.cn/v1', '{"temperature": 0.7, "maxTokens": 131072}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'Kimi 128K聊天模型');

-- OneAPI路由配置
INSERT INTO `ai_model_config` VALUES 
(22, 'operouter', 'chat', 'gpt-3.5-turbo', '', 'https://api.oneapi.com/v1', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'OneAPI路由GPT-3.5模型'),
(23, 'operouter', 'chat', 'gpt-4', '', 'https://api.oneapi.com/v1', '{"temperature": 0.7, "maxTokens": 4096}', 'N', 'N', '0', '0', 'admin', NOW(), 'admin', NOW(), 'OneAPI路由GPT-4模型');