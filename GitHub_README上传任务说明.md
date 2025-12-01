# GitHub README自动上传OSS定时任务

## 功能说明

该定时任务每天上午9点自动执行，负责：
1. 查询GitHub上榜仓库中README文件路径为空的记录
2. 通过GitHub API获取README文件内容
3. 将README文件上传到配置的云存储(OSS/COS/S3等)
4. 更新数据库中的readme_path字段

## 技术实现

- **框架**: Spring `@Scheduled`注解实现定时任务
- **执行时间**: 每天上午9点（Cron表达式：`0 0 9 * * ?`）
- **存储**: 支持阿里云OSS、腾讯云COS、亚马逊S3等多种云存储

## 配置说明

### 1. GitHub Token配置（可选）

在 `application.yml` 中配置GitHub Token以提高API请求限流额度：

```yaml
# GitHub配置
github:
  # GitHub API Token（可选，用于提高API请求限流额度）
  # 可在 https://github.com/settings/tokens 生成personal access token
  token: your_github_token_here
```

**获取GitHub Token步骤**：
1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token (classic)"
3. 设置权限：只需要 `public_repo` 权限
4. 生成并复制Token
5. 将Token配置到 `application.yml`

**注意**：
- 不配置Token时，使用GitHub API的匿名访问限制（每小时60次请求）
- 配置Token后，限制提升至每小时5000次请求

### 2. 云存储配置

确保在 `application.yml` 中正确配置了云存储信息：

```yaml
ruoyi:
  cloud-storage:
    # 存储类型：local-本地存储, aliyun-阿里云OSS, tencent-腾讯云COS, amazon-亚马逊S3, azure-微软Azure
    type: aliyun  # 根据实际使用的云存储类型修改
    
    # 阿里云OSS配置示例
    aliyun:
      access-key-id: your-access-key-id
      access-key-secret: your-access-key-secret
      bucket-name: your-bucket-name
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      prefix: github-readme/
      custom-domain: 
```

也可以在系统管理后台 -> 存储配置中进行配置。

## 文件存储路径

上传到OSS的文件路径格式：
```
github-readme/{owner}/{repo}/{README文件名}
```

示例：
```
github-readme/torvalds/linux/README.md
github-readme/microsoft/vscode/README.md
```

## 任务执行逻辑

### 1. 查询条件
- readme_path字段为NULL或空字符串
- 按star数降序排列
- 每次最多处理50个仓库

### 2. 处理流程
```
1. 查询符合条件的仓库列表
2. 遍历每个仓库：
   a. 调用GitHub API获取README信息
   b. Base64解码README内容
   c. 上传到云存储
   d. 更新数据库readme_path和readme_updated_at字段
3. 记录执行统计信息（成功数、失败数、耗时）
```

### 3. API限流保护
- 每次API调用间隔1秒，避免触发GitHub限流
- 建议配置GitHub Token以提高限流额度

### 4. 错误处理
- API调用失败：记录错误日志，继续处理下一个仓库
- 上传失败：记录错误日志，不更新数据库
- README不存在：记录警告日志，跳过该仓库

## 日志说明

任务执行时会输出详细日志：

```
========== 开始执行GitHub README上传OSS定时任务 ==========
查询到 15 个README文件为空的仓库
处理仓库 [1/15]: torvalds/linux
README文件上传成功，OSS路径: https://xxx.oss-cn-hangzhou.aliyuncs.com/github-readme/torvalds/linux/README.md
仓库 torvalds/linux README上传成功
...
========== GitHub README上传OSS定时任务执行完成 ==========
总数: 15, 成功: 13, 失败: 2, 耗时: 45678ms
```

## 手动触发（测试接口）

已提供测试接口用于手动触发任务，方便开发和测试：

### 接口地址

**Controller文件**: `com.ruoyi.project.github.controller.GithubTaskController`

#### 1. 异步执行接口（推荐）
- **接口地址**: `POST /github/task/readme/upload`
- **权限**: `github:task:execute`
- **说明**: 异步执行任务，立即返回，避免接口超时
- **返回**: 任务触发成功提示

#### 2. 同步执行接口
- **接口地址**: `POST /github/task/readme/upload/sync`
- **权限**: `github:task:execute`
- **说明**: 同步执行任务，等待任务完成后返回（可能超时）
- **返回**: 任务执行完成提示

### 使用方式

#### 方式1：通过Swagger测试
1. 启动项目后访问 Swagger 文档: `http://localhost:8080/doc.html`
2. 找到 "GitHub定时任务测试" 分组
3. 点击 "手动触发README上传任务" 接口
4. 点击 "试一试" 按钮执行
5. 查看控制台日志查看任务执行情况

#### 方式2：通过Postman/cURL
```bash
# 异步执行（推荐）
curl -X POST "http://localhost:8080/github/task/readme/upload" \
  -H "Authorization: Bearer your_token_here"

# 同步执行
curl -X POST "http://localhost:8080/github/task/readme/upload/sync" \
  -H "Authorization: Bearer your_token_here"
```

### 权限配置

如果遇到权限问题，请在系统管理中添加权限标识：
- 权限标识: `github:task:execute`
- 权限名称: GitHub任务执行
- 分配给相应的角色

或者临时移除接口上的 `@SaCheckPermission("github:task:execute")` 注解进行测试。

## 注意事项

1. **首次运行**：首次运行可能需要处理大量仓库，建议在业务低峰期执行
2. **Token安全**：请妥善保管GitHub Token，不要提交到公开代码仓库
3. **存储成本**：大量README文件会占用存储空间，请关注OSS账单
4. **网络问题**：GitHub API访问可能受网络影响，建议服务器有稳定的国际网络访问
5. **数据库性能**：批量更新操作会占用数据库连接，请确保连接池配置合理

## 监控建议

1. 定期检查任务执行日志，关注失败率
2. 监控GitHub API限流情况
3. 监控OSS存储使用量和成本
4. 设置告警规则，当失败率超过阈值时发送通知

## 优化方向

1. **批量更新**：将数据库更新操作改为批量更新，提高效率
2. **异步处理**：使用线程池并发处理多个仓库
3. **重试机制**：对失败的请求增加重试逻辑
4. **缓存优化**：对已上传的README文件进行缓存，避免重复上传
5. **增量同步**：只处理新增和更新的仓库

## 相关文件

- 任务类：`com.ruoyi.project.github.task.GithubReadmeUploadTask`
- 配置文件：`src/main/resources/application.yml`
- 实体类：`com.ruoyi.project.github.domain.GithubTrending`
- 服务类：`com.ruoyi.project.github.service.IGithubTrendingService`
- 存储服务：`com.ruoyi.common.storage.FileStorageService`
