package com.ruoyi.project.ai.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.ai.domain.AiChatMessage;
import com.ruoyi.project.ai.domain.AiChatSession;
import com.ruoyi.project.ai.dto.ChatRequest;
import com.ruoyi.project.ai.dto.ModelSwitchRequest;
import com.ruoyi.project.ai.service.IAiChatMessageService;
import com.ruoyi.project.ai.service.IAiChatSessionService;
import com.ruoyi.project.ai.service.impl.AiServiceImpl;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
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
    
    @Autowired
    private IAiChatSessionService aiChatSessionService;
    
    @Autowired
    private IAiChatMessageService aiChatMessageService;
    
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
            
            Long userId = StpUtil.getLoginIdAsLong();
            AiChatSession session = null;
            
            // 处理会话
            if (request.getSessionId() != null) {
                // 使用指定会话
                session = aiChatSessionService.getById(request.getSessionId());
                if (session == null || !session.getUserId().equals(userId)) {
                    return error("会话不存在或无权限访问");
                }
            } else {
                // 创建或获取默认会话
                session = aiChatSessionService.getOrCreateDefaultSession(userId, request.getModelConfigId());
            }
            
            // 保存用户消息
            AiChatMessage userMessage = new AiChatMessage();
            userMessage.setSessionId(session.getId());
            userMessage.setMessageRole("user");
            userMessage.setMessageContent(request.getMessage());
            userMessage.setMessageType("text");
            userMessage.setModelConfigId(request.getModelConfigId());
            userMessage.setCreateBy(String.valueOf(userId));
            aiChatMessageService.save(userMessage);
            
            // 获取聊天历史（如果没有提供）
            List<AiChatMessage> chatHistory = request.getChatHistory();
            if (chatHistory == null || chatHistory.isEmpty()) {
                chatHistory = aiChatMessageService.buildMessageHistory(session.getId(), false); // 获取聊天历史，不包含系统消息
            }
            
            String response;
            if (chatHistory != null && !chatHistory.isEmpty() && request.getModelConfigId() != null) {
                // 使用聊天历史和指定模型配置
                response = aiService.chatWithHistory(request.getMessage(), request.getSystemPrompt(), chatHistory, request.getModelConfigId());
            } else if (request.getModelConfigId() != null) {
                // 使用指定模型配置
                response = aiService.chatWithModelConfig(request.getMessage(), request.getSystemPrompt(), request.getModelConfigId());
            } else if (StrUtil.isNotBlank(request.getSystemPrompt())) {
                // 使用系统提示
                response = aiService.chatWithSystem(request.getSystemPrompt(), request.getMessage());
            } else {
                // 基础聊天
                response = aiService.chat(request.getMessage());
            }
            
            // 保存AI回复消息
            AiChatMessage assistantMessage = new AiChatMessage();
            assistantMessage.setSessionId(session.getId());
            assistantMessage.setMessageRole("assistant");
            assistantMessage.setMessageContent(response);
            assistantMessage.setMessageType("text");
            assistantMessage.setModelConfigId(request.getModelConfigId());
            assistantMessage.setCreateBy(String.valueOf(userId));
            aiChatMessageService.save(assistantMessage);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", response);
            result.put("sessionId", session.getId());
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
        // 增加超时时间到10分钟，避免长时间对话被中断
        SseEmitter emitter = new SseEmitter(600000L); // 10分钟超时
        
        // 连接状态管理
        final boolean[] isCompleted = {false};
        final Object completionLock = new Object();
        
        // 会话和消息管理
        final AiChatSession[] sessionHolder = {null};
        final AiChatMessage[] userMessageHolder = {null};
        
        try {
            if (StrUtil.isBlank(request.getMessage())) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("type", "error");
                errorData.put("content", "消息内容不能为空");
                emitter.send(SseEmitter.event().data(errorData));
                safeComplete(emitter, isCompleted, completionLock);
                return emitter;
            }
            
            // 设置SSE响应头和事件处理器
            emitter.onCompletion(() -> {
                synchronized (completionLock) {
                    if (!isCompleted[0]) {
                        isCompleted[0] = true;
                        logger.info("SSE连接正常完成");
                    }
                }
            });
            
            emitter.onTimeout(() -> {
                synchronized (completionLock) {
                    if (!isCompleted[0]) {
                        isCompleted[0] = true;
                        logger.warn("SSE连接超时，尝试发送超时信号");
                        try {
                            Map<String, Object> timeoutData = new HashMap<>();
                            timeoutData.put("type", "error");
                            timeoutData.put("content", "连接超时，请重新发起对话");
                            emitter.send(SseEmitter.event().data(timeoutData));
                        } catch (Exception e) {
                            logger.error("发送超时信号失败: {}", e.getMessage());
                        }
                        safeComplete(emitter, isCompleted, completionLock);
                    }
                }
            });
            
            emitter.onError((ex) -> {
                synchronized (completionLock) {
                    if (!isCompleted[0]) {
                        isCompleted[0] = true;
                        logger.error("SSE连接错误: {}", ex.getMessage(), ex);
                        // 不要在这里调用completeWithError，因为连接可能已经断开
                        // 只记录错误日志
                    }
                }
            });
            
            // 处理会话管理
            Long userId = StpUtil.getLoginIdAsLong();
            AiChatSession session = null;
            
            if (request.getSessionId() != null) {
                // 使用指定会话
                session = aiChatSessionService.getById(request.getSessionId());
                if (session == null || !session.getUserId().equals(userId)) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("type", "error");
                    errorData.put("content", "会话不存在或无权限访问");
                    emitter.send(SseEmitter.event().data(errorData));
                    safeComplete(emitter, isCompleted, completionLock);
                    return emitter;
                }
            } else {
                // 创建或获取默认会话
                session = aiChatSessionService.getOrCreateDefaultSession(userId, request.getModelConfigId());
            }
            sessionHolder[0] = session;
            
            // 保存用户消息
            AiChatMessage userMessage = new AiChatMessage();
            userMessage.setSessionId(session.getId());
            userMessage.setMessageRole("user");
            userMessage.setMessageContent(request.getMessage());
            userMessage.setMessageType("text");
            userMessage.setModelConfigId(request.getModelConfigId());
            userMessage.setCreateBy(String.valueOf(userId));
            aiChatMessageService.save(userMessage);
            userMessageHolder[0] = userMessage;
            
            // 获取聊天历史（如果没有提供）
            List<AiChatMessage> chatHistory = request.getChatHistory();
            if (chatHistory == null || chatHistory.isEmpty()) {
                chatHistory = aiChatMessageService.buildMessageHistory(session.getId(), false); // 获取聊天历史，不包含系统消息
            }
            request.setChatHistory(chatHistory);
            
            // 使用真正的流式接口，实时发送token
            // 如果指定了模型配置ID，使用指定的模型配置
            if (request.getChatHistory() != null && !request.getChatHistory().isEmpty() && request.getModelConfigId() != null) {
                // 使用聊天历史和指定模型配置
                aiService.streamChatWithHistory(
                    request.getMessage(),
                    request.getSystemPrompt(),
                    request.getChatHistory(),
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
                    // onToolCall: 工具调用时发送工具调用信息
                    (toolName, toolArgs) -> {
                        try {
                            Map<String, Object> toolCallData = new HashMap<>();
                            toolCallData.put("type", "tool_call");
                            toolCallData.put("toolName", toolName);
                            toolCallData.put("toolArgs", toolArgs);
                            emitter.send(SseEmitter.event().data(toolCallData));
                        } catch (Exception e) {
                            logger.error("发送SSE工具调用失败: {}", e.getMessage(), e);
                        }
                    },
                    // onToolResult: 工具执行结果时发送结果信息
                    (toolName, result) -> {
                        try {
                            Map<String, Object> toolResultData = new HashMap<>();
                            toolResultData.put("type", "tool_result");
                            toolResultData.put("toolName", toolName);
                            toolResultData.put("result", result);
                            emitter.send(SseEmitter.event().data(toolResultData));
                        } catch (Exception e) {
                            logger.error("发送SSE工具结果失败: {}", e.getMessage(), e);
                        }
                    },
                    // onComplete: 完成时发送结束信号
                    () -> {
                        try {
                            // 发送会话信息
                            Map<String, Object> sessionData = new HashMap<>();
                            sessionData.put("type", "session");
                            sessionData.put("sessionId", sessionHolder[0] != null ? sessionHolder[0].getId() : null);
                            emitter.send(SseEmitter.event().data(sessionData));
                            
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            safeComplete(emitter, isCompleted, completionLock);
                        } catch (Exception e) {
                            logger.error("发送SSE完成信号失败: {}", e.getMessage(), e);
                            safeComplete(emitter, isCompleted, completionLock);
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
                            safeComplete(emitter, isCompleted, completionLock);
                        } catch (Exception ex) {
                            logger.error("发送SSE错误信息失败: {}", ex.getMessage(), ex);
                            safeComplete(emitter, isCompleted, completionLock);
                        }
                    }
                );
            } else if (request.getModelConfigId() != null) {
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
                    // onToolCall: 工具调用时发送工具调用信息
                    (toolName, toolArgs) -> {
                        try {
                            Map<String, Object> toolCallData = new HashMap<>();
                            toolCallData.put("type", "tool_call");
                            toolCallData.put("toolName", toolName);
                            toolCallData.put("toolArgs", toolArgs);
                            emitter.send(SseEmitter.event().data(toolCallData));
                        } catch (Exception e) {
                            logger.error("发送SSE工具调用失败: {}", e.getMessage(), e);
                        }
                    },
                    // onToolResult: 工具执行结果时发送结果信息
                    (toolName, result) -> {
                        try {
                            Map<String, Object> toolResultData = new HashMap<>();
                            toolResultData.put("type", "tool_result");
                            toolResultData.put("toolName", toolName);
                            toolResultData.put("result", result);
                            emitter.send(SseEmitter.event().data(toolResultData));
                        } catch (Exception e) {
                            logger.error("发送SSE工具结果失败: {}", e.getMessage(), e);
                        }
                    },
                    // onComplete: 完成时发送结束信号
                    () -> {
                        try {
                            // 发送会话信息
                            Map<String, Object> sessionData = new HashMap<>();
                            sessionData.put("type", "session");
                            sessionData.put("sessionId", sessionHolder[0] != null ? sessionHolder[0].getId() : null);
                            emitter.send(SseEmitter.event().data(sessionData));
                            
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            safeComplete(emitter, isCompleted, completionLock);
                        } catch (Exception e) {
                            logger.error("发送SSE完成信号失败: {}", e.getMessage(), e);
                            safeComplete(emitter, isCompleted, completionLock);
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
                            safeComplete(emitter, isCompleted, completionLock);
                        } catch (Exception ex) {
                            logger.error("发送SSE错误信息失败: {}", ex.getMessage(), ex);
                            safeComplete(emitter, isCompleted, completionLock);
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
                            // 发送会话信息
                            Map<String, Object> sessionData = new HashMap<>();
                            sessionData.put("type", "session");
                            sessionData.put("sessionId", sessionHolder[0] != null ? sessionHolder[0].getId() : null);
                            emitter.send(SseEmitter.event().data(sessionData));
                            
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            safeComplete(emitter, isCompleted, completionLock);
                        } catch (Exception e) {
                            logger.error("发送SSE完成信号失败: {}", e.getMessage(), e);
                            safeComplete(emitter, isCompleted, completionLock);
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
                            safeComplete(emitter, isCompleted, completionLock);
                        } catch (Exception ex) {
                            logger.error("发送SSE错误信息失败: {}", ex.getMessage(), ex);
                            safeComplete(emitter, isCompleted, completionLock);
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
                            
                            // 发送会话信息
                            Map<String, Object> sessionData = new HashMap<>();
                            sessionData.put("type", "session");
                            sessionData.put("sessionId", sessionHolder[0] != null ? sessionHolder[0].getId() : null);
                            emitter.send(SseEmitter.event().data(sessionData));
                            
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            safeComplete(emitter, isCompleted, completionLock);
                        } catch (Exception e) {
                            logger.error("发送SSE完成信号失败: {}", e.getMessage(), e);
                            safeComplete(emitter, isCompleted, completionLock);
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
                            safeComplete(emitter, isCompleted, completionLock);
                        } catch (Exception ex) {
                            logger.error("发送SSE错误信息失败: {}", ex.getMessage(), ex);
                            safeComplete(emitter, isCompleted, completionLock);
                        }
                    }
                );
            }
            
        } catch (Exception e) {
            logger.error("创建流式聊天失败: {}", e.getMessage(), e);
            safeComplete(emitter, isCompleted, completionLock);
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
     * 获取聊天会话列表
     */
    @Operation(summary = "获取聊天会话列表")
    @SaCheckPermission("ai:chat:history")
    @GetMapping("/sessions")
    public TableDataInfo getChatSessions(AiChatSession aiChatSession) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        Long userId = StpUtil.getLoginIdAsLong();
        aiChatSession.setUserId(userId);
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(aiChatSession);
        
        // 使用 MyBatisFlex 的分页方法
        Page<AiChatSession> page = aiChatSessionService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }
    
    /**
     * 获取会话详情
     */
    @Operation(summary = "获取会话详情")
    @SaCheckPermission("ai:chat:history")
    @GetMapping("/sessions/{sessionId}")
    public AjaxResult getChatSession(@PathVariable Long sessionId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            AiChatSession session = aiChatSessionService.getById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                return error("会话不存在或无权限访问");
            }
            return success(session);
        } catch (Exception e) {
            logger.error("获取会话详情失败: {}", e.getMessage(), e);
            return error("获取会话详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取会话消息列表
     */
    @Operation(summary = "获取会话消息列表")
    @SaCheckPermission("ai:chat:history")
    @GetMapping("/sessions/{sessionId}/messages")
    public TableDataInfo getChatMessages(@PathVariable Long sessionId, AiChatMessage aiChatMessage) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            // 验证会话权限
            AiChatSession session = aiChatSessionService.getById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                TableDataInfo dataInfo = new TableDataInfo();
                dataInfo.setCode(200);
                dataInfo.setMsg("查询成功");
                dataInfo.setRows(new java.util.ArrayList<>());
                dataInfo.setTotal(0);
                return dataInfo;
            }
            
            // 获取分页参数
            PageDomain pageDomain = TableSupport.buildPageRequest();
            Integer pageNum = pageDomain.getPageNum();
            Integer pageSize = pageDomain.getPageSize();
            
            // 设置查询条件
            aiChatMessage.setSessionId(sessionId);
            
            // 构建查询条件
            QueryWrapper queryWrapper = QueryWrapper.create();
            queryWrapper.eq("session_id", sessionId);
            
            if (StrUtil.isNotEmpty(aiChatMessage.getMessageRole())) {
                queryWrapper.eq("message_role", aiChatMessage.getMessageRole());
            }
            if (StrUtil.isNotEmpty(aiChatMessage.getMessageType())) {
                queryWrapper.eq("message_type", aiChatMessage.getMessageType());
            }
            if (StrUtil.isNotEmpty(aiChatMessage.getIsDeleted())) {
                queryWrapper.eq("is_deleted", aiChatMessage.getIsDeleted());
            } else {
                // 默认只查询未删除的消息
                queryWrapper.eq("is_deleted", "0");
            }
            
            queryWrapper.orderBy("message_order", true);
            
            // 使用 MyBatis-Flex 的分页方法
            Page<AiChatMessage> page = aiChatMessageService.page(new Page<>(pageNum, pageSize), queryWrapper);
            return getDataTable(page);
        } catch (Exception e) {
            logger.error("获取会话消息失败: {}", e.getMessage(), e);
            TableDataInfo dataInfo = new TableDataInfo();
            dataInfo.setCode(500);
            dataInfo.setMsg("查询失败: " + e.getMessage());
            dataInfo.setRows(new java.util.ArrayList<>());
            dataInfo.setTotal(0);
            return dataInfo;
        }
    }
    
    /**
     * 创建新会话
     */
    @Operation(summary = "创建新会话")
    @SaCheckPermission("ai:chat:use")
    @PostMapping("/sessions")
    public AjaxResult createChatSession(@RequestBody AiChatSession aiChatSession) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            aiChatSession.setUserId(userId);
            aiChatSession.setCreateBy(String.valueOf(userId));
            
            // 如果没有指定会话名称，设置默认名称
            if (StrUtil.isBlank(aiChatSession.getSessionName())) {
                aiChatSession.setSessionName("新对话");
            }
            
            // 如果没有指定会话类型，设置默认类型
            if (StrUtil.isBlank(aiChatSession.getSessionType())) {
                aiChatSession.setSessionType("chat");
            }
            
            // 如果没有指定状态，设置默认状态
            if (StrUtil.isBlank(aiChatSession.getStatus())) {
                aiChatSession.setStatus("0");
            }
            
            boolean result = aiChatSessionService.save(aiChatSession);
            if (result) {
                return success(aiChatSession);
            } else {
                return error("创建会话失败");
            }
        } catch (Exception e) {
            logger.error("创建会话失败: {}", e.getMessage(), e);
            return error("创建会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新会话信息
     */
    @Operation(summary = "更新会话信息")
    @SaCheckPermission("ai:chat:use")
    @PostMapping("/sessions/{sessionId}")
    public AjaxResult updateChatSession(@PathVariable Long sessionId, @RequestBody AiChatSession aiChatSession) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            // 验证会话权限
            AiChatSession existingSession = aiChatSessionService.getById(sessionId);
            if (existingSession == null || !existingSession.getUserId().equals(userId)) {
                return error("会话不存在或无权限访问");
            }
            
            aiChatSession.setId(sessionId);
            aiChatSession.setUserId(userId);
            aiChatSession.setUpdateBy(String.valueOf(userId));
            boolean result = aiChatSessionService.updateById(aiChatSession);
            if (result) {
                return success("更新成功");
            } else {
                return error("更新失败");
            }
        } catch (Exception e) {
            logger.error("更新会话失败: {}", e.getMessage(), e);
            return error("更新会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除会话
     */
    @Operation(summary = "删除会话")
    @SaCheckPermission("ai:chat:history")
    @DeleteMapping("/sessions/{sessionId}")
    public AjaxResult deleteChatSession(@PathVariable Long sessionId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            // 验证会话权限
            AiChatSession session = aiChatSessionService.getById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                return error("会话不存在或无权限访问");
            }
            
            boolean result = aiChatSessionService.removeById(sessionId);
            if (result) {
                return success("删除成功");
            } else {
                return error("删除失败");
            }
        } catch (Exception e) {
            logger.error("删除会话失败: {}", e.getMessage(), e);
            return error("删除会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除会话
     */
    @Operation(summary = "批量删除会话")
    @SaCheckPermission("ai:chat:history")
    @DeleteMapping("/sessions")
    public AjaxResult deleteChatSessions(@RequestParam Long[] sessionIds) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            // 验证所有会话权限
            for (Long sessionId : sessionIds) {
                AiChatSession session = aiChatSessionService.getById(sessionId);
                if (session == null || !session.getUserId().equals(userId)) {
                    return error("部分会话不存在或无权限访问");
                }
            }
            
            boolean result = aiChatSessionService.removeByIds(java.util.Arrays.asList(sessionIds));
            if (result) {
                return success("删除成功");
            } else {
                return error("删除失败");
            }
        } catch (Exception e) {
            logger.error("批量删除会话失败: {}", e.getMessage(), e);
            return error("批量删除会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空用户所有聊天历史记录
     */
    @Operation(summary = "清空所有聊天历史记录")
    @SaCheckPermission("ai:chat:history")
    @DeleteMapping("/history/clear")
    public AjaxResult clearAllChatHistory() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId);
            boolean result = aiChatSessionService.remove(queryWrapper);
            return success("已清空聊天历史");
        } catch (Exception e) {
            logger.error("清空聊天历史失败: {}", e.getMessage(), e);
            return error("清空聊天历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存AI消息
     */
    @Operation(summary = "保存AI消息")
    @SaCheckPermission("ai:chat:message")
    @PostMapping("/message/save")
    public AjaxResult saveAiMessage(@RequestBody Map<String, Object> request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Long sessionId = Long.valueOf(request.get("sessionId").toString());
            String content = request.get("content").toString();
            Long modelConfigId = request.get("modelConfigId") != null ? 
                Long.valueOf(request.get("modelConfigId").toString()) : null;
            
            // 验证会话权限
            AiChatSession session = aiChatSessionService.getById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                return error("会话不存在或无权限访问");
            }
            
            // 创建AI消息
            AiChatMessage aiMessage = new AiChatMessage();
            aiMessage.setSessionId(sessionId);
            aiMessage.setMessageRole("assistant");
            aiMessage.setMessageContent(content);
            aiMessage.setMessageType("text");
            aiMessage.setModelConfigId(modelConfigId);
            aiMessage.setCreateBy(String.valueOf(userId));
            
            boolean result = aiChatMessageService.save(aiMessage);
            if (result) {
                return success("AI消息保存成功", aiMessage);
            } else {
                return error("AI消息保存失败");
            }
        } catch (Exception e) {
            logger.error("保存AI消息失败: {}", e.getMessage(), e);
            return error("保存AI消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 安全地完成SSE连接，避免重复完成导致的异常
     */
    private void safeComplete(SseEmitter emitter, boolean[] isCompleted, Object completionLock) {
        synchronized (completionLock) {
            if (!isCompleted[0]) {
                isCompleted[0] = true;
                try {
                    emitter.complete();
                } catch (Exception e) {
                    logger.warn("SSE连接完成时发生异常（可能已经断开）: {}", e.getMessage());
                    // 忽略异常，因为连接可能已经被客户端断开
                }
            }
        }
    }
}