package com.ruoyi.project.ai.tool.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.project.ai.tool.LangChain4jTool;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * GitHub仓库目录树获取工具
 * 
 * @author ruoyi
 * @date 2024-12-15
 */
@Component
public class GithubRepoTreeLangChain4jTool implements LangChain4jTool {
    
    @Value("${github.token:}")
    private String githubToken;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getToolName() {
        return "github_repo_tree";
    }
    
    @Override
    public String getToolDescription() {
        return "获取GitHub仓库的文件目录结构，支持指定分支和路径";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("owner", "仓库所有者（用户名或组织名）")
            .addStringProperty("repo", "仓库名称")
            .addStringProperty("branch", "分支名称，默认为main")
            .addStringProperty("path", "指定路径，默认为根目录")
            .addBooleanProperty("recursive", "是否递归获取子目录，默认为false")
            .required("owner", "repo")
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
            String owner = (String) parameters.get("owner");
            String repo = (String) parameters.get("repo");
            String branch = parameters.get("branch") != null ? 
                (String) parameters.get("branch") : "main";
            String path = parameters.get("path") != null ? 
                (String) parameters.get("path") : "";
            Boolean recursive = parameters.get("recursive") != null ? 
                Boolean.parseBoolean(parameters.get("recursive").toString()) : false;
            
            if (StrUtil.isBlank(owner) || StrUtil.isBlank(repo)) {
                return "错误：仓库所有者和仓库名称不能为空";
            }
            
            // 构建GitHub API URL
            String apiUrl;
            if (recursive) {
                // 使用Git Trees API获取递归目录结构
                apiUrl = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1", 
                    owner, repo, branch);
            } else {
                // 使用Contents API获取指定路径的内容
                if (StrUtil.isNotBlank(path)) {
                    apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s", 
                        owner, repo, path, branch);
                } else {
                    apiUrl = String.format("https://api.github.com/repos/%s/%s/contents?ref=%s", 
                        owner, repo, branch);
                }
            }
            
            // 调用GitHub API
            String response = callGithubApi(apiUrl);
            
            // 解析并格式化响应
            String formattedResult = formatTreeResponse(response, recursive, owner, repo, branch, path);
            
            return formattedResult;
            
        } catch (Exception e) {
            return "获取GitHub仓库目录时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 调用GitHub API
     */
    private String callGithubApi(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求方法和超时
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(30000);    // 30秒读取超时
            
            // 设置请求头
            if (githubToken != null && !githubToken.trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "token " + githubToken);
            }
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "ruoyi-magic-ai-tool");
            
            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // 读取错误信息
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                throw new IOException("GitHub API请求失败，响应码: " + responseCode + ", 错误信息: " + errorResponse.toString());
            }
            
            // 读取响应内容
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            return response.toString();
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * 格式化目录树响应
     */
    private String formatTreeResponse(String response, boolean recursive, String owner, String repo, String branch, String path) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            StringBuilder result = new StringBuilder();
            
            result.append(String.format("GitHub仓库目录结构：%s/%s", owner, repo));
            if (StrUtil.isNotBlank(branch)) {
                result.append(" (分支: ").append(branch).append(")");
            }
            if (StrUtil.isNotBlank(path)) {
                result.append(" (路径: ").append(path).append(")");
            }
            result.append("\n\n");
            
            if (recursive && rootNode.has("tree")) {
                // 递归模式 - Git Trees API响应
                JsonNode treeNode = rootNode.get("tree");
                result.append("文件和目录列表：\n");
                
                for (JsonNode item : treeNode) {
                    String itemPath = item.get("path").asText();
                    String type = item.get("type").asText();
                    String mode = item.get("mode").asText();
                    
                    String icon = "file".equals(type) ? "📄" : "📁";
                    result.append(String.format("%s %s (%s, mode: %s)\n", icon, itemPath, type, mode));
                }
            } else if (rootNode.isArray()) {
                // 非递归模式 - Contents API响应（数组）
                result.append("当前目录内容：\n");
                
                for (JsonNode item : rootNode) {
                    String name = item.get("name").asText();
                    String type = item.get("type").asText();
                    long size = item.has("size") ? item.get("size").asLong() : 0;
                    
                    String icon = "file".equals(type) ? "📄" : "📁";
                    String sizeInfo = "file".equals(type) ? String.format(" (%d bytes)", size) : "";
                    result.append(String.format("%s %s%s\n", icon, name, sizeInfo));
                }
            } else if (rootNode.has("name")) {
                // 单个文件 - Contents API响应（对象）
                String name = rootNode.get("name").asText();
                String type = rootNode.get("type").asText();
                long size = rootNode.has("size") ? rootNode.get("size").asLong() : 0;
                
                result.append(String.format("文件信息：\n📄 %s (%s, %d bytes)\n", name, type, size));
            } else {
                result.append("未找到文件或目录信息");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "解析GitHub API响应时发生错误: " + e.getMessage() + "\n原始响应: " + response;
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 验证必需参数
        if (!parameters.containsKey("owner") || !parameters.containsKey("repo")) {
            return false;
        }
        
        String owner = parameters.get("owner").toString();
        String repo = parameters.get("repo").toString();
        
        if (StrUtil.isBlank(owner) || StrUtil.isBlank(repo)) {
            return false;
        }
        
        // 验证仓库名格式（简单验证）
        if (owner.contains("/") || repo.contains("/")) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 获取仓库根目录内容：
           {"owner": "microsoft", "repo": "vscode"}
        
        2. 获取指定分支的目录内容：
           {"owner": "facebook", "repo": "react", "branch": "main"}
        
        3. 获取指定路径的内容：
           {"owner": "vuejs", "repo": "vue", "path": "src/core", "branch": "main"}
        
        4. 递归获取整个仓库的文件树：
           {"owner": "torvalds", "repo": "linux", "recursive": true}
        
        5. 递归获取指定路径下的所有文件：
           {"owner": "nodejs", "repo": "node", "path": "lib", "recursive": true, "branch": "main"}
        
        注意：
        - owner和repo是必需参数
        - branch默认为main，也可以指定其他分支或commit SHA
        - path为空时获取根目录内容
        - recursive=true时会获取所有子目录的完整结构
        - 使用GitHub API，有速率限制
        """;
    }
}