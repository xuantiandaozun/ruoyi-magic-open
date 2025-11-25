package com.ruoyi.project.ai.tool.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;

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
        
        // 使用 Hutool 从OSS地址读取文件内容
        String content = HttpUtil.get(ossUrl);
        
        if (StrUtil.isBlank(content)) {
            return ToolExecutionResult.empty("query", "文件内容为空或读取失败");
        }
        
        return ToolExecutionResult.querySuccess(content, String.format("成功读取文件内容（共%d字符）", content.length()));
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