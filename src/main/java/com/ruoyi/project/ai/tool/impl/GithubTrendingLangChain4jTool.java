package com.ruoyi.project.ai.tool.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.github.domain.GithubTrending;
import com.ruoyi.project.github.service.IGithubTrendingService;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的GitHub趋势查询工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class GithubTrendingLangChain4jTool implements LangChain4jTool {
    
    @Autowired
    private IGithubTrendingService githubTrendingService;
    
    @Override
    public String getToolName() {
        return "github_trending";
    }
    
    @Override
    public String getToolDescription() {
        return "查询GitHub今日上榜的热门仓库，支持按编程语言筛选";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        // 创建GitHub趋势查询工具规范
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("language", "编程语言筛选，如java、python、javascript等，不填则查询所有语言")
            .addIntegerProperty("limit", "返回结果数量限制，默认为10，最大50")
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
            String language = (String) parameters.get("language");
            Integer limit = parameters.get("limit") != null ? 
                Integer.parseInt(parameters.get("limit").toString()) : 10;
            
            // 限制最大数量
            if (limit > 50) limit = 50;
            if (limit < 1) limit = 10;
            
            // 构建查询条件
            QueryWrapper qw = QueryWrapper.create()
                .from("github_trending")
                .where(new QueryColumn("last_trending_date").eq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .orderBy(new QueryColumn("stars_count").desc(), new QueryColumn("id").desc())
                .limit(limit);
            
            // 如果指定了编程语言，添加语言筛选
            if (StrUtil.isNotBlank(language)) {
                qw.and(new QueryColumn("language").eq(language));
            }
            
            List<GithubTrending> repositories = githubTrendingService.list(qw);
            
            if (repositories.isEmpty()) {
                return "今日暂无GitHub趋势仓库数据" + (StrUtil.isNotBlank(language) ? "（语言：" + language + "）" : "");
            }
            
            // 格式化返回结果
            StringBuilder result = new StringBuilder();
            result.append("GitHub今日趋势仓库").append(StrUtil.isNotBlank(language) ? "（语言：" + language + "）" : "").append("：\n\n");
            
            for (int i = 0; i < repositories.size(); i++) {
                GithubTrending repo = repositories.get(i);
                result.append(String.format("%d. %s/%s\n", i + 1, repo.getOwner(), repo.getTitle()));
                result.append(String.format("   描述: %s\n", StrUtil.isNotBlank(repo.getDescription()) ? repo.getDescription() : "无描述"));
                result.append(String.format("   语言: %s\n", StrUtil.isNotBlank(repo.getLanguage()) ? repo.getLanguage() : "未知"));
                result.append(String.format("   星数: %s\n", StrUtil.isNotBlank(repo.getStarsCount()) ? repo.getStarsCount() : "0"));
                result.append(String.format("   Fork数: %s\n", StrUtil.isNotBlank(repo.getForksCount()) ? repo.getForksCount() : "0"));
                result.append(String.format("   链接: %s\n", StrUtil.isNotBlank(repo.getUrl()) ? repo.getUrl() : "无链接"));
                result.append(String.format("   上榜天数: %s\n", StrUtil.isNotBlank(repo.getTrendingDays()) ? repo.getTrendingDays() : "1"));
                result.append("\n");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "查询GitHub趋势数据时发生错误: " + e.getMessage();
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return true; // 所有参数都是可选的
        }
        
        // 验证limit参数
        if (parameters.containsKey("limit")) {
            try {
                int limit = Integer.parseInt(parameters.get("limit").toString());
                if (limit < 1 || limit > 50) {
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
        1. 查询所有语言的今日趋势仓库（默认10个）：
           {}
        
        2. 查询Java语言的今日趋势仓库：
           {"language": "java"}
        
        3. 查询Python语言的今日趋势仓库，限制5个结果：
           {"language": "python", "limit": 5}
        """;
    }
}