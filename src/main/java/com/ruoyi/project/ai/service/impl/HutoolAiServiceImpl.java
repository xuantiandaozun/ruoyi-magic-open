package com.ruoyi.project.ai.service.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.file.ByteArrayMultipartFile;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.project.ai.service.IHutoolAiService;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest.ChatCompletionRequestResponseFormat;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.service.ArkService;

import cn.hutool.ai.AIUtil;
import cn.hutool.ai.ModelName;
import cn.hutool.ai.core.AIConfig;
import cn.hutool.ai.core.AIConfigBuilder;
import cn.hutool.ai.core.Message;
import cn.hutool.ai.model.doubao.DoubaoService;
import cn.hutool.ai.model.openai.OpenaiService;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

/**
 * Hutool AI服务实现类
 * 基于hutool aiutil封装的AI服务，支持多种AI模型
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
public class HutoolAiServiceImpl implements IHutoolAiService {
    
    private static final Logger log = LoggerFactory.getLogger(HutoolAiServiceImpl.class);
    
    // 豆包配置
    @Value("${ai.doubao.api-key:}")
    private String doubaoApiKey;
    
    @Value("${ai.doubao.endpoint:https://ark.cn-beijing.volces.com/api/v3}")
    private String doubaoEndpoint;
    
    @Value("${ai.doubao.model:ep-20241212105607-kcmvs}")
    private String doubaoModel;
    
    @Value("${ai.doubao.image-model:ep-20250818101908-mhzcm}")
    private String doubaoImageModel;
    
    // OpenAI配置
    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;
    
    @Value("${ai.openai.endpoint:https://api.openai.com/v1}")
    private String openaiEndpoint;
    
    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String openaiModel;
    
    // DeepSeek配置
    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;
    
    @Value("${ai.deepseek.endpoint:https://api.deepseek.com/v1}")
    private String deepseekEndpoint;
    
    @Value("${ai.deepseek.model:deepseek-chat}")
    private String deepseekModel;
    
    // 默认使用的AI服务类型
    @Value("${ai.default.type:DOUBAO}")
    private String defaultAiType;
    
    private String currentAiType;
    
    @PostConstruct
    public void init() {
        // 设置默认AI类型
        currentAiType = defaultAiType;
        log.info("当前使用的AI服务类型: {}", currentAiType);
    }
    
    /**
     * 获取当前AI类型
     */
    public String getCurrentAiType() {
        return StrUtil.isNotBlank(currentAiType) ? currentAiType : defaultAiType;
    }
    
    /**
     * 设置当前AI类型
     * @param aiType AI类型
     */
    public void setCurrentAiType(String aiType) {
        if (StrUtil.isBlank(aiType)) {
            throw new IllegalArgumentException("AI类型不能为空");
        }
        
        String upperAiType = aiType.toUpperCase();
        if (!"DOUBAO".equals(upperAiType) && !"OPENAI".equals(upperAiType) && !"DEEPSEEK".equals(upperAiType)) {
            throw new IllegalArgumentException("不支持的AI类型: " + aiType);
        }
        
        this.currentAiType = upperAiType;
        log.info("AI服务类型已切换为: {}", this.currentAiType);
    }
    
    /**
     * 获取当前AI配置
     */
    private AIConfig getCurrentAiConfig() {
        String aiType = StrUtil.isNotBlank(currentAiType) ? currentAiType : defaultAiType;
        
        switch (aiType.toUpperCase()) {
            case "DOUBAO":
                if (StrUtil.isNotBlank(doubaoApiKey)) {
                    return new AIConfigBuilder(ModelName.DOUBAO.getValue())
                        .setApiKey(doubaoApiKey)
                        .setApiUrl(doubaoEndpoint)
                        .setModel(doubaoModel)
                        .build();
                }
                break;
            case "OPENAI":
                if (StrUtil.isNotBlank(openaiApiKey)) {
                    return new AIConfigBuilder(ModelName.OPENAI.getValue())
                        .setApiKey(openaiApiKey)
                        .setApiUrl(openaiEndpoint)
                        .setModel(openaiModel)
                        .build();
                }
                break;
            case "DEEPSEEK":
                if (StrUtil.isNotBlank(deepseekApiKey)) {
                    return new AIConfigBuilder(ModelName.DEEPSEEK.getValue())
                        .setApiKey(deepseekApiKey)
                        .setApiUrl(deepseekEndpoint)
                        .setModel(deepseekModel)
                        .build();
                }
                break;
        }
        
        // 如果当前类型不可用，尝试其他可用服务
        if (StrUtil.isNotBlank(doubaoApiKey)) {
            currentAiType = "DOUBAO";
            return new AIConfigBuilder(ModelName.DOUBAO.getValue())
                .setApiKey(doubaoApiKey)
                .setApiUrl(doubaoEndpoint)
                .setModel(doubaoModel)
                .build();
        }
        if (StrUtil.isNotBlank(openaiApiKey)) {
            currentAiType = "OPENAI";
            return new AIConfigBuilder(ModelName.OPENAI.getValue())
                .setApiKey(openaiApiKey)
                .setApiUrl(openaiEndpoint)
                .setModel(openaiModel)
                .build();
        }
        if (StrUtil.isNotBlank(deepseekApiKey)) {
            currentAiType = "DEEPSEEK";
            return new AIConfigBuilder(ModelName.DEEPSEEK.getValue())
                .setApiKey(deepseekApiKey)
                .setApiUrl(deepseekEndpoint)
                .setModel(deepseekModel)
                .build();
        }
        
        throw new RuntimeException("没有可用的AI服务，请检查配置");
    }
    

    
    @Override
    public String chat(String message) {
        return chat(message, false);
    }
    
    /**
     * 基础聊天对话
     * 
     * @param message 用户消息
     * @param returnJson 是否返回JSON格式
     * @return AI回复
     */
    public String chat(String message, boolean returnJson) {
        try {
            // 使用豆包的ArkService进行聊天
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
                Dispatcher dispatcher = new Dispatcher();
                ArkService service = ArkService.builder()
                        .dispatcher(dispatcher)
                        .connectionPool(connectionPool)
                        .apiKey(doubaoApiKey)
                        .build();
                
                List<ChatMessage> messagesForReqList = new ArrayList<>();
                ChatMessage userMessage = ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(message)
                        .build();
                messagesForReqList.add(userMessage);
                
                ChatCompletionRequest.Builder reqBuilder = ChatCompletionRequest.builder()
                        .model(doubaoModel)
                        .maxTokens(returnJson ? 8192 : 4096)
                        .messages(messagesForReqList);
                
                // 根据参数决定是否设置JSON格式
                if (returnJson) {
                    reqBuilder.responseFormat(new ChatCompletionRequestResponseFormat("json_object"));
                }
                
                ChatCompletionRequest req = reqBuilder.build();
                
                StringBuilder response = new StringBuilder();
                service.createChatCompletion(req)
                        .getChoices()
                        .forEach(choice -> response.append(choice.getMessage().getContent()));
                
                service.shutdownExecutor();
                return response.toString();
            }
            
            // 如果豆包不可用，回退到原有逻辑
            AIConfig config = getCurrentAiConfig();
            return AIUtil.chat(config, message);
        } catch (Exception e) {
            log.error("聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("聊天请求失败: " + e.getMessage());
        }
    }
    
    @Override
    public String chatWithSystem(String systemPrompt, String message) {
        return chatWithSystem(systemPrompt, message, false);
    }
    
    /**
     * 带系统提示的聊天对话
     * 
     * @param systemPrompt 系统提示
     * @param message 用户消息
     * @param returnJson 是否返回JSON格式
     * @return AI回复
     */
    public String chatWithSystem(String systemPrompt, String message, boolean returnJson) {
        try {
            // 使用豆包的ArkService进行带系统提示的聊天
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
                Dispatcher dispatcher = new Dispatcher();
                ArkService service = ArkService.builder()
                        .dispatcher(dispatcher)
                        .connectionPool(connectionPool)
                        .apiKey(doubaoApiKey)
                        .build();
                
                List<ChatMessage> messagesForReqList = new ArrayList<>();
                
                ChatMessage systemMessage = ChatMessage.builder()
                        .role(ChatMessageRole.SYSTEM)
                        .content(systemPrompt)
                        .build();
                
                ChatMessage userMessage = ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(message)
                        .build();
                
                messagesForReqList.add(systemMessage);
                messagesForReqList.add(userMessage);
                
                ChatCompletionRequest.Builder reqBuilder = ChatCompletionRequest.builder()
                        .model(doubaoModel)
                        .maxTokens(returnJson ? 8192 : 4096)
                        .messages(messagesForReqList);
                
                // 根据参数决定是否设置JSON格式
                if (returnJson) {
                    reqBuilder.responseFormat(new ChatCompletionRequestResponseFormat("json_object"));
                }
                
                ChatCompletionRequest req = reqBuilder.build();
                
                StringBuilder response = new StringBuilder();
                service.createChatCompletion(req)
                        .getChoices()
                        .forEach(choice -> response.append(choice.getMessage().getContent()));
                
                service.shutdownExecutor();
                return response.toString();
            }
            
            // 如果豆包不可用，回退到原有逻辑
            AIConfig config = getCurrentAiConfig();
            List<Message> messages = new ArrayList<>();
            messages.add(new Message("system", systemPrompt));
            messages.add(new Message("user", message));
            return AIUtil.chat(config, messages);
        } catch (Exception e) {
            log.error("带系统提示的聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("带系统提示的聊天请求失败: " + e.getMessage());
        }
    }
    
    @Override
    public String chatWithHistory(List<String> messages) {
        try {
            AIConfig config = getCurrentAiConfig();
            // 将字符串消息转换为Message对象列表
            List<Message> messageList = new ArrayList<>();
            for (int i = 0; i < messages.size(); i++) {
                String role = (i % 2 == 0) ? "user" : "assistant";
                messageList.add(new Message(role, messages.get(i)));
            }
            return AIUtil.chat(config, messageList);
        } catch (Exception e) {
            log.error("多轮对话请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("多轮对话请求失败: " + e.getMessage());
        }
    }
    
    @Override
    public String chatVision(String message, List<String> imageUrls) {
        try {
            // 目前主要支持豆包的视觉能力
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                AIConfig doubaoConfig = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                    .setApiKey(doubaoApiKey)
                    .setApiUrl(doubaoEndpoint)
                    .setModel(doubaoModel)
                    .build();
                DoubaoService service = AIUtil.getDoubaoService(doubaoConfig);
                return service.chatVision(message, imageUrls);
            }
            
            throw new RuntimeException("当前没有可用的视觉AI服务，请配置豆包API");
        } catch (Exception e) {
            log.error("图文理解请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("图文理解请求失败: " + e.getMessage());
        }
    }
    
    @Override
    public String embeddingText(String[] texts) {
        try {
            String aiType = StrUtil.isNotBlank(currentAiType) ? currentAiType : defaultAiType;
            
            switch (aiType.toUpperCase()) {
                case "DOUBAO":
                    if (StrUtil.isNotBlank(doubaoApiKey)) {
                        AIConfig config = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                            .setApiKey(doubaoApiKey)
                            .setApiUrl(doubaoEndpoint)
                            .setModel(doubaoModel)
                            .build();
                        DoubaoService service = AIUtil.getDoubaoService(config);
                        return service.embeddingText(texts);
                    }
                    break;
                case "OPENAI":
                    if (StrUtil.isNotBlank(openaiApiKey)) {
                        AIConfig config = new AIConfigBuilder(ModelName.OPENAI.getValue())
                            .setApiKey(openaiApiKey)
                            .setApiUrl(openaiEndpoint)
                            .setModel(openaiModel)
                            .build();
                        OpenaiService service = AIUtil.getOpenAIService(config);
                        // OpenAI的embeddingText只支持单个文本，这里处理第一个文本
                        return service.embeddingText(texts.length > 0 ? texts[0] : "");
                    }
                    break;
            }
            
            throw new RuntimeException("当前AI服务不支持文本向量化功能");
        } catch (Exception e) {
            log.error("文本向量化请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("文本向量化请求失败: " + e.getMessage());
        }
    }
    
    @Override
    public String embeddingVision(String text, String imageUrl) {
        try {
            // 目前主要支持豆包的图文向量化能力
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                AIConfig doubaoConfig = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                    .setApiKey(doubaoApiKey)
                    .setApiUrl(doubaoEndpoint)
                    .setModel(doubaoModel)
                    .build();
                DoubaoService service = AIUtil.getDoubaoService(doubaoConfig);
                return service.embeddingVision(text, imageUrl);
            }
            
            throw new RuntimeException("当前没有可用的图文向量化服务，请配置豆包API");
        } catch (Exception e) {
            log.error("图文向量化请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("图文向量化请求失败: " + e.getMessage());
        }
    }

    


    
    @Override
    public String batchChat(String prompt) {
        try {
            // 批量推理目前主要支持豆包
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                AIConfig doubaoConfig = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                    .setApiKey(doubaoApiKey)
                    .setApiUrl(doubaoEndpoint)
                    .setModel(doubaoModel)
                    .build();
                DoubaoService service = AIUtil.getDoubaoService(doubaoConfig);
                return service.batchChat(prompt);
            }
            
            throw new RuntimeException("当前没有可用的批量推理服务，请配置豆包API");
        } catch (Exception e) {
            log.error("批量推理请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量推理请求失败: " + e.getMessage());
        }
    }
    
    @Override
    public String createVideoTask(String prompt, String imageUrl) {
        try {
            // 视频生成目前主要支持豆包
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                AIConfig doubaoConfig = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                    .setApiKey(doubaoApiKey)
                    .setApiUrl(doubaoEndpoint)
                    .setModel(doubaoModel)
                    .build();
                DoubaoService service = AIUtil.getDoubaoService(doubaoConfig);
                return service.videoTasks(prompt, imageUrl);
            }
            
            throw new RuntimeException("当前没有可用的视频生成服务，请配置豆包API");
        } catch (Exception e) {
            log.error("创建视频生成任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建视频生成任务失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getVideoTaskStatus(String taskId) {
        try {
            // 视频生成目前主要支持豆包
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                AIConfig doubaoConfig = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                    .setApiKey(doubaoApiKey)
                    .setApiUrl(doubaoEndpoint)
                    .setModel(doubaoModel)
                    .build();
                DoubaoService service = AIUtil.getDoubaoService(doubaoConfig);
                return service.getVideoTasksInfo(taskId);
            }
            
            throw new RuntimeException("当前没有可用的视频生成服务，请配置豆包API");
        } catch (Exception e) {
            log.error("查询视频生成任务状态失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询视频生成任务状态失败: " + e.getMessage());
        }
    }
    
    @Override
    public String tokenization(String[] texts) {
        try {
            // 文本分词目前主要支持豆包
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                AIConfig doubaoConfig = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                    .setApiKey(doubaoApiKey)
                    .setApiUrl(doubaoEndpoint)
                    .setModel(doubaoModel)
                    .build();
                DoubaoService service = AIUtil.getDoubaoService(doubaoConfig);
                return service.tokenization(texts);
            }
            
            throw new RuntimeException("当前没有可用的文本分词服务，请配置豆包API");
        } catch (Exception e) {
            log.error("文本分词请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("文本分词请求失败: " + e.getMessage());
        }
    }
    
    @Override
    public String createContext(List<String> messages) {
        try {
            // 上下文缓存目前主要支持豆包
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                AIConfig doubaoConfig = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                    .setApiKey(doubaoApiKey)
                    .setApiUrl(doubaoEndpoint)
                    .setModel(doubaoModel)
                    .build();
                DoubaoService service = AIUtil.getDoubaoService(doubaoConfig);
                List<Message> messageList = new ArrayList<>();
                for (String msg : messages) {
                    messageList.add(new Message("user", msg));
                }
                return service.createContext(messageList);
            }
            
            throw new RuntimeException("当前没有可用的上下文缓存服务，请配置豆包API");
        } catch (Exception e) {
            log.error("创建上下文缓存失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建上下文缓存失败: " + e.getMessage());
        }
    }
    
    @Override
    public String chatWithContext(String message, String contextId) {
        try {
            // 上下文对话目前主要支持豆包
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                AIConfig doubaoConfig = new AIConfigBuilder(ModelName.DOUBAO.getValue())
                    .setApiKey(doubaoApiKey)
                    .setApiUrl(doubaoEndpoint)
                    .setModel(doubaoModel)
                    .build();
                DoubaoService service = AIUtil.getDoubaoService(doubaoConfig);
                return service.chatContext(message, contextId);
            }
            
            throw new RuntimeException("当前没有可用的上下文对话服务，请配置豆包API");
        } catch (Exception e) {
            log.error("基于上下文的对话请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("基于上下文的对话请求失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getCurrentModel() {
        return currentAiType;
    }
    
    @Override
    public boolean switchModel(String modelType) {
        try {
            String upperModelType = modelType.toUpperCase();
            
            switch (upperModelType) {
                case "DOUBAO":
                    if (StrUtil.isNotBlank(doubaoApiKey)) {
                        currentAiType = "DOUBAO";
                        log.info("已切换到豆包AI服务");
                        return true;
                    }
                    break;
                case "OPENAI":
                    if (StrUtil.isNotBlank(openaiApiKey)) {
                        currentAiType = "OPENAI";
                        log.info("已切换到OpenAI服务");
                        return true;
                    }
                    break;
                case "DEEPSEEK":
                    if (StrUtil.isNotBlank(deepseekApiKey)) {
                        currentAiType = "DEEPSEEK";
                        log.info("已切换到DeepSeek服务");
                        return true;
                    }
                    break;
            }
            
            log.warn("无法切换到AI服务类型: {}，该服务未配置或初始化失败", modelType);
            return false;
        } catch (Exception e) {
            log.error("切换AI模型失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String generateImage(String prompt, String size, Double guidanceScale, Integer seed, Boolean watermark) {
        try {
            // 目前主要支持豆包的文生图能力
            if (StrUtil.isNotBlank(doubaoApiKey)) {
                // 创建连接池和调度器
                ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
                Dispatcher dispatcher = new Dispatcher();
                
                // 创建ArkService
                ArkService service = ArkService.builder()
                    .dispatcher(dispatcher)
                    .connectionPool(connectionPool)
                    .apiKey(doubaoApiKey)
                    .build();
                
                // 构建图像生成请求
                GenerateImagesRequest.Builder requestBuilder = GenerateImagesRequest.builder()
                    .model(doubaoImageModel)
                    .prompt(prompt);
                
                // 设置可选参数
                if (StrUtil.isNotBlank(size)) {
                    requestBuilder.size(size);
                }
                if (guidanceScale != null) {
                    requestBuilder.guidanceScale(guidanceScale);
                }
                if (seed != null) {
                    requestBuilder.seed(seed);
                }
                if (watermark != null) {
                    requestBuilder.watermark(watermark);
                }
                
                GenerateImagesRequest generateRequest = requestBuilder.build();
                
                // 调用图像生成API
                ImagesResponse imagesResponse = service.generateImages(generateRequest);
                
                // 关闭服务
                service.shutdownExecutor();
                
                // 返回生成的图像URL并转存到OSS
                if (imagesResponse.getData() != null && !imagesResponse.getData().isEmpty()) {
                    String originalImageUrl = imagesResponse.getData().get(0).getUrl();
                    
                    try {
                        // 下载图片
                        URL url = new URL(originalImageUrl);
                        InputStream inputStream = url.openStream();
                        
                        // 将InputStream转换为字节数组
                        byte[] imageBytes = IoUtil.readBytes(inputStream);
                        inputStream.close();
                        
                        // 创建ByteArrayMultipartFile用于上传
                        String fileName = "generated_image_" + System.currentTimeMillis() + ".png";
                        ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                            "file", 
                            fileName, 
                            "image/png", 
                            imageBytes
                        );
                        
                        // 上传到OSS
                        String ossUrl = FileUploadUtils.upload(multipartFile);
                        
                        return ossUrl;
                    } catch (Exception e) {
                        log.warn("图片转存到OSS失败，返回原始URL: {}", e.getMessage());
                        return originalImageUrl;
                    }
                } else {
                    throw new RuntimeException("图像生成失败：未返回图像数据");
                }
            }
            
            throw new RuntimeException("当前没有可用的文生图AI服务，请配置豆包API");
        } catch (Exception e) {
            log.error("文生图请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("文生图请求失败: " + e.getMessage());
        }
    }
}