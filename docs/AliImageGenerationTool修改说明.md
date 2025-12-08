# AliImageGenerationLangChain4jTool 修改说明

## 修改时间
2025-12-05

## 修改目的
增强AI生图工具，使其在生成图片后自动：
1. 将图片上传到自己的OSS存储
2. 保存生成记录到数据库表 `ai_cover_generation_record`

## 主要修改内容

### 1. 新增依赖注入

添加了两个服务依赖：

```java
@Autowired
private IAiImageService aiImageService;

@Autowired
private IAiCoverGenerationRecordService aiCoverGenerationRecordService;
```

### 2. 修改 execute 方法

原来的流程：
```
调用阿里云API → 获取临时图片URL → 直接返回临时URL
```

修改后的流程：
```
调用阿里云API → 获取临时图片URL → 下载并上传到OSS → 保存记录到数据库 → 返回OSS永久URL
```

### 3. 核心代码逻辑

```java
// 1. 获取阿里云临时图片URL
String tempImageUrl = parseImageGenerationResponse(response, prompt, model, size, 1);

// 2. 下载并上传到OSS
String baseFileName = "ai_image_" + System.currentTimeMillis();
String ossImageUrl = aiImageService.downloadAndUploadToOss(tempImageUrl, baseFileName);

// 3. 保存生成记录到数据库
AiCoverGenerationRecord record = new AiCoverGenerationRecord();
record.setPrompt(prompt);
record.setImageUrl(ossImageUrl);
record.setAiModel(model);
record.setCoverType("0");  // 0-通用封面
record.setGenerationStatus("1");  // 1-成功
record.setIsUsed("0");  // 0-未使用
record.setDelFlag("0");
record.setGenerationTime(new Date());
aiCoverGenerationRecordService.save(record);
```

### 4. 返回值变化

**修改前：**
```json
{
  "imageUrl": "https://dashscope-result-sz.oss-cn-shenzhen.aliyuncs.com/xxx.png",
  "prompt": "...",
  "model": "...",
  "size": "..."
}
```

**修改后：**
```json
{
  "imageUrl": "https://your-oss-domain.com/ai_image_xxx.jpg",
  "tempImageUrl": "https://dashscope-result-sz.oss-cn-shenzhen.aliyuncs.com/xxx.png",
  "prompt": "...",
  "model": "...",
  "size": "..."
}
```

- `imageUrl`: OSS永久存储的图片URL（推荐使用）
- `tempImageUrl`: 阿里云临时URL（24小时有效，仅供参考）

## 数据库记录字段

保存到 `ai_cover_generation_record` 表的字段：

| 字段 | 值 | 说明 |
|------|-----|------|
| prompt | 用户输入的提示词 | AI生图的描述 |
| imageUrl | OSS图片URL | 永久存储的图片地址 |
| aiModel | 使用的模型名称 | 如 qwen-image-plus |
| coverType | "0" | 0-通用封面 |
| generationStatus | "1" | 1-成功 |
| isUsed | "0" | 0-未使用 |
| delFlag | "0" | 0-未删除 |
| generationTime | 当前时间 | 生成时间戳 |

## 异常处理

1. **OSS上传失败**：返回错误信息，不继续执行
2. **数据库保存失败**：仅记录日志，不影响返回结果（图片已成功上传到OSS）

## 依赖的服务

### IAiImageService
- `downloadAndUploadToOss(String tempUrl, String fileName)`: 下载临时图片并上传到OSS

### IAiCoverGenerationRecordService
- `save(AiCoverGenerationRecord record)`: 保存生成记录到数据库

## 使用示例

```java
Map<String, Object> params = new HashMap<>();
params.put("prompt", "一幅美丽的山水画");
params.put("model", "qwen-image-plus");
params.put("size", "1328*1328");

String result = aliImageGenerationTool.execute(params);
// 返回包含OSS永久URL的JSON字符串
```

## 优势

1. **永久存储**：图片上传到自己的OSS，不受阿里云24小时限制
2. **数据追溯**：所有生成记录都保存在数据库，方便查询和管理
3. **统一管理**：图片存储在自己的OSS，便于统一管理和备份
4. **兼容性好**：保留了临时URL，向后兼容

## 注意事项

1. 确保OSS配置正确（在 `AliyunOssStorageStrategy` 中配置）
2. 确保数据库表 `ai_cover_generation_record` 已创建
3. 图片文件名格式：`ai_image_<时间戳>.jpg`
4. 数据库保存失败不会影响图片上传结果
