package com.ruoyi.project.ai.tool.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.tool.LangChain4jTool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的阿里云通义千问AI生图工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class AliImageGenerationLangChain4jTool implements LangChain4jTool {
    
    @Autowired
    private IAiModelConfigService aiModelConfigService;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ALI_IMAGE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";
    
    @Override
    public String getToolName() {
        return "ali_image_generation";
    }
    
    @Override
    public String getToolDescription() {
        return "使用阿里云通义千问生成图片，根据文本提示词创建图像";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        // 创建AI生图工具规范
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("prompt", "图片生成的提示词，描述要生成的图片内容，必填")
            .addIntegerProperty("configId", "AI模型配置ID，用于获取API密钥和配置信息，必填")
            .addStringProperty("model", "使用的模型名称，默认为qwen-image-plus，可选")
            .addStringProperty("size", "图片尺寸，格式为宽*高，如1328*1328，默认为1024*1024，可选")
            .addIntegerProperty("n", "生成图片数量，默认为1，最大为4，可选")
            .addBooleanProperty("promptExtend", "是否启用提示词扩展，默认为true，可选")
            .addBooleanProperty("watermark", "是否添加水印，默认为true，可选")
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
            // 获取必填参数
            String prompt = (String) parameters.get("prompt");
            Integer configId = parameters.get("configId") != null ? 
                Integer.parseInt(parameters.get("configId").toString()) : null;
            
            if (StrUtil.isBlank(prompt)) {
                return "错误：提示词不能为空";
            }
            
            if (configId == null) {
                return "错误：配置ID不能为空";
            }
            
            // 获取AI模型配置
            AiModelConfig config = aiModelConfigService.getById(configId.longValue());
            if (config == null) {
                return "错误：找不到指定的AI模型配置，配置ID: " + configId;
            }
            
            if (!"Y".equals(config.getEnabled())) {
                return "错误：指定的AI模型配置已禁用，配置ID: " + configId;
            }
            
            if (StrUtil.isBlank(config.getApiKey())) {
                return "错误：AI模型配置中缺少API密钥，配置ID: " + configId;
            }
            
            // 获取可选参数
            String model = (String) parameters.getOrDefault("model", "qwen-image-plus");
            String size = (String) parameters.getOrDefault("size", "1024*1024");
            Integer n = parameters.get("n") != null ? 
                Integer.parseInt(parameters.get("n").toString()) : 1;
            Boolean promptExtend = parameters.get("promptExtend") != null ? 
                Boolean.parseBoolean(parameters.get("promptExtend").toString()) : true;
            Boolean watermark = parameters.get("watermark") != null ? 
                Boolean.parseBoolean(parameters.get("watermark").toString()) : true;
            
            // 限制生成数量
            if (n > 4) n = 4;
            if (n < 1) n = 1;
            
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", model);
            
            JSONObject input = new JSONObject();
            input.set("prompt", prompt);
            requestBody.set("input", input);
            
            JSONObject params = new JSONObject();
            params.set("size", size);
            params.set("n", n);
            params.set("prompt_extend", promptExtend);
            params.set("watermark", watermark);
            requestBody.set("parameters", params);
            
            // 发送HTTP请求
            String response = sendImageGenerationRequest(config.getApiKey(), requestBody.toString());
            
            // 解析响应
            return parseImageGenerationResponse(response, prompt, model, size, n);
            
        } catch (Exception e) {
            return "生成图片时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 发送图片生成请求
     */
    private String sendImageGenerationRequest(String apiKey, String requestBody) throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ALI_IMAGE_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("X-DashScope-Async", "enable")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(60))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("API请求失败，状态码: " + response.statusCode() + ", 响应: " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * 解析图片生成响应
     */
    private String parseImageGenerationResponse(String responseBody, String prompt, String model, String size, int n) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // 检查是否有错误
            if (jsonNode.has("code")) {
                String code = jsonNode.get("code").asText();
                String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "未知错误";
                return "图片生成失败，错误代码: " + code + ", 错误信息: " + message;
            }
            
            // 检查是否是异步任务
            if (jsonNode.has("output") && jsonNode.get("output").has("task_id")) {
                String taskId = jsonNode.get("output").get("task_id").asText();
                String taskStatus = jsonNode.get("output").has("task_status") ? 
                    jsonNode.get("output").get("task_status").asText() : "PENDING";
                
                StringBuilder result = new StringBuilder();
                result.append("图片生成任务已提交\n");
                result.append("任务ID: ").append(taskId).append("\n");
                result.append("任务状态: ").append(taskStatus).append("\n");
                result.append("提示词: ").append(prompt).append("\n");
                result.append("模型: ").append(model).append("\n");
                result.append("尺寸: ").append(size).append("\n");
                result.append("数量: ").append(n).append("\n");
                result.append("\n注意：这是异步任务，图片生成需要一些时间。请使用任务ID查询生成结果。");
                
                return result.toString();
            }
            
            // 检查是否有直接的图片结果
            if (jsonNode.has("output") && jsonNode.get("output").has("results")) {
                JsonNode results = jsonNode.get("output").get("results");
                
                StringBuilder result = new StringBuilder();
                result.append("图片生成成功\n");
                result.append("提示词: ").append(prompt).append("\n");
                result.append("模型: ").append(model).append("\n");
                result.append("尺寸: ").append(size).append("\n");
                result.append("生成数量: ").append(results.size()).append("\n\n");
                
                for (int i = 0; i < results.size(); i++) {
                    JsonNode imageResult = results.get(i);
                    if (imageResult.has("url")) {
                        result.append("图片 ").append(i + 1).append(": ").append(imageResult.get("url").asText()).append("\n");
                    }
                }
                
                return result.toString();
            }
            
            return "图片生成响应格式异常: " + responseBody;
            
        } catch (Exception e) {
            return "解析图片生成响应时发生错误: " + e.getMessage() + ", 原始响应: " + responseBody;
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 验证必填参数
        if (!parameters.containsKey("prompt") || StrUtil.isBlank(parameters.get("prompt").toString())) {
            return false;
        }
        
        if (!parameters.containsKey("configId")) {
            return false;
        }
        
        // 验证configId参数
        try {
            int configId = Integer.parseInt(parameters.get("configId").toString());
            if (configId <= 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        // 验证n参数
        if (parameters.containsKey("n")) {
            try {
                int n = Integer.parseInt(parameters.get("n").toString());
                if (n < 1 || n > 4) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // 验证size参数格式
        if (parameters.containsKey("size")) {
            String size = parameters.get("size").toString();
            if (!size.matches("\\d+\\*\\d+")) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 基本图片生成：
           {"prompt": "一副典雅庄重的对联悬挂于厅堂之中", "configId": 1}
        
        2. 指定模型和尺寸：
           {"prompt": "美丽的风景画", "configId": 1, "model": "qwen-image-plus", "size": "1328*1328"}
        
        3. 生成多张图片：
           {"prompt": "可爱的小猫", "configId": 1, "n": 2, "size": "1024*1024"}
        
        4. 完整参数示例：
           {"prompt": "中国风山水画", "configId": 1, "model": "qwen-image-plus", "size": "1328*1328", "n": 1, "promptExtend": true, "watermark": false}
        
        注意：
        - prompt: 必填，描述要生成的图片内容
        - configId: 必填，AI模型配置ID，需要在系统中预先配置阿里云API密钥
        - model: 可选，默认为qwen-image-plus
        - size: 可选，格式为宽*高，默认为1024*1024
        - n: 可选，生成数量1-4，默认为1
        - promptExtend: 可选，是否扩展提示词，默认为true
        - watermark: 可选，是否添加水印，默认为true
        """;
    }
}