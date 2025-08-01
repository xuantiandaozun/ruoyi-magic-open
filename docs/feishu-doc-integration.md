# 飞书文档集成功能（简化版）

## 功能概述

本功能实现了与飞书云盘的集成，专为博客功能设计。支持文档的查询、创建、内容获取和更新操作，简化了复杂的同步和权限控制逻辑。

## 功能特性

### 1. 文档查询
- 支持查询飞书云盘中的文档列表
- 支持按文件夹查询文档
- 支持分页查询，提高查询效率
- 支持多种文档类型：文档、电子表格、多维表格、思维笔记等

### 2. 文档操作
- 支持获取文档内容（用于博客展示）
- 支持创建新的飞书文档（用于博客写作）
- 支持更新文档内容（用于博客编辑）
- 支持文档内容缓存，提高访问效率

### 3. 多密钥支持
- 支持配置多个飞书应用密钥
- 支持按密钥名称选择不同的飞书应用
- 支持密钥配置的动态加载和缓存

### 4. 博客集成
- 专为博客功能设计的简化数据结构
- 支持文档内容的本地缓存
- 便于与博客系统的集成和扩展

## 数据库设计

### feishu_doc（飞书文档表-简化版）

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|----------|
| id | bigint | 20 | NO | | 主键ID |
| token | varchar | 100 | NO | | 文档token |
| name | varchar | 255 | NO | | 文档名称 |
| type | varchar | 50 | NO | | 文档类型 |
| url | varchar | 500 | YES | NULL | 文档访问URL |
| owner_id | varchar | 100 | YES | NULL | 拥有者ID |
| parent_token | varchar | 100 | YES | NULL | 父文件夹token |
| is_folder | tinyint | 1 | YES | 0 | 是否为文件夹 |
| content | longtext | | YES | NULL | 文档内容(缓存) |
| feishu_created_time | varchar | 20 | YES | NULL | 飞书创建时间 |
| feishu_modified_time | varchar | 20 | YES | NULL | 飞书修改时间 |
| key_name | varchar | 100 | YES | NULL | 关联的密钥名称 |
| create_by | varchar | 64 | YES | | 创建者 |
| create_time | datetime | | YES | NULL | 创建时间 |
| update_by | varchar | 64 | YES | | 更新者 |
| update_time | datetime | | YES | NULL | 更新时间 |
| remark | varchar | 500 | YES | NULL | 备注 |

**设计说明：**
- 简化了原有的复杂字段，移除了同步状态、权限控制等字段
- 新增了 `content` 字段用于缓存文档内容，便于博客功能使用
- 保留了核心的文档信息字段，满足基本的文档管理需求
- 支持多密钥配置，通过 `key_name` 字段区分不同的飞书应用

## API接口

### 1. 文档查询接口

**接口地址：** `GET /feishu/doc/list`

**请求参数：**
- `pageNum`：页码（可选，默认1）
- `pageSize`：每页大小（可选，默认10）
- `folderToken`：文件夹token（可选，查询指定文件夹下的文档）
- `keyName`：密钥名称（可选）

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 100,
    "rows": [
      {
        "token": "doccnxxxxxx",
        "name": "测试文档",
        "type": "doc",
        "url": "https://xxx.feishu.cn/docx/xxx",
        "isFolder": false,
        "createdTime": "2024-01-01 10:00:00",
        "modifiedTime": "2024-01-01 12:00:00"
      }
    ]
  }
}
```

### 2. 获取文档内容接口

**接口地址：** `GET /feishu/doc/content/{token}`

**请求参数：**
- `token`：文档token（路径参数）
- `keyName`：密钥名称（可选）

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "token": "doccnxxxxxx",
    "name": "测试文档",
    "content": "文档的具体内容...",
    "modifiedTime": "2024-01-01 12:00:00"
  }
}
```

### 3. 创建文档接口

**接口地址：** `POST /feishu/doc/create`

