# 自媒体素材脚本上传接口

## 接口信息

| 参数 | 值 |
|------|-----|
| **方法** | POST |
| **路径** | https://ai.zhoudw.vip/api/article/social-media-asset/upload/script |
| **认证** | HMAC-SHA256签名验证 |
| **登录** | 否 |

## 签名验证

### 请求头

```
X-Client-Id: cloud-function
X-Timestamp: 1702100000000
X-Nonce: abc123def456
X-Signature: xxxxx
```

### 签名生成

```
signContent = METHOD + PATH + TIMESTAMP + NONCE
signature = HMAC-SHA256(secret, signContent)
```

**示例**:
```
POST/article/social-media-asset/upload/script1702100000000abc123
```

密钥见 `application.yml` 中 `api.sign.secrets.cloud-function`

## 请求体

```json
{
  "platform": "twitter",
  "title": "素材标题（可选）",
  "summary": "摘要（可选）",
  "contentSnapshot": "内容快照（可选）",
  "sourceUrl": "https://example.com/post",
  "coverUrl": "https://example.com/cover.jpg",
  "author": "作者名（可选）",
  "publishTime": "2025-12-09T10:00:00",
  "tags": "标签1,标签2",
  "status": "active"
}
```

### 必填字段

- `platform`: 平台名称（toutiao/douyin/bilibili/weibo/twitter/medium/other）

### 可选字段

- `title`: 标题/首句
- `summary`: 摘要
- `contentSnapshot`: 正文/内容快照
- `sourceUrl`: 原始链接（建议填写用于去重）
- `coverUrl`: 封面URL
- `author`: 作者名
- `publishTime`: 发布时间
- `tags`: 标签（逗号分隔或JSON）
- `qualityLevel`: 质量等级（high/medium/low）
- `status`: 状态（active/archived/invalid，默认active）

## 响应

### 成功 (200)

```json
{
  "code": 0,
  "msg": "素材上传成功",
  "data": {
    "id": 123,
    "platform": "twitter",
    "title": "素材标题",
    "sourceUrl": "https://example.com/post",
    "captureMethod": "spider",
    "captureTime": "2025-12-09T10:00:00"
  }
}
```

### 失败 (401/400/500)

```json
{
  "code": 500,
  "msg": "错误信息"
}
```

## 客户端示例

### Python

```python
import requests
import hmac
import hashlib
import time
import uuid

def upload_asset(asset_data):
    client_id = "cloud-function"
    secret = "ZmZiY2ViZTAtYWQ3MC00ZWI2LTg0YjUtMzhmOGFjZDFkZGQxMzNlZWFiOTktNTg3Ny00OGJlLTkzYjAtMGY4ZjFhMDMxZWZl"
    base_url = "http://localhost:8080"
    
    timestamp = str(int(time.time() * 1000))
    nonce = str(uuid.uuid4()).replace('-', '')[:16]
    
    method = "POST"
    path = "/article/social-media-asset/upload/script"
    sign_content = method + path + timestamp + nonce
    
    signature = hmac.new(
        secret.encode(),
        sign_content.encode(),
        hashlib.sha256
    ).hexdigest()
    
    headers = {
        "X-Client-Id": client_id,
        "X-Timestamp": timestamp,
        "X-Nonce": nonce,
        "X-Signature": signature,
        "Content-Type": "application/json"
    }
    
    response = requests.post(
        base_url + path,
        json=asset_data,
        headers=headers
    )
    
    return response.json()

# 使用
asset = {
    "platform": "twitter",
    "summary": "推特内容",
    "sourceUrl": "https://twitter.com/xxx",
    "status": "active"
}

result = upload_asset(asset)
print(result)
```

### Node.js

```javascript
const crypto = require('crypto');
const axios = require('axios');

async function uploadAsset(assetData) {
  const clientId = 'cloud-function';
  const secret = 'ZmZiY2ViZTAtYWQ3MC00ZWI2LTg0YjUtMzhmOGFjZDFkZGQxMzNlZWFiOTktNTg3Ny00OGJlLTkzYjAtMGY4ZjFhMDMxZWZl';
  const baseUrl = 'http://localhost:8080';
  
  const timestamp = Date.now().toString();
  const nonce = crypto.randomBytes(8).toString('hex');
  const method = 'POST';
  const path = '/article/social-media-asset/upload/script';
  
  const signContent = method + path + timestamp + nonce;
  const signature = crypto
    .createHmac('sha256', secret)
    .update(signContent)
    .digest('hex');
  
  const headers = {
    'X-Client-Id': clientId,
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Signature': signature
  };
  
  const response = await axios.post(
    baseUrl + path,
    assetData,
    { headers }
  );
  
  return response.data;
}

// 使用
uploadAsset({
  platform: 'twitter',
  summary: '推特内容',
  sourceUrl: 'https://twitter.com/xxx',
  status: 'active'
}).then(console.log);
```

### cURL

```bash
#!/bin/bash

CLIENT_ID="cloud-function"
SECRET="ZmZiY2ViZTAtYWQ3MC00ZWI2LTg0YjUtMzhmOGFjZDFkZGQxMzNlZWFiOTktNTg3Ny00OGJlLTkzYjAtMGY4ZjFhMDMxZWZl"
BASE_URL="http://localhost:8080"
PATH="/article/social-media-asset/upload/script"
METHOD="POST"

TIMESTAMP=$(date +%s%N | cut -b1-13)
NONCE=$(cat /dev/urandom | tr -dc 'a-f0-9' | fold -w 16 | head -n 1)

SIGN_CONTENT="${METHOD}${PATH}${TIMESTAMP}${NONCE}"
SIGNATURE=$(echo -n "$SIGN_CONTENT" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')

curl -X POST "${BASE_URL}${PATH}" \
  -H "X-Client-Id: ${CLIENT_ID}" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Nonce: ${NONCE}" \
  -H "X-Signature: ${SIGNATURE}" \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "twitter",
    "summary": "推特内容",
    "sourceUrl": "https://twitter.com/xxx",
    "status": "active"
  }'
```

