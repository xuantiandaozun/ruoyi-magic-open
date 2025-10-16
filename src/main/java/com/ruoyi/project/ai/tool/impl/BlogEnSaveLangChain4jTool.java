package com.ruoyi.project.ai.tool.impl;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.article.domain.BlogEn;
import com.ruoyi.project.article.service.IBlogEnService;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的英文博客文章保存工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class BlogEnSaveLangChain4jTool implements LangChain4jTool {
    
    @Autowired
    private IBlogEnService blogEnService;
    
    @Override
    public String getToolName() {
        return "blog_en_save";
    }
    
    @Override
    public String getToolDescription() {
        return "Save English blog articles to database, supporting title, summary, content, category, tags and other information";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("title", "Blog title (required)")
            .addStringProperty("summary", "Blog summary (optional)")
            .addStringProperty("content", "Blog content (required)")
            .addStringProperty("coverImage", "Cover image URL (optional)")
            .addStringProperty("category", "Blog category in English (optional)")
            .addStringProperty("tags", "Blog tags separated by commas (optional)")
            .addStringProperty("status", "Status: 0=draft, 1=published, 2=offline, default is draft")
            .addStringProperty("isTop", "Is top: 0=no, 1=yes, default is no")
            .addStringProperty("isOriginal", "Is original: 0=repost, 1=original, default is original")
            .addStringProperty("zhBlogId", "Associated Chinese blog ID (optional)")
            .addStringProperty("feishuDocToken", "Associated Feishu document token (optional)")
            .addStringProperty("feishuDocName", "Associated Feishu document name (optional)")
            .build();
        
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(parametersSchema)
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            // Get required parameters
            String title = (String) parameters.get("title");
            String content = (String) parameters.get("content");
            
            if (StrUtil.isBlank(title)) {
                return "Error: Blog title cannot be empty";
            }
            
            if (StrUtil.isBlank(content)) {
                return "Error: Blog content cannot be empty";
            }
            
            // Create BlogEn entity
            BlogEn blogEn = new BlogEn();
            blogEn.setTitle(title);
            blogEn.setContent(content);
            
            // Set optional parameters
            String summary = (String) parameters.get("summary");
            if (StrUtil.isNotBlank(summary)) {
                blogEn.setSummary(summary);
            }
            
            String coverImage = (String) parameters.get("coverImage");
            if (StrUtil.isNotBlank(coverImage)) {
                blogEn.setCoverImage(coverImage);
            }
            
            String category = (String) parameters.get("category");
            if (StrUtil.isNotBlank(category)) {
                blogEn.setCategory(category);
            }
            
            String tags = (String) parameters.get("tags");
            if (StrUtil.isNotBlank(tags)) {
                blogEn.setTags(tags);
            }
            
            String status = (String) parameters.get("status");
            blogEn.setStatus(StrUtil.isNotBlank(status) ? status : "0"); // Default draft
            
            String isTop = (String) parameters.get("isTop");
            blogEn.setIsTop(StrUtil.isNotBlank(isTop) ? isTop : "0"); // Default not top
            
            String isOriginal = (String) parameters.get("isOriginal");
            blogEn.setIsOriginal(StrUtil.isNotBlank(isOriginal) ? isOriginal : "1"); // Default original
            
            String zhBlogId = (String) parameters.get("zhBlogId");
            if (StrUtil.isNotBlank(zhBlogId)) {
                blogEn.setZhBlogId(zhBlogId);
            }
            
            String feishuDocToken = (String) parameters.get("feishuDocToken");
            if (StrUtil.isNotBlank(feishuDocToken)) {
                blogEn.setFeishuDocToken(feishuDocToken);
            }
            
            String feishuDocName = (String) parameters.get("feishuDocName");
            if (StrUtil.isNotBlank(feishuDocName)) {
                blogEn.setFeishuDocName(feishuDocName);
            }
            
            // Set default values
            blogEn.setViewCount("0");
            blogEn.setLikeCount("0");
            blogEn.setCommentCount("0");
            blogEn.setFeishuSyncStatus("0"); // Not synced
            
            // Set publish time if status is published
            if ("1".equals(blogEn.getStatus())) {
                blogEn.setPublishTime(new Date());
            }
            
            // Save to database
            boolean success = blogEnService.save(blogEn);
            
            if (success) {
                return String.format("English blog article saved successfully!\n" +
                    "Article ID: %s\n" +
                    "Title: %s\n" +
                    "Status: %s\n" +
                    "Category: %s\n" +
                    "Tags: %s\n" +
                    "Associated Chinese Blog ID: %s", 
                    blogEn.getBlogId(),
                    blogEn.getTitle(),
                    getStatusText(blogEn.getStatus()),
                    StrUtil.isNotBlank(blogEn.getCategory()) ? blogEn.getCategory() : "Uncategorized",
                    StrUtil.isNotBlank(blogEn.getTags()) ? blogEn.getTags() : "No tags",
                    StrUtil.isNotBlank(blogEn.getZhBlogId()) ? blogEn.getZhBlogId() : "None");
            } else {
                return "Failed to save English blog article, please check database connection or parameters";
            }
            
        } catch (Exception e) {
            return "Error occurred while saving English blog article: " + e.getMessage();
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // Validate required parameters
        String title = (String) parameters.get("title");
        String content = (String) parameters.get("content");
        
        if (StrUtil.isBlank(title) || StrUtil.isBlank(content)) {
            return false;
        }
        
        // Validate status parameter
        if (parameters.containsKey("status")) {
            String status = (String) parameters.get("status");
            if (StrUtil.isNotBlank(status) && 
                !status.equals("0") && !status.equals("1") && !status.equals("2")) {
                return false;
            }
        }
        
        // Validate isTop parameter
        if (parameters.containsKey("isTop")) {
            String isTop = (String) parameters.get("isTop");
            if (StrUtil.isNotBlank(isTop) && 
                !isTop.equals("0") && !isTop.equals("1")) {
                return false;
            }
        }
        
        // Validate isOriginal parameter
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
        Usage examples:
        1. Save basic English blog article:
           {"title": "My First Blog Post", "content": "This is the content of my blog..."}
        
        2. Save complete English blog article:
           {"title": "Tech Sharing", "summary": "A technical sharing about Java development", "content": "Detailed content...", "category": "Technology", "tags": "Java,Spring Boot", "status": "1"}
        
        3. Save draft article:
           {"title": "Draft Article", "content": "Content still being written...", "status": "0"}
        
        4. Save top original article:
           {"title": "Important Announcement", "content": "Important content...", "isTop": "1", "isOriginal": "1", "status": "1"}
        
        5. Save with Chinese blog association:
           {"title": "English Version", "content": "English content...", "zhBlogId": "123", "status": "1"}
        """;
    }
    
    /**
     * Get status text description
     */
    private String getStatusText(String status) {
        switch (status) {
            case "0": return "Draft";
            case "1": return "Published";
            case "2": return "Offline";
            default: return "Unknown Status";
        }
    }
}