**请求参数：**
```json
{
  "name": "新文档标题",
  "content": "文档初始内容",
  "folderToken": "可选的父文件夹token",
  "keyName": "可选的密钥名称"
}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "文档创建成功",
  "data": {
    "token": "doccnxxxxxx",
    "name": "新文档标题",
    "url": "https://xxx.feishu.cn/docx/xxx"
  }
}
```

### 4. 更新文档内容接口

**接口地址：** `PUT /feishu/doc/content/{token}`

**请求参数：**
- `token`：文档token（路径参数）
```json
{
  "content": "更新后的文档内容",
  "keyName": "可选的密钥名称"
}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "文档更新成功",
  "data": {
    "token": "doccnxxxxxx",
    "modifiedTime": "2024-01-01 15:00:00"
  }
}
```

### 5. 配置状态检查接口

**接口地址：** `GET /feishu/doc/config/status`

**请求参数：**
- `keyName`：密钥名称（可选）

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "available": true,
    "keyName": "default",
    "message": "配置正常"
  }
}
```

### 6. 重新加载配置接口

**接口地址：** `POST /feishu/doc/config/reload`

**请求参数：**
- `keyName`：密钥名称（可选）

**响应示例：**
```json
{
  "code": 200,
  "msg": "配置重新加载成功",
  "data": null
}
```

## 配置说明

### 1. 飞书应用配置

在 `sys_secret_key` 表中配置飞书应用信息：

```sql
INSERT INTO sys_secret_key (key_name, key_value, key_desc, status, create_by, create_time) 
VALUES (
  'feishu', 
  '{"appId":"YOUR_APP_ID","appSecret":"YOUR_APP_SECRET","enabled":true}',
  '飞书文档应用配置',
  '0',
  'admin',
  NOW()
);
```

### 2. 多密钥配置

支持配置多个飞书应用：

```sql
-- 主应用
INSERT INTO sys_secret_key (key_name, key_value, key_desc, status) 
VALUES ('feishu', '{"appId":"app1","appSecret":"secret1","enabled":true}', '主飞书应用', '0');

-- 备用应用
INSERT INTO sys_secret_key (key_name, key_value, key_desc, status) 
VALUES ('feishu_backup', '{"appId":"app2","appSecret":"secret2","enabled":true}', '备用飞书应用', '0');
```

## 权限配置

### 菜单权限
- `system:feishu:doc:view` - 飞书文档管理菜单访问
- `system:feishu:doc:list` - 查询文档列表
- `system:feishu:doc:sync` - 同步文档
- `system:feishu:doc:config` - 配置管理
- `system:feishu:doc:detail` - 查看文档详情
- `system:feishu:doc:search` - 搜索文档

## 使用示例

### Java代码调用

```java
@Autowired
private IFeishuDocService feishuDocService;

// 查询文档列表
List<FeishuDocDto> docs = feishuDocService.listFiles(null, 50, null, null);

// 获取文档内容
String content = feishuDocService.getDocContent("doccnxxxxxx", null);

// 创建新文档
FeishuDocDto newDoc = feishuDocService.createDoc("文章标题", "初始内容", null, null);

// 更新文档内容
boolean updated = feishuDocService.updateDocContent("doccnxxxxxx", "新内容", null);

// 检查配置状态
boolean available = feishuDocService.isConfigAvailable();
```

### 前端调用

```javascript
// 查询文档列表
axios.get('/feishu/doc/list', {
  params: {
    pageSize: 10,
    folderToken: 'xxx'
  }
}).then(response => {
  console.log('文档列表:', response.data.data);
});

// 获取文档内容
axios.get('/feishu/doc/content/doccnxxxxxx').then(response => {
  console.log('文档内容:', response.data.data.content);
});

// 创建新文档
axios.post('/feishu/doc/create', {
  name: '我的博客文章',
  content: '文章内容...'
}).then(response => {
  console.log('创建成功:', response.data.data);
});

