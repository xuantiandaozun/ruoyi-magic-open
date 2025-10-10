package com.ruoyi.project.ai.strategy.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.project.ai.strategy.AiClientStrategy;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.service.ArkService;

import cn.hutool.core.util.StrUtil;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

/**
 * 豆包SDK策略实现（使用火山引擎 ArkService）
 */
public class DoubaoSdkClientStrategy implements AiClientStrategy {

    private static final Logger log = LoggerFactory.getLogger(DoubaoSdkClientStrategy.class);

    private final String apiKey;
    private final String endpoint; // ArkService 内部使用默认域名，保留以备扩展
    private final String model;
    private final String imageModel;

    public DoubaoSdkClientStrategy(String apiKey, String endpoint, String model, String imageModel) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.imageModel = imageModel;
    }

    private ArkService buildService() {
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        return ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String chat(String message) {
        try {
            ArkService service = buildService();
            List<ChatMessage> chatMessages = new ArrayList<>();
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(message)
                    .build();
            chatMessages.add(userMessage);
            
            ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages);
            return service.createChatCompletion(builder.build())
                    .getChoices().get(0).getMessage().getContent().toString();
        } catch (Exception e) {
            log.error("豆包SDK chat 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("豆包SDK chat 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithSystem(String systemPrompt, String message, boolean returnJson) {
        try {
            ArkService service = buildService();
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
            ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messagesForReqList);
            return service.createChatCompletion(builder.build())
                    .getChoices().get(0).getMessage().getContent().toString();
        } catch (Exception e) {
            log.error("豆包SDK chatWithSystem 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("豆包SDK chatWithSystem 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithHistory(List<String> messages) {
        try {
            ArkService service = buildService();
            List<ChatMessage> chatMessages = new ArrayList<>();
            for (String msg : messages) {
                chatMessages.add(ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(msg)
                        .build());
            }
            ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages);
            return service.createChatCompletion(builder.build())
                    .getChoices().get(0).getMessage().getContent().toString();
        } catch (Exception e) {
            log.error("豆包SDK chatWithHistory 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("豆包SDK chatWithHistory 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatVision(String message, List<String> imageUrls) {
        // Ark SDK 目前图文能力通常通过 Hutool DoubaoService；保留占位，直接串联文本返回
        return chat(message);
    }

    @Override
    public String generateImage(String prompt, String size, Double guidanceScale, Integer seed, Boolean watermark) {
        try {
            ArkService service = buildService();
            GenerateImagesRequest.Builder requestBuilder = GenerateImagesRequest.builder()
                    .model(StrUtil.isNotBlank(imageModel) ? imageModel : model)
                    .prompt(prompt);
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
            ImagesResponse response = service.generateImages(requestBuilder.build());
            return response.getData().get(0).getUrl();
        } catch (Exception e) {
            log.error("豆包SDK generateImage 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("豆包SDK generateImage 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String embeddingText(String[] texts) {
        // Ark SDK 未直接提供，返回占位或交由通用实现
        throw new UnsupportedOperationException("豆包SDK暂不支持 embeddingText，请使用通用实现");
    }

    @Override
    public String embeddingVision(String text, String imageUrl) {
        throw new UnsupportedOperationException("豆包SDK暂不支持 embeddingVision，请使用通用实现");
    }

    @Override
    public String batchChat(String prompt) {
        // 可复用 chat
        return chat(prompt);
    }

    @Override
    public String createVideoTask(String prompt, String imageUrl) {
        // 暂未统一封装，返回占位
        throw new UnsupportedOperationException("豆包SDK暂不支持 createVideoTask 封装，请保留原实现或使用通用实现");
    }

    @Override
    public String getVideoTaskStatus(String taskId) {
        throw new UnsupportedOperationException("豆包SDK暂不支持 getVideoTaskStatus 封装，请保留原实现或使用通用实现");
    }

    @Override
    public String tokenization(String[] texts) {
        // 简单分词占位
        return String.join(" ", texts);
    }

    @Override
    public String createContext(List<String> messages) {
        // 简易上下文占位：返回拼接hash
        return Integer.toHexString(String.join("|", messages).hashCode());
    }

    @Override
    public String chatWithContext(String message, String contextId) {
        // 简易占位：忽略context，直接chat
        return chat(message);
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            ArkService service = buildService();
            List<ChatMessage> chatMessages = new ArrayList<>();
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(message)
                    .build();
            chatMessages.add(userMessage);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages)
                    .stream(true) // 启用流式响应
                    .build();
            
            // 使用豆包SDK的流式接口
            service.streamChatCompletion(request)
                    .doOnNext(chunk -> {
                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            String content = chunk.getChoices().get(0).getMessage().getContent().toString();
                            if (content != null && !content.isEmpty()) {
                                onToken.accept(content);
                            }
                        }
                    })
                    .doOnComplete(() -> onComplete.run())
                    .doOnError(throwable -> onError.accept(throwable))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("豆包SDK streamChat 调用失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("豆包SDK streamChat 调用失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithSystem(String systemPrompt, String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            ArkService service = buildService();
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
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messagesForReqList)
                    .stream(true) // 启用流式响应
                    .build();
            
            // 使用豆包SDK的流式接口
            service.streamChatCompletion(request)
                    .doOnNext(chunk -> {
                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            String content = chunk.getChoices().get(0).getMessage().getContent().toString();
                            if (content != null && !content.isEmpty()) {
                                onToken.accept(content);
                            }
                        }
                    })
                    .doOnComplete(() -> onComplete.run())
                    .doOnError(throwable -> onError.accept(throwable))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("豆包SDK streamChatWithSystem 调用失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("豆包SDK streamChatWithSystem 调用失败: " + e.getMessage()));
        }
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