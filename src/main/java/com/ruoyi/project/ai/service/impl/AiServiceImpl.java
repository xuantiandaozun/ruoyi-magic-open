package com.ruoyi.project.ai.service.impl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ruoyi.project.ai.domain.AiChatMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.row.Db;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiService;
import com.ruoyi.project.ai.strategy.AiClientFactory;
import com.ruoyi.project.ai.strategy.AiClientStrategy;
import com.ruoyi.project.system.service.ISysConfigService;

import cn.hutool.ai.AIUtil;
import cn.hutool.ai.ModelName;
import cn.hutool.ai.core.AIConfig;
import cn.hutool.ai.core.AIConfigBuilder;
import cn.hutool.ai.model.doubao.DoubaoService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;

/**
 * AI服务实现类
 * 支持多种AI模型
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
public class AiServiceImpl implements IAiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private IAiModelConfigService aiModelConfigService;

    // 当前使用的策略
    private AiClientStrategy currentStrategy;

    private String currentAiType;

    @PostConstruct
    public void init() {
        // 从系统参数获取AI类型
        String aiTypeFromConfig = sysConfigService.selectConfigByKey("ai.default.type");
        currentAiType = StrUtil.isNotBlank(aiTypeFromConfig) ? aiTypeFromConfig : "DOUBAO";

        // 初始化策略
        initializeStrategy();

        log.info("当前使用的AI服务类型: {}", currentAiType);
    }

    /**
     * 初始化AI策略
     */
    private void initializeStrategy() {
        try {
            // 从数据库获取默认配置
            AiModelConfig defaultConfig = aiModelConfigService
                    .getDefaultByProviderAndCapability(currentAiType.toLowerCase(), "chat");
            if (defaultConfig != null) {
                currentStrategy = AiClientFactory.fromConfig(defaultConfig);
                log.info("使用数据库配置初始化AI策略: {}", defaultConfig.getProvider());
            } else {
                log.warn("未找到默认AI配置，请在数据库中配置AI模型");
            }
        } catch (Exception e) {
            log.error("从数据库获取AI配置失败: {}", e.getMessage());
        }
    }

    /**
     * 获取当前AI类型
     */
    public String getCurrentAiType() {
        return StrUtil.isNotBlank(currentAiType) ? currentAiType : "DOUBAO";
    }

    /**
     * 设置当前AI类型
     * 
     * @param aiType AI类型
     */
    public void setCurrentAiType(String aiType) {
        if (StrUtil.isBlank(aiType)) {
            throw new IllegalArgumentException("AI类型不能为空");
        }

        this.currentAiType = aiType.toUpperCase();
        log.info("AI服务类型已切换为: {}", this.currentAiType);

        // 重新初始化策略
        initializeStrategy();
    }

    /**
     * 获取当前AI配置
     */
    private AIConfig getCurrentAiConfig() {
        try {
            // 从数据库获取当前AI类型的配置
            String aiType = StrUtil.isNotBlank(currentAiType) ? currentAiType : "DOUBAO";
            AiModelConfig config = aiModelConfigService.getDefaultByProviderAndCapability(aiType.toLowerCase(), "chat");

            if (config != null && StrUtil.isNotBlank(config.getApiKey())) {
                ModelName modelName = getModelNameByProvider(config.getProvider());
                return new AIConfigBuilder(modelName.getValue())
                        .setApiKey(config.getApiKey())
                        .setApiUrl(config.getEndpoint())
                        .setModel(config.getModel())
                        .build();
            }
            // 当前类型不可用时不再回退到其他配置，直接报错
        } catch (Exception e) {
            log.error("获取AI配置失败: {}", e.getMessage());
        }

        throw new RuntimeException("没有可用的AI服务，请检查数据库配置");
    }

    /**
     * 根据提供商获取模型名称
     */
    private ModelName getModelNameByProvider(String provider) {
        switch (provider.toLowerCase()) {
            case "doubao":
                return ModelName.DOUBAO;
            case "openai":
                return ModelName.OPENAI;
            case "deepseek":
                return ModelName.DEEPSEEK;
            default:
                return ModelName.DOUBAO; // 默认使用豆包
        }
    }

    @Override
    public String chat(String message) {
        try {
            if (currentStrategy != null) {
                return currentStrategy.chat(message);
            }
            throw new RuntimeException("聊天请求失败: 策略模式不可用");
        } catch (Exception e) {
            log.error("聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("聊天请求失败: " + e.getMessage());
        }
    }

    /**
     * 基础聊天对话
     * 
     * @param message    用户消息
     * @param returnJson 是否返回JSON格式
     * @return AI回复
     */
    public String chat(String message, boolean returnJson) {
        // 为兼容旧接口，忽略 returnJson，调用无参版本
        return chat(message);
    }

    @Override
    public String chatWithSystem(String systemPrompt, String message) {
        return chatWithSystem(systemPrompt, message, false);
    }

    /**
     * 带系统提示的聊天对话
     * 
     * @param systemPrompt 系统提示
     * @param message      用户消息
     * @param returnJson   是否返回JSON格式
     * @return AI回复
     */
    public String chatWithSystem(String systemPrompt, String message, boolean returnJson) {
        try {
            // 使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.chatWithSystem(systemPrompt, message, returnJson);
            }
            throw new RuntimeException("带系统提示的聊天请求失败: 策略模式不可用");
        } catch (Exception e) {
            log.error("带系统提示的聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("带系统提示的聊天请求失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithModelConfig(String message, String systemPrompt, Long modelConfigId) {
        try {
            // 根据模型配置ID获取配置
            AiModelConfig config = aiModelConfigService.getById(modelConfigId);
            if (config == null) {
                throw new RuntimeException("模型配置不存在: " + modelConfigId);
            }
            
            if (!"Y".equals(config.getEnabled())) {
                throw new RuntimeException("模型配置已禁用: " + modelConfigId);
            }

            // 使用AiClientFactory.fromConfig创建临时策略
            AiClientStrategy tempStrategy = AiClientFactory.fromConfig(config);
            
            // 使用临时策略进行聊天
            if (StrUtil.isNotBlank(systemPrompt)) {
                return tempStrategy.chatWithSystem(systemPrompt, message, false);
            } else {
                return tempStrategy.chat(message);
            }
        } catch (Exception e) {
            log.error("使用指定模型配置聊天失败: {}", e.getMessage(), e);
            throw new RuntimeException("使用指定模型配置聊天失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithHistory(List<String> messages) {
        try {
            // 优先使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.chatWithHistory(messages);
            }
            throw new RuntimeException("多轮对话请求失败: 策略模式不可用");
        } catch (Exception e) {
            log.error("多轮对话请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("多轮对话请求失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithHistory(String message, String systemPrompt, List<com.ruoyi.project.ai.domain.AiChatMessage> chatHistory, Long modelConfigId) {
        try {
            // 根据模型配置ID获取配置
            AiModelConfig config = aiModelConfigService.getById(modelConfigId);
            if (config == null) {
                throw new RuntimeException("模型配置不存在: " + modelConfigId);
            }
            
            if (!"Y".equals(config.getEnabled())) {
                throw new RuntimeException("模型配置已禁用: " + modelConfigId);
            }

            // 使用AiClientFactory.fromConfig创建临时策略
            AiClientStrategy tempStrategy = AiClientFactory.fromConfig(config);
            
            // 使用临时策略进行带历史的聊天
            return tempStrategy.chatWithHistory(message, systemPrompt, chatHistory);
            
        } catch (Exception e) {
            log.error("使用聊天历史的对话失败: {}", e.getMessage(), e);
            throw new RuntimeException("使用聊天历史的对话失败: " + e.getMessage());
        }
    }

    @Override
    public String chatVision(String message, List<String> imageUrls) {
        try {
            // 使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.chatVision(message, imageUrls);
            }

            // 如果策略不可用，尝试从数据库获取支持视觉的配置
            AIConfig config = getCurrentAiConfig();
            if (config.getModelName().equals(ModelName.DOUBAO.getValue())) {
                DoubaoService service = AIUtil.getDoubaoService(config);
                return service.chatVision(message, imageUrls);
            }

            throw new RuntimeException("当前AI服务不支持视觉功能");
        } catch (Exception e) {
            log.error("图文理解请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("图文理解请求失败: " + e.getMessage());
        }
    }

    @Override
    public String embeddingText(String[] texts) {
        try {
            // 使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.embeddingText(texts);
            }
            throw new RuntimeException("当前AI服务不支持图文向量化功能");

        } catch (Exception e) {
            log.error("文本向量化请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("文本向量化请求失败: " + e.getMessage());
        }
    }

    @Override
    public String embeddingVision(String text, String imageUrl) {
        try {
            // 使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.embeddingVision(text, imageUrl);
            }

            throw new RuntimeException("当前AI服务不支持图文向量化功能");
        } catch (Exception e) {
            log.error("图文向量化请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("图文向量化请求失败: " + e.getMessage());
        }
    }

    @Override
    public String batchChat(String prompt) {
        try {
            // 优先使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.batchChat(prompt);
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
            // 优先使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.createVideoTask(prompt, imageUrl);
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
            // 优先使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.getVideoTaskStatus(taskId);
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
            // 优先使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.tokenization(texts);
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
            // 优先使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.createContext(messages);
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
            // 优先使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.chatWithContext(message, contextId);
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

            // 更新系统参数
            Db.updateBySql("UPDATE sys_config SET config_value = ? WHERE config_key = ?", upperModelType,
                    "ai.default.type");

            // 更新当前类型
            currentAiType = upperModelType;

            // 重新初始化策略
            initializeStrategy();

            log.info("已切换到AI服务类型: {}", upperModelType);
            return true;
        } catch (Exception e) {
            log.error("切换AI模型失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateImage(String prompt, String size, Double guidanceScale, Integer seed, Boolean watermark) {
        try {
            // 优先使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.generateImage(prompt, size, guidanceScale, seed, watermark);
            }
            throw new RuntimeException("当前没有可用的文生图AI服务，请配置豆包API");
        } catch (Exception e) {
            log.error("文生图请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("文生图请求失败: " + e.getMessage());
        }
    }

    @Override
    public void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            if (currentStrategy != null) {
                currentStrategy.streamChat(message, onToken, onComplete, onError);
            } else {
                onError.accept(new RuntimeException("流式聊天请求失败: 策略模式不可用"));
            }
        } catch (Exception e) {
            log.error("流式聊天请求失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("流式聊天请求失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithSystem(String systemPrompt, String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            if (currentStrategy != null) {
                currentStrategy.streamChatWithSystem(systemPrompt, message, onToken, onComplete, onError);
            } else {
                onError.accept(new RuntimeException("流式带系统提示的聊天请求失败: 策略模式不可用"));
            }
        } catch (Exception e) {
            log.error("流式带系统提示的聊天请求失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("流式带系统提示的聊天请求失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithModelConfig(String message, String systemPrompt, Long modelConfigId, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 根据模型配置ID获取配置
            AiModelConfig config = aiModelConfigService.getById(modelConfigId);
            if (config == null) {
                onError.accept(new RuntimeException("模型配置不存在: " + modelConfigId));
                return;
            }
            
            if (!"Y".equals(config.getEnabled())) {
                onError.accept(new RuntimeException("模型配置已禁用: " + modelConfigId));
                return;
            }

            // 使用AiClientFactory.fromConfig创建临时策略
            AiClientStrategy tempStrategy = AiClientFactory.fromConfig(config);
            
            // 使用临时策略进行流式聊天
            tempStrategy.streamChatWithModelConfig(message, systemPrompt, onToken, onComplete, onError);
            
        } catch (Exception e) {
            log.error("使用指定模型配置流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("使用指定模型配置流式聊天失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, Long modelConfigId, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 根据模型配置ID获取配置
            AiModelConfig config = aiModelConfigService.getById(modelConfigId);
            if (config == null) {
                onError.accept(new RuntimeException("模型配置不存在: " + modelConfigId));
                return;
            }
            
            if (!"Y".equals(config.getEnabled())) {
                onError.accept(new RuntimeException("模型配置已禁用: " + modelConfigId));
                return;
            }

            // 使用AiClientFactory.fromConfig创建临时策略
            AiClientStrategy tempStrategy = AiClientFactory.fromConfig(config);
            
            // 使用临时策略进行带历史的流式聊天
            tempStrategy.streamChatWithHistory(message, systemPrompt, chatHistory, onToken, onComplete, onError);
            
        } catch (Exception e) {
            log.error("使用聊天历史的流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("使用聊天历史的流式聊天失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, Long modelConfigId, Consumer<String> onToken, BiConsumer<String, String> onToolCall, BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 根据模型配置ID获取配置
            AiModelConfig config = aiModelConfigService.getById(modelConfigId);
            if (config == null) {
                onError.accept(new RuntimeException("模型配置不存在: " + modelConfigId));
                return;
            }
            
            if (!"Y".equals(config.getEnabled())) {
                onError.accept(new RuntimeException("模型配置已禁用: " + modelConfigId));
                return;
            }

            // 使用AiClientFactory.fromConfig创建临时策略
            AiClientStrategy tempStrategy = AiClientFactory.fromConfig(config);
            
            // 使用临时策略进行带历史的流式聊天（带工具调用回调）
            tempStrategy.streamChatWithHistory(message, systemPrompt, chatHistory, onToken, onToolCall, onToolResult, onComplete, onError);
            
        } catch (Exception e) {
            log.error("使用聊天历史的流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("使用聊天历史的流式聊天失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithModelConfig(String message, String systemPrompt, Long modelConfigId, Consumer<String> onToken, BiConsumer<String, String> onToolCall, BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 根据模型配置ID获取配置
            AiModelConfig config = aiModelConfigService.getById(modelConfigId);
            if (config == null) {
                onError.accept(new RuntimeException("模型配置不存在: " + modelConfigId));
                return;
            }
            
            if (!"Y".equals(config.getEnabled())) {
                onError.accept(new RuntimeException("模型配置已禁用: " + modelConfigId));
                return;
            }

            // 使用AiClientFactory.fromConfig创建临时策略
            AiClientStrategy tempStrategy = AiClientFactory.fromConfig(config);
            
            // 使用临时策略进行流式聊天（带工具调用回调）
            tempStrategy.streamChatWithModelConfig(message, systemPrompt, onToken, onToolCall, onToolResult, onComplete, onError);
            
        } catch (Exception e) {
            log.error("使用指定模型配置流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("使用指定模型配置流式聊天失败: " + e.getMessage()));
        }
    }
}