// 更新文档内容
axios.put('/feishu/doc/content/doccnxxxxxx', {
  content: '更新后的内容...'
}).then(response => {
  console.log('更新成功:', response.data.msg);
});
```

## 使用说明

### 1. 配置飞书应用

在系统管理 -> 密钥管理中配置飞书应用信息：
- **密钥名称**：自定义名称，用于区分不同的飞书应用
- **App ID**：飞书应用的App ID
- **App Secret**：飞书应用的App Secret
- **密钥类型**：选择"feishu"

### 2. 查询文档列表

查询飞书云盘中的文档列表：
```bash
curl -X GET "http://localhost:8080/feishu/doc/list?pageSize=10&folderToken=xxx"
```

### 3. 获取文档内容

获取指定文档的内容（用于博客展示）：
```bash
curl -X GET "http://localhost:8080/feishu/doc/content/doccnxxxxxx"
```

### 4. 创建新文档

创建新的飞书文档（用于博客写作）：
```bash
curl -X POST "http://localhost:8080/feishu/doc/create" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "我的博客文章",
    "content": "这是文章的初始内容..."
  }'
```

### 5. 更新文档内容

更新现有文档的内容（用于博客编辑）：
```bash
curl -X PUT "http://localhost:8080/feishu/doc/content/doccnxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "更新后的文章内容..."
  }'
```

### 6. 多密钥使用

如果配置了多个飞书应用，可以通过keyName参数指定使用哪个应用：
```bash
curl -X GET "http://localhost:8080/feishu/doc/list?keyName=blog_app"
```

### 7. 博客集成示例

在博客系统中的典型使用流程：

1. **创建博客文章时**：
   - 调用创建文档接口，在飞书中创建新文档
   - 获取返回的文档token和URL
   - 将文档信息保存到博客文章表中

2. **编辑博客文章时**：
   - 调用更新文档内容接口，同步更新飞书文档
   - 同时更新本地缓存的文档内容

3. **展示博客文章时**：
   - 优先使用本地缓存的文档内容
   - 如果缓存过期，调用获取文档内容接口刷新缓存

## 文档类型说明

| 类型 | 描述 | 说明 |
|------|------|------|
| folder | 文件夹 | 可包含其他文档和文件夹 |
| doc | 文档 | 传统飞书文档 |
| docx | 新版文档 | 新版飞书文档 |
| sheet | 电子表格 | 飞书表格 |
| bitable | 多维表格 | 飞书多维表格 |
| mindnote | 思维笔记 | 飞书思维导图 |
| slides | 演示文稿 | 飞书演示文稿 |
| file | 文件 | 普通文件 |

## 故障排除

### 1. 配置问题
- 检查 `sys_secret_key` 表中的飞书配置是否正确
- 确认 `appId` 和 `appSecret` 是否有效
- 检查 `enabled` 字段是否为 `true`

### 2. 权限问题
- 确认飞书应用是否有访问云盘的权限
- 检查用户是否有相应的系统权限
- 确认飞书应用的权限范围设置

### 3. 网络问题
- 检查服务器是否能访问飞书API
- 确认防火墙设置
- 检查代理配置

### 4. 数据同步问题
- 查看 `feishu_doc_sync_log` 表中的错误日志
- 检查数据库连接和权限
- 确认表结构是否正确创建

## 安全注意事项

1. **密钥安全**：
   - 飞书应用密钥存储在数据库中，确保数据库安全
   - 定期更换应用密钥
   - 限制应用权限范围

2. **访问控制**：
   - 使用SA-Token进行权限控制
   - 限制API访问频率
   - 记录操作日志

3. **数据安全**：
   - 敏感文档信息加密存储
   - 定期备份同步数据
   - 遵循数据保护法规

## 扩展功能

### 待实现功能
1. 文档内容搜索
2. 文档权限详细管理
3. 文档版本历史
4. 自动定时同步
5. 文档变更通知
6. 批量操作支持

### 自定义扩展
- 可通过实现 `IFeishuDocService` 接口扩展功能
- 支持自定义文档处理逻辑
- 可集成其他第三方服务

## 版本历史

- **v1.0.0** (2025-01-30)
  - 初始版本
  - 支持基本的文档查询和同步功能
  - 实现多密钥配置
  - 添加权限控制