package com.ruoyi.project.ai.tool.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.ai.domain.AiBlogProductionRecord;
import com.ruoyi.project.ai.service.IAiBlogProductionRecordService;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.service.IBlogService;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的博客历史查询工具
 * 用于查询历史博客记录，帮助AI避免重复选题，实现每日不重样的内容创作
 * 
 * @author ruoyi-magic
 * @date 2024-12-03
 */
@Component
public class BlogHistoryQueryLangChain4jTool implements LangChain4jTool {
    
    private static final Logger log = LoggerFactory.getLogger(BlogHistoryQueryLangChain4jTool.class);
    
    @Autowired
    private IBlogService blogService;
    
    @Autowired
    private IAiBlogProductionRecordService blogProductionRecordService;
    
    @Override
    public String getToolName() {
        return "blog_history_query";
    }
    
    @Override
    public String getToolDescription() {
        return "查询博客历史记录，用于检查特定仓库或主题是否已写过博客，避免重复选题。支持按仓库URL、仓库名称、博客标题、时间范围等多维度查询，确保每日内容不重样";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("queryType", "查询类型：repo_url(按仓库URL)、repo_name(按仓库名称)、title(按博客标题)、date_range(按时间范围)、today(今日已生成)、recent(最近N天)")
            .addStringProperty("repoUrl", "仓库URL，queryType为repo_url时必填")
            .addStringProperty("repoName", "仓库名称（owner/repo格式或仅repo名称），queryType为repo_name时必填")
            .addStringProperty("titleKeyword", "博客标题关键词，queryType为title时必填")
            .addIntegerProperty("days", "查询最近N天的记录，queryType为recent时使用，默认30天")
            .addStringProperty("startDate", "开始日期(yyyy-MM-dd格式)，queryType为date_range时使用")
            .addStringProperty("endDate", "结束日期(yyyy-MM-dd格式)，queryType为date_range时使用")
            .addIntegerProperty("limit", "返回结果数量限制，默认50，最大200")
            .addBooleanProperty("includeContent", "是否包含博客内容，默认false（仅返回标题和摘要）")
            .build();
        
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(parametersSchema)
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        String queryType = parameters.get("queryType") != null ? 
            parameters.get("queryType").toString() : "recent";
        Integer limit = parameters.get("limit") != null ? 
            Integer.parseInt(parameters.get("limit").toString()) : 50;
        Boolean includeContent = parameters.get("includeContent") != null ?
            Boolean.parseBoolean(parameters.get("includeContent").toString()) : false;
        
        log.info("[BlogHistoryQueryTool] 开始查询博客历史, queryType={}, limit={}", queryType, limit);
        
        // 限制最大数量
        if (limit > 200) limit = 200;
        if (limit < 1) limit = 50;
        
        try {
            switch (queryType.toLowerCase()) {
                case "repo_url":
                    return queryByRepoUrl(parameters, limit, includeContent);
                case "repo_name":
                    return queryByRepoName(parameters, limit, includeContent);
                case "title":
                    return queryByTitle(parameters, limit, includeContent);
                case "date_range":
                    return queryByDateRange(parameters, limit, includeContent);
                case "today":
                    return queryTodayGenerated(limit, includeContent);
                case "recent":
                default:
                    return queryRecent(parameters, limit, includeContent);
            }
        } catch (Exception e) {
            log.error("[BlogHistoryQueryTool] 查询失败: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("query", "博客历史查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 按仓库URL查询
     */
    private String queryByRepoUrl(Map<String, Object> parameters, int limit, boolean includeContent) {
        String repoUrl = (String) parameters.get("repoUrl");
        if (StrUtil.isBlank(repoUrl)) {
            return ToolExecutionResult.failure("query", "查询类型为repo_url时，repoUrl参数必填");
        }
        
        // 标准化URL
        String normalizedUrl = normalizeUrl(repoUrl);
        
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where("del_flag = ?", "0")
            .and("status = ?", "1") // 只查询成功的记录
            .and("(repo_url LIKE ? OR repo_url = ?)", "%" + normalizedUrl + "%", repoUrl)
            .orderBy("completion_time DESC")
            .limit(limit);
        
        List<AiBlogProductionRecord> records = blogProductionRecordService.list(qw);
        
        if (records.isEmpty()) {
            return ToolExecutionResult.querySuccess(
                "未找到仓库 " + repoUrl + " 的博客生产记录，该仓库可以用于创作新博客。",
                "仓库URL查询完成"
            );
        }
        
        return formatProductionRecords(records, "仓库URL: " + repoUrl, includeContent);
    }
    
    /**
     * 按仓库名称查询
     */
    private String queryByRepoName(Map<String, Object> parameters, int limit, boolean includeContent) {
        String repoName = (String) parameters.get("repoName");
        if (StrUtil.isBlank(repoName)) {
            return ToolExecutionResult.failure("query", "查询类型为repo_name时，repoName参数必填");
        }
        
        // 分割 owner/repo 格式
        String owner = null;
        String title = repoName;
        if (repoName.contains("/")) {
            String[] parts = repoName.split("/", 2);
            owner = parts[0];
            title = parts[1];
        }
        
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where("del_flag = ?", "0")
            .and("status = ?", "1");
        
        if (StrUtil.isNotBlank(owner)) {
            qw.and("repo_owner = ?", owner);
            qw.and("repo_title = ?", title);
        } else {
            qw.and("repo_title LIKE ?", "%" + title + "%");
        }
        
        qw.orderBy("completion_time DESC")
            .limit(limit);
        
        List<AiBlogProductionRecord> records = blogProductionRecordService.list(qw);
        
        if (records.isEmpty()) {
            return ToolExecutionResult.querySuccess(
                "未找到仓库 " + repoName + " 的博客生产记录，该仓库可以用于创作新博客。",
                "仓库名称查询完成"
            );
        }
        
        return formatProductionRecords(records, "仓库名称: " + repoName, includeContent);
    }
    
    /**
     * 按博客标题关键词查询
     */
    private String queryByTitle(Map<String, Object> parameters, int limit, boolean includeContent) {
        String titleKeyword = (String) parameters.get("titleKeyword");
        if (StrUtil.isBlank(titleKeyword)) {
            return ToolExecutionResult.failure("query", "查询类型为title时，titleKeyword参数必填");
        }
        
        QueryWrapper qw = QueryWrapper.create()
            .from("blog")
            .where("del_flag = ?", "0")
            .and("title LIKE ?", "%" + titleKeyword + "%")
            .orderBy("create_time DESC")
            .limit(limit);
        
        List<Blog> blogs = blogService.list(qw);
        
        if (blogs.isEmpty()) {
            return ToolExecutionResult.querySuccess(
                "未找到标题包含 \"" + titleKeyword + "\" 的博客，可以围绕此主题创作新博客。",
                "标题关键词查询完成"
            );
        }
        
        return formatBlogList(blogs, "标题关键词: " + titleKeyword, includeContent);
    }
    
    /**
     * 按时间范围查询
     */
    private String queryByDateRange(Map<String, Object> parameters, int limit, boolean includeContent) {
        String startDateStr = (String) parameters.get("startDate");
        String endDateStr = (String) parameters.get("endDate");
        
        LocalDate startDate = StrUtil.isNotBlank(startDateStr) ? 
            LocalDate.parse(startDateStr) : LocalDate.now().minusDays(30);
        LocalDate endDate = StrUtil.isNotBlank(endDateStr) ? 
            LocalDate.parse(endDateStr) : LocalDate.now();
        
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where("del_flag = ?", "0")
            .and("status = ?", "1")
            .and("completion_time >= ?", startDate.atStartOfDay())
            .and("completion_time <= ?", endDate.plusDays(1).atStartOfDay())
            .orderBy("completion_time DESC")
            .limit(limit);
        
        List<AiBlogProductionRecord> records = blogProductionRecordService.list(qw);
        
        String dateRangeDesc = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + 
            " 至 " + endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        if (records.isEmpty()) {
            return ToolExecutionResult.querySuccess(
                "在 " + dateRangeDesc + " 期间没有博客生产记录。",
                "时间范围查询完成"
            );
        }
        
        return formatProductionRecords(records, "时间范围: " + dateRangeDesc, includeContent);
    }
    
    /**
     * 查询今日已生成的博客
     */
    private String queryTodayGenerated(int limit, boolean includeContent) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where("del_flag = ?", "0")
            .and("status = ?", "1")
            .and("completion_time >= ?", startOfDay)
            .and("completion_time < ?", endOfDay)
            .orderBy("completion_time DESC")
            .limit(limit);
        
        List<AiBlogProductionRecord> records = blogProductionRecordService.list(qw);
        
        if (records.isEmpty()) {
            return ToolExecutionResult.querySuccess(
                "今日尚未生成任何博客，可以开始创作新内容。\n今日日期: " + today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "今日博客查询完成"
            );
        }
        
        StringBuilder result = new StringBuilder();
        result.append("今日（").append(today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .append("）已生成 ").append(records.size()).append(" 篇博客：\n\n");
        
        // 提取今日已写的仓库信息
        List<String> todayRepos = new ArrayList<>();
        for (AiBlogProductionRecord record : records) {
            String repoInfo = "";
            if (StrUtil.isNotBlank(record.getRepoOwner()) && StrUtil.isNotBlank(record.getRepoTitle())) {
                repoInfo = record.getRepoOwner() + "/" + record.getRepoTitle();
            } else if (StrUtil.isNotBlank(record.getRepoUrl())) {
                repoInfo = record.getRepoUrl();
            }
            if (StrUtil.isNotBlank(repoInfo)) {
                todayRepos.add(repoInfo);
            }
        }
        
        result.append("【今日已写仓库列表（请勿重复选择）】\n");
        for (int i = 0; i < todayRepos.size(); i++) {
            result.append(String.format("%d. %s\n", i + 1, todayRepos.get(i)));
        }
        result.append("\n");
        
        // 详细记录
        result.append("【详细生产记录】\n");
        for (int i = 0; i < records.size(); i++) {
            AiBlogProductionRecord record = records.get(i);
            result.append(String.format("%d. ", i + 1));
            if (StrUtil.isNotBlank(record.getRepoOwner()) && StrUtil.isNotBlank(record.getRepoTitle())) {
                result.append(record.getRepoOwner()).append("/").append(record.getRepoTitle());
            } else if (StrUtil.isNotBlank(record.getRepoUrl())) {
                result.append(record.getRepoUrl());
            } else {
                result.append("其他类型博客");
            }
            if (record.getCompletionTime() != null) {
                result.append(" (完成于: ").append(
                    new java.text.SimpleDateFormat("HH:mm:ss").format(record.getCompletionTime())
                ).append(")");
            }
            result.append("\n");
        }
        
        return ToolExecutionResult.querySuccess(result.toString(), "今日博客查询完成");
    }
    
    /**
     * 查询最近N天的博客
     */
    private String queryRecent(Map<String, Object> parameters, int limit, boolean includeContent) {
        Integer days = parameters.get("days") != null ? 
            Integer.parseInt(parameters.get("days").toString()) : 30;
        
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where("del_flag = ?", "0")
            .and("status = ?", "1")
            .and("completion_time >= ?", startTime)
            .orderBy("completion_time DESC")
            .limit(limit);
        
        List<AiBlogProductionRecord> records = blogProductionRecordService.list(qw);
        
        if (records.isEmpty()) {
            return ToolExecutionResult.querySuccess(
                "最近 " + days + " 天内没有博客生产记录，可以自由选择任意主题创作。",
                "最近博客查询完成"
            );
        }
        
        // 按日期分组统计
        Map<String, List<AiBlogProductionRecord>> byDate = records.stream()
            .filter(r -> r.getCompletionTime() != null)
            .collect(Collectors.groupingBy(r -> 
                new java.text.SimpleDateFormat("yyyy-MM-dd").format(r.getCompletionTime())
            ));
        
        StringBuilder result = new StringBuilder();
        result.append("最近 ").append(days).append(" 天内共生成 ").append(records.size()).append(" 篇博客\n\n");
        
        // 按日期显示
        result.append("【按日期统计】\n");
        byDate.entrySet().stream()
            .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
            .limit(10) // 最多显示10天
            .forEach(entry -> {
                result.append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" 篇\n");
            });
        result.append("\n");
        
        // 提取所有已写仓库
        List<String> writtenRepos = new ArrayList<>();
        for (AiBlogProductionRecord record : records) {
            String repoInfo = "";
            if (StrUtil.isNotBlank(record.getRepoOwner()) && StrUtil.isNotBlank(record.getRepoTitle())) {
                repoInfo = record.getRepoOwner() + "/" + record.getRepoTitle();
            }
            if (StrUtil.isNotBlank(repoInfo) && !writtenRepos.contains(repoInfo)) {
                writtenRepos.add(repoInfo);
            }
        }
        
        result.append("【已写仓库列表（共 ").append(writtenRepos.size()).append(" 个，请避免重复选择）】\n");
        int displayCount = Math.min(writtenRepos.size(), 30); // 最多显示30个
        for (int i = 0; i < displayCount; i++) {
            result.append(String.format("%d. %s\n", i + 1, writtenRepos.get(i)));
        }
        if (writtenRepos.size() > 30) {
            result.append("... 还有 ").append(writtenRepos.size() - 30).append(" 个仓库\n");
        }
        
        return ToolExecutionResult.querySuccess(result.toString(), "最近博客查询完成");
    }
    
    /**
     * 格式化生产记录列表
     */
    private String formatProductionRecords(List<AiBlogProductionRecord> records, String queryDesc, boolean includeContent) {
        StringBuilder result = new StringBuilder();
        result.append("查询条件：").append(queryDesc).append("\n");
        result.append("共找到 ").append(records.size()).append(" 条博客生产记录：\n\n");
        
        for (int i = 0; i < records.size(); i++) {
            AiBlogProductionRecord record = records.get(i);
            result.append(String.format("%d. ", i + 1));
            
            if (StrUtil.isNotBlank(record.getRepoOwner()) && StrUtil.isNotBlank(record.getRepoTitle())) {
                result.append("仓库: ").append(record.getRepoOwner()).append("/").append(record.getRepoTitle()).append("\n");
            }
            if (StrUtil.isNotBlank(record.getRepoUrl())) {
                result.append("   URL: ").append(record.getRepoUrl()).append("\n");
            }
            if (StrUtil.isNotBlank(record.getRepoLanguage())) {
                result.append("   语言: ").append(record.getRepoLanguage()).append("\n");
            }
            if (StrUtil.isNotBlank(record.getProductionType())) {
                result.append("   类型: ").append(record.getProductionType()).append("\n");
            }
            if (record.getCompletionTime() != null) {
                result.append("   完成时间: ").append(
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getCompletionTime())
                ).append("\n");
            }
            result.append("\n");
        }
        
        return ToolExecutionResult.querySuccess(result.toString(), "博客历史查询完成");
    }
    
    /**
     * 格式化博客列表
     */
    private String formatBlogList(List<Blog> blogs, String queryDesc, boolean includeContent) {
        StringBuilder result = new StringBuilder();
        result.append("查询条件：").append(queryDesc).append("\n");
        result.append("共找到 ").append(blogs.size()).append(" 篇博客：\n\n");
        
        for (int i = 0; i < blogs.size(); i++) {
            Blog blog = blogs.get(i);
            result.append(String.format("%d. %s\n", i + 1, blog.getTitle()));
            if (StrUtil.isNotBlank(blog.getSummary())) {
                result.append("   摘要: ").append(
                    blog.getSummary().length() > 100 ? 
                        blog.getSummary().substring(0, 100) + "..." : 
                        blog.getSummary()
                ).append("\n");
            }
            if (StrUtil.isNotBlank(blog.getCategory())) {
                result.append("   分类: ").append(blog.getCategory()).append("\n");
            }
            if (StrUtil.isNotBlank(blog.getTags())) {
                result.append("   标签: ").append(blog.getTags()).append("\n");
            }
            if (blog.getCreateTime() != null) {
                result.append("   创建时间: ").append(
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(blog.getCreateTime())
                ).append("\n");
            }
            if (includeContent && StrUtil.isNotBlank(blog.getContent())) {
                result.append("   内容预览: ").append(
                    blog.getContent().length() > 200 ? 
                        blog.getContent().substring(0, 200) + "..." : 
                        blog.getContent()
                ).append("\n");
            }
            result.append("\n");
        }
        
        return ToolExecutionResult.querySuccess(result.toString(), "博客列表查询完成");
    }
    
    /**
     * 标准化URL
     */
    private String normalizeUrl(String url) {
        if (StrUtil.isBlank(url)) return "";
        url = url.trim();
        // 移除协议前缀
        url = url.replaceFirst("^https?://", "");
        // 移除末尾斜杠
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url.toLowerCase();
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return true; // 所有参数都有默认值
        }
        
        String queryType = parameters.get("queryType") != null ? 
            parameters.get("queryType").toString() : "recent";
        
        // 验证必填参数
        switch (queryType.toLowerCase()) {
            case "repo_url":
                if (StrUtil.isBlank((String) parameters.get("repoUrl"))) {
                    return false;
                }
                break;
            case "repo_name":
                if (StrUtil.isBlank((String) parameters.get("repoName"))) {
                    return false;
                }
                break;
            case "title":
                if (StrUtil.isBlank((String) parameters.get("titleKeyword"))) {
                    return false;
                }
                break;
        }
        
        // 验证limit参数
        if (parameters.containsKey("limit")) {
            try {
                int limit = Integer.parseInt(parameters.get("limit").toString());
                if (limit < 1 || limit > 200) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 查询今日已生成的博客（检查今天写了哪些，避免重复）：
           {"queryType": "today"}
        
        2. 查询最近30天的博客历史：
           {"queryType": "recent", "days": 30}
        
        3. 检查某个仓库是否已写过博客：
           {"queryType": "repo_url", "repoUrl": "https://github.com/owner/repo"}
        
        4. 按仓库名称查询：
           {"queryType": "repo_name", "repoName": "owner/repo"}
        
        5. 按博客标题关键词查询：
           {"queryType": "title", "titleKeyword": "Spring Boot"}
        
        6. 按时间范围查询：
           {"queryType": "date_range", "startDate": "2024-11-01", "endDate": "2024-11-30"}
        """;
    }
}
