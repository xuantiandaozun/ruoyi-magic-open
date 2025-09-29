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
import com.ruoyi.project.ai.service.impl.HutoolAiServiceImpl;

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
    private HutoolAiServiceImpl aiService;
    
    /**
     * 聊天请求对象
     */
    public static class ChatRequest {
        private String message;
        private String systemPrompt;
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getSystemPrompt() {
            return systemPrompt;
        }
        
        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }
    }
    
    /**
     * 模型切换请求对象
     */
    public static class ModelSwitchRequest {
        private String modelType;
        
        public String getModelType() {
            return modelType;
        }
        
        public void setModelType(String modelType) {
            this.modelType = modelType;
        }
    }
    
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
            if (StrUtil.isNotBlank(request.getSystemPrompt())) {
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
    @SaCheckPermission("ai:chat:use")
    @PostMapping("/stream")
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时
        
        try {
            if (StrUtil.isBlank(request.getMessage())) {
                emitter.send(SseEmitter.event().name("error").data("消息内容不能为空"));
                emitter.complete();
                return emitter;
            }
            
            // 在新线程中处理AI请求，避免阻塞
            new Thread(() -> {
                try {
                    String response;
                    if (StrUtil.isNotBlank(request.getSystemPrompt())) {
                        response = aiService.chatWithSystem(request.getSystemPrompt(), request.getMessage());
                    } else {
                        response = aiService.chat(request.getMessage());
                    }
                    
                    // 模拟流式输出，将完整响应分块发送
                    String[] words = response.split("");
                    for (int i = 0; i < words.length; i++) {
                        emitter.send(SseEmitter.event().name("message").data(words[i]));
                        Thread.sleep(50); // 模拟打字效果
                    }
                    
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    logger.error("流式聊天请求失败: {}", e.getMessage(), e);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("AI聊天请求失败: " + e.getMessage()));
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                }
            }).start();
            
        } catch (Exception e) {
            logger.error("创建流式聊天失败: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
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