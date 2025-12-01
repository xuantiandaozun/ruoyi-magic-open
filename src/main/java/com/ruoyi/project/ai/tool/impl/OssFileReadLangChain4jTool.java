package com.ruoyi.project.ai.tool.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的OSS文件读取工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class OssFileReadLangChain4jTool implements LangChain4jTool {
    
    private static final Logger log = LoggerFactory.getLogger(OssFileReadLangChain4jTool.class);
    
    // 超时配置
    private static final int CONNECTION_TIMEOUT = 10000; // 连接超时10秒
    private static final int READ_TIMEOUT = 30000; // 读取超时30秒
    private static final int MAX_RETRIES = 2; // 最大重试次数
    
    @Override
    public String getToolName() {
        return "oss_file_read";
    }
    
    @Override
    public String getToolDescription() {
        return "通过OSS URL读取文件内容，支持文本文件的读取";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("ossUrl", "OSS文件的完整URL地址")
            .required("ossUrl")
            .build();
        
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(parametersSchema)
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        String ossUrl = (String) parameters.get("ossUrl");
        
        if (StrUtil.isBlank(ossUrl)) {
            return ToolExecutionResult.empty("query", "OSS URL为空");
        }
        
        log.info("[OssFileReadTool] 开始下载并读取文件: url={}", ossUrl);
        
        File tempFile = null;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // 创建临时文件
                String fileName = "oss_temp_" + System.currentTimeMillis() + ".txt";
                tempFile = FileUtil.createTempFile(fileName, true);
                
                log.debug("[OssFileReadTool] 开始下载文件到临时目录: {}", tempFile.getAbsolutePath());
                
                // 使用 Hutool 下载文件到临时目录，设置超时时间
                HttpUtil.downloadFile(ossUrl, tempFile, READ_TIMEOUT);
                
                // 检查文件是否下载成功
                if (!tempFile.exists() || tempFile.length() == 0) {
                    log.warn("[OssFileReadTool] 文件下载失败或为空: url={}, attempt={}/{}", 
                        ossUrl, attempt, MAX_RETRIES);
                    lastException = new Exception("下载的文件为空");
                    continue;
                }
                
                log.debug("[OssFileReadTool] 文件下载成功，大小: {} bytes", tempFile.length());
                
                // 读取文件内容（使用UTF-8编码）
                String content = FileUtil.readString(tempFile, StandardCharsets.UTF_8);
                
                // 成功后删除临时文件
                FileUtil.del(tempFile);
                
                if (StrUtil.isBlank(content)) {
                    return ToolExecutionResult.empty("query", "文件内容为空");
                }
                
                log.info("[OssFileReadTool] 读取成功: url={}, size={}字符", ossUrl, content.length());
                return ToolExecutionResult.querySuccess(content, 
                    String.format("成功读取文件内容（共%d字符）", content.length()));
                
            } catch (Exception e) {
                lastException = e;
                log.warn("[OssFileReadTool] 下载或读取文件失败: url={}, attempt={}/{}, error={}", 
                    ossUrl, attempt, MAX_RETRIES, e.getMessage());
                
                // 清理临时文件
                if (tempFile != null && tempFile.exists()) {
                    try {
                        FileUtil.del(tempFile);
                    } catch (Exception cleanupEx) {
                        log.debug("[OssFileReadTool] 清理临时文件失败: {}", cleanupEx.getMessage());
                    }
                }
                
                // 如果还有重试机会，等待后重试
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000 * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 所有重试都失败，确保清理临时文件
        if (tempFile != null && tempFile.exists()) {
            try {
                FileUtil.del(tempFile);
            } catch (Exception cleanupEx) {
                log.debug("[OssFileReadTool] 最终清理临时文件失败: {}", cleanupEx.getMessage());
            }
        }
        
        String errorMsg = String.format("下载并读取文件失败（重试%d次后仍失败）: %s", 
            MAX_RETRIES, lastException != null ? lastException.getMessage() : "未知错误");
        log.error("[OssFileReadTool] {}", errorMsg);
        return ToolExecutionResult.failure("operation", errorMsg);
    }
    

    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 验证ossUrl参数
        if (!parameters.containsKey("ossUrl")) {
            return false;
        }
        
        String ossUrl = parameters.get("ossUrl").toString();
        if (StrUtil.isBlank(ossUrl)) {
            return false;
        }
        
        // 简单验证URL格式
        if (!ossUrl.startsWith("http://") && !ossUrl.startsWith("https://")) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 读取OSS文件内容：
           {"ossUrl": "https://your-oss-bucket.oss-cn-hangzhou.aliyuncs.com/readme/project-readme.md"}
        
        2. 读取GitHub项目的README文件：
           {"ossUrl": "https://your-oss-bucket.oss-cn-hangzhou.aliyuncs.com/github/owner/repo/README.md"}
        
        3. 读取任意可访问的文本文件：
           {"ossUrl": "https://example.com/docs/api-documentation.txt"}
        
        注意：
        - OSS URL必须是可公开访问的
        - 支持的文件类型：文本文件（.txt, .md, .json, .xml等）
        - 会读取完整的文件内容，无字符数限制
        """;
    }
}