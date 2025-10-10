package com.ruoyi.project.ai.tool;

import com.volcengine.ark.runtime.model.completion.chat.ChatFunction;
import com.volcengine.ark.runtime.model.completion.chat.ChatTool;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.StrUtil;

import java.util.*;

/**
 * GitHub趋势数据查询工具
 * 基于豆包SDK的ChatTool和ChatFunction实现
 */
public class GitHubTrendingTool {

    /**
     * 获取GitHub趋势仓库的工具定义
     */
    public static ChatTool getGitHubTrendingTool() {
        return new ChatTool(
            "function",
            new ChatFunction.Builder()
                .name("query_github_trending")
                .description("查询GitHub趋势仓库，可以按编程语言、时间范围等条件筛选")
                .parameters(new GitHubTrendingParameters(
                    "object",
                    new HashMap<String, Object>() {{
                        put("language", new HashMap<String, String>() {{
                            put("type", "string");
                            put("description", "编程语言，如java、python、javascript等，不填则查询所有语言");
                        }});
                        put("since", new HashMap<String, String>() {{
                            put("type", "string");
                            put("description", "时间范围：daily(今日)、weekly(本周)、monthly(本月)，默认为daily");
                        }});
                        put("limit", new HashMap<String, String>() {{
                            put("type", "integer");
                            put("description", "返回结果数量限制，默认为10，最大100");
                        }});
                    }},
                    Arrays.asList() // 所有参数都是可选的
                ))
                .build()
        );
    }

    /**
     * 搜索GitHub仓库的工具定义
     */
    public static ChatTool getGitHubSearchTool() {
        return new ChatTool(
            "function",
            new ChatFunction.Builder()
                .name("search_github_repositories")
                .description("根据关键词搜索GitHub仓库")
                .parameters(new GitHubSearchParameters(
                    "object",
                    new HashMap<String, Object>() {{
                        put("keyword", new HashMap<String, String>() {{
                            put("type", "string");
                            put("description", "搜索关键词");
                        }});
                        put("language", new HashMap<String, String>() {{
                            put("type", "string");
                            put("description", "编程语言筛选，可选");
                        }});
                        put("sort", new HashMap<String, String>() {{
                            put("type", "string");
                            put("description", "排序方式：stars(按星数)、forks(按fork数)、updated(按更新时间)，默认为stars");
                        }});
                        put("limit", new HashMap<String, String>() {{
                            put("type", "integer");
                            put("description", "返回结果数量限制，默认为10，最大100");
                        }});
                    }},
                    Arrays.asList("keyword") // keyword是必需参数
                ))
                .build()
        );
    }

    /**
     * 执行GitHub趋势查询
     */
    public static String executeGitHubTrending(Map<String, Object> parameters) {
        try {
            String language = (String) parameters.get("language");
            String since = (String) parameters.getOrDefault("since", "daily");
            Integer limit = parameters.get("limit") != null ? 
                Integer.parseInt(parameters.get("limit").toString()) : 10;
            
            // 限制最大数量
            if (limit > 100) limit = 100;
            if (limit < 1) limit = 10;

            // 构建GitHub API URL
            String url = "https://api.github.com/search/repositories";
            String query = "stars:>1";
            
            // 添加语言筛选
            if (StrUtil.isNotBlank(language)) {
                query += " language:" + language;
            }
            
            // 添加时间筛选
            String dateFilter = "";
            switch (since.toLowerCase()) {
                case "daily":
                    dateFilter = "created:>" + java.time.LocalDate.now().minusDays(1);
                    break;
                case "weekly":
                    dateFilter = "created:>" + java.time.LocalDate.now().minusWeeks(1);
                    break;
                case "monthly":
                    dateFilter = "created:>" + java.time.LocalDate.now().minusMonths(1);
                    break;
            }
            
            if (StrUtil.isNotBlank(dateFilter)) {
                query += " " + dateFilter;
            }

            // 发送请求
            String response = HttpUtil.get(url + "?q=" + query + "&sort=stars&order=desc&per_page=" + limit);
            JSONObject jsonResponse = JSONUtil.parseObj(response);
            JSONArray items = jsonResponse.getJSONArray("items");

            // 格式化返回结果
            StringBuilder result = new StringBuilder();
            result.append("GitHub趋势仓库查询结果：\n\n");
            
            for (int i = 0; i < Math.min(items.size(), limit); i++) {
                JSONObject repo = items.getJSONObject(i);
                result.append(String.format("%d. %s\n", i + 1, repo.getStr("full_name")));
                result.append(String.format("   描述: %s\n", repo.getStr("description", "无描述")));
                result.append(String.format("   语言: %s\n", repo.getStr("language", "未知")));
                result.append(String.format("   星数: %d\n", repo.getInt("stargazers_count", 0)));
                result.append(String.format("   Fork数: %d\n", repo.getInt("forks_count", 0)));
                result.append(String.format("   链接: %s\n\n", repo.getStr("html_url")));
            }

            return result.toString();
            
        } catch (Exception e) {
            return "查询GitHub趋势数据时发生错误: " + e.getMessage();
        }
    }

