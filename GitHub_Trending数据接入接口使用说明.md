# GitHub Trending 数据接入接口使用说明

## 概述

该接口用于接收云函数推送的 GitHub Trending 仓库数据，实现数据的去重、落库和更新。云函数只负责爬取新的仓库信息，同步工作由后端接口完成。

## 接口信息

### 基础信息

- **接口路径**: `POST /github/trending/ingest`
- **Content-Type**: `application/json`
- **鉴权方式**: HMAC-SHA256 签名验证
- **可选鉴权**: Bearer Token (通过 `Authorization` 请求头，需在配置中启用)

### 请求头

#### 签名验证请求头（推荐）

| 名称 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `X-Client-Id` | String | 是 | 客户端ID，需在服务端配置中注册 |
| `X-Timestamp` | Long | 是 | 当前时间戳（毫秒），用于防重放攻击 |
| `X-Nonce` | String | 是 | 随机字符串，确保每次请求唯一 |
| `X-Signature` | String | 是 | HMAC-SHA256签名，计算方式见下文 |
| `Idempotency-Key` | String | 否 | 幂等键，用于防止重复请求。如不提供，系统会自动生成 |
| `Content-Type` | String | 是 | 固定值: `application/json` |

#### Bearer Token 请求头（可选）

如果服务端未启用签名验证，可使用 Bearer Token 方式：

| 名称 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Authorization` | String | 是 | Bearer Token 格式: `Bearer {API_TOKEN}` |
| `Idempotency-Key` | String | 否 | 幂等键 |
| `Content-Type` | String | 是 | 固定值: `application/json` |

### 签名计算方法

#### 签名算法

使用 HMAC-SHA256 算法对请求进行签名：

```
signature = HMAC-SHA256(secret, signContent)
signContent = method + path + timestamp + nonce
```

#### 参数说明

- `method`: HTTP方法，全大写（如 `POST`）
- `path`: 请求路径（如 `/github/trending/ingest`）
- `timestamp`: X-Timestamp 请求头的值
- `nonce`: X-Nonce 请求头的值
- `secret`: 客户端密钥（由服务端分配）

#### 签名示例

假设：
- `method` = `POST`
- `path` = `https://ai.zhoudw.vip/github/trending/ingest`
- `timestamp` = `1733126400000`
- `nonce` = `abc123xyz`
- `secret` = `my-secret-key-123`

待签名字符串：
```
POST/github/trending/ingest1733126400000abc123xyz
```

使用 HMAC-SHA256(secret, signContent) 计算签名结果（Hex格式）。

### 查询参数

| 名称 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `source` | String | 否 | weibosider | 数据来源标识 |
| `period` | String | 否 | daily | 时间周期: daily/weekly/monthly |

### 请求体

```json
{
  "repos": [
    {
      "owner": "octocat",
      "title": "Hello-World",
      "fullName": "octocat/Hello-World",
      "url": "https://github.com/octocat/Hello-World",
      "description": "My first repository on GitHub!",
      "language": "Java",
      "starsCount": 1000,
      "forksCount": 200,
      "trendingDate": "2025-12-02"
    }
  ],
  "meta": {
    "fetchedAt": "2025-12-02 10:30:00",
    "source": "weibosider",
    "period": "daily",
    "version": "v1"
  }
}
```

#### 字段说明

**repos** (必填，Array):
- `owner` (必填): 仓库所有者
- `title` (必填): 仓库名称
- `fullName` (可选): 完整仓库名(owner/title)，如不提供会自动生成
- `url` (必填): 仓库地址，用于去重
- `description` (可选): 仓库描述
- `language` (可选): 主要编程语言
- `starsCount` (可选): Star 数量
- `forksCount` (可选): Fork 数量
- `trendingDate` (可选): 上榜日期 (YYYY-MM-DD)

**meta** (可选，Object):
- `fetchedAt`: 抓取时间
- `source`: 数据来源
- `period`: 时间周期
- `version`: 数据版本

### 响应示例

#### 成功响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "requestId": "abc123def456",
    "totalReceived": 25,
    "newInserted": 10,
    "updated": 15,
    "skipped": 0,
    "failed": 0,
    "message": "处理完成: 新增 10, 更新 15, 失败 0"
  }
}
```

#### 幂等拦截响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "requestId": "abc123def456",
    "totalReceived": 25,
    "skipped": 25,
    "message": "重复请求，已被幂等性拦截"
  }
}
```

