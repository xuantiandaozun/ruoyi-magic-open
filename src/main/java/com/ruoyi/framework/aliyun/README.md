# 阿里云SDK统一封装

## 概述

本模块提供了阿里云SDK的统一封装，支持多密钥、多区域的管理，具有良好的扩展性和易用性。

## 架构设计

### 核心组件

1. **AliyunCredential** - 阿里云凭证信息封装
2. **AliyunClientFactory** - 客户端工厂接口
3. **AliyunClientManager** - 客户端管理器
4. **AliyunCredentialProvider** - 凭证提供者
5. **AliyunService** - 统一服务门面

### 设计特点

- **多密钥支持**：支持管理多个阿里云账号的密钥
- **多区域支持**：支持不同区域的服务调用
- **统一接口**：提供统一的服务调用接口
- **客户端缓存**：自动管理客户端实例的创建和销毁
- **扩展性强**：通过工厂模式轻松添加新的阿里云服务

## 支持的服务

- **RDS** - 关系型数据库服务
- **OSS** - 对象存储服务
- **ECS** - 弹性计算服务（示例）

## 使用方法

### 1. 基本配置

在 `application.yml` 中添加配置：

```yaml
ruoyi:
  aliyun:
    enabled: true
    default-region: cn-hangzhou
    connect-timeout: 10000
    read-timeout: 30000
    enable-client-cache: true
    client-cache-expire-minutes: 60
    rds:
      enabled: true
    oss:
      enabled: true
      enable-https: true
    ecs:
      enabled: true
```

### 2. 注入服务

```java
@Autowired
private AliyunService aliyunService;
```

### 3. 使用示例

#### 使用默认凭证

```java
// 获取RDS实例列表
List<String> instanceIds = aliyunService.executeWithDefaultCredential("RDS", (AsyncClient client) -> {
    DescribeDBInstancesRequest request = DescribeDBInstancesRequest.builder().build();
    CompletableFuture<DescribeDBInstancesResponse> future = client.describeDBInstances(request);
    DescribeDBInstancesResponse response = future.get();
    
    return response.getBody().getItems().getDBInstance().stream()
            .map(instance -> instance.getDBInstanceId())
            .toList();
});
```

#### 使用指定密钥ID

```java
// 使用指定密钥操作OSS
List<String> objectKeys = aliyunService.executeWithSecretKeyId("OSS", secretKeyId, (OSS client) -> {
    return client.listObjects(bucketName).getObjectSummaries().stream()
            .map(summary -> summary.getKey())
            .toList();
});
```

#### 使用指定区域

```java
// 使用指定区域的凭证
Integer instanceCount = aliyunService.executeWithRegionCredential("RDS", "cn-beijing", (AsyncClient client) -> {
    DescribeDBInstancesRequest request = DescribeDBInstancesRequest.builder()
            .regionId("cn-beijing")
            .build();
    CompletableFuture<DescribeDBInstancesResponse> future = client.describeDBInstances(request);
    DescribeDBInstancesResponse response = future.get();
    
    return response.getBody().getItems().getDBInstance().size();
});
```

#### 批量操作所有凭证

```java
// 使用所有可用凭证批量操作
List<Integer> instanceCounts = aliyunService.executeWithAllCredentials("RDS", (AsyncClient client) -> {
    DescribeDBInstancesRequest request = DescribeDBInstancesRequest.builder().build();
    CompletableFuture<DescribeDBInstancesResponse> future = client.describeDBInstances(request);
    DescribeDBInstancesResponse response = future.get();
    
    return response.getBody().getItems().getDBInstance().size();
});
```

#### 直接获取客户端

```java
// 获取客户端进行复杂操作
AliyunCredential credential = aliyunService.getDefaultCredential();
AsyncClient rdsClient = aliyunService.getClient("RDS", credential);
OSS ossClient = aliyunService.getClient("OSS", credential);
```

### 4. 管理操作

```java
// 获取支持的服务类型
List<String> serviceTypes = aliyunService.getSupportedServiceTypes();

// 获取所有可用凭证
List<AliyunCredential> credentials = aliyunService.getAllCredentials();

// 刷新凭证
aliyunService.refreshCredentials();

// 清理客户端缓存
aliyunService.clearClientCache();
```

## 扩展新服务

### 1. 创建客户端工厂

```java
@Component
public class NewServiceClientFactory implements AliyunClientFactory<NewServiceClient> {
    
    @Override
    public NewServiceClient createClient(AliyunCredential credential) {
        // 实现客户端创建逻辑
    }
    
    @Override
    public String getServiceType() {
        return "NEW_SERVICE";
    }
    
    @Override
    public void closeClient(NewServiceClient client) {
        // 实现客户端关闭逻辑
    }
}
```

### 2. 使用新服务

```java
NewServiceClient client = aliyunService.getClient("NEW_SERVICE", credential);
// 或者
Result result = aliyunService.executeWithDefaultCredential("NEW_SERVICE", (NewServiceClient client) -> {
    // 业务逻辑
    return result;
});
```

## 注意事项

1. **密钥安全**：确保密钥信息的安全存储和传输
2. **客户端管理**：框架会自动管理客户端的生命周期，无需手动关闭
3. **异常处理**：业务代码中需要适当处理可能的异常
4. **性能考虑**：客户端会被缓存，避免频繁创建和销毁
5. **区域配置**：确保密钥对应的区域配置正确

## 数据库表结构

确保 `sys_secret_key` 表包含以下字段：

- `secret_key_id` - 密钥ID（主键）
- `provider_type` - 厂商类型（如：aliyun）
- `provider_name` - 厂商名称
- `key_type` - 密钥类型
- `key_name` - 密钥名称/别名
- `access_key` - 访问密钥
- `secret_key` - 密钥
- `region` - 地域
- `status` - 状态（0-正常，1-停用）