package com.ruoyi.project.ai.tool.impl;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.article.domain.SocialMediaArticle;
import com.ruoyi.project.article.service.ISocialMediaArticleService;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的自媒体文章保存工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class SocialMediaArticleSaveLangChain4jTool implements LangChain4jTool {
    
    @Autowired
    private ISocialMediaArticleService socialMediaArticleService;
    
    @Override
    public String getToolName() {
        return "social_media_article_save";
    }
    
    @Override
    public String getToolDescription() {
        return "保存自媒体文章到数据库，支持中英文双语内容、多平台发布、GitHub项目分析等功能";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("titleZh", "中文标题，30个字以内，符合今日头条的标题习惯（吸引眼球、包含关键词），必填")
            .addStringProperty("titleEn", "英文标题，可选")
            .addStringProperty("summaryZh", "中文微头条，基于文章内容提炼的核心观点或热点评论（100字以内），需具有话题性、引发讨论的特点，可选")
            .addStringProperty("summaryEn", "英文微头条/Twitter，基于文章内容的核心观点或热点评论（280字以内），可选")
            .addStringProperty("contentZh", "中文完整内容，可选")
            .addStringProperty("contentEn", "英文完整内容，可选")
            .addStringProperty("keywordsZh", "中文关键词，多个用逗号分隔，可选")
            .addStringProperty("keywordsEn", "英文关键词，多个用逗号分隔，可选")
            .addStringProperty("articleType", "文章类型：GITHUB_RANKING-GitHub排行榜，PROJECT_ANALYSIS-项目分析等，可选")
            .addStringProperty("contentAngle", "内容角度，可选")
            .addStringProperty("targetPlatform", "目标平台：toutiao-今日头条，twitter-推特，medium-Medium等，可选")
            .addStringProperty("publishStatus", "发布状态：0-草稿，1-已发布，2-已下线，默认为草稿")
            .addStringProperty("sourceRepos", "来源GitHub仓库信息(JSON格式)，可选")
            .addStringProperty("relatedBlogIds", "关联的博客文章ID列表(逗号分隔)，可选")
            .addStringProperty("blogName", "博客名称，可选")
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
        String titleZh = (String) parameters.get("titleZh");
        
        if (StrUtil.isBlank(titleZh)) {
            return "错误：中文标题不能为空";
        }
            
            // 创建SocialMediaArticle实体
            SocialMediaArticle article = new SocialMediaArticle();
            article.setTitleZh(titleZh);
            
            // 设置可选参数
            String titleEn = (String) parameters.get("titleEn");
            if (StrUtil.isNotBlank(titleEn)) {
                article.setTitleEn(titleEn);
            }
            
            String summaryZh = (String) parameters.get("summaryZh");
            if (StrUtil.isNotBlank(summaryZh)) {
                article.setSummaryZh(summaryZh);
            }
            
            String summaryEn = (String) parameters.get("summaryEn");
            if (StrUtil.isNotBlank(summaryEn)) {
                article.setSummaryEn(summaryEn);
            }
            
            String contentZh = (String) parameters.get("contentZh");
            if (StrUtil.isNotBlank(contentZh)) {
                article.setContentZh(contentZh);
            }
            
            String contentEn = (String) parameters.get("contentEn");
            if (StrUtil.isNotBlank(contentEn)) {
                article.setContentEn(contentEn);
            }
            
            String keywordsZh = (String) parameters.get("keywordsZh");
            if (StrUtil.isNotBlank(keywordsZh)) {
                article.setKeywordsZh(keywordsZh);
            }
            
            String keywordsEn = (String) parameters.get("keywordsEn");
            if (StrUtil.isNotBlank(keywordsEn)) {
                article.setKeywordsEn(keywordsEn);
            }
            
            String articleType = (String) parameters.get("articleType");
            if (StrUtil.isNotBlank(articleType)) {
                article.setArticleType(articleType);
            }
            
            String contentAngle = (String) parameters.get("contentAngle");
            if (StrUtil.isNotBlank(contentAngle)) {
                article.setContentAngle(contentAngle);
            }
            
            String targetPlatform = (String) parameters.get("targetPlatform");
            if (StrUtil.isNotBlank(targetPlatform)) {
                article.setTargetPlatform(targetPlatform);
            }
            
            String publishStatus = (String) parameters.get("publishStatus");
            article.setPublishStatus(StrUtil.isNotBlank(publishStatus) ? publishStatus : "0"); // 默认草稿
            
            String sourceRepos = (String) parameters.get("sourceRepos");
            if (StrUtil.isNotBlank(sourceRepos)) {
                article.setSourceRepos(sourceRepos);
            }
            
            String relatedBlogIds = (String) parameters.get("relatedBlogIds");
            if (StrUtil.isNotBlank(relatedBlogIds)) {
                article.setRelatedBlogIds(relatedBlogIds);
            }
            
            String blogName = (String) parameters.get("blogName");
            if (StrUtil.isNotBlank(blogName)) {
                article.setBlogName(blogName);
            }
            
            // 设置默认值
            article.setViewCount("0");
            article.setLikeCount("0");
            article.setShareCount("0");
            article.setCommentCount("0");
            article.setGenerationDate(new Date());
            
            // 保存到数据库
            boolean success = socialMediaArticleService.save(article);
            
            if (success) {
                return String.format("自媒体文章保存成功！\n" +
                    "文章ID: %s\n" +
                    "中文标题: %s\n" +
                    "英文标题: %s\n" +
                    "文章类型: %s\n" +
                    "目标平台: %s\n" +
                    "发布状态: %s\n" +
                    "博客名称: %s", 
                    article.getArticleId(),
                    article.getTitleZh(),
                    StrUtil.isNotBlank(article.getTitleEn()) ? article.getTitleEn() : "无",
                    StrUtil.isNotBlank(article.getArticleType()) ? article.getArticleType() : "未分类",
                    StrUtil.isNotBlank(article.getTargetPlatform()) ? article.getTargetPlatform() : "未指定",
                    getPublishStatusText(article.getPublishStatus()),
                    StrUtil.isNotBlank(article.getBlogName()) ? article.getBlogName() : "未指定");
        } else {
            return "自媒体文章保存失败，请检查数据库连接或参数是否正确";
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 验证必填参数
        String titleZh = (String) parameters.get("titleZh");
        if (StrUtil.isBlank(titleZh)) {
            return false;
        }
        
        // 验证发布状态参数
        if (parameters.containsKey("publishStatus")) {
            String publishStatus = (String) parameters.get("publishStatus");
            if (StrUtil.isNotBlank(publishStatus) && 
                !publishStatus.equals("0") && !publishStatus.equals("1") && !publishStatus.equals("2")) {
                return false;
            }
        }
        
        // 验证文章类型参数
        if (parameters.containsKey("articleType")) {
            String articleType = (String) parameters.get("articleType");
            if (StrUtil.isNotBlank(articleType)) {
                String[] validTypes = {"GITHUB_RANKING", "PROJECT_ANALYSIS", "TECH_NEWS", "TUTORIAL", "REVIEW"};
                boolean isValid = false;
                for (String validType : validTypes) {
                    if (validType.equals(articleType)) {
                        isValid = true;
                        break;
                    }
                }
                if (!isValid) {
                    return false;
                }
            }
        }
        
        // 验证目标平台参数
        if (parameters.containsKey("targetPlatform")) {
            String targetPlatform = (String) parameters.get("targetPlatform");
            if (StrUtil.isNotBlank(targetPlatform)) {
                String[] validPlatforms = {"toutiao", "twitter", "medium", "zhihu", "csdn", "juejin"};
                boolean isValid = false;
                for (String validPlatform : validPlatforms) {
                    if (validPlatform.equals(targetPlatform)) {
                        isValid = true;
                        break;
                    }
                }
                if (!isValid) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 保存基本自媒体文章：
           {"titleZh": "今日GitHub热门项目推荐", "summaryZh": "为大家推荐几个今日GitHub上的热门开源项目"}
        
        2. 保存双语自媒体文章：
           {"titleZh": "AI项目分析", "titleEn": "AI Project Analysis", "summaryZh": "深度分析最新AI项目", "summaryEn": "In-depth analysis of latest AI projects", "articleType": "PROJECT_ANALYSIS"}
        
        3. 保存GitHub排行榜文章：
           {"titleZh": "GitHub今日趋势", "articleType": "GITHUB_RANKING", "targetPlatform": "toutiao", "sourceRepos": "[{\"name\":\"project1\",\"stars\":1000}]"}
        
        4. 保存完整的自媒体文章：
           {"titleZh": "技术分享", "titleEn": "Tech Sharing", "contentZh": "详细的中文内容...", "contentEn": "Detailed English content...", "keywordsZh": "技术,分享,开源", "keywordsEn": "tech,sharing,opensource", "articleType": "TUTORIAL", "targetPlatform": "medium", "publishStatus": "1", "blogName": "我的技术博客"}
        
        5. 保存关联博客的文章：
           {"titleZh": "项目推荐", "relatedBlogIds": "1,2,3", "targetPlatform": "zhihu", "publishStatus": "0"}
        """;
    }
    
    /**
     * 获取发布状态文本描述
     */
    private String getPublishStatusText(String publishStatus) {
        switch (publishStatus) {
            case "0": return "草稿";
            case "1": return "已发布";
            case "2": return "已下线";
            default: return "未知状态";
        }
    }
}