#### 错误响应

```json
{
  "code": 500,
  "msg": "仓库列表不能为空"
}
```

## 云函数示例代码

### Python 示例

```python
import requests
import json
import hmac
import hashlib
import time
import uuid
from datetime import datetime

def generate_signature(method, path, timestamp, nonce, secret):
    """
    生成 HMAC-SHA256 签名
    
    Args:
        method: HTTP方法 (POST, GET等)
        path: 请求路径
        timestamp: 时间戳(毫秒)
        nonce: 随机字符串
        secret: 客户端密钥
    
    Returns:
        签名字符串 (Hex格式)
    """
    # 构建待签名字符串
    sign_content = f"{method}{path}{timestamp}{nonce}"
    
    # 计算 HMAC-SHA256 签名
    signature = hmac.new(
        secret.encode('utf-8'),
        sign_content.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    
    return signature

def sync_github_trending(repos_data):
    """
    同步 GitHub Trending 数据到后端接口
    
    Args:
        repos_data: 从 GithubManager.get_trending_repos 获取的仓库列表
    """
    # 配置信息（从环境变量读取）
    api_base_url = os.getenv("API_BASE_URL", "https://api.example.com")
    client_id = os.getenv("API_CLIENT_ID", "cloud-function")
    client_secret = os.getenv("API_CLIENT_SECRET", "your-secret-key")
    
    # 构建请求数据
    payload = {
        "repos": [],
        "meta": {
            "fetchedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "source": "weibosider",
            "period": "daily",
            "version": "v1"
        }
    }
    
    # 转换数据格式并补充 full_name
    for repo in repos_data:
        payload["repos"].append({
            "owner": repo.get("owner"),
            "title": repo.get("title"),
            "fullName": f"{repo.get('owner')}/{repo.get('title')}",
            "url": repo.get("url"),
            "description": repo.get("description"),
            "language": repo.get("language"),
            "starsCount": repo.get("stars_count"),
            "forksCount": repo.get("forks_count"),
            "trendingDate": repo.get("trending_date")
        })
    
    # 生成幂等键（可使用日期+随机字符串）
    idempotency_key = f"trending-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"
    
    # 生成签名参数
    timestamp = str(int(time.time() * 1000))  # 毫秒级时间戳
    nonce = uuid.uuid4().hex[:16]  # 16位随机字符串
    method = "POST"
    path = "/github/trending/ingest"
    
    # 计算签名
    signature = generate_signature(method, path, timestamp, nonce, client_secret)
    
    # 发送请求
    headers = {
        "X-Client-Id": client_id,
        "X-Timestamp": timestamp,
        "X-Nonce": nonce,
        "X-Signature": signature,
        "Idempotency-Key": idempotency_key,
        "Content-Type": "application/json"
    }
    
    try:
        response = requests.post(
            f"{api_base_url}/github/trending/ingest",
            headers=headers,
            json=payload,
            timeout=30
        )
        response.raise_for_status()
        
        result = response.json()
        print(f"同步成功: {result.get('data', {}).get('message')}")
        return result
        
    except requests.exceptions.RequestException as e:
        print(f"同步失败: {str(e)}")
        raise
```

### JavaScript 示例

