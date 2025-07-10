package com.ruoyi.project.gen.tools.ai;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;

/**
 * 文件操作工具
 * 提供文件读取和内容处理的相关操作
 */
@Service
public class FileOperationTool {
    private static final Logger logger = LoggerFactory.getLogger(FileOperationTool.class);

    /**
     * 读取OSS上的MD文件内容并进行适量压缩，确保AI能够正确理解
     */
    @Tool(name = "readMarkdownFromOss", description = "读取OSS上的MD文件内容并进行适量压缩，确保AI能够正确理解")
    public Map<String, Object> readMarkdownFromOss(String ossUrl) {
        try {
            logger.info("readMarkdownFromOss读取OSS文件: {}", ossUrl);
            
            String content;
            String fileName = "";
            
            if (ossUrl.startsWith("http://") || ossUrl.startsWith("https://")) {
                // 从URL下载内容
                content = HttpUtil.get(ossUrl, StandardCharsets.UTF_8);
                // 从URL中提取文件名
                String[] urlParts = ossUrl.split("/");
                if (urlParts.length > 0) {
                    fileName = urlParts[urlParts.length - 1];
                }
            } else {
                // 本地文件路径
                File file = new File(ossUrl);
                if (!file.exists()) {
                    throw new ServiceException("文件不存在: " + ossUrl);
                }
                content = FileUtil.readString(file, StandardCharsets.UTF_8);
                fileName = file.getName();
            }
            
            if (StrUtil.isBlank(content)) {
                throw new ServiceException("文件内容为空");
            }
            
            int originalLength = content.length();
            String compressedContent = compressMarkdownContent(content);
            int compressedLength = compressedContent.length();
            
            Map<String, Object> result = new HashMap<>();
            result.put("ossUrl", ossUrl);
            result.put("fileName", fileName);
            result.put("success", true);
            result.put("originalLength", originalLength);
            result.put("compressedLength", compressedLength);
            result.put("content", content);
            result.put("compressedContent", compressedContent);
            result.put("message", String.format("文件读取成功，原始长度: %d，压缩后长度: %d", originalLength, compressedLength));
            
            return result;
        } catch (Exception e) {
            logger.error("读取OSS文件失败", e);
            throw new ServiceException("读取OSS文件失败：" + e.getMessage());
        }
    }

    /**
     * 压缩Markdown内容，保留重要信息
     */
    private String compressMarkdownContent(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        
        StringBuilder compressed = new StringBuilder();
        String[] lines = content.split("\n");
        
        boolean inCodeBlock = false;
        String codeBlockType = "";
        StringBuilder codeContent = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 检查代码块开始
            if (trimmedLine.startsWith("```")) {
                if (!inCodeBlock) {
                    // 代码块开始
                    inCodeBlock = true;
                    codeBlockType = trimmedLine.substring(3).trim();
                    codeContent = new StringBuilder();
                } else {
                    // 代码块结束
                    inCodeBlock = false;
                    String compressedCode = compressCodeBlock(codeContent.toString(), codeBlockType);
                    compressed.append("```").append(codeBlockType).append("\n");
                    compressed.append(compressedCode);
                    compressed.append("\n```\n");
                    codeBlockType = "";
                }
                continue;
            }
            
            if (inCodeBlock) {
                // 在代码块内
                codeContent.append(line).append("\n");
            } else {
                // 在代码块外
                if (StrUtil.isBlank(trimmedLine)) {
                    // 跳过空行
                    continue;
                }
                
                // 保留标题、列表、重要内容
                if (trimmedLine.startsWith("#") || 
                    trimmedLine.startsWith("-") || 
                    trimmedLine.startsWith("*") || 
                    trimmedLine.startsWith("1.") || 
                    containsImportantKeywords(trimmedLine)) {
                    compressed.append(line).append("\n");
                } else {
                    // 普通段落，进行适度压缩
                    if (line.length() > 100) {
                        compressed.append(line.substring(0, 100)).append("...\n");
                    } else {
                        compressed.append(line).append("\n");
                    }
                }
            }
        }
        
        return compressed.toString();
    }

    /**
     * 压缩代码块内容
     */
    private String compressCodeBlock(String codeContent, String codeType) {
        if (StrUtil.isBlank(codeContent)) {
            return codeContent;
        }
        
        String[] lines = codeContent.split("\n");
        StringBuilder compressed = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 跳过空行和注释行（简单处理）
            if (StrUtil.isBlank(trimmedLine) || 
                trimmedLine.startsWith("//") || 
                trimmedLine.startsWith("#") || 
                trimmedLine.startsWith("/*") || 
                trimmedLine.startsWith("*")) {
                continue;
            }
            
            // 保留重要的代码行
            compressed.append(line).append("\n");
        }
        
        return compressed.toString();
    }

    /**
     * 检查是否包含重要关键词
     */
    private boolean containsImportantKeywords(String line) {
        String lowerLine = line.toLowerCase();
        String[] keywords = {
            "重要", "注意", "警告", "错误", "异常", "配置", "安装", "部署",
            "important", "note", "warning", "error", "exception", "config", "install", "deploy",
            "api", "接口", "方法", "函数", "参数", "返回", "示例", "example"
        };
        
        for (String keyword : keywords) {
            if (lowerLine.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}