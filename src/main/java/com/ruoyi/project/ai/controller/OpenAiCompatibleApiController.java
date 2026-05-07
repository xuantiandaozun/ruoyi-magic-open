package com.ruoyi.project.ai.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.domain.AiOpenApiKey;
import com.ruoyi.project.ai.domain.AiUsageRecord;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiOpenApiKeyService;
import com.ruoyi.project.ai.service.IAiUsageRecordService;
import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;
import com.ruoyi.project.system.domain.SysConfig;
import com.ruoyi.project.system.service.ISysConfigService;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 对外 OpenAI 兼容接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/openai/v1")
public class OpenAiCompatibleApiController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_MODEL_CONFIG_KEY = "ai.openai.compat.defaultModel";

    private final IAiModelConfigService modelConfigService;
    private final IAiOpenApiKeyService openApiKeyService;
    private final IAiUsageRecordService usageRecordService;
    private final ISysConfigService sysConfigService;
    private final LangChain4jAgentService langChain4jAgentService;

    public OpenAiCompatibleApiController(IAiModelConfigService modelConfigService,
            IAiOpenApiKeyService openApiKeyService,
            IAiUsageRecordService usageRecordService,
            ISysConfigService sysConfigService,
            LangChain4jAgentService langChain4jAgentService) {
        this.modelConfigService = modelConfigService;
        this.openApiKeyService = openApiKeyService;
        this.usageRecordService = usageRecordService;
        this.sysConfigService = sysConfigService;
        this.langChain4jAgentService = langChain4jAgentService;
    }

    @GetMapping("/models")
    public ResponseEntity<?> listModels(HttpServletRequest request) {
        ensureCompatConfigs();
        AuthResult authResult = validateAuthorization(request);
        if (authResult.errorResponse() != null) {
            return authResult.errorResponse();
        }
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"));
        List<AiModelConfig> models = modelConfigService.list(qw);
        List<Map<String, Object>> data = new ArrayList<>();
        for (AiModelConfig model : models) {
            if (!isModelAllowed(authResult.openApiKey(), model.getModel())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", model.getModel());
            item.put("object", "model");
            item.put("created", model.getCreateTime() == null ? Instant.now().getEpochSecond() : model.getCreateTime().getTime() / 1000);
            item.put("owned_by", model.getProvider());
            data.add(item);
        }
        return ResponseEntity.ok(Map.of("object", "list", "data", data));
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(@RequestBody JsonNode requestBody, HttpServletRequest request) {
        ensureCompatConfigs();
        AuthResult authResult = validateAuthorization(request);
        if (authResult.errorResponse() != null) {
            return authResult.errorResponse();
        }

        AiOpenApiKey openApiKey = authResult.openApiKey();
        String requestModel = textValue(requestBody.get("model"));
        if (StrUtil.isBlank(requestModel)) {
            requestModel = sysConfigService.selectConfigByKey(DEFAULT_MODEL_CONFIG_KEY);
        }
        if (StrUtil.isBlank(requestModel)) {
            return error(HttpStatus.BAD_REQUEST, "invalid_request_error", "model 不能为空，且未配置默认模型 ai.openai.compat.defaultModel");
        }

        AiModelConfig modelConfig = modelConfigService.getEnabledByModel(requestModel);
        if (modelConfig == null) {
            return error(HttpStatus.BAD_REQUEST, "invalid_model", "模型不存在或未启用: " + requestModel);
        }
        if (!isModelAllowed(openApiKey, modelConfig.getModel())) {
            return error(HttpStatus.FORBIDDEN, "model_not_allowed", "当前 API Key 不允许调用模型: " + modelConfig.getModel());
        }

        List<ChatMessage> messages;
        try {
            messages = toChatMessages(requestBody.get("messages"));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "invalid_request_error", e.getMessage());
        }

        boolean stream = requestBody.path("stream").asBoolean(false);
        if (stream) {
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(buildStreamEmitter(modelConfig, openApiKey, messages, request));
        }

        long start = System.currentTimeMillis();
        LangChain4jAgentService.ChatExecutionResult result;
        try {
            result = langChain4jAgentService.chatWithMessagesDetailed(modelConfig.getId(), messages);
        } catch (Exception e) {
            log.error("OpenAI兼容接口调用失败: model={}", requestModel, e);
            recordFailure(openApiKey, modelConfig, request, start, e.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "server_error", e.getMessage());
        }

        recordSuccess(openApiKey, modelConfig, request, start,
                result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens());

        long created = Instant.now().getEpochSecond();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", buildCompletionId());
        response.put("object", "chat.completion");
        response.put("created", created);
        response.put("model", modelConfig.getModel());
        response.put("choices", List.of(Map.of(
            "index", 0,
            "message", Map.of("role", "assistant", "content", StrUtil.nullToDefault(result.getContent(), "")),
            "finish_reason", StrUtil.blankToDefault(result.getFinishReason(), "stop").toLowerCase()
        )));
        response.put("usage", Map.of(
            "prompt_tokens", defaultInt(result.getInputTokens()),
            "completion_tokens", defaultInt(result.getOutputTokens()),
            "total_tokens", defaultInt(result.getTotalTokens())
        ));
        return ResponseEntity.ok(response);
    }

    private SseEmitter buildStreamEmitter(AiModelConfig modelConfig, AiOpenApiKey openApiKey,
            List<ChatMessage> messages, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        String completionId = buildCompletionId();
        long created = Instant.now().getEpochSecond();
        long start = System.currentTimeMillis();
        AtomicReference<Boolean> firstChunk = new AtomicReference<>(true);
        langChain4jAgentService.streamChatWithMessagesDetailed(modelConfig.getId(), messages,
            token -> {
                try {
                    Map<String, Object> delta = new LinkedHashMap<>();
                    if (Boolean.TRUE.equals(firstChunk.get())) {
                        delta.put("role", "assistant");
                        firstChunk.set(false);
                    }
                    delta.put("content", token);
                    emitter.send(SseEmitter.event().data(toJson(Map.of(
                        "id", completionId,
                        "object", "chat.completion.chunk",
                        "created", created,
                        "model", modelConfig.getModel(),
                        "choices", List.of(Map.of(
                            "index", 0,
                            "delta", delta,
                            "finish_reason", null
                        ))
                    ))));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            result -> {
                try {
                    emitter.send(SseEmitter.event().data(toJson(Map.of(
                        "id", completionId,
                        "object", "chat.completion.chunk",
                        "created", created,
                        "model", modelConfig.getModel(),
                        "choices", List.of(Map.of(
                            "index", 0,
                            "delta", Map.of(),
                            "finish_reason", StrUtil.blankToDefault(result.getFinishReason(), "stop").toLowerCase()
                        ))
                    ))));
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    recordSuccess(openApiKey, modelConfig, request, start,
                            result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens());
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            error -> {
                log.error("OpenAI兼容流式接口调用失败: model={}", modelConfig.getModel(), error);
                recordFailure(openApiKey, modelConfig, request, start, error.getMessage());
                emitter.completeWithError(error);
            });
        return emitter;
    }

    private List<ChatMessage> toChatMessages(JsonNode messagesNode) {
        if (messagesNode == null || !messagesNode.isArray() || messagesNode.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }
        List<ChatMessage> messages = new ArrayList<>();
        for (JsonNode messageNode : messagesNode) {
            String role = textValue(messageNode.get("role"));
            String content = parseContent(messageNode.get("content"));
            if (StrUtil.isBlank(role)) {
                throw new IllegalArgumentException("messages.role 不能为空");
            }
            switch (role) {
                case "system" -> messages.add(SystemMessage.from(content));
                case "user" -> messages.add(UserMessage.from(content));
                case "assistant" -> messages.add(AiMessage.from(content));
                default -> throw new IllegalArgumentException("暂不支持的 role: " + role);
            }
        }
        return messages;
    }

    private String parseContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if ("text".equals(textValue(item.get("type")))) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(textValue(item.get("text")));
                }
            }
            return builder.toString();
        }
        throw new IllegalArgumentException("messages.content 仅支持字符串或 text 数组");
    }

    private AuthResult validateAuthorization(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(auth) || !StrUtil.startWithIgnoreCase(auth, "Bearer ")) {
            return new AuthResult(null, error(HttpStatus.UNAUTHORIZED, "invalid_api_key", "缺少 Bearer API Key"));
        }
        String providedKey = StrUtil.trim(auth.substring(7));
        AiOpenApiKey openApiKey = openApiKeyService.validateKey(providedKey);
        if (openApiKey == null) {
            return new AuthResult(null, error(HttpStatus.UNAUTHORIZED, "invalid_api_key", "API Key 无效或已过期"));
        }
        return new AuthResult(openApiKey, null);
    }

    private void ensureCompatConfigs() {
        ensureConfig(DEFAULT_MODEL_CONFIG_KEY, "OpenAI兼容接口默认模型", "", "未传 model 时使用，对应 ai_model_config.model");
    }

    private void ensureConfig(String key, String name, String value, String remark) {
        QueryWrapper qw = QueryWrapper.create().from("sys_config").where(new QueryColumn("config_key").eq(key));
        SysConfig config = sysConfigService.getOne(qw);
        if (config != null) {
            return;
        }
        config = new SysConfig();
        config.setConfigName(name);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType("N");
        config.setCreateBy("system");
        config.setUpdateBy("system");
        config.setRemark(remark);
        config.setDelFlag("0");
        sysConfigService.save(config);
        sysConfigService.resetConfigCache();
    }

    private boolean isModelAllowed(AiOpenApiKey openApiKey, String model) {
        if (StrUtil.isBlank(openApiKey.getAllowedModels())) {
            return true;
        }
        for (String item : openApiKey.getAllowedModels().split(",")) {
            if (StrUtil.equals(item.trim(), model)) {
                return true;
            }
        }
        return false;
    }

    private void recordSuccess(AiOpenApiKey openApiKey, AiModelConfig modelConfig, HttpServletRequest request,
            long startMillis, Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        saveUsageRecord(openApiKey, modelConfig, request, startMillis, inputTokens, outputTokens, totalTokens, "success", null);
        openApiKeyService.recordUsage(openApiKey.getId(), true, inputTokens, outputTokens, totalTokens, request.getRemoteAddr());
    }

    private void recordFailure(AiOpenApiKey openApiKey, AiModelConfig modelConfig, HttpServletRequest request,
            long startMillis, String errorMessage) {
        saveUsageRecord(openApiKey, modelConfig, request, startMillis, 0, 0, 0, "failed", errorMessage);
        openApiKeyService.recordUsage(openApiKey.getId(), false, 0, 0, 0, request.getRemoteAddr());
    }

    private void saveUsageRecord(AiOpenApiKey openApiKey, AiModelConfig modelConfig, HttpServletRequest request,
            long startMillis, Integer inputTokens, Integer outputTokens, Integer totalTokens,
            String status, String errorMessage) {
        AiUsageRecord record = new AiUsageRecord();
        record.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        record.setUserId(0L);
        record.setAppCode("openai_compat_api");
        record.setOpenApiKeyId(openApiKey.getId());
        record.setOpenApiKeyName(openApiKey.getName());
        record.setProductType("chat");
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
        record.setRequestMeta(request.getRequestURI());
        record.setCreateBy("open-api");
        record.setUpdateBy("open-api");
        record.setDelFlag("0");
        usageRecordService.save(record);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "error", Map.of(
                "message", message,
                "type", code,
                "code", code
            )
        ));
    }

    private String buildCompletionId() {
        return "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String textValue(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String toJson(Object value) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(value);
    }

    private record AuthResult(AiOpenApiKey openApiKey, ResponseEntity<?> errorResponse) {
    }
}