```javascript
const axios = require('axios');
const crypto = require('crypto');

/**
 * 生成 HMAC-SHA256 签名
 */
function generateSignature(method, path, timestamp, nonce, secret) {
  const signContent = `${method}${path}${timestamp}${nonce}`;
  return crypto
    .createHmac('sha256', secret)
    .update(signContent)
    .digest('hex');
}

async function syncGithubTrending(reposData) {
  const apiBaseUrl = process.env.API_BASE_URL || 'https://api.example.com';
  const clientId = process.env.API_CLIENT_ID || 'cloud-function';
  const clientSecret = process.env.API_CLIENT_SECRET || 'your-secret-key';
  
  // 构建请求数据
  const payload = {
    repos: reposData.map(repo => ({
      owner: repo.owner,
      title: repo.title,
      fullName: `${repo.owner}/${repo.title}`,
      url: repo.url,
      description: repo.description,
      language: repo.language,
      starsCount: repo.stars_count,
      forksCount: repo.forks_count,
      trendingDate: repo.trending_date
    })),
    meta: {
      fetchedAt: new Date().toISOString().replace('T', ' ').substring(0, 19),
      source: 'weibosider',
      period: 'daily',
      version: 'v1'
    }
  };
  
  // 生成幂等键
  const idempotencyKey = `trending-${Date.now()}-${Math.random().toString(36).substr(2, 8)}`;
  
  // 生成签名参数
  const timestamp = Date.now().toString();
  const nonce = crypto.randomBytes(8).toString('hex');
  const method = 'POST';
  const path = '/github/trending/ingest';
  
  // 计算签名
  const signature = generateSignature(method, path, timestamp, nonce, clientSecret);
  
  try {
    const response = await axios.post(
      `${apiBaseUrl}/github/trending/ingest`,
      payload,
      {
        headers: {
          'X-Client-Id': clientId,
          'X-Timestamp': timestamp,
          'X-Nonce': nonce,
          'X-Signature': signature,
          'Idempotency-Key': idempotencyKey,
          'Content-Type': 'application/json'
        },
        timeout: 30000
      }
    );
    
    console.log('同步成功:', response.data.data.message);
    return response.data;
    
  } catch (error) {
    console.error('同步失败:', error.message);
    throw error;
  }
}

module.exports = { syncGithubTrending };
```
```

## 数据处理逻辑

### 1. 幂等性检查

- 使用 `Idempotency-Key` 进行幂等性校验
- 24小时内相同的幂等键会被拦截
- 如果被拦截，返回 `skipped` 数量等于总数

### 2. 去重逻辑

- 根据 `url` 字段进行去重
- URL 已存在则更新，不存在则新增

### 3. 新增逻辑

对于新仓库：
- 生成唯一ID
- 设置上榜统计：总上榜天数 = 1，连续上榜天数 = 1
- 记录首次上榜日期和最后上榜日期

### 4. 更新逻辑

对于已存在仓库：
- 更新基础信息（描述、语言、Star、Fork数量）
- 总上榜天数 +1
- 如果与上次上榜日期连续，连续上榜天数 +1，否则重置为 1
- 更新最后上榜日期

## 环境变量配置建议

### 使用签名验证（推荐）

```bash
# API 基础地址
API_BASE_URL=https://api.example.com

# API 客户端ID
API_CLIENT_ID=cloud-function

# API 客户端密钥
API_CLIENT_SECRET=your-secret-key-here

# 请求超时时间（秒）
API_TIMEOUT=30

# 重试次数
API_RETRY=3
```

### 使用 Bearer Token（备选）

```bash
# API 基础地址
API_BASE_URL=https://api.example.com

# API 认证 Token
API_TOKEN=your_bearer_token_here

# 请求超时时间（秒）
API_TIMEOUT=30

# 重试次数
API_RETRY=3
```

## 服务端配置

在服务端的 `application.yml` 或 `application.properties` 中配置：

```yaml
# API签名配置
api:
  sign:
    # 是否启用签名验证（生产环境建议设为true）
    enabled: true
    # 时间戳允许的误差（毫秒），默认5分钟
    timestamp-tolerance: 300000
    # 客户端密钥配置
    secrets:
      cloud-function: your-secret-key-here
      # 可配置多个客户端
      another-client: another-secret-key
```

## 错误处理建议

1. **网络超时**: 建议设置超时时间为 30 秒
2. **重试机制**: 对于网络错误建议重试 3 次
3. **幂等保障**: 使用固定的幂等键确保数据不重复
4. **日志记录**: 记录每次同步的结果，便于排查问题

## 监控指标

建议监控以下指标：
- 每日推送成功率
- 新增仓库数量
- 更新仓库数量
- 失败数量和原因
- 接口响应时间

## 注意事项

1. 确保云函数有正确的 API Token
2. 推荐使用日期+时间戳作为幂等键的一部分
3. 云函数不需要关心数据库操作，只负责推送数据
4. 后端会自动补充 `fullName` 字段（如果未提供）
5. 建议每天定时执行云函数，避免频繁调用
