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
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.ruoyi.project.ai.service.impl.LangChain4jAgentService.ProxyChatExecutionResult;
import com.ruoyi.project.system.domain.SysConfig;
import com.ruoyi.project.system.service.ISysConfigService;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 对外 OpenAI 兼容接口。
 * <p>
 * 支持客户端提供 tools 定义（Client-side tool 模式）：
 * 服务端将 tools 透传给 LLM，LLM 返回 tool_calls 后原样回传给客户端，
 * 由客户端本地执行 tool 并将结果作为 role=tool 消息发回继续对话。
 */
@Slf4j
@RestController
@RequestMapping({ "/api/openai/v1", "/openai/v1" })
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

    // -----------------------------------------------------------------------
    // GET /models
    // -----------------------------------------------------------------------

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
            item.put("created", model.getCreateTime() == null
                    ? Instant.now().getEpochSecond() : model.getCreateTime().getTime() / 1000);
            item.put("owned_by", model.getProvider());
            data.add(item);
        }
        return ResponseEntity.ok(Map.of("object", "list", "data", data));
    }

    // -----------------------------------------------------------------------
    // POST /chat/completions
    // -----------------------------------------------------------------------

    @PostMapping("/chat/completions")
    public Object chatCompletions(@RequestBody JsonNode requestBody, HttpServletRequest request,
            HttpServletResponse httpResponse) {
        ensureCompatConfigs();
        AuthResult authResult = validateAuthorization(request);
        if (authResult.errorResponse() != null) {
            return authResult.errorResponse();
        }

        AiOpenApiKey openApiKey = authResult.openApiKey();

        // 1. 解析 model
        String requestModel = textValue(requestBody.get("model"));
        if (StrUtil.isBlank(requestModel)) {
            requestModel = sysConfigService.selectConfigByKey(DEFAULT_MODEL_CONFIG_KEY);
        }
        if (StrUtil.isBlank(requestModel)) {
            return error(HttpStatus.BAD_REQUEST, "invalid_request_error",
                    "model 不能为空，且未配置默认模型 ai.openai.compat.defaultModel");
        }

        AiModelConfig modelConfig = modelConfigService.getEnabledByModel(requestModel);
        if (modelConfig == null) {
            return error(HttpStatus.BAD_REQUEST, "invalid_model", "模型不存在或未启用: " + requestModel);
        }
        if (!isModelAllowed(openApiKey, modelConfig.getModel())) {
            return error(HttpStatus.FORBIDDEN, "model_not_allowed",
                    "当前 API Key 不允许调用模型: " + modelConfig.getModel());
        }

        // 2. 解析 messages（支持 tool_calls / tool role）
        List<ChatMessage> messages;
        try {
            messages = toChatMessages(requestBody.get("messages"));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "invalid_request_error", e.getMessage());
        }

        // 3. 解析客户端提供的 tools（可选）
        List<ToolSpecification> clientTools = parseClientTools(requestBody);

        boolean stream = requestBody.path("stream").asBoolean(false);

        // 4. 流式
        if (stream) {
            httpResponse.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8");
            httpResponse.setCharacterEncoding("UTF-8");
            return buildStreamEmitter(modelConfig, openApiKey, messages, clientTools, request);
        }

        // 5. 非流式
        long start = System.currentTimeMillis();
        ProxyChatExecutionResult result;
        try {
            result = langChain4jAgentService.proxyChat(modelConfig.getId(), messages, clientTools);
        } catch (Exception e) {
            log.error("OpenAI兼容接口调用失败: model={}", requestModel, e);
            recordFailure(openApiKey, modelConfig, request, start, e.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "server_error", e.getMessage());
        }

        recordSuccess(openApiKey, modelConfig, request, start,
                result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens());

        return ResponseEntity.ok(buildNonStreamResponse(modelConfig, result));
    }

    // -----------------------------------------------------------------------
    // POST /embeddings
    // -----------------------------------------------------------------------

    @PostMapping("/embeddings")
    public ResponseEntity<?> embeddings(HttpServletRequest request) {
        AuthResult authResult = validateAuthorization(request);
        if (authResult.errorResponse() != null) {
            return authResult.errorResponse();
        }
        // Embeddings will be added later when the downstream model layer is ready.
        return error(HttpStatus.BAD_REQUEST, "unsupported_endpoint",
                "当前暂不支持 embeddings 接口，后续版本再升级");
    }

    // -----------------------------------------------------------------------
    // 非流式响应构建
    // -----------------------------------------------------------------------

    /**
     * 构建 chat.completion 格式响应，兼容普通文本回复和 tool_calls 两种情况。
     */
    private Map<String, Object> buildNonStreamResponse(AiModelConfig modelConfig, ProxyChatExecutionResult result) {
        long created = Instant.now().getEpochSecond();

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");

        String finishReason;
        if (result.hasToolCalls()) {
            // LLM 要求执行工具：content 为 null，附带 tool_calls 列表
            message.put("content", null);
            message.put("tool_calls", buildToolCallsJson(result.getToolCalls()));
            finishReason = "tool_calls";
        } else {
            message.put("content", StrUtil.nullToDefault(result.getContent(), ""));
            finishReason = normalizeFinishReason(result.getFinishReason());
        }

        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", message);
        choice.put("finish_reason", finishReason);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", buildCompletionId());
        response.put("object", "chat.completion");
        response.put("created", created);
        response.put("model", modelConfig.getModel());
        response.put("choices", List.of(choice));
        response.put("usage", Map.of(
            "prompt_tokens", defaultInt(result.getInputTokens()),
            "completion_tokens", defaultInt(result.getOutputTokens()),
            "total_tokens", defaultInt(result.getTotalTokens())
        ));
        return response;
    }

    // -----------------------------------------------------------------------
    // 流式 SSE 构建
    // -----------------------------------------------------------------------

    private SseEmitter buildStreamEmitter(AiModelConfig modelConfig, AiOpenApiKey openApiKey,
            List<ChatMessage> messages, List<ToolSpecification> clientTools, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        String completionId = buildCompletionId();
        long created = Instant.now().getEpochSecond();
        long start = System.currentTimeMillis();
        AtomicBoolean firstChunk = new AtomicBoolean(true);

        langChain4jAgentService.proxyChatStream(modelConfig.getId(), messages, clientTools,
            // onToken：文本 delta
            token -> {
                try {
                    Map<String, Object> delta = new LinkedHashMap<>();
                    if (firstChunk.compareAndSet(true, false)) {
                        delta.put("role", "assistant");
                    }
                    delta.put("content", token == null ? "" : token);
                    emitter.send(SseEmitter.event().data(toJsonQuiet(buildChunk(completionId, created,
                            modelConfig.getModel(), delta, null))));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            // onComplete：结束帧，处理 tool_calls 或普通 stop
            result -> {
                try {
                    if (result.hasToolCalls()) {
                        // 工具调用：发送携带 tool_calls 的 delta chunk
                        // 第一帧先发 role
                        if (firstChunk.compareAndSet(true, false)) {
                            Map<String, Object> roleDelta = new LinkedHashMap<>();
                            roleDelta.put("role", "assistant");
                            roleDelta.put("content", null);
                            emitter.send(SseEmitter.event().data(toJsonQuiet(
                                    buildChunk(completionId, created, modelConfig.getModel(), roleDelta, null))));
                        }
                        // 发送 tool_calls delta（index=0 整体输出，与 OpenAI 行为一致）
                        Map<String, Object> toolDelta = new LinkedHashMap<>();
                        toolDelta.put("tool_calls", buildToolCallsJson(result.getToolCalls()));
                        emitter.send(SseEmitter.event().data(toJsonQuiet(
                                buildChunk(completionId, created, modelConfig.getModel(), toolDelta, "tool_calls"))));
                    } else {
                        // 普通结束帧
                        String finishReason = normalizeFinishReason(result.getFinishReason());
                        emitter.send(SseEmitter.event().data(toJsonQuiet(
                                buildChunk(completionId, created, modelConfig.getModel(), Map.of(), finishReason))));
                    }
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    recordSuccess(openApiKey, modelConfig, request, start,
                            result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens());
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            // onError
            error -> {
                log.error("OpenAI兼容流式接口调用失败: model={}", modelConfig.getModel(), error);
                recordFailure(openApiKey, modelConfig, request, start, error.getMessage());
                emitter.completeWithError(error);
            });
        return emitter;
    }

    /** 构造一个标准 chat.completion.chunk Map */
    private Map<String, Object> buildChunk(String id, long created, String model,
            Map<String, Object> delta, String finishReason) {
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", finishReason); // LinkedHashMap 支持 null value

        Map<String, Object> chunk = new LinkedHashMap<>();
        chunk.put("id", id);
        chunk.put("object", "chat.completion.chunk");
        chunk.put("created", created);
        chunk.put("model", model);
        chunk.put("choices", List.of(choice));
        return chunk;
    }

    // -----------------------------------------------------------------------
    // 消息解析：支持完整的 OpenAI message 格式
    // -----------------------------------------------------------------------

    /**
     * 将 OpenAI 格式的 messages 数组转换为 LangChain4j ChatMessage 列表。
     * <ul>
     *   <li>system / developer → SystemMessage</li>
     *   <li>user → UserMessage（支持 text/image_url 数组）</li>
     *   <li>assistant → AiMessage（支持纯文本 或 带 tool_calls）</li>
     *   <li>tool → ToolExecutionResultMessage（需要 tool_call_id）</li>
     *   <li>function（旧版）→ ToolExecutionResultMessage（兼容）</li>
     * </ul>
     */
    private List<ChatMessage> toChatMessages(JsonNode messagesNode) {
        if (messagesNode == null || !messagesNode.isArray() || messagesNode.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }
        List<ChatMessage> messages = new ArrayList<>();
        for (JsonNode messageNode : messagesNode) {
            String role = textValue(messageNode.get("role"));
            if (StrUtil.isBlank(role)) {
                throw new IllegalArgumentException("messages.role 不能为空");
            }
            switch (role) {
                case "system", "developer" -> {
                    String content = parseTextContent(messageNode.get("content"));
                    messages.add(SystemMessage.from(content));
                }
                case "user" -> {
                    String content = parseUserContent(messageNode.get("content"));
                    messages.add(UserMessage.from(content));
                }
                case "assistant" -> messages.add(parseAssistantMessage(messageNode));
                case "tool" -> messages.add(parseToolResultMessage(messageNode));
                // 旧版 function role（OpenAI v1 早期格式）
                case "function" -> {
                    String name = textValue(messageNode.get("name"));
                    String content = parseTextContent(messageNode.get("content"));
                    // 构造一个假的 ToolExecutionRequest 来包装
                    ToolExecutionRequest fakeReq = ToolExecutionRequest.builder()
                            .id(StrUtil.blankToDefault(name, "function"))
                            .name(StrUtil.blankToDefault(name, "function"))
                            .arguments("{}")
                            .build();
                    messages.add(ToolExecutionResultMessage.from(fakeReq, content));
                }
                default -> throw new IllegalArgumentException("暂不支持的 role: " + role);
            }
        }
        return messages;
    }

    /**
     * 解析 assistant 消息：可能是纯文本，也可能带有 tool_calls。
     */
    private AiMessage parseAssistantMessage(JsonNode node) {
        JsonNode toolCallsNode = node.get("tool_calls");
        if (toolCallsNode != null && toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
            // 带 tool_calls 的 assistant 消息
            List<ToolExecutionRequest> requests = new ArrayList<>();
            for (JsonNode tc : toolCallsNode) {
                String id = textValue(tc.get("id"));
                JsonNode fnNode = tc.get("function");
                if (fnNode == null) {
                    continue;
                }
                String name = textValue(fnNode.get("name"));
                String arguments = fnNode.has("arguments") ? fnNode.get("arguments").asText("{}") : "{}";
                requests.add(ToolExecutionRequest.builder()
                        .id(StrUtil.blankToDefault(id, UUID.randomUUID().toString()))
                        .name(StrUtil.blankToDefault(name, "unknown"))
                        .arguments(arguments)
                        .build());
            }
            return AiMessage.from(requests);
        }
        // 普通文本回复
        String content = parseTextContent(node.get("content"));
        return AiMessage.from(StrUtil.nullToDefault(content, ""));
    }

    /**
     * 解析 role=tool 的消息（客户端执行工具后把结果发回来）。
     * 需要 tool_call_id 对应之前 assistant 返回的 tool_calls[].id。
     */
    private ToolExecutionResultMessage parseToolResultMessage(JsonNode node) {
        String toolCallId = textValue(node.get("tool_call_id"));
        String content = parseTextContent(node.get("content"));
        // name 字段可选
        String name = textValue(node.get("name"));
        if (StrUtil.isBlank(toolCallId)) {
            throw new IllegalArgumentException("role=tool 的消息必须提供 tool_call_id");
        }
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id(toolCallId)
                .name(StrUtil.blankToDefault(name, toolCallId))
                .arguments("{}")
                .build();
        return ToolExecutionResultMessage.from(req, StrUtil.nullToDefault(content, ""));
    }

    /**
     * 解析用户消息 content，支持字符串和 [{type:"text",...}, {type:"image_url",...}] 数组。
     * image_url 目前忽略（仅取文本部分），待后续多模态支持。
     */
    private String parseUserContent(JsonNode contentNode) {
        return parseTextContent(contentNode);
    }

    /**
     * 从 content 字段提取纯文本。
     * - null / "null" → 空字符串
     * - 字符串 → 直接返回
     * - 数组 → 拼接所有 type=text 的部分
     */
    private String parseTextContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                String itemType = textValue(item.get("type"));
                if ("text".equals(itemType)) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(textValue(item.get("text")));
                }
                // image_url: 暂时跳过，不报错（向前兼容）
            }
            return builder.toString();
        }
        return "";
    }

    // -----------------------------------------------------------------------
    // 客户端 tools 解析：将 OpenAI tools 格式转为 LangChain4j ToolSpecification
    // -----------------------------------------------------------------------

    /**
     * 从请求体解析客户端提供的 tools 或 functions（旧格式）定义。
     * 返回 null 或空列表表示客户端未提供 tools。
     */
    private List<ToolSpecification> parseClientTools(JsonNode requestBody) {
        // 新格式：tools: [{type:"function", function:{name,description,parameters}}]
        JsonNode toolsNode = requestBody.get("tools");
        if (toolsNode != null && toolsNode.isArray() && !toolsNode.isEmpty()) {
            List<ToolSpecification> specs = new ArrayList<>();
            for (JsonNode tool : toolsNode) {
                String type = textValue(tool.get("type"));
                if (!"function".equals(type)) {
                    continue; // 目前只支持 function 类型
                }
                JsonNode fn = tool.get("function");
                if (fn == null) {
                    continue;
                }
                ToolSpecification spec = parseFunctionToSpec(fn);
                if (spec != null) {
                    specs.add(spec);
                }
            }
            if (!specs.isEmpty()) {
                return specs;
            }
        }
        // 旧格式：functions: [{name,description,parameters}]
        JsonNode functionsNode = requestBody.get("functions");
        if (functionsNode != null && functionsNode.isArray() && !functionsNode.isEmpty()) {
            List<ToolSpecification> specs = new ArrayList<>();
            for (JsonNode fn : functionsNode) {
                ToolSpecification spec = parseFunctionToSpec(fn);
                if (spec != null) {
                    specs.add(spec);
                }
            }
            if (!specs.isEmpty()) {
                return specs;
            }
        }
        return null;
    }

    /**
     * 将单个 function 定义节点转为 {@link ToolSpecification}。
     * parameters 字段遵循 JSON Schema（OpenAI 规范），逐属性解析。
     */
    private ToolSpecification parseFunctionToSpec(JsonNode fn) {
        String name = textValue(fn.get("name"));
        if (StrUtil.isBlank(name)) {
            return null;
        }
        String description = textValue(fn.get("description"));
        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(name)
                .description(StrUtil.blankToDefault(description, ""));

        JsonNode parametersNode = fn.get("parameters");
        if (parametersNode != null && parametersNode.isObject()) {
            JsonObjectSchema schema = parseJsonObjectSchema(parametersNode);
            builder.parameters(schema);
        }
        return builder.build();
    }

    /**
     * 将 JSON Schema object 节点解析为 {@link JsonObjectSchema}。
     */
    private JsonObjectSchema parseJsonObjectSchema(JsonNode schemaNode) {
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();

        JsonNode propertiesNode = schemaNode.get("properties");
        JsonNode requiredNode = schemaNode.get("required");

        if (propertiesNode != null && propertiesNode.isObject()) {
            propertiesNode.fields().forEachRemaining(entry -> {
                String propName = entry.getKey();
                JsonNode propSchema = entry.getValue();
                String propDesc = textValue(propSchema.get("description"));
                JsonSchemaElement element = parseSchemaElement(propSchema, propDesc);
                schemaBuilder.addProperty(propName, element);
            });
        }

        // 标记必填字段
        if (requiredNode != null && requiredNode.isArray()) {
            List<String> required = new ArrayList<>();
            requiredNode.forEach(r -> required.add(r.asText()));
            schemaBuilder.required(required);
        }

        return schemaBuilder.build();
    }

    /**
     * 根据 JSON Schema type 字段将属性解析为对应的 {@link JsonSchemaElement}。
     */
    private JsonSchemaElement parseSchemaElement(JsonNode propSchema, String description) {
        String type = textValue(propSchema.get("type"));
        if (type == null) {
            type = "string";
        }
        return switch (type) {
            case "integer", "number" -> JsonNumberSchema.builder()
                    .description(StrUtil.blankToDefault(description, ""))
                    .build();
            case "boolean" -> JsonBooleanSchema.builder()
                    .description(StrUtil.blankToDefault(description, ""))
                    .build();
            case "array" -> {
                JsonNode itemsNode = propSchema.get("items");
                JsonSchemaElement items = itemsNode != null
                        ? parseSchemaElement(itemsNode, null)
                        : JsonStringSchema.builder().build();
                yield JsonArraySchema.builder()
                        .description(StrUtil.blankToDefault(description, ""))
                        .items(items)
                        .build();
            }
            case "object" -> parseJsonObjectSchema(propSchema);
            // string 及其他未知类型统一用 string
            default -> JsonStringSchema.builder()
                    .description(StrUtil.blankToDefault(description, ""))
                    .build();
        };
    }

    // -----------------------------------------------------------------------
    // tool_calls 响应格式构建
    // -----------------------------------------------------------------------

    /**
     * 将 LangChain4j ToolExecutionRequest 列表转为 OpenAI tool_calls JSON 结构。
     * <pre>
     * [
     *   {
     *     "id": "call_xxx",
     *     "type": "function",
     *     "function": { "name": "xxx", "arguments": "{...}" }
     *   }
     * ]
     * </pre>
     */
    private List<Map<String, Object>> buildToolCallsJson(List<ToolExecutionRequest> toolCalls) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            ToolExecutionRequest req = toolCalls.get(i);
            Map<String, Object> fnMap = new LinkedHashMap<>();
            fnMap.put("name", req.name());
            fnMap.put("arguments", StrUtil.blankToDefault(req.arguments(), "{}"));

            Map<String, Object> tcMap = new LinkedHashMap<>();
            tcMap.put("id", StrUtil.blankToDefault(req.id(), "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24)));
            tcMap.put("type", "function");
            tcMap.put("function", fnMap);
            result.add(tcMap);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // 鉴权
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // 配置初始化
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // 用量记录
    // -----------------------------------------------------------------------

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
        try {
            record.setRequestMeta(OBJECT_MAPPER.writeValueAsString(Map.of(
                "uri", request.getRequestURI(),
                "method", request.getMethod(),
                "userAgent", StrUtil.nullToDefault(request.getHeader(HttpHeaders.USER_AGENT), "")
            )));
        } catch (IOException e) {
            record.setRequestMeta("{\"uri\":\"" + request.getRequestURI() + "\"}");
        }
        record.setCreateBy("open-api");
        record.setUpdateBy("open-api");
        record.setDelFlag("0");
        usageRecordService.save(record);
    }

    // -----------------------------------------------------------------------
    // 工具方法
    // -----------------------------------------------------------------------

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

    private String normalizeFinishReason(String finishReason) {
        if (StrUtil.isBlank(finishReason)) {
            return "stop";
        }
        // LangChain4j finishReason.name() 是大写枚举，转为 OpenAI 格式小写
        String lower = finishReason.toLowerCase();
        // TOOL_EXECUTION_REQUESTED → tool_calls
        if (lower.contains("tool")) {
            return "tool_calls";
        }
        // LENGTH → length, STOP → stop
        return lower;
    }

    private String toJsonQuiet(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            return "{}";
        }
    }

    private record AuthResult(AiOpenApiKey openApiKey, ResponseEntity<?> errorResponse) {
    }
}