    /**
     * 执行GitHub仓库搜索
     */
    public static String executeGitHubSearch(Map<String, Object> parameters) {
        try {
            String keyword = (String) parameters.get("keyword");
            String language = (String) parameters.get("language");
            String sort = (String) parameters.getOrDefault("sort", "stars");
            Integer limit = parameters.get("limit") != null ? 
                Integer.parseInt(parameters.get("limit").toString()) : 10;
            
            if (StrUtil.isBlank(keyword)) {
                return "搜索关键词不能为空";
            }
            
            // 限制最大数量
            if (limit > 100) limit = 100;
            if (limit < 1) limit = 10;

            // 构建搜索查询
            String query = keyword;
            if (StrUtil.isNotBlank(language)) {
                query += " language:" + language;
            }

            // 构建GitHub API URL
            String url = "https://api.github.com/search/repositories";
            String response = HttpUtil.get(url + "?q=" + query + "&sort=" + sort + "&order=desc&per_page=" + limit);
            JSONObject jsonResponse = JSONUtil.parseObj(response);
            JSONArray items = jsonResponse.getJSONArray("items");

            // 格式化返回结果
            StringBuilder result = new StringBuilder();
            result.append(String.format("GitHub仓库搜索结果 (关键词: %s)：\n\n", keyword));
            
            for (int i = 0; i < Math.min(items.size(), limit); i++) {
                JSONObject repo = items.getJSONObject(i);
                result.append(String.format("%d. %s\n", i + 1, repo.getStr("full_name")));
                result.append(String.format("   描述: %s\n", repo.getStr("description", "无描述")));
                result.append(String.format("   语言: %s\n", repo.getStr("language", "未知")));
                result.append(String.format("   星数: %d\n", repo.getInt("stargazers_count", 0)));
                result.append(String.format("   Fork数: %d\n", repo.getInt("forks_count", 0)));
                result.append(String.format("   链接: %s\n\n", repo.getStr("html_url")));
            }

            return result.toString();
            
        } catch (Exception e) {
            return "搜索GitHub仓库时发生错误: " + e.getMessage();
        }
    }

    /**
     * GitHub趋势查询参数类
     */
    public static class GitHubTrendingParameters {
        public String type;
        public Map<String, Object> properties;
        public List<String> required;

        public GitHubTrendingParameters(String type, Map<String, Object> properties, List<String> required) {
            this.type = type;
            this.properties = properties;
            this.required = required;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
        public List<String> getRequired() { return required; }
        public void setRequired(List<String> required) { this.required = required; }
    }

    /**
     * GitHub搜索参数类
     */
    public static class GitHubSearchParameters {
        public String type;
        public Map<String, Object> properties;
        public List<String> required;

        public GitHubSearchParameters(String type, Map<String, Object> properties, List<String> required) {
            this.type = type;
            this.properties = properties;
            this.required = required;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
        public List<String> getRequired() { return required; }
        public void setRequired(List<String> required) { this.required = required; }
    }
}