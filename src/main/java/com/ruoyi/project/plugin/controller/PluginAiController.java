package com.ruoyi.project.plugin.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.domain.AiModelRoute;
import com.ruoyi.project.ai.domain.AiUsageRecord;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiModelRouteService;
import com.ruoyi.project.ai.service.IAiQuotaCheckService;
import com.ruoyi.project.ai.service.IAiUsageRecordService;
import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 插件 AI 调用接口
 * <p>
 * 所有接口需要已登录（Sa-Token），配额检查通过后路由到 OpenRouter 免费模型。
 * 对外保持 OpenAI 兼容格式，方便插件直接复用。
 */
@Slf4j
@Tag(name = "插件-AI对话")
@RestController
@RequestMapping("/plugin/ai")
public class PluginAiController {

    private static final String PRODUCT_TYPE = "plugin";
    private static final String USER_TIER_FREE = "free";
    private static final String SCENE_CODE_CHAT = "chat";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private IAiQuotaCheckService quotaCheckService;

    @Autowired
    private IAiModelRouteService modelRouteService;

    @Autowired
    private IAiModelConfigService modelConfigService;

    @Autowired
    private IAiUsageRecordService usageRecordService;

    @Autowired
    private LangChain4jAgentService langChain4jAgentService;

    /**
     * 对话接口（支持流式 / 非流式）
     * <p>
     * 请求格式与 OpenAI /chat/completions 兼容，可选 stream=true 开启 SSE 流。
     * 遇到上游 429（rate-limited）自动轮询候选模型重试，全部失败才返回错误。
     */
    @Operation(summary = "AI 对话（OpenAI 兼容格式）")
    @PostMapping("/chat/completions")
    public Object chatCompletions(
            @RequestBody JsonNode requestBody,
            HttpServletRequest request,
            HttpServletResponse httpResponse) {

        // 1. 鉴权
        if (!StpUtil.isLogin()) {
            return AjaxResult.error(401, "请先登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 配额检查
        try {
            quotaCheckService.checkAndConsume(userId, USER_TIER_FREE, PRODUCT_TYPE);
        } catch (ServiceException e) {
            return AjaxResult.error(429, e.getMessage());
        }

        // 3. 解析 messages
        List<ChatMessage> messages;
        try {
            messages = parseChatMessages(requestBody.get("messages"));
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }

        // 4. 获取模型候选链
        List<AiModelConfig> candidates = resolveModelConfigChain();
        if (candidates.isEmpty()) {
            return AjaxResult.error("暂无可用的 AI 模型，请联系管理员");
        }

        // 5. 调用（流式：使用 openrouter/free 自动路由模式，由 OpenRouter 自动选可用免费模型）
        boolean stream = requestBody.path("stream").asBoolean(false);
        if (stream) {
            httpResponse.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8");
            httpResponse.setCharacterEncoding("UTF-8");
            AiModelConfig streamModel = resolveStreamModel();
            if (streamModel == null) {
                return AjaxResult.error("暂无可用的流式 AI 模型，请联系管理员");
            }
            return buildStreamEmitter(userId, streamModel, messages, request);
        }

        // 6. 非流式：依次轮询候选模型，遇到上游 429 自动切下一个
        long start = System.currentTimeMillis();
        Exception lastError = null;
        for (AiModelConfig modelConfig : candidates) {
            try {
                LangChain4jAgentService.ChatExecutionResult result =
                        langChain4jAgentService.chatWithMessagesDetailed(modelConfig.getId(), messages);
                saveUsageRecord(userId, modelConfig, request, start,
                        result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens(),
                        "success", null);
                return buildNonStreamResponse(modelConfig, result);
            } catch (Exception e) {
                lastError = e;
                if (isUpstreamRateLimit(e)) {
                    log.warn("插件 AI 上游限速，切换模型: {} -> 下一个候选", modelConfig.getModel());
                    saveUsageRecord(userId, modelConfig, request, start, 0, 0, 0, "rate_limited", e.getMessage());
                } else {
                    // 非限速错误（网络、模型错误等），直接记录并终止
                    log.error("插件 AI 调用失败: model={}", modelConfig.getModel(), e);
                    saveUsageRecord(userId, modelConfig, request, start, 0, 0, 0, "failed", e.getMessage());
                    return AjaxResult.error("AI 服务暂时不可用，请稍后重试");
                }
            }
        }

        // 所有候选都被限速了
        log.error("插件 AI 全部候选模型均被限速，lastError={}", lastError != null ? lastError.getMessage() : "unknown");
        return AjaxResult.error("AI 服务繁忙，请稍后重试");
    }

    // -----------------------------------------------------------------------
    // 私有方法
    // -----------------------------------------------------------------------

    /**
     * 构建有序的模型候选链（中文文档场景优先）
     * <p>
     * 优先级：路由表主模型 → 路由表 fallback → 预设中文友好模型列表 → 任意可用免费模型
     */
    private List<AiModelConfig> resolveModelConfigChain() {
        List<AiModelConfig> chain = new ArrayList<>();

        // 1. 从路由表取主/备模型
        QueryWrapper qw = QueryWrapper.create()
                .from("ai_model_route")
                .where("product_type = '" + PRODUCT_TYPE + "'")
                .and("user_tier = '" + USER_TIER_FREE + "'")
                .and("scene_code = '" + SCENE_CODE_CHAT + "'")
                .and("enabled = 'Y'")
                .and("del_flag = '0'")
                .limit(1);
        AiModelRoute route = modelRouteService.getOne(qw);
        if (route != null) {
            addIfEnabled(chain, route.getPrimaryModelConfigId());
            addIfEnabled(chain, route.getFallbackModelConfigId());
        }

        // 2. 预设中文友好模型（按优先级排列），自动跳过已加入的
        List<Long> preferredIds = List.of(
                45L,  // qwen/qwen3-next-80b-a3b-instruct:free  — 中文最强
                49L,  // z-ai/glm-4.5-air:free                  — 智谱国产中文极强
                39L,  // minimax/minimax-m2.5:free               — 国产中文好
                52L,  // meta-llama/llama-3.3-70b-instruct:free  — 英文强，中文可用
                35L,  // google/gemma-4-31b-it:free              — 兜底
                54L   // nousresearch/hermes-3-llama-3.1-405b:free — 大模型兜底
        );
        for (Long id : preferredIds) {
            addIfEnabled(chain, id);
        }

        return chain;
    }

    /**
     * 流式模式专用：使用 openrouter/free 自动路由模型（id=40）
     * OpenRouter 会自动从可用免费模型中选择，无需客户端处理限速重试
     */
    private AiModelConfig resolveStreamModel() {
        // 优先用 openrouter/free 自动模式
        AiModelConfig auto = modelConfigService.getById(40L);
        if (auto != null && "Y".equals(auto.getEnabled()) && "0".equals(auto.getStatus())) {
            return auto;
        }
        // 降级：取候选链第一个
        List<AiModelConfig> chain = resolveModelConfigChain();
        return chain.isEmpty() ? null : chain.get(0);
    }

    /** 根据 configId 查模型，若启用则加入链（去重） */
    private void addIfEnabled(List<AiModelConfig> chain, Long configId) {
        if (configId == null) return;
        if (chain.stream().anyMatch(c -> c.getId().equals(configId))) return;
        AiModelConfig cfg = modelConfigService.getById(configId);
        if (cfg != null && "Y".equals(cfg.getEnabled()) && "0".equals(cfg.getStatus())) {
            chain.add(cfg);
        }
    }

    /**
     * 判断异常是否为上游 429 限速（OpenRouter 返回的 rate-limited 错误）
     */
    private boolean isUpstreamRateLimit(Exception e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("429")
                || msg.contains("rate-limited")
                || msg.contains("rate_limited")
                || msg.contains("Rate limit")
                || msg.contains("temporarily rate");
    }

    private List<ChatMessage> parseChatMessages(JsonNode messagesNode) {
        if (messagesNode == null || !messagesNode.isArray() || messagesNode.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }
        List<ChatMessage> messages = new ArrayList<>();
        for (JsonNode node : messagesNode) {
            String role = node.path("role").asText();
            String content = node.path("content").asText("");
            switch (role) {
                case "system", "developer" -> messages.add(SystemMessage.from(content));
                case "user" -> messages.add(UserMessage.from(content));
                case "assistant" -> messages.add(AiMessage.from(content));
                default -> throw new IllegalArgumentException("不支持的 role: " + role);
            }
        }
        return messages;
    }

    private Object buildNonStreamResponse(AiModelConfig modelConfig,
            LangChain4jAgentService.ChatExecutionResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", "chatcmpl-" + UUID.randomUUID().toString().replace("-", ""));
        response.put("object", "chat.completion");
        response.put("created", System.currentTimeMillis() / 1000);
        response.put("model", modelConfig.getModel());
        response.put("choices", List.of(Map.of(
                "index", 0,
                "message", Map.of("role", "assistant",
                        "content", StrUtil.nullToDefault(result.getContent(), "")),
                "finish_reason", StrUtil.blankToDefault(result.getFinishReason(), "stop").toLowerCase()
        )));
        response.put("usage", Map.of(
                "prompt_tokens", defaultInt(result.getInputTokens()),
                "completion_tokens", defaultInt(result.getOutputTokens()),
                "total_tokens", defaultInt(result.getTotalTokens())
        ));
        return response;
    }

    private SseEmitter buildStreamEmitter(Long userId, AiModelConfig modelConfig,
            List<ChatMessage> messages, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        String completionId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
        long created = System.currentTimeMillis() / 1000;
        long start = System.currentTimeMillis();
        java.util.concurrent.atomic.AtomicBoolean firstChunk =
                new java.util.concurrent.atomic.AtomicBoolean(true);

        langChain4jAgentService.streamChatWithMessagesDetailed(modelConfig.getId(), messages,
                token -> {
                    try {
                        Map<String, Object> delta = new LinkedHashMap<>();
                        if (firstChunk.compareAndSet(true, false)) {
                            delta.put("role", "assistant");
                        }
                        delta.put("content", token == null ? "" : token);
                        Map<String, Object> chunk = new LinkedHashMap<>();
                        chunk.put("id", completionId);
                        chunk.put("object", "chat.completion.chunk");
                        chunk.put("created", created);
                        chunk.put("model", modelConfig.getModel());
                        Map<String, Object> choice = new LinkedHashMap<>();
                        choice.put("index", 0);
                        choice.put("delta", delta);
                        choice.put("finish_reason", null);
                        chunk.put("choices", List.of(choice));
                        emitter.send(SseEmitter.event().data(MAPPER.writeValueAsString(chunk)));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                result -> {
                    try {
                        Map<String, Object> doneChunk = new LinkedHashMap<>();
                        doneChunk.put("id", completionId);
                        doneChunk.put("object", "chat.completion.chunk");
                        doneChunk.put("created", created);
                        doneChunk.put("model", modelConfig.getModel());
                        doneChunk.put("choices", List.of(Map.of(
                                "index", 0,
                                "delta", Map.of(),
                                "finish_reason",
                                StrUtil.blankToDefault(result.getFinishReason(), "stop").toLowerCase()
                        )));
                        emitter.send(SseEmitter.event().data(MAPPER.writeValueAsString(doneChunk)));
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        saveUsageRecord(userId, modelConfig, request, start,
                                result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens(),
                                "success", null);
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("插件流式 AI 调用失败: model={}", modelConfig.getModel(), error);
                    saveUsageRecord(userId, modelConfig, request, start, 0, 0, 0, "failed", error.getMessage());
                    emitter.completeWithError(error);
                });
        return emitter;
    }

    private void saveUsageRecord(Long userId, AiModelConfig modelConfig, HttpServletRequest request,
            long startMillis, Integer inputTokens, Integer outputTokens, Integer totalTokens,
            String status, String errorMessage) {
        try {
            AiUsageRecord record = new AiUsageRecord();
            record.setRequestId(UUID.randomUUID().toString().replace("-", ""));
            record.setUserId(userId);
            record.setAppCode("plugin_chat");
            record.setProductType(PRODUCT_TYPE);
            record.setModelConfigId(modelConfig.getId());
            record.setProvider(modelConfig.getProvider());
            record.setModelName(modelConfig.getModel());
            record.setInputTokens(defaultInt(inputTokens));
            record.setOutputTokens(defaultInt(outputTokens));
            record.setCachedInputTokens(0);
            record.setTotalTokens(defaultInt(totalTokens));
            record.setImageCount(0);
            record.setEstimatedCost(BigDecimal.ZERO);
            record.setCurrency("USD");
            record.setStatus(status);
            record.setErrorMessage(StrUtil.maxLength(errorMessage, 500));
            record.setStartTime(new Date(startMillis));
            record.setEndTime(new Date());
            record.setDurationMs(System.currentTimeMillis() - startMillis);
            record.setClientIp(request.getRemoteAddr());
            record.setUserAgent(request.getHeader(HttpHeaders.USER_AGENT));
            record.setCreateBy("plugin");
            record.setUpdateBy("plugin");
            record.setDelFlag("0");
            usageRecordService.save(record);
        } catch (Exception e) {
            log.error("保存插件用量记录失败", e);
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
