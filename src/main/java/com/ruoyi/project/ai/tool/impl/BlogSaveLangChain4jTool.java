package com.ruoyi.project.ai.tool.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;
import com.ruoyi.project.article.service.AiBlogPublishService;
import com.ruoyi.project.blogapi.domain.dto.AiBlogPublishRequest;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的博客文章保存工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class BlogSaveLangChain4jTool implements LangChain4jTool {
    
    @Autowired
    private AiBlogPublishService aiBlogPublishService;
    
    @Override
    public String getToolName() {
        return "blog_save";
    }
    
    @Override
    public String getToolDescription() {
        return "保存中文博客文章到数据库，支持设置标题、摘要、内容、分类、标签等信息";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("title", "博客标题，必填")
            .addStringProperty("summary", "博客摘要，可选")
            .addStringProperty("content", "博客内容，必填")
            .addStringProperty("coverImage", "封面图片URL，可选")
            .addStringProperty("category", "博客分类，可选")
            .addStringProperty("tags", "博客标签，多个用逗号分隔，可选")
            .addStringProperty("status", "状态：0=草稿，1=已发布，2=已下线，默认为草稿")
            .addStringProperty("isTop", "是否置顶：0=否，1=是，默认为否")
            .addStringProperty("isOriginal", "是否原创：0=转载，1=原创，默认为原创")
            .addStringProperty("feishuDocToken", "关联的飞书文档token，可选")
            .addStringProperty("feishuDocName", "关联的飞书文档名称，可选")
            .addStringProperty("repoUrl", "关联的GitHub仓库完整URL，可选，用于记录博客来源")
            .addStringProperty("repoName", "关联的GitHub仓库名称（如owner/repo），可选")
            .build();
        
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(parametersSchema)
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        // 获取必填参数
        String title = (String) parameters.get("title");
        String content = (String) parameters.get("content");
        
        if (StrUtil.isBlank(title)) {
            return ToolExecutionResult.failure("save", "博客标题不能为空");
        }
        
        if (StrUtil.isBlank(content)) {
            return ToolExecutionResult.failure("save", "博客内容不能为空");
        }

        AiBlogPublishRequest request = new AiBlogPublishRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setSummary((String) parameters.get("summary"));
        request.setCoverImage((String) parameters.get("coverImage"));
        request.setCategory((String) parameters.get("category"));
        request.setTags((String) parameters.get("tags"));
        request.setStatus((String) parameters.get("status"));
        request.setIsTop((String) parameters.get("isTop"));
        request.setIsOriginal((String) parameters.get("isOriginal"));
        request.setFeishuDocToken((String) parameters.get("feishuDocToken"));
        request.setFeishuDocName((String) parameters.get("feishuDocName"));
        request.setRepoUrl((String) parameters.get("repoUrl"));
        request.setRepoName((String) parameters.get("repoName"));

        Map<String, Object> resultData = aiBlogPublishService.publish(request, "0", "blog_generation");
        if (Boolean.TRUE.equals(resultData.get("duplicateSkipped"))) {
            return ToolExecutionResult.builder()
                    .success(true)
                    .operationType("save")
                    .data(resultData)
                    .message("该仓库今日博客已存在，跳过重复保存。Blog ID: " + resultData.get("blogId"))
                    .metadata(Map.of("duplicateSkipped", true))
                    .build()
                    .toJsonString();
        }
        return ToolExecutionResult.saveSuccess(resultData,
                "博客文章保存成功。Blog ID: " + resultData.get("blogId"));
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 验证必填参数
        String title = (String) parameters.get("title");
        String content = (String) parameters.get("content");
        
        if (StrUtil.isBlank(title) || StrUtil.isBlank(content)) {
            return false;
        }
        
        // 验证状态参数
        if (parameters.containsKey("status")) {
            String status = (String) parameters.get("status");
            if (StrUtil.isNotBlank(status) && 
                !status.equals("0") && !status.equals("1") && !status.equals("2")) {
                return false;
            }
        }
        
        // 验证置顶参数
        if (parameters.containsKey("isTop")) {
            String isTop = (String) parameters.get("isTop");
            if (StrUtil.isNotBlank(isTop) && 
                !isTop.equals("0") && !isTop.equals("1")) {
                return false;
            }
        }
        
        // 验证原创参数
        if (parameters.containsKey("isOriginal")) {
            String isOriginal = (String) parameters.get("isOriginal");
            if (StrUtil.isNotBlank(isOriginal) && 
                !isOriginal.equals("0") && !isOriginal.equals("1")) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 保存基本博客文章：
           {"title": "我的第一篇博客", "content": "这是博客的内容..."}
        
        2. 保存完整博客文章：
           {"title": "技术分享", "summary": "关于Java开发的技术分享", "content": "详细内容...", "category": "技术", "tags": "Java,Spring Boot", "status": "1"}
        
        3. 保存草稿文章：
           {"title": "草稿文章", "content": "还在编写中的内容...", "status": "0"}
        
        4. 保存置顶原创文章：
           {"title": "重要公告", "content": "重要内容...", "isTop": "1", "isOriginal": "1", "status": "1"}
        
        5. 保存 GitHub 项目实战教程（推荐）：
           {"title": "手把手教你用 XX 完成 YY", "content": "教程正文...", "category": "技术教程", "tags": "GitHub,教程,实战", "status": "1", "repoUrl": "https://github.com/owner/repo", "repoName": "owner/repo"}
        """;
    }
    
}
