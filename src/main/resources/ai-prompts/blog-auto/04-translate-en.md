当前日期：{{current_date}}

请使用 blog_en_save 工具，将中文博客翻译成本地化英文技术文章并保存。

中文博客：

{{chinese_article}}

项目分析：

{{github_analysis}}

翻译要求：
- 英文表达自然，适合英文技术博客。
- 保留所有代码块；代码本身不翻译，只翻译中文注释。
- 保留原文技术判断，不扩写不存在的事实。
- 标题、摘要、正文都要英文。
- repoUrl、repoName 从 github_analysis 提取。
- zhBlogId 从 chinese_article 的工具返回结果中提取。

blog_en_save 工具参数：

```json
{
  "title": "English title",
  "summary": "English summary",
  "content": "Full English Markdown content",
  "category": "Open Source",
  "tags": "GitHub,OpenSource,technical tags",
  "zhBlogId": "中文博客ID",
  "repoUrl": "GitHub URL",
  "repoName": "仓库名"
}
```
