当前日期：{{current_date}}（{{current_weekday}}）

博客生成工作流已完成，请使用 feishu_send_message 发送通知。

选题：
{{selected_project}}

中文博客：
{{chinese_article}}

英文博客：
{{english_article}}

封面：
{{cover_image}}

通知内容要包含：
- 项目名称和 GitHub 地址。
- 中文博客标题和 ID。
- 英文博客标题和 ID。
- 封面状态。
- 生成时间。

消息要简洁，使用 text 类型即可。

输出 JSON：

```json
{
  "status": "success/failed",
  "message": "飞书通知发送结果",
  "notificationTime": "{{current_date}}"
}
```
