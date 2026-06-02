当前日期：{{current_date}}

请使用 blog_en_save 工具，将中文教程博客翻译成本地化英文技术教程并保存。

中文博客：

{{chinese_article}}

项目与教程背景：

{{github_analysis}}

翻译要求：
- 保持**教程/How-to Guide**风格，不是 project review 或 repository walkthrough。
- 英文表达自然，适合英文技术博客；标题可用 "How to ...", "Getting Started with ...", "A Practical Guide to ..." 等。
- 保留所有代码块；代码本身不翻译，只翻译中文注释。
- 保留原文操作步骤与实用建议，不扩写不存在的事实。
- 标题、摘要、正文都要英文。
- repoUrl、repoName 从 github_analysis 提取。
- zhBlogId 从 chinese_article 的工具返回结果中提取。
- **只调用 blog_en_save 一次**；若工具返回 duplicateSkipped 或已存在 Blog ID，直接输出该结果，禁止再次保存。

blog_en_save 工具参数：

```json
{
  "title": "English title",
  "summary": "English summary",
  "content": "Full English Markdown content",
  "category": "Tutorial",
  "tags": "GitHub,OpenSource,technical tags",
  "zhBlogId": "中文博客ID",
  "repoUrl": "GitHub URL",
  "repoName": "仓库名"
}
```
