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

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.ai.domain.AiBlogProductionRecord;
import com.ruoyi.project.ai.service.IAiBlogProductionRecordService;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.service.IBlogService;
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
    
    private static final Logger log = LoggerFactory.getLogger(GithubTrendingLangChain4jTool.class);
    
    @Autowired
    private IGithubTrendingService githubTrendingService;
    
    @Autowired
    private IAiBlogProductionRecordService blogProductionRecordService;
    
    @Autowired
    private IBlogService blogService;
    
    @Override
    public String getToolName() {
        return "github_trending";
    }
    
    @Override
    public String getToolDescription() {
        return "智能查询GitHub热门仓库,优先查询今日首次上榜项目,若无结果则自动降级到本周或本月。支持多维度筛选,自动过滤已生成过博客的仓库。注意:language参数是可选的,如果不指定则返回所有编程语言的仓库,建议默认不指定language以获取更全面的结果";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        // 创建GitHub趋势查询工具规范
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addIntegerProperty("limit", "返回结果数量限制,默认为10,最大150")
            .addStringProperty("language", "编程语言筛选,如java、python、javascript等,可选。建议不指定以获取所有语言的仓库")
            .addIntegerProperty("minStars", "最小星数筛选,可选")
            .addIntegerProperty("maxStars", "最大星数筛选,可选")
            .addStringProperty("orderBy", "排序方式:stars(按星数)、forks(按fork数)、issues(按问题数)、created(按创建Mysql时间),默认按星数")
            .addBooleanProperty("hasReadme", "是否有README文件,可选")
            .addStringProperty("timeRange", "时间范围:today(今日)、week(本周)、month(本月)、auto(自动降级,默认)。auto模式下优先查询今日,若无结果则自动尝试本周、本月")
            .addBooleanProperty("includeGenerated", "是否包含已生成过博客的仓库,默认false（不包含）")
            .build();
        
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(parametersSchema)
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        // 获取参数
        Integer limit = parameters.get("limit") != null ? 
            Integer.parseInt(parameters.get("limit").toString()) : 10;
        String language = (String) parameters.get("language");
        Integer minStars = parameters.get("minStars") != null ? 
            Integer.parseInt(parameters.get("minStars").toString()) : null;
        Integer maxStars = parameters.get("maxStars") != null ? 
            Integer.parseInt(parameters.get("maxStars").toString()) : null;
        String orderBy = (String) parameters.get("orderBy");
        Boolean hasReadme = parameters.get("hasReadme") != null ? 
            Boolean.parseBoolean(parameters.get("hasReadme").toString()) : null;
        String timeRange = parameters.get("timeRange") != null ?
            parameters.get("timeRange").toString() : "auto";
        Boolean includeGenerated = parameters.get("includeGenerated") != null ?
            Boolean.parseBoolean(parameters.get("includeGenerated").toString()) : false;
        
        log.info("[GithubTrendingTool] 开始查询GitHub趋势仓库, params: limit={}, language={}, minStars={}, maxStars={}, timeRange={}, includeGenerated={}",
                limit, language, minStars, maxStars, timeRange, includeGenerated);
        
        // 限制最大数量
        if (limit > 150) limit = 150;
        if (limit < 1) limit = 10;
        
        // 第一步：查询最近生成成功的博客记录（用于过滤）
        List<GeneratedRepoInfo> generatedRepos = includeGenerated ? new ArrayList<>() : getRecentGeneratedRepoInfos();
        
        // 第二步：根据时间范围查询项目
        List<GithubTrending> repositories = new ArrayList<>();
        String actualTimeRange = timeRange;
        
        // 构建基础查询条件
        if ("auto".equalsIgnoreCase(timeRange)) {
            // 自动降级模式：今日 -> 本周 -> 本月
            repositories = queryRepositories("today", language, minStars, maxStars, hasReadme, orderBy, generatedRepos, limit * 3);
            actualTimeRange = "today";
            
            // 如果指定了语言但查不到,尝试不指定语言再查一次
            if (repositories.isEmpty() && StrUtil.isNotBlank(language)) {
                log.warn("[GithubTrendingTool] 指定语言{}查不到数据,尝试查询所有语言", language);
                repositories = queryRepositories("today", null, minStars, maxStars, hasReadme, orderBy, generatedRepos, limit * 3);
            }
            
            if (repositories.size() < limit) {
                List<GithubTrending> weekRepos = queryRepositories("week", language, minStars, maxStars, hasReadme, orderBy, generatedRepos, limit * 3);
                // 如果指定了语言但查不到,尝试不指定语言再查一次
                if (weekRepos.isEmpty() && StrUtil.isNotBlank(language)) {
                    log.warn("[GithubTrendingTool] 本周指定语言{}查不到数据,尝试查询所有语言", language);
                    weekRepos = queryRepositories("week", null, minStars, maxStars, hasReadme, orderBy, generatedRepos, limit * 3);
                }
                // 合并并去重
                repositories = mergeAndDeduplicate(repositories, weekRepos);
                if (repositories.size() >= limit || weekRepos.size() > 0) {
                    actualTimeRange = "week";
                }
            }
            
            if (repositories.size() < limit) {
                List<GithubTrending> monthRepos = queryRepositories("month", language, minStars, maxStars, hasReadme, orderBy, generatedRepos, limit * 3);
                // 如果指定了语言但查不到,尝试不指定语言再查一次
                if (monthRepos.isEmpty() && StrUtil.isNotBlank(language)) {
                    log.warn("[GithubTrendingTool] 本月指定语言{}查不到数据,尝试查询所有语言", language);
                    monthRepos = queryRepositories("month", null, minStars, maxStars, hasReadme, orderBy, generatedRepos, limit * 3);
                }
                // 合并并去重
                repositories = mergeAndDeduplicate(repositories, monthRepos);
                if (monthRepos.size() > 0) {
                    actualTimeRange = "month";
                }
            }
        } else {
            // 指定时间范围
            repositories = queryRepositories(timeRange, language, minStars, maxStars, hasReadme, orderBy, generatedRepos, limit * 3);
            // 如果指定了语言但查不到,尝试不指定语言再查一次
            if (repositories.isEmpty() && StrUtil.isNotBlank(language)) {
                log.warn("[GithubTrendingTool] 指定语言{}查不到数据,尝试查询所有语言", language);
                repositories = queryRepositories(timeRange, null, minStars, maxStars, hasReadme, orderBy, generatedRepos, limit * 3);
            }
            actualTimeRange = timeRange;
        }
        
        // 第三步：如果不包含已生成的，进行过滤
        if (!includeGenerated && !generatedRepos.isEmpty()) {
            repositories = filterAlreadyGenerated(repositories, generatedRepos);
        }
        
        // 限制返回数量
        if (repositories.size() > limit) {
            repositories = repositories.subList(0, limit);
        }
        
        if (repositories.isEmpty()) {
            log.warn("[GithubTrendingTool] 查询结果为空, timeRange={}, language={}, minStars={}, maxStars={}, hasReadme={}",
                    actualTimeRange, language, minStars, maxStars, hasReadme);
            return ToolExecutionResult.empty("query", "未找到符合条件的GitHub趋势仓库");
        }
        
        log.info("[GithubTrendingTool] 查询成功, timeRange={}, resultSize={}", actualTimeRange, repositories.size());
        
        // 格式化返回结果
        StringBuilder result = new StringBuilder();
        StringBuilder filterInfo = new StringBuilder();
        if (StrUtil.isNotBlank(language)) filterInfo.append("语言：").append(language).append(" ");
        if (minStars != null) filterInfo.append("最小星数：").append(minStars).append(" ");
        if (maxStars != null) filterInfo.append("最大星数：").append(maxStars).append(" ");
        
        String timeRangeDesc = getTimeRangeDescription(actualTimeRange);
        result.append("GitHub").append(timeRangeDesc).append("首次上榜仓库")
            .append(filterInfo.length() > 0 ? "（筛选条件：" + filterInfo.toString().trim() + "）" : "")
            .append(!includeGenerated ? "（已过滤" + generatedRepos.size() + "个已生成博客的仓库）" : "")
            .append("：\n\n");
        
        for (int i = 0; i < repositories.size(); i++) {
            GithubTrending repo = repositories.get(i);
            result.append(String.format("%d. %s/%s\n", i + 1, repo.getOwner(), repo.getTitle()));
            result.append(String.format("   描述: %s\n", StrUtil.isNotBlank(repo.getDescription()) ? repo.getDescription() : "无描述"));
            result.append(String.format("   语言: %s\n", StrUtil.isNotBlank(repo.getLanguage()) ? repo.getLanguage() : "未知"));
            result.append(String.format("   星数: %s\n", StrUtil.isNotBlank(repo.getStarsCount()) ? repo.getStarsCount() : "0"));
            result.append(String.format("   Fork数: %s\n", StrUtil.isNotBlank(repo.getForksCount()) ? repo.getForksCount() : "0"));
            result.append(String.format("   问题数: %s\n", StrUtil.isNotBlank(repo.getOpenIssuesCount()) ? repo.getOpenIssuesCount() : "0"));
            result.append(String.format("   仓库地址: %s\n", StrUtil.isNotBlank(repo.getUrl()) ? repo.getUrl() : "无链接"));
            result.append(String.format("   上榜天数: %s\n", StrUtil.isNotBlank(repo.getTrendingDays()) ? repo.getTrendingDays() : "1"));
            result.append(String.format("   连续上榜: %s天\n", StrUtil.isNotBlank(repo.getContinuousTrendingDays()) ? repo.getContinuousTrendingDays() : "1"));
            
            // 添加README文件相关信息
            if (StrUtil.isNotBlank(repo.getReadmePath())) {
                result.append(String.format("   README文件地址: %s\n", repo.getReadmePath()));
            }
            if (StrUtil.isNotBlank(repo.getAiReadmePath())) {
                result.append(String.format("   AI翻译README地址: %s\n", repo.getAiReadmePath()));
            }
            
            // 添加仓库价值信息
            if (StrUtil.isNotBlank(repo.getRepValue())) {
                result.append(String.format("   仓库价值: %s\n", repo.getRepValue()));
            }
            
            // 添加时间相关信息
            if (repo.getGithubCreatedAt() != null) {
                result.append(String.format("   GitHub创建时间: %s\n", repo.getGithubCreatedAt().toString().substring(0, 10)));
            }
            if (repo.getGithubUpdatedAt() != null) {
                result.append(String.format("   GitHub最后更新: %s\n", repo.getGithubUpdatedAt().toString().substring(0, 10)));
            }
            if (repo.getFirstTrendingDate() != null) {
                result.append(String.format("   首次上榜日期: %s\n", repo.getFirstTrendingDate().toString()));
            }
            if (repo.getLastTrendingDate() != null) {
                result.append(String.format("   最后上榜日期: %s\n", repo.getLastTrendingDate().toString()));
            }
            
            // 添加推广文章信息
            if (StrUtil.isNotBlank(repo.getPromotionArticle())) {
                result.append(String.format("   推广文章: %s\n", repo.getPromotionArticle()));
            }
            
            // 添加README更新时间
            if (repo.getReadmeUpdatedAt() != null) {
                result.append(String.format("   README更新时间: %s\n", repo.getReadmeUpdatedAt().toString().substring(0, 10)));
            }
            
            // 添加是否需要更新标识
            if (StrUtil.isNotBlank(repo.getIsNeedUpdate())) {
                String needUpdate = "1".equals(repo.getIsNeedUpdate()) ? "是" : "否";
                result.append(String.format("   需要更新: %s\n", needUpdate));
            }
            
            result.append("\n");
        }
        
        return ToolExecutionResult.querySuccess(result.toString(), "成功查询GitHub趋势仓库");
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
        1. 智能查询（自动降级）- 优先今日，无结果则查本周/本月：
           {}
        
        2. 查询Java语言的热门仓库（自动降级）：
           {"language": "java"}
        
        3. 仅查询今日首次上榜仓库：
           {"timeRange": "today"}
        
        4. 查询本周首次上榜仓库：
           {"timeRange": "week"}
        
        5. 查询本月首次上榜仓库：
           {"timeRange": "month"}
        
        6. 查询星数在1000-10000之间的仓库，限制20个结果：
           {"minStars": 1000, "maxStars": 10000, "limit": 20}
        
        7. 包含已生成过博客的仓库（不过滤）：
           {"includeGenerated": true}
        
        8. 综合查询示例 - 查询本周Python仓库，星数大于500：
           {"timeRange": "week", "language": "python", "minStars": 500, "hasReadme": true}
        """;
    }
    
    /**
     * 查询最近60天内生成成功的博客对应的仓库信息
     * 使用多维度信息进行精确匹配
     * @return 已生成博客的仓库信息列表
     */
    private List<GeneratedRepoInfo> getRecentGeneratedRepoInfos() {
        List<GeneratedRepoInfo> repoInfos = new ArrayList<>();
        
        try {
            // 查询最近60天内状态为成功(status=1)的生产记录
            LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
            QueryWrapper recordQw = QueryWrapper.create()
                .from("ai_blog_production_record")
                .where(new QueryColumn("status").eq("1"))
                .and(new QueryColumn("del_flag").eq("0"))
                .and(new QueryColumn("completion_time").ge(sixtyDaysAgo));
            
            List<AiBlogProductionRecord> records = blogProductionRecordService.list(recordQw);
            
            if (records.isEmpty()) {
                return repoInfos;
            }
            
            // 方式1：直接从生产记录中提取仓库信息（最精确）
            for (AiBlogProductionRecord record : records) {
                GeneratedRepoInfo info = new GeneratedRepoInfo();
                info.repoUrl = record.getRepoUrl();
                info.repoTitle = record.getRepoTitle();
                info.repoOwner = record.getRepoOwner();
                
                // 只有当有有效信息时才加入
                if (StrUtil.isNotBlank(info.repoUrl) || 
                    (StrUtil.isNotBlank(info.repoOwner) && StrUtil.isNotBlank(info.repoTitle))) {
                    repoInfos.add(info);
                }
            }
            
            // 方式2：从博客内容中提取GitHub仓库信息（作为补充）
            List<Long> blogIds = records.stream()
                .map(AiBlogProductionRecord::getBlogId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
            
            if (!blogIds.isEmpty()) {
                QueryWrapper blogQw = QueryWrapper.create()
                    .from("blog")
                    .where(new QueryColumn("blog_id").in(blogIds))
                    .and(new QueryColumn("del_flag").eq("0"));
                
                List<Blog> blogs = blogService.list(blogQw);
                
                for (Blog blog : blogs) {
                    // 从博客内容中提取GitHub仓库URL
                    List<String> extractedUrls = extractGitHubUrls(blog.getContent());
                    for (String url : extractedUrls) {
                        GeneratedRepoInfo info = parseGitHubUrl(url);
                        if (info != null && !containsRepoInfo(repoInfos, info)) {
                            repoInfos.add(info);
                        }
                    }
                    
                    // 将博客标题也作为匹配依据
                    if (StrUtil.isNotBlank(blog.getTitle())) {
                        GeneratedRepoInfo titleInfo = new GeneratedRepoInfo();
                        titleInfo.blogTitle = blog.getTitle();
                        repoInfos.add(titleInfo);
                    }
                }
            }
        } catch (Exception e) {
            // 查询失败时返回空列表，不影响主流程
            System.err.println("查询最近生成的博客仓库信息失败: " + e.getMessage());
        }
        
        return repoInfos;
    }
    
    /**
     * 过滤掉已生成过博客的仓库（优化版）
     * 使用多维度匹配策略，精确度更高
     * @param repositories 待过滤的仓库列表
     * @param generatedRepos 已生成博客的仓库信息列表
     * @return 过滤后的仓库列表
     */
    private List<GithubTrending> filterAlreadyGenerated(List<GithubTrending> repositories, List<GeneratedRepoInfo> generatedRepos) {
        if (generatedRepos.isEmpty()) {
            return repositories;
        }
        
        List<GithubTrending> filtered = new ArrayList<>();
        
        for (GithubTrending repo : repositories) {
            if (!isRepoAlreadyGenerated(repo, generatedRepos)) {
                filtered.add(repo);
            }
        }
        
        return filtered;
    }
    
    /**
     * 判断仓库是否已生成过博客
     * @param repo 待检查的仓库
     * @param generatedRepos 已生成博客的仓库信息列表
     * @return 是否已生成
     */
    private boolean isRepoAlreadyGenerated(GithubTrending repo, List<GeneratedRepoInfo> generatedRepos) {
        String repoUrl = repo.getUrl();
        String repoName = repo.getTitle();
        String repoOwner = repo.getOwner();
        String fullRepoName = StrUtil.isNotBlank(repoOwner) && StrUtil.isNotBlank(repoName) 
            ? repoOwner + "/" + repoName : "";
        
        for (GeneratedRepoInfo genInfo : generatedRepos) {
            // 优先级1：URL完全匹配（最精确）
            if (StrUtil.isNotBlank(genInfo.repoUrl) && StrUtil.isNotBlank(repoUrl)) {
                // 标准化URL比较（移除末尾斜杠，忽略大小写）
                String normalizedRepoUrl = normalizeUrl(repoUrl);
                String normalizedGenUrl = normalizeUrl(genInfo.repoUrl);
                if (normalizedRepoUrl.equalsIgnoreCase(normalizedGenUrl)) {
                    return true;
                }
            }
            
            // 优先级2：owner/repo 完全匹配
            if (StrUtil.isNotBlank(genInfo.repoOwner) && StrUtil.isNotBlank(genInfo.repoTitle)) {
                String genFullName = genInfo.repoOwner + "/" + genInfo.repoTitle;
                if (StrUtil.isNotBlank(fullRepoName) && fullRepoName.equalsIgnoreCase(genFullName)) {
                    return true;
                }
            }
            
            // 优先级3：仓库名称精确匹配（需要名称长度大于4，避免误匹配）
            if (StrUtil.isNotBlank(genInfo.repoTitle) && StrUtil.isNotBlank(repoName)) {
                if (repoName.length() > 4 && repoName.equalsIgnoreCase(genInfo.repoTitle)) {
                    return true;
                }
            }
            
            // 优先级4：博客标题包含完整仓库名
            if (StrUtil.isNotBlank(genInfo.blogTitle) && StrUtil.isNotBlank(fullRepoName)) {
                if (genInfo.blogTitle.contains(fullRepoName) || 
                    genInfo.blogTitle.toLowerCase().contains(fullRepoName.toLowerCase())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 根据时间范围查询仓库
     */
    private List<GithubTrending> queryRepositories(String timeRange, String language, Integer minStars, 
            Integer maxStars, Boolean hasReadme, String orderBy, List<GeneratedRepoInfo> generatedRepos, int maxLimit) {
        
        LocalDate startDate;
        LocalDate endDate = LocalDate.now();
        
        switch (timeRange.toLowerCase()) {
            case "week":
                startDate = endDate.minusDays(7);
                break;
            case "month":
                startDate = endDate.minusDays(30);
                break;
            case "today":
            default:
                startDate = endDate;
                break;
        }
        
        log.info("[GithubTrendingTool] 查询时间范围: startDate={}, endDate={}, timeRange={}", startDate, endDate, timeRange);
        
        QueryWrapper qw = QueryWrapper.create()
            .from("github_trending")
            .where(new QueryColumn("first_trending_date").ge(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .and(new QueryColumn("first_trending_date").le(endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        
        // 添加各种筛选条件
        if (StrUtil.isNotBlank(language)) {
            qw.and(new QueryColumn("language").eq(language));
        }
        
        if (minStars != null) {
            qw.and(new QueryColumn("stars_count").ge(minStars));
        }
        
        if (maxStars != null) {
            qw.and(new QueryColumn("stars_count").le(maxStars));
        }
        
        if (hasReadme != null) {
            if (hasReadme) {
                qw.and(new QueryColumn("readme_path").isNotNull());
            } else {
                qw.and(new QueryColumn("readme_path").isNull());
            }
        }
        
        // 设置排序方式
        applyOrderBy(qw, orderBy);
        
        // 限制查询数量
        qw.limit(maxLimit);
        
        return githubTrendingService.list(qw);
    }
    
    /**
     * 应用排序方式
     */
    private void applyOrderBy(QueryWrapper qw, String orderBy) {
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
            qw.orderBy(new QueryColumn("stars_count").desc(), new QueryColumn("id").desc());
        }
    }
    
    /**
     * 合并并去重仓库列表
     */
    private List<GithubTrending> mergeAndDeduplicate(List<GithubTrending> list1, List<GithubTrending> list2) {
        Map<String, GithubTrending> map = new java.util.LinkedHashMap<>();
        for (GithubTrending repo : list1) {
            map.put(repo.getId(), repo);
        }
        for (GithubTrending repo : list2) {
            map.putIfAbsent(repo.getId(), repo);
        }
        return new ArrayList<>(map.values());
    }
    
    /**
     * 获取时间范围描述
     */
    private String getTimeRangeDescription(String timeRange) {
        switch (timeRange.toLowerCase()) {
            case "week":
                return "本周";
            case "month":
                return "本月";
            case "today":
            default:
                return "今日";
        }
    }
    
    /**
     * 标准化URL（移除末尾斜杠，转小写）
     */
    private String normalizeUrl(String url) {
        if (StrUtil.isBlank(url)) return "";
        url = url.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url.toLowerCase();
    }
    
    /**
     * 从文本中提取GitHub仓库URL
     */
    private List<String> extractGitHubUrls(String content) {
        List<String> urls = new ArrayList<>();
        if (StrUtil.isBlank(content)) return urls;
        
        // 匹配 GitHub 仓库 URL 的正则表达式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "https?://github\\.com/([\\w-]+)/([\\w.-]+)(?:/|$|[?#])", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            urls.add(matcher.group(0).replaceAll("[?#].*$", "").replaceAll("/$", ""));
        }
        
        return urls;
    }
    
    /**
     * 解析GitHub URL为仓库信息
     */
    private GeneratedRepoInfo parseGitHubUrl(String url) {
        if (StrUtil.isBlank(url)) return null;
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "github\\.com/([\\w-]+)/([\\w.-]+)", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            GeneratedRepoInfo info = new GeneratedRepoInfo();
            info.repoUrl = url;
            info.repoOwner = matcher.group(1);
            info.repoTitle = matcher.group(2);
            return info;
        }
        
        return null;
    }
    
    /**
     * 检查列表中是否已包含相同的仓库信息
     */
    private boolean containsRepoInfo(List<GeneratedRepoInfo> list, GeneratedRepoInfo info) {
        for (GeneratedRepoInfo existing : list) {
            if (StrUtil.isNotBlank(existing.repoUrl) && StrUtil.isNotBlank(info.repoUrl)) {
                if (normalizeUrl(existing.repoUrl).equals(normalizeUrl(info.repoUrl))) {
                    return true;
                }
            }
            if (StrUtil.isNotBlank(existing.repoOwner) && StrUtil.isNotBlank(existing.repoTitle) &&
                StrUtil.isNotBlank(info.repoOwner) && StrUtil.isNotBlank(info.repoTitle)) {
                if (existing.repoOwner.equalsIgnoreCase(info.repoOwner) && 
                    existing.repoTitle.equalsIgnoreCase(info.repoTitle)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 已生成博客的仓库信息内部类
     */
    private static class GeneratedRepoInfo {
        String repoUrl;       // 仓库URL
        String repoTitle;     // 仓库名称
        String repoOwner;     // 仓库所有者
        String blogTitle;     // 博客标题（用于模糊匹配）
    }
}