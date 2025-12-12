package com.ruoyi.project.ai.tool.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.project.ai.domain.AiCoverGenerationRecord;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.service.IAiCoverGenerationRecordService;
import com.ruoyi.project.ai.service.IAiImageService;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
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

    @Autowired
    private IAiImageService aiImageService;

    @Autowired
    private IAiCoverGenerationRecordService aiCoverGenerationRecordService;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ALI_IMAGE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    @Override
    public String getToolName() {
        return "ali_image_generation";
    }

    @Override
    public String getToolDescription() {
        return "使用阿里云通义万相模型同步生成图片，立即返回图片URL地址，支持多种尺寸和参数配置";
    }

    @Override
    public ToolSpecification getToolSpecification() {
        // 创建AI生图工具规范
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
                .addStringProperty("prompt", "图片生成的提示词，描述要生成的图片内容，必填")
                .addStringProperty("model", "使用的模型名称，默认为qwen-image-plus，可选")
                .addStringProperty("size",
                        "图片尺寸，仅支持以下值: 1664*928, 1472*1140, 1328*1328, 1140*1472, 928*1664。默认为1328*1328，可选")
                .addStringProperty("negativePrompt", "反向提示词，描述不希望在画面中看到的内容，可选")
                .addBooleanProperty("promptExtend", "是否启用提示词扩展，默认为true，可选")
                .addBooleanProperty("watermark", "是否添加水印，默认为false，可选")
                .addIntegerProperty("seed", "随机数种子，取值范围[0, 2147483647]，可选")
                .required("prompt")
                .build();

        return ToolSpecification.builder()
                .name(getToolName())
                .description(getToolDescription())
                .parameters(parametersSchema)
                .build();
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        // 获取必填参数
        String prompt = (String) parameters.get("prompt");

        if (StrUtil.isBlank(prompt)) {
            return ToolExecutionResult.failure("operation", "提示词不能为空");
        }

        // 使用固定的配置ID: 24
        Long configId = 24L;

        // 获取AI模型配置
        AiModelConfig config = aiModelConfigService.getById(configId);
        if (config == null) {
            return ToolExecutionResult.failure("operation", "找不到指定的AI模型配置，配置ID: " + configId);
        }

        if (!"Y".equals(config.getEnabled())) {
            return ToolExecutionResult.failure("operation", "指定的AI模型配置已禁用，配置ID: " + configId);
        }

        if (StrUtil.isBlank(config.getApiKey())) {
            return ToolExecutionResult.failure("operation", "AI模型配置中缺少API密钥，配置ID: " + configId);
        }

        // 获取可选参数
        String model = (String) parameters.getOrDefault("model", "qwen-image-plus");
        String size = (String) parameters.getOrDefault("size", "1328*1328");
        String negativePrompt = (String) parameters.getOrDefault("negativePrompt", "");
        Boolean promptExtend = parameters.get("promptExtend") != null
                ? Boolean.parseBoolean(parameters.get("promptExtend").toString())
                : true;
        Boolean watermark = parameters.get("watermark") != null
                ? Boolean.parseBoolean(parameters.get("watermark").toString())
                : false;
        Integer seed = parameters.get("seed") != null ? Integer.parseInt(parameters.get("seed").toString()) : null;

        // 构建请求体 - 使用新的同步接口格式
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", model);

        // 构建input对象，包含messages数组
        JSONObject input = new JSONObject();
        JSONObject message = new JSONObject();
        message.set("role", "user");

        // 构建content数组，包含text对象
        JSONObject textContent = new JSONObject();
        textContent.set("text", prompt);
        message.set("content", new Object[] { textContent });

        input.set("messages", new Object[] { message });
        requestBody.set("input", input);

        // 构建parameters对象
        JSONObject params = new JSONObject();
        params.set("size", size);
        // 注意：同步接口当前仅支持生成1张图像
        params.set("n", 1);
        params.set("prompt_extend", promptExtend);
        params.set("watermark", watermark);
        params.set("negative_prompt", negativePrompt);
        if (seed != null) {
            params.set("seed", seed);
        }
        requestBody.set("parameters", params);

        try {
            // 发送HTTP请求
            String response = sendImageGenerationRequest(config.getApiKey(), requestBody.toString());

            // 解析响应，获取临时图片URL
            String tempImageUrl = parseImageGenerationResponse(response, prompt, model, size, 1);

            // 检查是否是错误响应
            if (tempImageUrl.startsWith("ERROR:")) {
                return ToolExecutionResult.failure("operation", tempImageUrl.substring(7)); // 去掉 "ERROR: " 前缀
            }

            // 下载图片并上传到OSS
            String baseFileName = "ai_image_" + System.currentTimeMillis();
            String ossImageUrl;
            try {
                ossImageUrl = aiImageService.downloadAndUploadToOss(tempImageUrl, baseFileName);
            } catch (Exception e) {
                return ToolExecutionResult.failure("operation", "上传图片到OSS失败: " + e.getMessage());
            }

            // 保存AI生图记录到数据库
            try {
                AiCoverGenerationRecord record = new AiCoverGenerationRecord();
                record.setPrompt(prompt);
                record.setImageUrl(ossImageUrl);
                record.setAiModel(model);
                record.setCoverType("0"); // 0-通用封面
                record.setGenerationStatus("1"); // 1-成功
                record.setIsUsed("0"); // 0-未使用
                record.setDelFlag("0");
                record.setGenerationTime(new Date());
                aiCoverGenerationRecordService.save(record);
            } catch (Exception e) {
                // 记录保存失败不影响返回结果，仅记录日志
                System.err.println("保存AI生图记录失败: " + e.getMessage());
            }

            // 成功返回图片URL（返回OSS的永久URL，而不是临时URL）
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("imageUrl", ossImageUrl);
            result.put("tempImageUrl", tempImageUrl); // 也返回临时URL供参考
            result.put("prompt", prompt);
            result.put("model", model);
            result.put("size", size);

            return ToolExecutionResult.operationSuccess(result, "图片生成成功并已上传到OSS");
        } catch (Exception e) {
            return ToolExecutionResult.failure("operation", "生成图片时发生错误: " + e.getMessage());
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
     * 解析图片生成响应 - 同步接口版本
     */
    private String parseImageGenerationResponse(String responseBody, String prompt, String model, String size, int n) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 检查是否有错误
            if (jsonNode.has("code")) {
                String code = jsonNode.get("code").asText();
                String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "未知错误";
                return "ERROR: " + code + " - " + message;
            }

            // 解析同步响应格式
            if (jsonNode.has("output") && jsonNode.get("output").has("choices")) {
                JsonNode choices = jsonNode.get("output").get("choices");

                if (choices.size() > 0) {
                    JsonNode choice = choices.get(0);

                    // 检查finish_reason
                    if (choice.has("finish_reason") && !"stop".equals(choice.get("finish_reason").asText())) {
                        return "ERROR: 生成未正常完成，finish_reason: " + choice.get("finish_reason").asText();
                    }

                    // 获取消息内容
                    if (choice.has("message") && choice.get("message").has("content")) {
                        JsonNode content = choice.get("message").get("content");

                        if (content.isArray() && content.size() > 0) {
                            JsonNode imageContent = content.get(0);
                            if (imageContent.has("image")) {
                                return imageContent.get("image").asText();
                            }
                        }
                    }
                }
            }

            return "ERROR: 响应格式异常，未找到图片URL";

        } catch (Exception e) {
            return "ERROR: 解析响应失败 - " + e.getMessage();
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

        // 验证seed参数
        if (parameters.containsKey("seed")) {
            try {
                int seed = Integer.parseInt(parameters.get("seed").toString());
                if (seed < 0 || seed > 2147483647) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // 验证size参数格式
        if (parameters.containsKey("size")) {
            String size = parameters.get("size").toString();
            // 允许的尺寸列表
            java.util.List<String> allowedSizes = java.util.Arrays.asList(
                    "1664*928", "1472*1140", "1328*1328", "1140*1472", "928*1664");
            if (!allowedSizes.contains(size)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getUsageExample() {
        return """
                示例用法（同步接口）：
                1. 基本图片生成：
                   {"prompt": "一副典雅庄重的对联悬挂于厅堂之中"}
                   返回：https://dashscope-result-sz.oss-cn-shenzhen.aliyuncs.com/xxx.png

                2. 指定模型和尺寸：
                   {"prompt": "美丽的风景画", "model": "qwen-image-plus", "size": "1328*1328"}
                   返回：https://dashscope-result-sz.oss-cn-shenzhen.aliyuncs.com/xxx.png

                3. 使用反向提示词：
                   {"prompt": "可爱的小猫", "negativePrompt": "低分辨率、错误、最差质量", "size": "1328*1328"}
                   返回：https://dashscope-result-sz.oss-cn-shenzhen.aliyuncs.com/xxx.png

                4. 完整参数示例：
                   {"prompt": "中国风山水画", "model": "qwen-image-plus", "size": "1328*1328", "negativePrompt": "模糊、低质量", "promptExtend": true, "watermark": false, "seed": 12345}
                   返回：https://dashscope-result-sz.oss-cn-shenzhen.aliyuncs.com/xxx.png

                返回值说明：
                - 成功时：直接返回图片URL（同步接口立即返回结果）
                - 失败时：返回 "ERROR: xxx" 格式的错误信息

                参数说明：
                - prompt: 必填，描述要生成的图片内容（最多800字符）
                - model: 可选，默认为qwen-image-plus，也可选择qwen-image
                - size: 可选，支持1664*928(16:9)、1472*1140(4:3)、1328*1328(1:1)、1140*1472(3:4)、928*1664(9:16)，默认为1328*1328
                - negativePrompt: 可选，反向提示词，描述不希望看到的内容（最多500字符）
                - promptExtend: 可选，是否启用提示词智能改写，默认为true
                - watermark: 可选，是否添加水印，默认为false
                - seed: 可选，随机数种子，取值范围[0, 2147483647]，用于生成相对稳定的结果

                注意：同步接口每次只能生成1张图片，图片URL有效期24小时。
                系统将自动使用配置ID为24的AI模型配置来获取API密钥。
                """;
    }
}