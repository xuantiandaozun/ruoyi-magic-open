package com.ruoyi.project.ai.service.impl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ruoyi.project.ai.domain.AiChatMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiService;
import com.ruoyi.project.ai.strategy.AiClientFactory;
import com.ruoyi.project.ai.strategy.AiClientStrategy;
import com.ruoyi.project.system.service.ISysConfigService;

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
        initializeStrategy();
    }

    @Override
    public String chat(String message) {
        try {
            // 强制使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.chat(message);
            }

            throw new RuntimeException("当前AI策略服务不可用，请检查策略配置");
        } catch (Exception e) {
            log.error("聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("聊天请求失败: " + e.getMessage());
        }
    }

    @Override
    public String chat(String message, boolean returnJson) {
        try {
            String response = chat(message);
            if (returnJson) {
                // 确保返回JSON格式
                if (!response.trim().startsWith("{") && !response.trim().startsWith("[")) {
                    response = "{\"result\":\"" + response.replace("\"", "\\\"") + "\"}";
                }
            }
            return response;
        } catch (Exception e) {
            log.error("聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("聊天请求失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithSystem(String systemPrompt, String message) {
        try {
            // 强制使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.chatWithSystem(systemPrompt, message, false);
            }

            throw new RuntimeException("当前AI策略服务不可用，请检查策略配置");
        } catch (Exception e) {
            log.error("带系统提示的聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("带系统提示的聊天请求失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithSystem(String systemPrompt, String message, boolean returnJson) {
        try {
            String response = chatWithSystem(systemPrompt, message);
            if (returnJson) {
                // 确保返回JSON格式
                if (!response.trim().startsWith("{") && !response.trim().startsWith("[")) {
                    response = "{\"result\":\"" + response.replace("\"", "\\\"") + "\"}";
                }
            }
            return response;
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
                // 将字符串列表历史转换为简单的合并消息
                String mergedMessage = messages.stream().filter(StrUtil::isNotBlank).collect(Collectors.joining("\n"));
                return currentStrategy.chat(mergedMessage);
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
    public String batchChat(String prompt) {
        return chat(prompt);
    }


    @Override
    public String tokenization(String[] texts) {
        return String.join(" ", texts);
    }

    @Override
    public String createContext(List<String> messages) {
        return Integer.toHexString(String.join("|", messages).hashCode());
    }

    @Override
    public String chatWithContext(String message, String contextId) {
        return chat("[ctx:" + contextId + "] " + message);
    }

    @Override
    public String getCurrentModel() {
        try {
            // 使用策略模式
            if (currentStrategy != null) {
                return currentStrategy.getModelName();
            }
            return "unknown";
        } catch (Exception e) {
            log.error("获取当前模型失败: {}", e.getMessage(), e);
            return "unknown";
        }
    }

    @Override
    public boolean switchModel(String modelType) {
        try {
            setCurrentAiType(modelType);
            return true;
        } catch (Exception e) {
            log.error("切换模型失败: {}", e.getMessage(), e);
            return false;
        }
    }


    @Override
    public void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 优先使用策略模式
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
            // 优先使用策略模式
            if (currentStrategy != null) {
                currentStrategy.streamChatWithSystem(systemPrompt, message, onToken, onComplete, onError);
            } else {
                onError.accept(new RuntimeException("流式聊天请求失败: 策略模式不可用"));
            }
        } catch (Exception e) {
            log.error("流式聊天请求失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("流式聊天请求失败: " + e.getMessage()));
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
