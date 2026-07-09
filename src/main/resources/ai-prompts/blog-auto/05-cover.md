当前日期：{{current_date}}

> 说明：`blog-auto-v3` 的 cover 步骤已改为确定性处理器 `handler: blog_cover`，
> 由代码直接调用 `ali_image_generation` + `blog_cover_update`，不再依赖 LLM 工具编排。
> 本文件保留作文档与兼容参考。

请根据中文教程博客、英文教程博客和项目背景生成博客封面，并更新博客记录。

中文博客：
{{chinese_article}}

英文博客：
{{english_article}}

项目与教程背景：
{{github_analysis}}

硬性规则（必须遵守）：
1. **必须**先调用 `ali_image_generation` 生成封面；禁止跳过工具、禁止自己编造图片 URL。
2. **禁止**使用 pollinations.ai、placeholder、dummyimage 等外部临时图床。
3. 尺寸必须使用 `1664*928`（16:9），不要使用 `1024*576` 或其他未支持尺寸。
4. 从工具返回结果中取出 OSS 永久 URL，再调用 `blog_cover_update` 更新中英文博客封面。
5. 从 chinese_article / english_article 中提取博客 ID（`blogId` / `zhBlogId`）。

图片要求：
- 现代、清晰、技术博客风格。
- 不要人物面部、不要水印、不要大段文字。
- 提示词使用英文。

输出 JSON：

```json
{
  "coverImageUrl": "图片URL",
  "prompt": "使用的图片提示词",
  "zhBlogId": "中文博客ID",
  "enBlogId": "英文博客ID",
  "status": "success/partial_success/failed",
  "message": "结果说明"
}
```
