# 阿里云地域同步功能使用说明

## 功能概述

本功能用于将阿里云RDS地域信息同步到系统数据字典中，方便在系统中统一管理和使用阿里云地域数据。

## 数据结构

### 字典类型
- **字典类型**: `aliyun_region`
- **字典名称**: `阿里云地域`

### 字典数据格式
- **字典标签**: 地域本地名称（如：华东1（杭州））
- **字典键值**: 地域ID（如：cn-hangzhou）

### 阿里云返回数据示例
```json
[
  {
    "localName": "郑州（联通云）",
    "regionEndpoint": "rds.cn-zhengzhou-jva.aliyuncs.com",
    "regionId": "cn-zhengzhou-jva",
    "zoneId": "cn-zhengzhou-jva-a",
    "zoneName": "郑州（联通云） 可用区A"
  }
]
```

## API接口

### 1. 获取阿里云地域列表
```
GET /system/aliyun/region/list
```
**权限要求**: `system:dict:list`

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "rows": [
    {
      "localName": "华东1（杭州）",
      "regionEndpoint": "rds.cn-hangzhou.aliyuncs.com",
      "regionId": "cn-hangzhou",
      "zoneId": "cn-hangzhou-a",
      "zoneName": "华东1（杭州） 可用区A"
    }
  ],
  "total": 1
}
```

### 2. 同步阿里云地域到数据字典
```
POST /system/aliyun/region/sync
```
**权限要求**: `system:dict:edit`

**功能说明**:
- 自动初始化字典类型（如果不存在）
- 清除旧的地域数据
- 从阿里云获取最新地域信息
- 插入新的字典数据

**响应示例**:
```json
{
  "code": 200,
  "msg": "同步阿里云地域成功"
}
```

### 3. 初始化阿里云地域字典类型
```
POST /system/aliyun/region/init
```
**权限要求**: `system:dict:add`

**响应示例**:
```json
{
  "code": 200,
  "msg": "初始化阿里云地域字典类型成功"
}
```

## 使用步骤

### 1. 数据库初始化
执行SQL脚本：
```sql
source sql/aliyun_region_dict.sql
```

### 2. 配置阿里云凭证
确保系统中已配置有效的阿里云访问凭证。

### 3. 同步地域数据
调用同步接口：
```bash
curl -X POST "http://localhost:8080/system/aliyun/region/sync" \
  -H "Authorization: Bearer your-token"
```

### 4. 查看同步结果
在系统管理 -> 字典管理中查看 `aliyun_region` 字典类型的数据。

## 注意事项

1. **权限要求**: 确保用户具有相应的字典管理权限
2. **网络连接**: 确保服务器能够访问阿里云API
3. **凭证配置**: 确保阿里云访问凭证有效且具有RDS地域查询权限
4. **数据覆盖**: 每次同步会清除旧数据，请谨慎操作
5. **事务处理**: 同步过程使用事务，失败时会自动回滚

## 错误处理

常见错误及解决方案：

1. **凭证无效**: 检查阿里云访问密钥配置
2. **网络超时**: 检查网络连接和防火墙设置
3. **权限不足**: 确保用户具有相应的操作权限
4. **数据库错误**: 检查数据库连接和表结构

## 扩展说明

如需扩展其他阿里云服务的地域同步，可参考本实现方式：
1. 创建对应的VO类
2. 实现相应的服务接口
3. 添加控制器接口
4. 配置相应的权限