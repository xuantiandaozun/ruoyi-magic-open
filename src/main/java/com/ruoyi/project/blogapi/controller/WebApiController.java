package com.ruoyi.project.blogapi.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aspectj.lang.annotation.Anonymous;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.article.service.IBlogService;
import com.ruoyi.project.github.domain.GithubTrending;
import com.ruoyi.project.github.service.IGithubTrendingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Web公开API Controller
 * 提供给博客网站前端的统计和概览数据接口（免登录访问）
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Tag(name = "Web公开API")
@Anonymous
@RestController
@RequestMapping("/api/web")
public class WebApiController extends BaseController
{
    @Autowired
    private IBlogService blogService;
    
    @Autowired
    private IGithubTrendingService githubTrendingService;

    /**
     * 获取博客统计概览数据
     * 
     * @return 统计数据
     */
    @Operation(summary = "获取博客统计概览数据")
    @GetMapping("/totalDate")
    public AjaxResult getTotalDate()
    {
        Map<String, Object> result = new HashMap<>();
        
        // 统计总文章数（已发布且未删除）
        QueryWrapper totalQuery = QueryWrapper.create()
            .eq("del_flag", "0")
            .eq("status", "1");
        long totalArticles = blogService.count(totalQuery);
        
        // 统计今日新增文章数
        LocalDate today = LocalDate.now();
        QueryWrapper todayQuery = QueryWrapper.create()
            .eq("del_flag", "0")
            .eq("status", "1")
            .ge("create_time", today.atStartOfDay())
            .lt("create_time", today.plusDays(1).atStartOfDay());
        long todayNewCount = blogService.count(todayQuery);
        
        // 统计本周活跃文章数（本周有更新的）
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        QueryWrapper weekQuery = QueryWrapper.create()
            .eq("del_flag", "0")
            .eq("status", "1")
            .ge("update_time", weekStart.atStartOfDay());
        long weekActiveCount = blogService.count(weekQuery);
        
        // 统计GitHub trending的总stars数（今日）
        QueryWrapper trendingTodayQuery = QueryWrapper.create()
            .ge("first_trending_date", today.atStartOfDay())
            .lt("first_trending_date", today.plusDays(1).atStartOfDay());
        List<GithubTrending> todayTrending = githubTrendingService.list(trendingTodayQuery);
        long todayTotalStars = todayTrending.stream()
            .mapToLong(t -> {
                try {
                    return Long.parseLong(t.getStarsCount());
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .sum();
        
        // 统计本周平均stars
        QueryWrapper trendingWeekQuery = QueryWrapper.create()
            .ge("first_trending_date", weekStart.atStartOfDay());
        List<GithubTrending> weekTrending = githubTrendingService.list(trendingWeekQuery);
        long weekAvgStars = weekTrending.isEmpty() ? 0 : 
            weekTrending.stream()
                .mapToLong(t -> {
                    try {
                        return Long.parseLong(t.getStarsCount());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .sum() / weekTrending.size();
        
        result.put("totalArticles", totalArticles);
        result.put("todayNewCount", todayNewCount);
        result.put("weekActiveCount", weekActiveCount);
        result.put("todayTotalStars", todayTotalStars);
        result.put("weekAvgStars", weekAvgStars);
        
        return success(result);
    }

    /**
     * 获取GitHub热门仓库列表（今日新上榜）
     * 
     * @param limit 返回数量限制，默认6
     * @return 热门仓库列表
     */
    @Operation(summary = "获取GitHub热门仓库列表（今日新上榜）")
    @GetMapping("/trending")
    public AjaxResult getTrending(@RequestParam(defaultValue = "6") Integer limit)
    {
        // 默认返回6条
        int size = (limit != null && limit > 0) ? limit : 6;
        
        // 构建查询条件，按首次上榜日期和stars数排序
        QueryWrapper queryWrapper = QueryWrapper.create()
            .orderBy("first_trending_date", false)
            .orderBy("CAST(stars_count AS UNSIGNED)", false)
            .limit(size);
        
        List<GithubTrending> trendingList = githubTrendingService.list(queryWrapper);
        
        return success(trendingList);
    }

    /**
     * 获取本周热门仓库列表
     * 
     * @param limit 返回数量限制，默认6
     * @return 本周热门仓库列表
     */
    @Operation(summary = "获取本周热门仓库列表")
    @GetMapping("/workHot")
    public AjaxResult getWeekHot(@RequestParam(defaultValue = "6") Integer limit)
    {
        // 默认返回6条
        int size = (limit != null && limit > 0) ? limit : 6;
        
        // 获取本周开始日期
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // 构建查询条件：本周内的仓库，按stars数排序
        QueryWrapper queryWrapper = QueryWrapper.create()
            .ge("first_trending_date", weekStart.atStartOfDay())
            .orderBy("CAST(stars_count AS UNSIGNED)", false)
            .limit(size);
        
        List<GithubTrending> trendingList = githubTrendingService.list(queryWrapper);
        
        return success(trendingList);
    }

    /**
     * 获取本月热门仓库列表
     * 
     * @param limit 返回数量限制，默认6
     * @return 本月热门仓库列表
     */
    @Operation(summary = "获取本月热门仓库列表")
    @GetMapping("/monthHot")
    public AjaxResult getMonthHot(@RequestParam(defaultValue = "6") Integer limit)
    {
        // 默认返回6条
        int size = (limit != null && limit > 0) ? limit : 6;
        
        // 获取本月开始日期
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        
        // 构建查询条件：本月内的仓库，按stars数排序
        QueryWrapper queryWrapper = QueryWrapper.create()
            .ge("first_trending_date", monthStart.atStartOfDay())
            .orderBy("CAST(stars_count AS UNSIGNED)", false)
            .limit(size);
        
        List<GithubTrending> trendingList = githubTrendingService.list(queryWrapper);
        
        return success(trendingList);
    }

    /**
     * 获取GitHub仓库总榜单（分页）
     * 
     * @param page 页码，从0开始（前端传入），会自动转换为1开始
     * @param size 每页数量，默认12
     * @param language 编程语言（可选）
     * @return 仓库列表分页数据
     */
    @Operation(summary = "获取GitHub仓库总榜单")
    @GetMapping("/total")
    public AjaxResult getTotal(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "12") Integer size,
        @RequestParam(required = false) String language
    )
    {
        // 将前端的0开始的页码转换为1开始（MyBatis-Flex要求）
        int pageNum = page + 1;
        
        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
            .orderBy("CAST(stars_count AS UNSIGNED)", false);
        
        // 按编程语言筛选
        if (language != null && !language.isEmpty()) {
            queryWrapper.eq("language", language);
        }
        
        // 分页查询
        Page<GithubTrending> pageResult = githubTrendingService.page(
            new Page<>(pageNum, size),
            queryWrapper
        );
        
        return success(pageResult);
    }

    /**
     * 获取GitHub仓库中实际存在的编程语言列表
     * 
     * @return 编程语言列表，包含每种语言的仓库数量
     */
    @Operation(summary = "获取GitHub编程语言列表")
    @GetMapping("/languages")
    public AjaxResult getLanguages()
    {
        // 查询所有不为空的语言
        QueryWrapper queryWrapper = QueryWrapper.create()
            .isNotNull("language")
            .ne("language", "");
        
        List<GithubTrending> allRepos = githubTrendingService.list(queryWrapper);
        
        // 按语言分组统计
        Map<String, Long> languageCount = allRepos.stream()
            .map(GithubTrending::getLanguage)
            .filter(lang -> lang != null && !lang.isEmpty())
            .collect(Collectors.groupingBy(
                lang -> lang,
                Collectors.counting()
            ));
        
        // 转换为前端需要的格式
        List<Map<String, Object>> languages = new ArrayList<>();
        languageCount.forEach((lang, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("language", lang);
            item.put("repoCount", count);
            languages.add(item);
        });
        
        // 按仓库数量降序排序
        languages.sort((a, b) -> Long.compare(
            (Long) b.get("repoCount"),
            (Long) a.get("repoCount")
        ));
        
        return success(languages);
    }

    /**
     * 获取编程语言分类统计
     * 
     * @return 各编程语言的统计数据
     */
    @Operation(summary = "获取编程语言分类统计")
    @GetMapping("/totalByLanguage")
    public AjaxResult getTotalByLanguage()
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .isNotNull("language")
            .ne("language", "");
        
        List<GithubTrending> allRepos = githubTrendingService.list(queryWrapper);
        
        // 按语言分组统计
        Map<String, Map<String, Object>> languageStats = new LinkedHashMap<>();
        
        allRepos.forEach(repo -> {
            String language = repo.getLanguage();
            if (language != null && !language.isEmpty()) {
                languageStats.computeIfAbsent(language, k -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("language", language);
                    stats.put("repoCount", 0);
                    stats.put("avgStars", 0L);
                    stats.put("totalStars", 0L);
                    stats.put("maxStars", 0L);
                    return stats;
                });
                
                Map<String, Object> stats = languageStats.get(language);
                stats.put("repoCount", (Integer) stats.get("repoCount") + 1);
                
                long starsCount = 0;
                try {
                    starsCount = Long.parseLong(repo.getStarsCount());
                } catch (NumberFormatException e) {
                    starsCount = 0;
                }
                
                long totalStars = (Long) stats.get("totalStars") + starsCount;
                stats.put("totalStars", totalStars);
                
                long maxStars = (Long) stats.get("maxStars");
                if (starsCount > maxStars) {
                    stats.put("maxStars", starsCount);
                }
            }
        });
        
        // 计算平均stars
        languageStats.values().forEach(stats -> {
            long totalStars = (Long) stats.get("totalStars");
            int repoCount = (Integer) stats.get("repoCount");
            long avgStars = repoCount > 0 ? totalStars / repoCount : 0;
            stats.put("avgStars", avgStars);
        });
        
        return success(new ArrayList<>(languageStats.values()));
    }

    /**
     * 获取上榜趋势统计（最近30天）
     * 
     * @return 按日期统计的上榜数据
     */
    @Operation(summary = "获取上榜趋势统计")
    @GetMapping("/trendingTotal")
    public AjaxResult getTrendingTotal()
    {
        // 获取最近30天的日期范围
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        
        List<GithubTrending> allTrending = githubTrendingService.list();
        
        // 按日期分组统计，只统计最近30天的数据
        Map<String, Map<String, Object>> trendingStats = new LinkedHashMap<>();
        
        allTrending.forEach(trending -> {
            if (trending.getFirstTrendingDate() == null) {
                return;
            }
            
            // 将 java.util.Date 转换为 LocalDate
            java.time.Instant instant = trending.getFirstTrendingDate().toInstant();
            java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
            LocalDate trendingDate = instant.atZone(zoneId).toLocalDate();
            
            // 只统计最近30天的数据
            if (trendingDate.isBefore(thirtyDaysAgo)) {
                return;
            }
            
            String date = formatDate(trending.getFirstTrendingDate());
            
            trendingStats.computeIfAbsent(date, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("trendingDate", date);
                stats.put("newRepoCount", 0);
                stats.put("totalStars", 0L);
                stats.put("avgStars", 0L);
                return stats;
            });
            
            Map<String, Object> stats = trendingStats.get(date);
            stats.put("newRepoCount", (Integer) stats.get("newRepoCount") + 1);
            
            long starsCount = 0;
            try {
                starsCount = Long.parseLong(trending.getStarsCount());
            } catch (NumberFormatException e) {
                starsCount = 0;
            }
            
            long totalStars = (Long) stats.get("totalStars") + starsCount;
            stats.put("totalStars", totalStars);
        });
        
        // 计算平均stars
        trendingStats.values().forEach(stats -> {
            long totalStars = (Long) stats.get("totalStars");
            int repoCount = (Integer) stats.get("newRepoCount");
            long avgStars = repoCount > 0 ? totalStars / repoCount : 0;
            stats.put("avgStars", avgStars);
        });
        
        return success(new ArrayList<>(trendingStats.values()));
    }

    /**
     * 获取仓库活跃度分析
     * 
     * @return 按热度等级统计的仓库数据
     */
    @Operation(summary = "获取仓库活跃度分析")
    @GetMapping("/githubHy")
    public AjaxResult getGithubActivity()
    {
        List<GithubTrending> allRepos = githubTrendingService.list();
        
        // 按仓库价值分组统计
        Map<String, Map<String, Object>> activityStats = new LinkedHashMap<>();
        
        allRepos.forEach(repo -> {
            String repValue = repo.getRepValue() != null ? 
                repo.getRepValue() : "普通";
            
            activityStats.computeIfAbsent(repValue, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("popularityLevel", repValue);
                stats.put("repoCount", 0);
                stats.put("totalTrendingDays", 0);
                stats.put("totalContinuousDays", 0);
                stats.put("avgTrendingDays", 0.0);
                stats.put("avgContinuousDays", 0.0);
                return stats;
            });
            
            Map<String, Object> stats = activityStats.get(repValue);
            stats.put("repoCount", (Integer) stats.get("repoCount") + 1);
            
            int trendingDays = 0;
            int continuousDays = 0;
            
            try {
                trendingDays = repo.getTrendingDays() != null ? Integer.parseInt(repo.getTrendingDays()) : 0;
            } catch (NumberFormatException e) {
                trendingDays = 0;
            }
            
            try {
                continuousDays = repo.getContinuousTrendingDays() != null ? Integer.parseInt(repo.getContinuousTrendingDays()) : 0;
            } catch (NumberFormatException e) {
                continuousDays = 0;
            }
            
            stats.put("totalTrendingDays", (Integer) stats.get("totalTrendingDays") + trendingDays);
            stats.put("totalContinuousDays", (Integer) stats.get("totalContinuousDays") + continuousDays);
        });
        
        // 计算平均值
        activityStats.values().forEach(stats -> {
            int repoCount = (Integer) stats.get("repoCount");
            int totalTrendingDays = (Integer) stats.get("totalTrendingDays");
            int totalContinuousDays = (Integer) stats.get("totalContinuousDays");
            
            double avgTrendingDays = repoCount > 0 ? (double) totalTrendingDays / repoCount : 0.0;
            double avgContinuousDays = repoCount > 0 ? (double) totalContinuousDays / repoCount : 0.0;
            
            stats.put("avgTrendingDays", avgTrendingDays);
            stats.put("avgContinuousDays", avgContinuousDays);
        });
        
        return success(new ArrayList<>(activityStats.values()));
    }

    /**
     * 获取语言热度变化趋势
     * 
     * @return 按日期和语言统计的趋势数据
     */
    @Operation(summary = "获取语言热度变化趋势")
    @GetMapping("/languageTotal")
    public AjaxResult getLanguageTotal()
    {
        List<GithubTrending> allRepos = githubTrendingService.list();
        
        // 按日期和语言分组统计
        List<Map<String, Object>> languageTrends = new ArrayList<>();
        Map<String, Map<String, Object>> trendMap = new LinkedHashMap<>();
        
        allRepos.forEach(repo -> {
            String language = repo.getLanguage() != null ? repo.getLanguage() : "Unknown";
            String date = formatDate(repo.getFirstTrendingDate());
            
            String key = date + "_" + language;
            
            trendMap.computeIfAbsent(key, k -> {
                Map<String, Object> trend = new HashMap<>();
                trend.put("trendingDate", date);
                trend.put("language", language);
                trend.put("repoCount", 0);
                trend.put("totalStars", 0L);
                return trend;
            });
            
            Map<String, Object> trend = trendMap.get(key);
            trend.put("repoCount", (Integer) trend.get("repoCount") + 1);
            
            long starsCount = 0;
            try {
                starsCount = Long.parseLong(repo.getStarsCount());
            } catch (NumberFormatException e) {
                starsCount = 0;
            }
            
            long totalStars = (Long) trend.get("totalStars") + starsCount;
            trend.put("totalStars", totalStars);
        });
        
        languageTrends.addAll(trendMap.values());
        
        return success(languageTrends);
    }

    /**
     * 格式化日期为 yyyy-MM-dd 格式
     * 
     * @param date 日期对象
     * @return 格式化后的日期字符串
     */
    private String formatDate(Date date)
    {
        if (date == null) {
            return LocalDate.now().toString();
        }
        
        // 将 java.util.Date 转换为 LocalDate
        java.time.Instant instant = date.toInstant();
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        LocalDate localDate = instant.atZone(zoneId).toLocalDate();
        
        return localDate.toString();
    }
}
