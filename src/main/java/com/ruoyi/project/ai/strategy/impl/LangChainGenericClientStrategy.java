package com.ruoyi.project.ai.strategy.impl;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.project.ai.strategy.AiClientStrategy;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * LangChain4j 通用客户端策略骨架
 * 后续可注入具体的聊天/视觉/embedding 客户端
 */
public class LangChainGenericClientStrategy implements AiClientStrategy {
    private static final Logger log = LoggerFactory.getLogger(LangChainGenericClientStrategy.class);

    private final String provider;
    private final String model;
    private final String endpoint;
    private final String apiKey;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final EmbeddingModel embeddingModel;

    public LangChainGenericClientStrategy(String provider, String model, String endpoint, String apiKey) {
        this.provider = provider;
        this.model = model;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.chatModel = buildChatModel();
        this.streamingChatModel = buildStreamingChatModel();
        this.embeddingModel = buildEmbeddingModel();
    }

    @Override
    public String chat(String message) {
        log.info("[LC4J-{}] chat with model {}", provider, model);
        try {
            String reply = chatModel != null ? chatModel.chat(message) : message;
            return reply;
        } catch (Exception e) {
            log.error("[LC4J-{}] chat error: {}", provider, e.getMessage(), e);
            throw new RuntimeException("聊天请求失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithSystem(String systemPrompt, String message, boolean returnJson) {
        try {
            String prompt = StrUtil.isNotBlank(systemPrompt)
                    ? (systemPrompt + "\n" + message)
                    : message;
            return chat(prompt);
        } catch (Exception e) {
            log.error("[LC4J-{}] chatWithSystem error: {}", provider, e.getMessage(), e);
            throw new RuntimeException("带系统提示的聊天请求失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithHistory(List<String> messages) {
        try {
            if (messages == null || messages.isEmpty()) {
                return chat("");
            }
            // 简化：将历史合并成一个输入，避免角色缺失导致的歧义
            String merged = messages.stream().filter(StrUtil::isNotBlank).collect(Collectors.joining("\n"));
            return chat(merged);
        } catch (Exception e) {
            log.error("[LC4J-{}] chatWithHistory error: {}", provider, e.getMessage(), e);
            throw new RuntimeException("多轮对话请求失败: " + e.getMessage());
        }
    }

    @Override
    public String chatVision(String message, List<String> imageUrls) {
        // 兼容实现：多数OpenAI兼容后端支持在文本中引用URL进行视觉理解
        // 更高级的多模态消息可后续接入：UserMessage.with(Text + ImageUrlContent)
        String urlPart = (imageUrls == null || imageUrls.isEmpty()) ? "" : ("\nImages:" + String.join(",", imageUrls));
        return chat(message + urlPart);
    }

    @Override
    public String generateImage(String prompt, String size, Double guidanceScale, Integer seed, Boolean watermark) {
        throw new UnsupportedOperationException("通用策略暂未实现 generateImage，用具体提供方扩展");
    }

    @Override
    public String embeddingText(String[] texts) {
        try {
            if (texts == null || texts.length == 0) {
                return "{\"model\":\"" + safe(getEmbeddingModelName()) + "\",\"vectors\":[]}";
            }
            List<List<Float>> vectors = new ArrayList<>();
            for (String t : texts) {
                var emb = embeddingModel.embed(t).content();
                // LangChain4j Embedding 返回的是 List<Float>
                vectors.add(emb.vectorAsList());
            }
            String vecJson = vectors.stream()
                    .map(vec -> vec.stream().map(f -> f == null ? "0" : stripTrailingZeros(f))
                            .collect(Collectors.joining(",", "[", "]")))
                    .collect(Collectors.joining(",", "[", "]"));
            return "{"
                    + "\"model\":\"" + safe(getEmbeddingModelName()) + "\"," 
                    + "\"vectors\":" + vecJson 
                    + "}";
        } catch (Exception e) {
            log.error("[LC4J-{}] embeddingText error: {}", provider, e.getMessage(), e);
            throw new RuntimeException("文本向量化请求失败: " + e.getMessage());
        }
    }

    @Override
    public String embeddingVision(String text, String imageUrl) {
        // OpenAI兼容多模态嵌入通常需要专用API，通用策略暂不直接支持
        throw new UnsupportedOperationException("通用策略暂未实现 embeddingVision，用具体提供方扩展");
    }

    @Override
    public String batchChat(String prompt) {
        return chat(prompt);
    }

    @Override
    public String createVideoTask(String prompt, String imageUrl) {
        throw new UnsupportedOperationException("通用策略暂未实现 createVideoTask");
    }

    @Override
    public String getVideoTaskStatus(String taskId) {
        throw new UnsupportedOperationException("通用策略暂未实现 getVideoTaskStatus");
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
    public String getModelName() {
        return model;
    }

    private ChatModel buildChatModel() {
        try {
            // 统一走 OpenAI 兼容接口；若 endpoint 非空，则使用自定义 baseUrl
            if (StrUtil.isNotBlank(endpoint)) {
                return OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .baseUrl(endpoint)
                        .build();
            } else {
                return OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .build();
            }
        } catch (Exception e) {
            log.warn("[LC4J-{}] buildChatModel failed: {}", provider, e.getMessage());
            return null;
        }
    }

    private StreamingChatModel buildStreamingChatModel() {
        try {
            // 统一走 OpenAI 兼容接口；若 endpoint 非空，则使用自定义 baseUrl
            if (StrUtil.isNotBlank(endpoint)) {
                return OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .baseUrl(endpoint)
                        .timeout(Duration.ofMinutes(5))
                        .logRequests(false)
                        .logResponses(true)
                        .customHeaders(Collections.singletonMap("Accept-Charset", "utf-8"))
                        .build();
            } else {
                return OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .timeout(Duration.ofMinutes(5))
                        .logRequests(false)
                        .logResponses(true)
                        .customHeaders(Collections.singletonMap("Accept-Charset", "utf-8"))
                        .build();
            }
        } catch (Exception e) {
            log.warn("[LC4J-{}] buildStreamingChatModel failed: {}", provider, e.getMessage());
            return null;
        }
    }

    private EmbeddingModel buildEmbeddingModel() {
        try {
            String embModelName = getEmbeddingModelName();
            if (StrUtil.isNotBlank(endpoint)) {
                return OpenAiEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .modelName(embModelName)
                        .baseUrl(endpoint)
                        .build();
            } else {
                return OpenAiEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .modelName(embModelName)
                        .build();
            }
        } catch (Exception e) {
            log.warn("[LC4J-{}] buildEmbeddingModel failed: {}", provider, e.getMessage());
            return null;
        }
    }

    private String getEmbeddingModelName() {
        // 如果配置的模型已是嵌入模型，直接使用；否则使用默认嵌入模型
        String m = StrUtil.emptyToDefault(model, "text-embedding-3-small");
        if (StrUtil.containsIgnoreCase(m, "embedding")) {
            return m;
        }
        return "text-embedding-3-small";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String stripTrailingZeros(Float f) {
        String str = String.valueOf(f);
        // 统一为尽量短的表示
        if (str.contains(".")) {
            // 去掉多余的0
            str = str.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return str;
    }

    @Override
    public void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        log.info("[LC4J-{}] streamChat with model {}", provider, model);
        try {
            if (streamingChatModel == null) {
                onError.accept(new RuntimeException("流式聊天模型未初始化"));
                return;
            }
            
            streamingChatModel.chat(message, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    // 确保字符编码正确处理
                    if (partialResponse != null) {
                        try {
                            // 检查是否存在编码问题，如果是乱码则尝试修复
                            String fixedResponse = fixEncodingIfNeeded(partialResponse);
                            onToken.accept(fixedResponse);
                        } catch (Exception e) {
                            log.warn("[LC4J-{}] 处理流式响应时出现编码问题: {}", provider, e.getMessage());
                            onToken.accept(partialResponse); // 降级处理，直接返回原始响应
                        }
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    onComplete.run();
                }
                
                @Override
                public void onError(Throwable error) {
                    onError.accept(error);
                }
            });
                    
        } catch (Exception e) {
            log.error("[LC4J-{}] streamChat error: {}", provider, e.getMessage(), e);
            onError.accept(new RuntimeException("流式聊天请求失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithSystem(String systemPrompt, String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        log.info("[LC4J-{}] streamChatWithSystem with model {}", provider, model);
        try {
            if (streamingChatModel == null) {
                onError.accept(new RuntimeException("流式聊天模型未初始化"));
                return;
            }
            
            String prompt = StrUtil.isNotBlank(systemPrompt)
                    ? (systemPrompt + "\n" + message)
                    : message;
            
            streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    // 确保字符编码正确处理
                    if (partialResponse != null) {
                        try {
                            // 检查是否存在编码问题，如果是乱码则尝试修复
                            String fixedResponse = fixEncodingIfNeeded(partialResponse);
                            onToken.accept(fixedResponse);
                        } catch (Exception e) {
                            log.warn("[LC4J-{}] 处理流式响应时出现编码问题: {}", provider, e.getMessage());
                            onToken.accept(partialResponse); // 降级处理，直接返回原始响应
                        }
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    onComplete.run();
                }
                
                @Override
                public void onError(Throwable error) {
                    onError.accept(error);
                }
            });
                    
        } catch (Exception e) {
            log.error("[LC4J-{}] streamChatWithSystem error: {}", provider, e.getMessage(), e);
            onError.accept(new RuntimeException("带系统提示的流式聊天请求失败: " + e.getMessage()));
        }
    }

    /**
     * 修复可能的编码问题
     * @param text 原始文本
     * @return 修复后的文本
     */
    private String fixEncodingIfNeeded(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // 检查是否包含常见的UTF-8编码错误字符
            if (text.contains("浣犲ソ") || text.contains("锛�") || text.contains("馃槉")) {
                // 尝试修复编码问题：先转为字节数组，再用UTF-8重新解码
                byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
                String fixed = new String(bytes, StandardCharsets.UTF_8);
                log.debug("[LC4J-{}] 检测到编码问题，已修复: {} -> {}", provider, text, fixed);
                return fixed;
            }
            
            // 检查是否包含其他可能的编码问题字符
            boolean hasEncodingIssue = false;
            for (char c : text.toCharArray()) {
                // 检查是否包含异常的Unicode字符范围
                if ((c >= 0x80 && c <= 0xFF) || (c >= 0x100 && c <= 0x17F)) {
                    hasEncodingIssue = true;
                    break;
                }
            }
            
            if (hasEncodingIssue) {
                // 尝试重新编码
                byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
                String fixed = new String(bytes, StandardCharsets.UTF_8);
                
                // 验证修复后的文本是否更合理（包含更多中文字符）
                long chineseCount = fixed.chars().filter(c -> c >= 0x4E00 && c <= 0x9FFF).count();
                long originalChineseCount = text.chars().filter(c -> c >= 0x4E00 && c <= 0x9FFF).count();
                
                if (chineseCount > originalChineseCount) {
                    log.debug("[LC4J-{}] 编码修复成功: {} -> {}", provider, text, fixed);
                    return fixed;
                }
            }
            
        } catch (Exception e) {
            log.warn("[LC4J-{}] 编码修复失败: {}", provider, e.getMessage());
        }
        
        return text; // 如果修复失败，返回原始文本
    }

    @Override
    public void streamChatWithModelConfig(String message, String systemPrompt, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        if (StrUtil.isNotBlank(systemPrompt)) {
            streamChatWithSystem(systemPrompt, message, onToken, onComplete, onError);
        } else {
            streamChat(message, onToken, onComplete, onError);
        }
    }
}