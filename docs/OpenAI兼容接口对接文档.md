# OpenAI兼容接口对接文档

本文档用于对接当前系统提供的 OpenAI 兼容接口，可直接用于支持 OpenAI 协议的客户端、SDK、Postman 或第三方工具测试。

## 1. 接口基础信息

- 服务域名: `https://ai.zhoudw.vip`
- 接口前缀: `https://ai.zhoudw.vip/api/openai/v1`
- 鉴权方式: `Authorization: Bearer <API_KEY>`
- API Key 格式: `omk_<publicId>_<secret>`

注意:
- 你提供的公网访问域名里虽然有 `/api/`，但当前兼容接口控制器实际路径是 `/api/openai/v1/*`。
- 所以最终调用地址应为 `https://ai.zhoudw.vip/api/openai/v1/...`。

## 2. 已支持接口

当前已实现以下最小兼容接口:

- `GET /models`
- `POST /chat/completions`

当前暂不支持:

- `embeddings`
- `responses`
- `images`
- `audio`
- `tools / tool_calls`
- 多模态图片输入

## 3. 鉴权说明

所有接口都必须带请求头:

```http
Authorization: Bearer omk_xxx_xxx
Content-Type: application/json
```

如果 API Key 无效、过期、禁用，接口会返回 `401`。

## 4. 模型说明

- `GET /models` 返回当前系统中已启用且当前 API Key 允许访问的模型列表。
- `POST /chat/completions` 的 `model` 字段建议显式传入。
- 如果请求里不传 `model`，系统会尝试使用系统参数 `ai.openai.compat.defaultModel`。
- 如果 API Key 配置了“允许模型”，则只能调用被授权的模型。

## 5. 接口详情

### 5.1 查询模型列表

请求:

```bash
curl --location 'https://ai.zhoudw.vip/api/openai/v1/models' \
  --header 'Authorization: Bearer YOUR_API_KEY'
```

示例响应:

```json
{
  "object": "list",
  "data": [
    {
      "id": "openrouter/free",
      "object": "model",
      "created": 1746576000,
      "owned_by": "openrouter"
    },
    {
      "id": "deepseek-chat",
      "object": "model",
      "created": 1746576000,
      "owned_by": "deepseek"
    }
  ]
}
```

### 5.2 非流式聊天

请求:

```bash
curl --location 'https://ai.zhoudw.vip/api/openai/v1/chat/completions' \
  --header 'Authorization: Bearer YOUR_API_KEY' \
  --header 'Content-Type: application/json' \
  --data '{
    "model": "openrouter/free",
    "messages": [
      {
        "role": "system",
        "content": "你是一个简洁的AI助手。"
      },
      {
        "role": "user",
        "content": "用一句话介绍你自己"
      }
    ],
    "stream": false
  }'
```

示例响应:

```json
{
  "id": "chatcmpl-0e4c4d9f5a654b69b2a2b8b4af0c1234",
  "object": "chat.completion",
  "created": 1746600000,
  "model": "openrouter/free",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "我是一个可以帮助你完成问答和文本生成的 AI 助手。"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 25,
    "completion_tokens": 18,
    "total_tokens": 43
  }
}
```

### 5.3 流式聊天

请求:

```bash
curl --location 'https://ai.zhoudw.vip/api/openai/v1/chat/completions' \
  --header 'Authorization: Bearer YOUR_API_KEY' \
  --header 'Content-Type: application/json' \
  --data '{
    "model": "openrouter/free",
    "messages": [
      {
        "role": "user",
        "content": "写一段50字内的自我介绍"
      }
    ],
    "stream": true
  }'
```

响应格式为 `text/event-stream`，示例片段:

