当前日期：{{current_date}}

请根据中文博客、英文博客和项目分析生成博客封面，并更新博客记录。

中文博客：
{{chinese_article}}

英文博客：
{{english_article}}

项目分析：
{{github_analysis}}

流程：
1. 提取项目主题、语言、技术领域和视觉关键词。
2. 使用 ali_image_generation 生成 1024*576 封面图。
3. 从 chinese_article 和 english_article 中提取博客 ID。
4. 使用 blog_cover_update 更新中文和英文博客封面。

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
