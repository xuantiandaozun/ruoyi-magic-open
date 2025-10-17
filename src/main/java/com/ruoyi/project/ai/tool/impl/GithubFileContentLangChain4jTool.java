package com.ruoyi.project.ai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * GitHub文件内容获取工具
 * 
 * @author ruoyi
 */
@Component
public class GithubFileContentLangChain4jTool {

    @Value("${github.token:}")
    private String githubToken;
    
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取GitHub仓库中指定文件的内容
     * 
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param path 文件路径（相对于仓库根目录）
     * @param branch 分支名称，默认为main
     * @return 文件内容
     */
    @Tool("获取GitHub仓库中指定文件的内容。参数：owner(仓库所有者), repo(仓库名称), path(文件路径), branch(分支名称，可选，默认main)")
    public String getGithubFileContent(String owner, String repo, String path, String branch) {
        try {
            // 参数验证
            if (owner == null || owner.trim().isEmpty()) {
                return "错误：仓库所有者不能为空";
            }
            if (repo == null || repo.trim().isEmpty()) {
                return "错误：仓库名称不能为空";
            }
            if (path == null || path.trim().isEmpty()) {
                return "错误：文件路径不能为空";
            }
            
            // 默认分支
            if (branch == null || branch.trim().isEmpty()) {
                branch = "main";
            }

            // 构建API URL
            String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s", 
                GITHUB_API_BASE, owner.trim(), repo.trim(), path.trim(), branch.trim());

            // 创建HTTP客户端
            HttpClient client = HttpClient.newHttpClient();
            
            // 构建请求
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "RuoyiMagic/1.0")
                .GET();
            
            // 如果配置了GitHub Token，则添加认证头
            if (githubToken != null && !githubToken.trim().isEmpty()) {
                requestBuilder.header("Authorization", "token " + githubToken);
            }
            
            HttpRequest request = requestBuilder.build();

            // 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 解析响应
                JsonNode jsonNode = objectMapper.readTree(response.body());
                
                // 检查是否是文件
                String type = jsonNode.get("type").asText();
                if (!"file".equals(type)) {
                    return "错误：指定路径不是文件，而是：" + type;
                }
                
                // 获取文件内容（Base64编码）
                String content = jsonNode.get("content").asText();
                String encoding = jsonNode.get("encoding").asText();
                
                if ("base64".equals(encoding)) {
                    // 解码Base64内容
                    byte[] decodedBytes = Base64.getDecoder().decode(content.replaceAll("\\s", ""));
                    String decodedContent = new String(decodedBytes, StandardCharsets.UTF_8);
                    
                    // 获取文件信息
                    String fileName = jsonNode.get("name").asText();
                    int size = jsonNode.get("size").asInt();
                    
                    return String.format("文件：%s\n大小：%d 字节\n内容：\n%s", fileName, size, decodedContent);
                } else {
                    return "错误：不支持的编码格式：" + encoding;
                }
                
            } else if (response.statusCode() == 404) {
                return "错误：文件未找到，请检查仓库所有者、仓库名称、文件路径和分支名称是否正确";
            } else if (response.statusCode() == 403) {
                return "错误：访问被拒绝，可能是私有仓库或token权限不足";
            } else {
                return "错误：GitHub API请求失败，状态码：" + response.statusCode() + "，响应：" + response.body();
            }

        } catch (IOException e) {
            return "错误：网络请求失败 - " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "错误：请求被中断 - " + e.getMessage();
        } catch (Exception e) {
            return "错误：获取文件内容失败 - " + e.getMessage();
        }
    }

    /**
     * 获取GitHub仓库中指定文件的内容（使用默认分支）
     * 
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param path 文件路径（相对于仓库根目录）
     * @return 文件内容
     */
    @Tool("获取GitHub仓库中指定文件的内容（使用默认分支main）。参数：owner(仓库所有者), repo(仓库名称), path(文件路径)")
    public String getGithubFileContent(String owner, String repo, String path) {
        return getGithubFileContent(owner, repo, path, "main");
    }
}