```text
data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1746600000,"model":"openrouter/free","choices":[{"index":0,"delta":{"role":"assistant","content":"我"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1746600000,"model":"openrouter/free","choices":[{"index":0,"delta":{"content":"是一个"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1746600000,"model":"openrouter/free","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

## 6. messages 支持格式

当前支持的 `role`:

- `system`
- `user`
- `assistant`

当前支持的 `content`:

1. 纯字符串

```json
{
  "role": "user",
  "content": "你好"
}
```

2. 文本数组

```json
{
  "role": "user",
  "content": [
    {
      "type": "text",
      "text": "你好"
    }
  ]
}
```

不支持:

- `image_url`
- 音频/图片等多模态内容
- `tool_calls`

## 7. 常见错误响应

### 7.1 缺少 API Key

状态码:

```http
401 Unauthorized
```

响应:

```json
{
  "error": {
    "message": "缺少 Bearer API Key",
    "type": "invalid_api_key",
    "code": "invalid_api_key"
  }
}
```

### 7.2 API Key 无效或已过期

```json
{
  "error": {
    "message": "API Key 无效或已过期",
    "type": "invalid_api_key",
    "code": "invalid_api_key"
  }
}
```

### 7.3 model 未传且系统默认模型未配置

状态码:

```http
400 Bad Request
```

响应:

```json
{
  "error": {
    "message": "model 不能为空，且未配置默认模型 ai.openai.compat.defaultModel",
    "type": "invalid_request_error",
    "code": "invalid_request_error"
  }
}
```

### 7.4 模型不存在或未启用

```json
{
  "error": {
    "message": "模型不存在或未启用: openrouter/free",
    "type": "invalid_model",
    "code": "invalid_model"
  }
}
```

### 7.5 API Key 无权访问该模型

状态码:

```http
403 Forbidden
```

响应:

```json
{
  "error": {
    "message": "当前 API Key 不允许调用模型: openrouter/free",
    "type": "model_not_allowed",
    "code": "model_not_allowed"
  }
}
```

## 8. 第三方工具配置示例

### 8.1 OpenAI SDK

Node.js 示例:

```js
import OpenAI from 'openai'

const client = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
  baseURL: 'https://ai.zhoudw.vip/api/openai/v1'
})

const resp = await client.chat.completions.create({
  model: 'openrouter/free',
  messages: [
    { role: 'user', content: '你好，介绍一下你自己' }
  ]
})

console.log(resp.choices[0].message.content)
```

### 8.2 支持 OpenAI 格式的通用客户端

如果工具支持自定义 Base URL，通常配置如下:

- Base URL: `https://ai.zhoudw.vip/api/openai/v1`
- API Key: 你在后台生成的 `omk_...`
- Model: 从 `/models` 返回结果里选择，例如 `openrouter/free`

有些客户端要求填写“OpenAI Host”或“Endpoint”，也同样填:

```text
https://ai.zhoudw.vip/api/openai/v1
```

### 8.3 Postman

请求头:

```http
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json
```

地址:

```text
POST https://ai.zhoudw.vip/api/openai/v1/chat/completions
```

Body:

```json
{
  "model": "openrouter/free",
  "messages": [
    {
      "role": "user",
      "content": "请返回一句测试成功的话"
    }
  ]
}
```

## 9. 联调建议

推荐按以下顺序测试:

1. 先调用 `GET /models`，确认 API Key 生效、模型列表正常返回。
2. 选择一个返回的模型，测试非流式 `POST /chat/completions`。
3. 再测试 `stream: true` 的流式输出。
4. 如果工具不工作，优先检查:
   - Base URL 是否填成 `https://ai.zhoudw.vip/api/openai/v1`
   - 是否带了 `Bearer ` 前缀
   - 选择的模型是否在 `/models` 返回列表中
   - API Key 是否限制了模型范围

## 10. 当前实现边界

当前接口是“OpenAI 最小兼容实现”，目标是让通用客户端先可接入测试。

已兼容:

- 模型列表
- 聊天补全
- 流式输出
- Token usage 返回

暂未兼容:

- Function Calling
- Tools
- JSON Schema 严格输出控制
- 图片理解输入
- Embeddings
- Assistants / Responses API
