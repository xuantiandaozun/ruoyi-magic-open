package com.ruoyi.project.ai.tool.impl;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.domain.AiBlogProductionRecord;
import com.ruoyi.project.ai.service.IAiBlogProductionRecordService;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.service.IBlogService;

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
    private IBlogService blogService;
    
    @Autowired
    private IAiBlogProductionRecordService aiBlogProductionRecordService;
    
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
            return "错误：博客标题不能为空";
        }
        
        if (StrUtil.isBlank(content)) {
            return "错误：博客内容不能为空";
        }
            
            // 创建Blog实体
            Blog blog = new Blog();
            blog.setTitle(title);
            blog.setContent(content);
            
            // 设置可选参数
            String summary = (String) parameters.get("summary");
            if (StrUtil.isNotBlank(summary)) {
                blog.setSummary(summary);
            }
            
            String coverImage = (String) parameters.get("coverImage");
            if (StrUtil.isNotBlank(coverImage)) {
                blog.setCoverImage(coverImage);
            }
            
            String category = (String) parameters.get("category");
            if (StrUtil.isNotBlank(category)) {
                blog.setCategory(category);
            }
            
            String tags = (String) parameters.get("tags");
            if (StrUtil.isNotBlank(tags)) {
                blog.setTags(tags);
            }
            
            String status = (String) parameters.get("status");
            blog.setStatus(StrUtil.isNotBlank(status) ? status : "0"); // 默认草稿
            
            String isTop = (String) parameters.get("isTop");
            blog.setIsTop(StrUtil.isNotBlank(isTop) ? isTop : "0"); // 默认不置顶
            
            String isOriginal = (String) parameters.get("isOriginal");
            blog.setIsOriginal(StrUtil.isNotBlank(isOriginal) ? isOriginal : "1"); // 默认原创
            
            String feishuDocToken = (String) parameters.get("feishuDocToken");
            if (StrUtil.isNotBlank(feishuDocToken)) {
                blog.setFeishuDocToken(feishuDocToken);
            }
            
            String feishuDocName = (String) parameters.get("feishuDocName");
            if (StrUtil.isNotBlank(feishuDocName)) {
                blog.setFeishuDocName(feishuDocName);
            }
            
            // 设置默认值
            blog.setViewCount("0");
            blog.setLikeCount("0");
            blog.setCommentCount(0L);
            blog.setFeishuSyncStatus("0"); // 未同步
            
            // 如果是已发布状态，设置发布时间
            if ("1".equals(blog.getStatus())) {
                blog.setPublishTime(LocalDateTime.now());
            }
            
            // 保存到数据库
            boolean success = blogService.save(blog);
            
            if (success) {
                // 保存生产记录
                AiBlogProductionRecord productionRecord = new AiBlogProductionRecord();
                if (StrUtil.isNotBlank(blog.getBlogId())) {
                    productionRecord.setBlogId(Long.parseLong(blog.getBlogId()));
                }
                productionRecord.setProductionType("blog_generation");
                productionRecord.setStatus("1"); // 成功
                productionRecord.setProductionTime(new Date());
                productionRecord.setCompletionTime(new Date());
                aiBlogProductionRecordService.save(productionRecord);
                
                return String.format("博客文章保存成功！\n" +
                    "文章ID: %s\n" +
                    "标题: %s\n" +
                    "状态: %s\n" +
                    "分类: %s\n" +
                    "标签: %s", 
                    blog.getBlogId(),
                    blog.getTitle(),
                    getStatusText(blog.getStatus()),
                    StrUtil.isNotBlank(blog.getCategory()) ? blog.getCategory() : "未分类",
                    StrUtil.isNotBlank(blog.getTags()) ? blog.getTags() : "无标签");
        } else {
            return "博客文章保存失败，请检查数据库连接或参数是否正确";
        }
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
        """;
    }
    
    /**
     * 获取状态文本描述
     */
    private String getStatusText(String status) {
        switch (status) {
            case "0": return "草稿";
            case "1": return "已发布";
            case "2": return "已下线";
            default: return "未知状态";
        }
    }
}