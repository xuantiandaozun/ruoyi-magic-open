package com.ruoyi.project.ai.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.ai.dto.ChatRequest;
import com.ruoyi.project.ai.dto.ModelSwitchRequest;
import com.ruoyi.project.ai.service.impl.AiServiceImpl;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI聊天控制器
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Tag(name = "AI聊天")
@RestController
@RequestMapping("/ai/chat")
public class AiChatController extends BaseController {
    
    @Autowired
    private AiServiceImpl aiService;
    
    /**
     * 基础聊天接口
     */
    @Operation(summary = "基础聊天")
    @SaCheckPermission("ai:chat:use")
    @PostMapping
    public AjaxResult chat(@RequestBody ChatRequest request) {
        try {
            if (StrUtil.isBlank(request.getMessage())) {
                return error("消息内容不能为空");
            }
            
            String response;
            // 如果指定了模型配置ID，使用指定的模型配置
            if (request.getModelConfigId() != null) {
                response = aiService.chatWithModelConfig(request.getMessage(), request.getSystemPrompt(), request.getModelConfigId());
            } else if (StrUtil.isNotBlank(request.getSystemPrompt())) {
                response = aiService.chatWithSystem(request.getSystemPrompt(), request.getMessage());
            } else {
                response = aiService.chat(request.getMessage());
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", response);
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
        } catch (Exception e) {
            logger.error("AI聊天请求失败: {}", e.getMessage(), e);
            return error("AI聊天请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 流式聊天接口
     */
    @Operation(summary = "流式聊天")
    @PostMapping("/stream")
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        try {
            if (StrUtil.isBlank(request.getMessage())) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("type", "error");
                errorData.put("content", "消息内容不能为空");
                emitter.send(SseEmitter.event().data(errorData));
                emitter.complete();
                return emitter;
            }
            
            // 设置SSE响应头
            emitter.onCompletion(() -> logger.info("SSE连接完成"));
            emitter.onTimeout(() -> {
                logger.warn("SSE连接超时");
                emitter.complete();
            });
            emitter.onError((ex) -> {
                logger.error("SSE连接错误: {}", ex.getMessage(), ex);
                emitter.completeWithError(ex);
            });
            
            // 使用真正的流式接口，实时发送token
            // 如果指定了模型配置ID，使用指定的模型配置
            if (request.getModelConfigId() != null) {
                aiService.streamChatWithModelConfig(
                    request.getMessage(), 
                    request.getSystemPrompt(), 
                    request.getModelConfigId(),
                    // onToken: 接收到每个token时立即发送
                    token -> {
                        try {
                            Map<String, Object> messageData = new HashMap<>();
                            messageData.put("type", "message");
                            messageData.put("content", token);
                            emitter.send(SseEmitter.event().data(messageData));
                        } catch (Exception e) {
                            logger.error("发送SSE token失败: {}", e.getMessage(), e);
                        }
                    },
                    // onComplete: 完成时发送结束信号
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            emitter.complete();
                        } catch (Exception e) {
                            logger.error("发送SSE完成信号失败: {}", e.getMessage(), e);
                            emitter.completeWithError(e);
                        }
                    },
                    // onError: 出错时发送错误信息
                    error -> {
                        logger.error("流式聊天请求失败: {}", error.getMessage(), error);
                        try {
                            Map<String, Object> errorData = new HashMap<>();
                            errorData.put("type", "error");
                            errorData.put("content", "AI聊天请求失败: " + error.getMessage());
                            emitter.send(SseEmitter.event().data(errorData));
                            emitter.complete();
                        } catch (Exception ex) {
                            emitter.completeWithError(ex);
                        }
                    }
                );
            } else if (StrUtil.isNotBlank(request.getSystemPrompt())) {
                aiService.streamChatWithSystem(
                    request.getSystemPrompt(), 
                    request.getMessage(),
                    // onToken: 接收到每个token时立即发送
                    token -> {
                        try {
                            Map<String, Object> messageData = new HashMap<>();
                            messageData.put("type", "message");
                            messageData.put("content", token);
                            emitter.send(SseEmitter.event().data(messageData));
                        } catch (Exception e) {
                            logger.error("发送SSE token失败: {}", e.getMessage(), e);
                        }
                    },
                    // onComplete: 完成时发送结束信号
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            emitter.complete();
                        } catch (Exception e) {
                            logger.error("发送SSE完成信号失败: {}", e.getMessage(), e);
                            emitter.completeWithError(e);
                        }
                    },
                    // onError: 出错时发送错误信息
                    error -> {
                        logger.error("流式聊天请求失败: {}", error.getMessage(), error);
                        try {
                            Map<String, Object> errorData = new HashMap<>();
                            errorData.put("type", "error");
                            errorData.put("content", "AI聊天请求失败: " + error.getMessage());
                            emitter.send(SseEmitter.event().data(errorData));
                            emitter.complete();
                        } catch (Exception ex) {
                            emitter.completeWithError(ex);
                        }
                    }
                );
            } else {
                aiService.streamChat(
                    request.getMessage(),
                    // onToken: 接收到每个token时立即发送
                    token -> {
                        try {
                            Map<String, Object> messageData = new HashMap<>();
                            messageData.put("type", "message");
                            messageData.put("content", token);
                            emitter.send(SseEmitter.event().data(messageData));
                        } catch (Exception e) {
                            logger.error("发送SSE token失败: {}", e.getMessage(), e);
                        }
                    },
                    // onComplete: 完成时发送结束信号
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            emitter.complete();
                        } catch (Exception e) {
                            logger.error("发送SSE完成信号失败: {}", e.getMessage(), e);
                            emitter.completeWithError(e);
                        }
                    },
                    // onError: 出错时发送错误信息
                    error -> {
                        logger.error("流式聊天请求失败: {}", error.getMessage(), error);
                        try {
                            Map<String, Object> errorData = new HashMap<>();
                            errorData.put("type", "error");
                            errorData.put("content", "AI聊天请求失败: " + error.getMessage());
                            emitter.send(SseEmitter.event().data(errorData));
                            emitter.complete();
                        } catch (Exception ex) {
                            emitter.completeWithError(ex);
                        }
                    }
                );
            }
            
        } catch (Exception e) {
            logger.error("创建流式聊天失败: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * 将响应文本分割成合适的块
     */
    /**
     * 获取当前AI模型信息
     */
    @Operation(summary = "获取当前AI模型信息")
    @SaCheckPermission("ai:chat:query")
    @GetMapping("/model/current")
    public AjaxResult getCurrentModel() {
        try {
            Map<String, Object> modelInfo = new HashMap<>();
            modelInfo.put("currentType", aiService.getCurrentAiType());
            modelInfo.put("availableTypes", new String[]{"DOUBAO", "OPENAI", "DEEPSEEK"});
            modelInfo.put("timestamp", System.currentTimeMillis());
            
            return success(modelInfo);
        } catch (Exception e) {
            logger.error("获取AI模型信息失败: {}", e.getMessage(), e);
            return error("获取AI模型信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 切换AI模型
     */
    @Operation(summary = "切换AI模型")
    @SaCheckPermission("ai:chat:config")
    @PostMapping("/model/switch")
    public AjaxResult switchModel(@RequestBody ModelSwitchRequest request) {
        try {
            if (StrUtil.isBlank(request.getModelType())) {
                return error("模型类型不能为空");
            }
            
            String modelType = request.getModelType().toUpperCase();
            if (!"DOUBAO".equals(modelType) && !"OPENAI".equals(modelType) && !"DEEPSEEK".equals(modelType)) {
                return error("不支持的模型类型: " + modelType);
            }
            
            aiService.setCurrentAiType(modelType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("currentType", modelType);
            result.put("message", "模型切换成功");
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
        } catch (Exception e) {
            logger.error("切换AI模型失败: {}", e.getMessage(), e);
            return error("切换AI模型失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试AI连接
     */
    @Operation(summary = "测试AI连接")
    @SaCheckPermission("ai:chat:test")
    @GetMapping("/test")
    public AjaxResult testConnection(@RequestParam(defaultValue = "你好") String message) {
        try {
            String response = aiService.chat(message);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "AI连接正常");
            result.put("response", response);
            result.put("currentType", aiService.getCurrentAiType());
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
        } catch (Exception e) {
            logger.error("AI连接测试失败: {}", e.getMessage(), e);
            return error("AI连接测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取聊天历史记录
     */
    @Operation(summary = "获取聊天历史记录")
    @SaCheckPermission("ai:chat:history")
    @GetMapping("/history")
    public AjaxResult getChatHistory() {
        try {
            // 这里暂时返回空数组，实际项目中可以从数据库获取历史记录
            // 可以根据用户ID获取该用户的聊天历史
            return success(new java.util.ArrayList<>());
        } catch (Exception e) {
            logger.error("获取聊天历史失败: {}", e.getMessage(), e);
            return error("获取聊天历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空聊天历史记录
     */
    @Operation(summary = "清空聊天历史记录")
    @SaCheckPermission("ai:chat:history")
    @DeleteMapping("/history")
    public AjaxResult clearChatHistory() {
        try {
            // 这里暂时返回成功，实际项目中可以清空数据库中的历史记录
            // 可以根据用户ID清空该用户的聊天历史
            return success("聊天历史已清空");
        } catch (Exception e) {
            logger.error("清空聊天历史失败: {}", e.getMessage(), e);
            return error("清空聊天历史失败: " + e.getMessage());
        }
    }
}