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
        return "查询GitHub今日首次上榜的热门仓库，支持多维度筛选条件";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        // 创建GitHub趋势查询工具规范
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addIntegerProperty("limit", "返回结果数量限制，默认为10，最大150")
            .addStringProperty("language", "编程语言筛选，如java、python、javascript等，可选")
            .addIntegerProperty("minStars", "最小星数筛选，可选")
            .addIntegerProperty("maxStars", "最大星数筛选，可选")
            .addStringProperty("repValue", "仓库价值筛选：普通/值得关注/值得收藏/商业价值，可选")
            .addStringProperty("orderBy", "排序方式：stars(按星数)、forks(按fork数)、issues(按问题数)、created(按创建时间)，默认按星数")
            .addBooleanProperty("hasReadme", "是否有README文件，可选")
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
            // 获取参数
            Integer limit = parameters.get("limit") != null ? 
                Integer.parseInt(parameters.get("limit").toString()) : 10;
            String language = (String) parameters.get("language");
            Integer minStars = parameters.get("minStars") != null ? 
                Integer.parseInt(parameters.get("minStars").toString()) : null;
            Integer maxStars = parameters.get("maxStars") != null ? 
                Integer.parseInt(parameters.get("maxStars").toString()) : null;
            String repValue = (String) parameters.get("repValue");
            String orderBy = (String) parameters.get("orderBy");
            Boolean hasReadme = parameters.get("hasReadme") != null ? 
                Boolean.parseBoolean(parameters.get("hasReadme").toString()) : null;
            
            // 限制最大数量
            if (limit > 150) limit = 150;
            if (limit < 1) limit = 10;
            
            // 构建查询条件 - 改为查询今天首次上榜的项目
            QueryWrapper qw = QueryWrapper.create()
                .from("github_trending")
                .where(new QueryColumn("first_trending_date").eq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .limit(limit);
            
            // 添加各种筛选条件
            if (StrUtil.isNotBlank(language)) {
                qw.and(new QueryColumn("language").eq(language));
            }
            
            if (minStars != null) {
                qw.and(new QueryColumn("stars_count").ge(minStars.toString()));
            }
            
            if (maxStars != null) {
                qw.and(new QueryColumn("stars_count").le(maxStars.toString()));
            }
            
            if (StrUtil.isNotBlank(repValue)) {
                qw.and(new QueryColumn("rep_value").eq(repValue));
            }
            
            if (hasReadme != null) {
                if (hasReadme) {
                    qw.and(new QueryColumn("readme_path").isNotNull());
                } else {
                    qw.and(new QueryColumn("readme_path").isNull());
                }
            }
            
            // 设置排序方式
            if (StrUtil.isNotBlank(orderBy)) {
                switch (orderBy.toLowerCase()) {
                    case "forks":
                        qw.orderBy(new QueryColumn("forks_count").desc(), new QueryColumn("id").desc());
                        break;
                    case "issues":
                        qw.orderBy(new QueryColumn("open_issues_count").desc(), new QueryColumn("id").desc());
                        break;
                    case "created":
                        qw.orderBy(new QueryColumn("github_created_at").desc(), new QueryColumn("id").desc());
                        break;
                    default: // stars 或其他情况
                        qw.orderBy(new QueryColumn("stars_count").desc(), new QueryColumn("id").desc());
                        break;
                }
            } else {
                // 默认按星数排序
                qw.orderBy(new QueryColumn("stars_count").desc(), new QueryColumn("id").desc());
            }
            
            List<GithubTrending> repositories = githubTrendingService.list(qw);
            
            if (repositories.isEmpty()) {
                StringBuilder filterInfo = new StringBuilder();
                if (StrUtil.isNotBlank(language)) filterInfo.append("语言：").append(language).append(" ");
                if (minStars != null) filterInfo.append("最小星数：").append(minStars).append(" ");
                if (maxStars != null) filterInfo.append("最大星数：").append(maxStars).append(" ");
                if (StrUtil.isNotBlank(repValue)) filterInfo.append("仓库价值：").append(repValue).append(" ");
                
                return "今日暂无符合条件的GitHub首次上榜仓库数据" + 
                    (filterInfo.length() > 0 ? "（筛选条件：" + filterInfo.toString().trim() + "）" : "");
            }
            
            // 格式化返回结果
            StringBuilder result = new StringBuilder();
            StringBuilder filterInfo = new StringBuilder();
            if (StrUtil.isNotBlank(language)) filterInfo.append("语言：").append(language).append(" ");
            if (minStars != null) filterInfo.append("最小星数：").append(minStars).append(" ");
            if (maxStars != null) filterInfo.append("最大星数：").append(maxStars).append(" ");
            if (StrUtil.isNotBlank(repValue)) filterInfo.append("仓库价值：").append(repValue).append(" ");
            
            result.append("GitHub今日首次上榜仓库")
                .append(filterInfo.length() > 0 ? "（筛选条件：" + filterInfo.toString().trim() + "）" : "")
                .append("：\n\n");
            
            for (int i = 0; i < repositories.size(); i++) {
                GithubTrending repo = repositories.get(i);
                result.append(String.format("%d. %s/%s\n", i + 1, repo.getOwner(), repo.getTitle()));
                result.append(String.format("   描述: %s\n", StrUtil.isNotBlank(repo.getDescription()) ? repo.getDescription() : "无描述"));
                result.append(String.format("   语言: %s\n", StrUtil.isNotBlank(repo.getLanguage()) ? repo.getLanguage() : "未知"));
                result.append(String.format("   星数: %s\n", StrUtil.isNotBlank(repo.getStarsCount()) ? repo.getStarsCount() : "0"));
                result.append(String.format("   Fork数: %s\n", StrUtil.isNotBlank(repo.getForksCount()) ? repo.getForksCount() : "0"));
                result.append(String.format("   问题数: %s\n", StrUtil.isNotBlank(repo.getOpenIssuesCount()) ? repo.getOpenIssuesCount() : "0"));
                result.append(String.format("   链接: %s\n", StrUtil.isNotBlank(repo.getUrl()) ? repo.getUrl() : "无链接"));
                result.append(String.format("   上榜天数: %s\n", StrUtil.isNotBlank(repo.getTrendingDays()) ? repo.getTrendingDays() : "1"));
                result.append(String.format("   连续上榜: %s天\n", StrUtil.isNotBlank(repo.getContinuousTrendingDays()) ? repo.getContinuousTrendingDays() : "1"));
                if (StrUtil.isNotBlank(repo.getRepValue())) {
                    result.append(String.format("   仓库价值: %s\n", repo.getRepValue()));
                }
                if (repo.getGithubCreatedAt() != null) {
                    result.append(String.format("   创建时间: %s\n", repo.getGithubCreatedAt().toString().substring(0, 10)));
                }
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
                if (limit < 1 || limit > 150) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // 验证minStars参数
        if (parameters.containsKey("minStars")) {
            try {
                int minStars = Integer.parseInt(parameters.get("minStars").toString());
                if (minStars < 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // 验证maxStars参数
        if (parameters.containsKey("maxStars")) {
            try {
                int maxStars = Integer.parseInt(parameters.get("maxStars").toString());
                if (maxStars < 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // 验证stars范围逻辑
        if (parameters.containsKey("minStars") && parameters.containsKey("maxStars")) {
            try {
                int minStars = Integer.parseInt(parameters.get("minStars").toString());
                int maxStars = Integer.parseInt(parameters.get("maxStars").toString());
                if (minStars > maxStars) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // 验证repValue参数
        if (parameters.containsKey("repValue")) {
            String repValue = parameters.get("repValue").toString();
            if (!repValue.equals("普通") && !repValue.equals("值得关注") && 
                !repValue.equals("值得收藏") && !repValue.equals("商业价值")) {
                return false;
            }
        }
        
        // 验证orderBy参数
        if (parameters.containsKey("orderBy")) {
            String orderBy = parameters.get("orderBy").toString().toLowerCase();
            if (!orderBy.equals("stars") && !orderBy.equals("forks") && 
                !orderBy.equals("issues") && !orderBy.equals("created")) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 查询所有语言的今日首次上榜仓库（默认10个）：
           {}
        
        2. 查询Java语言的今日首次上榜仓库：
           {"language": "java"}
        
        3. 查询星数在1000-10000之间的仓库，限制20个结果：
           {"minStars": 1000, "maxStars": 10000, "limit": 20}
        
        4. 查询值得收藏的Python仓库，按fork数排序：
           {"language": "python", "repValue": "值得收藏", "orderBy": "forks"}
        
        5. 查询有README文件的商业价值仓库：
           {"repValue": "商业价值", "hasReadme": true}
        
        6. 综合查询示例：
           {"language": "javascript", "minStars": 500, "repValue": "值得关注", "orderBy": "stars", "limit": 50}
        """;
    }
}