package com.ruoyi.project.plugin.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.domain.AiModelRoute;
import com.ruoyi.project.ai.domain.AiUsageRecord;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiModelRouteService;
import com.ruoyi.project.ai.service.IAiQuotaCheckService;
import com.ruoyi.project.ai.service.IAiUsageRecordService;
import com.ruoyi.project.ai.service.IAiUsageSummaryDailyService;
import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 插件 AI 调用接口
 * <p>
 * 所有接口需要已登录（Sa-Token），配额检查通过后路由到 DeepSeek 模型。
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
    /** Redis 熔断 key 前缀，TTL 1天，标记不稳定模型 */
    private static final String CIRCUIT_KEY_PREFIX = "plugin:ai:circuit:";
    private static final int CIRCUIT_TTL_HOURS = 24;

    @Autowired
    private IAiQuotaCheckService quotaCheckService;

    @Autowired
    private IAiModelRouteService modelRouteService;

    @Autowired
    private IAiModelConfigService modelConfigService;

    @Autowired
    private IAiUsageRecordService usageRecordService;

    @Autowired
    private IAiUsageSummaryDailyService usageSummaryDailyService;

    @Autowired
    private LangChain4jAgentService langChain4jAgentService;

    @Autowired
    private RedisCache redisCache;

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

        // 5. 调用（流式 / 非流式均走 resolveStreamModel / 候选链）
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

        // 6. 非流式：依次轮询候选模型，遇到上游 429/500 自动切下一个
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
                } else if (isUpstreamServerError(e)) {
                    // 上游 500：熔断该模型 24h，切下一个候选
                    tripCircuit(modelConfig.getId(), modelConfig.getModel(), e.getMessage());
                    saveUsageRecord(userId, modelConfig, request, start, 0, 0, 0, "failed", e.getMessage());
                } else {
                    // 其他错误（网络、配置等），直接终止
                    log.error("插件 AI 调用失败: model={}", modelConfig.getModel(), e);
                    saveUsageRecord(userId, modelConfig, request, start, 0, 0, 0, "failed", e.getMessage());
                    return AjaxResult.error("AI 服务暂时不可用，请稍后重试");
                }
            }
        }

        // 所有候选都不可用
        log.error("插件 AI 全部候选模型均不可用，lastError={}", lastError != null ? lastError.getMessage() : "unknown");
        return AjaxResult.error("AI 服务繁忙，请稍后重试");
    }

    // -----------------------------------------------------------------------
    // 私有方法
    // -----------------------------------------------------------------------

    /**
     * 构建有序的模型候选链
     * <p>
     * 优先级：路由表主模型（id=55 deepseek-v4-flash）→ 路由表 fallback（id=56 deepseek-v4-pro）
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

        // 2. 兜底：直接加载 deepseek-v4-flash / deepseek-v4-pro（防路由表未配置场景）
        addIfEnabled(chain, 55L); // deepseek-v4-flash
        addIfEnabled(chain, 56L); // deepseek-v4-pro

        return chain;
    }

    /**
     * 流式模式专用：优先使用 deepseek-v4-flash（id=55），熔断时降级到 deepseek-v4-pro（id=56）
     */
    private AiModelConfig resolveStreamModel() {
        // 取候选链第一个（addIfEnabled 已过滤熔断模型）
        List<AiModelConfig> chain = resolveModelConfigChain();
        return chain.isEmpty() ? null : chain.get(0);
    }

    /** 根据 configId 查模型，若启用且未熔断则加入链（去重） */
    private void addIfEnabled(List<AiModelConfig> chain, Long configId) {
        if (configId == null) return;
        if (chain.stream().anyMatch(c -> c.getId().equals(configId))) return;
        if (isCircuitOpen(configId)) {
            log.debug("模型 id={} 熔断中，跳过", configId);
            return;
        }
        AiModelConfig cfg = modelConfigService.getById(configId);
        if (cfg != null && "Y".equals(cfg.getEnabled()) && "0".equals(cfg.getStatus())) {
            chain.add(cfg);
        }
    }

    /** 打开熔断器：将模型标记为不稳定，TTL = CIRCUIT_TTL_HOURS 小时 */
    private void tripCircuit(Long modelConfigId, String modelName, String reason) {
        String key = CIRCUIT_KEY_PREFIX + modelConfigId;
        redisCache.setCacheObject(key, reason, CIRCUIT_TTL_HOURS, TimeUnit.HOURS);
        log.warn("模型熔断: id={}, model={}, reason={}, TTL={}h", modelConfigId, modelName, reason, CIRCUIT_TTL_HOURS);
    }

    /** 检查模型熔断器是否打开 */
    private boolean isCircuitOpen(Long modelConfigId) {
        return Boolean.TRUE.equals(redisCache.hasKey(CIRCUIT_KEY_PREFIX + modelConfigId));
    }

    /**
     * 判断异常是否为上游 429 限速
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

    /**
     * 判断是否为上游 500 服务器错误（可降级重试）
     */
    private boolean isUpstreamServerError(Throwable e) {
        if (e == null) return false;
        String type = e.getClass().getSimpleName();
        String msg = e.getMessage();
        return type.contains("InternalServerException")
                || type.contains("ServerErrorException")
                || (msg != null && (msg.contains("500") || msg.contains("Internal Server Error")));
    }

    private List<ChatMessage> parseChatMessages(JsonNode messagesNode) {
        if (messagesNode == null || !messagesNode.isArray() || messagesNode.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }
        List<ChatMessage> messages = new ArrayList<>();
        for (JsonNode node : messagesNode) {
            String role = node.path("role").asText();
            switch (role) {
                case "system", "developer" -> {
                    String content = node.path("content").asText("");
                    messages.add(SystemMessage.from(content));
                }
                case "user" -> {
                    String content = node.path("content").asText("");
                    messages.add(UserMessage.from(content));
                }
                case "assistant" -> {
                    // 支持带 tool_calls 的 assistant 消息
                    JsonNode toolCallsNode = node.get("tool_calls");
                    if (toolCallsNode != null && toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                        List<ToolExecutionRequest> requests = new ArrayList<>();
                        for (JsonNode tc : toolCallsNode) {
                            String id = tc.path("id").asText();
                            JsonNode fnNode = tc.get("function");
                            if (fnNode == null) continue;
                            String name = fnNode.path("name").asText();
                            String arguments = fnNode.path("arguments").asText("{}");
                            requests.add(ToolExecutionRequest.builder()
                                    .id(id).name(name).arguments(arguments).build());
                        }
                        messages.add(AiMessage.from(requests));
                    } else {
                        messages.add(AiMessage.from(node.path("content").asText("")));
                    }
                }
                case "tool" -> {
                    // 客户端执行工具后把结果发回来
                    String toolCallId = node.path("tool_call_id").asText();
                    String content = node.path("content").asText("");
                    String name = node.path("name").asText(toolCallId);
                    if (StrUtil.isBlank(toolCallId)) {
                        throw new IllegalArgumentException("role=tool 的消息必须提供 tool_call_id");
                    }
                    ToolExecutionRequest req = ToolExecutionRequest.builder()
                            .id(toolCallId).name(name).arguments("{}").build();
                    messages.add(ToolExecutionResultMessage.from(req, content));
                }
                case "function" -> {
                    // 旧版 function 格式兼容
                    String name = node.path("name").asText("function");
                    String content = node.path("content").asText("");
                    ToolExecutionRequest req = ToolExecutionRequest.builder()
                            .id(name).name(name).arguments("{}").build();
                    messages.add(ToolExecutionResultMessage.from(req, content));
                }
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
        return buildStreamEmitter(userId, modelConfig, messages, request, false);
    }

    private SseEmitter buildStreamEmitter(Long userId, AiModelConfig modelConfig,
            List<ChatMessage> messages, HttpServletRequest request, boolean isFallback) {
        SseEmitter emitter = new SseEmitter(0L);
        String completionId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
        long created = System.currentTimeMillis() / 1000;
        long start = System.currentTimeMillis();
        AtomicBoolean firstChunk = new AtomicBoolean(true);

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
                        saveUsageRecord(userId, modelConfig, request, start,
                                result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens(),
                                "success", null);
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("插件流式 AI 调用失败: model={}, fallback={}", modelConfig.getModel(), isFallback, error);
                    saveUsageRecord(userId, modelConfig, request, start, 0, 0, 0, "failed", error.getMessage());

                    // flash(id=55) 500 时：熔断 → 降级到 pro(id=56) 重试一次（isFallback=true 防递归）
                    if (!isFallback && firstChunk.get() && isUpstreamServerError(error)) {
                        // 熔断当前模型
                        tripCircuit(modelConfig.getId(), modelConfig.getModel(), error.getMessage());
                        List<AiModelConfig> chain = resolveModelConfigChain();
                        if (!chain.isEmpty()) {
                            AiModelConfig fallbackModel = chain.get(0);
                            log.warn("流式自动降级: {} -> {}", modelConfig.getModel(), fallbackModel.getModel());
                            SseEmitter fallbackEmitter = buildStreamEmitter(userId, fallbackModel, messages, request, true);
                            fallbackEmitter.onCompletion(emitter::complete);
                            fallbackEmitter.onError(emitter::completeWithError);
                            return;
                        }
                    }
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

            // 实时更新每日汇总（保证配额检查能拿到最新用量）
            int successDelta = "success".equals(status) ? 1 : 0;
            int failedDelta  = "failed".equals(status)  ? 1 : 0;
            usageSummaryDailyService.upsertDailyRecord(
                    userId, PRODUCT_TYPE,
                    modelConfig.getProvider(), modelConfig.getModel(),
                    1, successDelta, failedDelta,
                    defaultInt(inputTokens), defaultInt(outputTokens), defaultInt(totalTokens));
        } catch (Exception e) {
            log.error("保存插件用量记录失败", e);
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
