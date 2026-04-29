当前日期：{{current_date}}

请使用 blog_save 工具，将下面的技术分析润色成可发布的中文博客并保存。

原始分析：

{{github_analysis}}

编辑要求：
- 保留技术细节、代码块、项目判断和 repo 信息。
- 标题 25 字以内，体现技术价值。
- 摘要 150 字以内。
- 正文 1500-2500 字，不含代码。
- 避免“首先、其次、最后、综上所述”等模板词。
- 不要添加无法从分析内容或 README 支撑的事实。

blog_save 工具参数必须包含：

```json
{
  "title": "文章标题",
  "summary": "文章摘要",
  "content": "完整 Markdown 内容",
  "category": "开源项目",
  "tags": "标签",
  "status": "1",
  "isOriginal": "1",
  "repoUrl": "从 github_analysis 中提取",
  "repoName": "从 github_analysis 中提取"
}
```

保存成功后，输出 blog_save 的结果摘要。
