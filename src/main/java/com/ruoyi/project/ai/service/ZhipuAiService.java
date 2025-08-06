package com.ruoyi.project.ai.service;


import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.core.Constants;
import ai.z.openapi.service.embedding.EmbeddingCreateParams;
import ai.z.openapi.service.embedding.EmbeddingResponse;
import ai.z.openapi.service.image.CreateImageRequest;
import ai.z.openapi.service.image.ImageResponse;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import ai.z.openapi.service.model.ChatTool;
import jakarta.annotation.PostConstruct;

@Service
public class ZhipuAiService {

    @Value("${ai.zhipu.api-key}")
    private String apiKey;

    private ZhipuAiClient client;

    @PostConstruct
    public void init() {
        this.client = ZhipuAiClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * 基础聊天对话
     */
    public String chat(String message) {
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(Constants.ModelChatGLM4_5)
                .messages(Arrays.asList(
                        ChatMessage.builder()
                                .role(ChatMessageRole.USER.value())
                                .content(message)
                                .build()
                ))
                .build();

        ChatCompletionResponse response = client.chat().createChatCompletion(request);
        if (response.isSuccess()) {
            return response.getData().getChoices().get(0).getMessage().getContent().toString();
        } else {
            throw new RuntimeException("聊天请求失败: " + response.getMsg());
        }
    }

    /**
     * 带系统消息的聊天
     */
    public String chatWithSystem(String systemMessage, String userMessage) {
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(Constants.ModelChatGLM4_5)
                .messages(Arrays.asList(
                        ChatMessage.builder()
                                .role(ChatMessageRole.SYSTEM.value())
                                .content(systemMessage)
                                .build(),
                        ChatMessage.builder()
                                .role(ChatMessageRole.USER.value())
                                .content(userMessage)
                                .build()
                ))
                .build();

        ChatCompletionResponse response = client.chat().createChatCompletion(request);
        if (response.isSuccess()) {
            return response.getData().getChoices().get(0).getMessage().getContent().toString();
        } else {
            throw new RuntimeException("聊天请求失败: " + response.getMsg());
        }
    }

    /**
     * 多轮对话
     */
    public String chatWithHistory(List<ChatMessage> conversationHistory) {
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(Constants.ModelChatGLM4_5)
                .messages(conversationHistory)
                .build();

        ChatCompletionResponse response = client.chat().createChatCompletion(request);
        if (response.isSuccess()) {
            return response.getData().getChoices().get(0).getMessage().getContent().toString();
        } else {
            throw new RuntimeException("聊天请求失败: " + response.getMsg());
        }
    }

    /**
     * 流式聊天
     */
    public ChatCompletionResponse chatStream(String message) {
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(Constants.ModelChatGLM4_5)
                .messages(Arrays.asList(
                        ChatMessage.builder()
                                .role(ChatMessageRole.USER.value())
                                .content(message)
                                .build()
                ))
                .stream(true)
                .build();

        return client.chat().createChatCompletion(request);
    }

    /**
     * 函数调用
     */
    public ChatCompletionResponse chatWithTools(String message, List<ChatTool> tools) {
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(Constants.ModelChatGLM4_5)
                .messages(Arrays.asList(
                        ChatMessage.builder()
                                .role(ChatMessageRole.USER.value())
                                .content(message)
                                .build()
                ))
                .tools(tools)
                .toolChoice("auto")
                .build();

        return client.chat().createChatCompletion(request);
    }

    /**
     * 图像理解
     */
    public String analyzeImage(String imageUrl, String question) {
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(Constants.ModelChatGLM4V)
                .messages(Arrays.asList(
                        ChatMessage.builder()
                                .role(ChatMessageRole.USER.value())
                                .content(question)
                                .build()
                ))
                .build();

        ChatCompletionResponse response = client.chat().createChatCompletion(request);
        if (response.isSuccess()) {
            return response.getData().getChoices().get(0).getMessage().getContent().toString();
        } else {
            throw new RuntimeException("图像分析失败: " + response.getMsg());
        }
    }

    /**
     * 图像生成
     */
    public String generateImage(String prompt) {
        CreateImageRequest request = CreateImageRequest.builder()
                .model(Constants.ModelCogView3)
                .prompt(prompt)
                .size("1024x1024")
                .build();

        ImageResponse response = client.images().createImage(request);
        if (response.isSuccess()) {
            return response.getData().getData().get(0).getUrl();
        } else {
            throw new RuntimeException("图像生成失败: " + response.getMsg());
        }
    }

    /**
     * 文本向量嵌入
     */
    public EmbeddingResponse createEmbedding(List<String> texts) {
        EmbeddingCreateParams request = EmbeddingCreateParams.builder()
                .model(Constants.ModelEmbedding2)
                .input(texts)
                .build();

        EmbeddingResponse response = client.embeddings().createEmbeddings(request);
        if (response.isSuccess()) {
            return response;
        } else {
            throw new RuntimeException("文本嵌入失败: " + response.getMsg());
        }
    }

    /**
     * 创建聊天消息
     */
    public ChatMessage createChatMessage(String role, String content) {
        return ChatMessage.builder()
                .role(role)
                .content(content)
                .build();
    }

    /**
     * 创建用户消息
     */
    public ChatMessage createUserMessage(String content) {
        return createChatMessage(ChatMessageRole.USER.value(), content);
    }

    /**
     * 创建助手消息
     */
    public ChatMessage createAssistantMessage(String content) {
        return createChatMessage(ChatMessageRole.ASSISTANT.value(), content);
    }

    /**
     * 创建系统消息
     */
    public ChatMessage createSystemMessage(String content) {
        return createChatMessage(ChatMessageRole.SYSTEM.value(), content);
    }
}