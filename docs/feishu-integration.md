# 飞书消息发送集成文档

## 概述

本项目已集成飞书消息发送功能，支持发送文本消息和富文本消息。飞书密钥通过数据库动态配置，支持运行时重新加载配置。

## 功能特性

- ✅ 支持发送文本消息
- ✅ 支持发送富文本消息（Post消息）
- ✅ 密钥从数据库动态获取
- ✅ 支持运行时重新加载配置
- ✅ 完整的错误处理和日志记录
- ✅ RESTful API接口
- ✅ 权限控制

## 配置步骤

### 1. 创建飞书应用

1. 登录 [飞书开放平台](https://open.feishu.cn/)
2. 创建企业自建应用
3. 获取应用的 `App ID` 和 `App Secret`
4. 配置应用权限：
   - `im:message` - 获取与发送单聊、群组消息
   - `im:message:send_as_bot` - 以应用的身份发送消息

### 2. 数据库配置

执行SQL脚本创建密钥管理表：

```sql
-- 执行 sql/sys_secret_key.sql 文件
source sql/sys_secret_key.sql;
```

### 3. 配置飞书密钥

在 `sys_secret_key` 表中插入飞书应用配置：

```sql
INSERT INTO `sys_secret_key` VALUES 
(1, '2', '飞书', 'feishu', 'app_secret', '飞书应用密钥', 
'YOUR_APP_ID', 'YOUR_APP_SECRET', 
'global', '全局', NULL, NULL, '0', '0', 
'admin', NOW(), 'admin', NOW(), 
'飞书应用密钥配置，用于发送消息');
```

**注意：** 请将 `YOUR_APP_ID` 和 `YOUR_APP_SECRET` 替换为实际的飞书应用密钥。

## API接口

### 1. 发送文本消息

**接口地址：** `POST /system/feishu/sendText`

**请求参数：**
- `receiveId` (必填): 接收者ID
- `receiveIdType` (可选): 接收者ID类型，默认为 `user_id`
- `content` (必填): 消息内容
- `keyName` (可选): 指定使用的密钥名称

**示例：**
```bash
# 使用默认密钥
curl -X POST "http://localhost:8080/system/feishu/sendText" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "receiveId=ou_7d8a6e6df7621556ce0d21922b676706ccs&content=Hello World"

# 使用指定密钥
curl -X POST "http://localhost:8080/system/feishu/sendText" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "receiveId=ou_7d8a6e6df7621556ce0d21922b676706ccs&content=Hello World&keyName=飞书智能机器人"
```

### 2. 发送富文本消息

**接口地址：** `POST /system/feishu/sendPost`

**请求参数：**
- `receiveId` (必填): 接收者ID
- `receiveIdType` (可选): 接收者ID类型，默认为 `user_id`
- `title` (必填): 消息标题
- `content` (必填): 消息内容
- `keyName` (可选): 指定使用的密钥名称

**示例：**
```bash
# 使用默认密钥
curl -X POST "http://localhost:8080/system/feishu/sendPost" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "receiveId=ou_7d8a6e6df7621556ce0d21922b676706ccs&title=系统通知&content=这是一条重要通知"

# 使用指定密钥
curl -X POST "http://localhost:8080/system/feishu/sendPost" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "receiveId=ou_7d8a6e6df7621556ce0d21922b676706ccs&title=系统通知&content=这是一条重要通知&keyName=飞书智能机器人"
```

### 3. 发送消息（通用接口）

**接口地址：** `POST /system/feishu/send`

**请求参数：**
- `keyName` (可选): 指定使用的密钥名称，可通过URL参数传递

**请求体：**
```json
{
  "receiveId": "ou_7d8a6e6df7621556ce0d21922b676706ccs",
  "receiveIdType": "user_id",
  "msgType": "text",
  "content": "{\"text\":\"Hello World\"}",
  "uuid": "optional-uuid-for-idempotency"
}
```

**示例：**
```bash
# 使用指定密钥
curl -X POST "http://localhost:8080/system/feishu/send?keyName=飞书智能机器人" \
  -H "Content-Type: application/json" \
  -d '{
    "receiveId": "ou_7d8a6e6df7621556ce0d21922b676706ccs",
    "receiveIdType": "user_id",
    "msgType": "text",
    "content": "{\"text\":\"Hello World\"}"
  }'
```

### 4. 检查配置状态

**接口地址：** `GET /system/feishu/status`

### 5. 重新加载配置

**接口地址：** `POST /system/feishu/reload`

**请求参数：**
- `keyName` (可选): 指定重新加载的密钥名称

**示例：**
```bash
# 重新加载默认配置
curl -X POST "http://localhost:8080/system/feishu/reload"

# 重新加载指定密钥配置
curl -X POST "http://localhost:8080/system/feishu/reload?keyName=飞书智能机器人"
```

## 接收者ID类型说明

- `user_id`: 用户ID（推荐）
- `email`: 用户邮箱
- `open_id`: 用户Open ID
- `union_id`: 用户Union ID
- `chat_id`: 群聊ID

## 权限配置

需要为用户分配以下权限：
- `system:feishu:send` - 发送消息权限
- `system:feishu:config` - 配置管理权限

## 代码示例

## Java代码调用示例

### 1. 注入服务
```java
@Autowired
private IFeishuService feishuService;
```

### 2. 发送文本消息
```java
// 使用默认密钥
boolean result = feishuService.sendTextMessage(
    "ou_7d8a6e6df7621556ce0d21922b676706ccs", // 用户ID
    "user_id", // ID类型
    "Hello World!" // 消息内容
);

// 使用指定密钥
boolean result = feishuService.sendTextMessage(
    "ou_7d8a6e6df7621556ce0d21922b676706ccs", // 用户ID
    "user_id", // ID类型
    "Hello from 智能机器人!", // 消息内容
    "飞书智能机器人" // 密钥名称
);
```

### 3. 发送富文本消息
```java
// 使用默认密钥
FeishuMessageDto messageDto = FeishuMessageDto.createPostMessage(
    "ou_7d8a6e6df7621556ce0d21922b676706ccs", // 用户ID
    "user_id", // ID类型
    "通知标题", // 标题
    "这是消息内容" // 内容
);
boolean result = feishuService.sendMessage(messageDto);

// 使用指定密钥
boolean result = feishuService.sendMessage(messageDto, "飞书智能机器人");
```

### 4. 检查配置状态
```java
boolean available = feishuService.isConfigAvailable();
if (!available) {
    // 配置不可用，处理逻辑
}
```

### 5. 重新加载配置
```java
// 重新加载默认配置
feishuService.reloadConfig();

// 重新加载指定密钥配置
feishuService.reloadConfig("飞书智能机器人");
```

## 故障排除

### 1. 配置检查

```bash
# 检查配置状态
curl -X GET "http://localhost:8080/system/feishu/status"

# 重新加载配置
curl -X POST "http://localhost:8080/system/feishu/reload"
```

### 2. 常见错误

- **配置不可用**: 检查 `sys_secret_key` 表中的飞书配置
- **权限不足**: 确保飞书应用有发送消息的权限
- **用户ID无效**: 确保接收者ID正确且用户在应用可见范围内

### 3. 日志查看

查看应用日志获取详细错误信息：

```bash
tail -f logs/ruoyi.log | grep -i feishu
```

## 安全注意事项

1. **密钥安全**: 飞书应用密钥存储在数据库中，确保数据库访问安全
2. **权限控制**: 合理分配飞书消息发送权限
3. **日志脱敏**: 避免在日志中记录敏感信息
4. **网络安全**: 确保与飞书API的通信安全

## 扩展功能

可以根据需要扩展以下功能：
- 支持更多消息类型（图片、文件、卡片等）
- 消息发送记录和统计
- 批量消息发送
- 消息模板管理
- 定时消息发送

## 多密钥支持

系统支持配置多个飞书应用密钥，通过以下方式管理：

1. **默认密钥选择**：如果没有指定密钥名称，系统会使用第一个创建的可用密钥
2. **指定密钥使用**：通过 `keyName` 参数可以指定使用特定的密钥
3. **密钥识别**：系统通过 `provider_name = '飞书'` 来识别飞书密钥

### 数据库配置示例
```sql
-- 主要的飞书应用
INSERT INTO sys_secret_key (
    provider_name, key_name, access_key, secret_key, status
) VALUES (
    '飞书', '飞书智能机器人', 'cli_a6d431366279100c', 'eKtBqntn8crK3DJTARIGvhGQwtLMLXT2', '0'
);

-- 备用的飞书应用
INSERT INTO sys_secret_key (
    provider_name, key_name, access_key, secret_key, status
) VALUES (
    '飞书', '飞书通知机器人', 'cli_b7e542477380211d', 'fLuCroto9dsL4EKUBSJHwiHRxuNMYU3', '0'
);
```

### API调用示例
```bash
# 使用默认密钥（第一个创建的）
curl -X POST "http://localhost:8080/system/feishu/sendText" \
  -d "receiveId=用户ID&content=Hello World"

# 使用指定密钥
curl -X POST "http://localhost:8080/system/feishu/sendText" \
  -d "receiveId=用户ID&content=Hello World&keyName=飞书智能机器人"
```

## 注意事项

1. **密钥安全**：请妥善保管飞书应用的 App Secret，不要在代码中硬编码
2. **权限配置**：确保飞书应用有发送消息的权限
3. **接收者ID**：接收者ID必须是有效的飞书用户ID
4. **消息限制**：注意飞书API的消息发送频率限制
5. **错误处理**：建议在业务代码中添加适当的错误处理逻辑
6. **多密钥管理**：
   - 密钥通过 `provider_name = '飞书'` 识别
   - 如果不指定 `keyName`，使用第一个创建的可用密钥
   - 指定 `keyName` 时，会查找对应名称的密钥
   - 如果指定的密钥不存在，会回退到使用第一个可用密钥

## 相关文档

- [飞书开放平台文档](https://open.feishu.cn/document/)
- [飞书消息发送API](https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/reference/im-v1/message/create)
- [飞书Java SDK](https://github.com/larksuite/oapi-sdk-java)