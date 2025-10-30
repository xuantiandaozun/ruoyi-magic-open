package com.ruoyi.project.ai.strategy.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.project.ai.domain.AiChatMessage;
import com.ruoyi.project.ai.domain.AiWorkflow;
import com.ruoyi.project.ai.domain.AiWorkflowStep;
import com.ruoyi.project.ai.service.IAiWorkflowService;
import com.ruoyi.project.ai.service.IAiWorkflowStepService;
import com.ruoyi.project.ai.strategy.AiClientStrategy;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
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

    // SQL安全检查的正则表达式
    // 使用单词边界\b确保只匹配完整的SQL关键词，避免误判字段名或表名中包含这些词的情况
    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
        "(?i).*\\b(drop|delete|truncate|alter|create\\s+table|create\\s+database|create\\s+index|create\\s+view|insert|update|grant|revoke|exec|execute|xp_|sp_)\\b.*"
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    public String chatWithHistory(String message, String systemPrompt, List<com.ruoyi.project.ai.domain.AiChatMessage> chatHistory) {
        try {
            // 构建消息列表
            List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
            
            // 添加系统消息（总是构建增强系统消息）
            String enhancedSystemPrompt = buildEnhancedSystemPrompt(StrUtil.isNotBlank(systemPrompt) ? systemPrompt : "");
            messages.add(new SystemMessage(enhancedSystemPrompt));
            
            // 添加聊天历史
            if (chatHistory != null && !chatHistory.isEmpty()) {
                for (com.ruoyi.project.ai.domain.AiChatMessage historyMessage : chatHistory) {
                    String role = safeGetMessageRole(historyMessage);
                    switch (role) {
                        case "user":
                            messages.add(safeCreateUserMessage(historyMessage.getMessageContent()));
                            break;
                        case "assistant":
                            messages.add(safeCreateAiMessage(historyMessage.getMessageContent()));
                            break;
                        case "system":
                            messages.add(safeCreateSystemMessage(historyMessage.getMessageContent()));
                            break;
                        default:
                            log.warn("未知的消息角色: {}", role);
                            break;
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(new UserMessage(message));
            
            // 构建工具规范列表
            List<ToolSpecification> toolSpecs = buildToolSpecifications();
            
            // 构建聊天请求
            dev.langchain4j.model.chat.request.ChatRequest chatRequest = dev.langchain4j.model.chat.request.ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(toolSpecs)
                .build();
            
            // 执行同步聊天
            ChatModel chatModel = buildChatModel();
            if (chatModel == null) {
                throw new RuntimeException("无法构建聊天模型");
            }
            
            dev.langchain4j.model.chat.response.ChatResponse response = chatModel.chat(chatRequest);
            return response.aiMessage().text();
            
        } catch (Exception e) {
            log.error("同步聊天失败: {}", e.getMessage(), e);
            throw new RuntimeException("同步聊天失败: " + e.getMessage());
        }
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
                        .logResponses(false)
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

    /**
     * 安全获取消息角色，防止空指针异常
     * @param message 聊天消息
     * @return 消息角色（小写），如果为空则返回 "user"
     */
    private String safeGetMessageRole(AiChatMessage message) {
        if (message == null) {
            log.warn("AiChatMessage 为 null，使用默认角色 'user'");
            return "user";
        }
        
        String role = message.getMessageRole();
        if (StrUtil.isBlank(role)) {
            log.warn("消息角色为空，消息ID: {}, 使用默认角色 'user'", message.getId());
            return "user";
        }
        
        return role.toLowerCase();
    }

    /**
     * 安全创建用户消息，防止 text null 异常
     * @param text 消息文本
     * @return UserMessage 对象
     */
    private UserMessage safeCreateUserMessage(String text) {
        if (StrUtil.isBlank(text)) {
            log.warn("用户消息内容为空，使用默认内容");
            return new UserMessage("用户消息内容为空");
        }
        return new UserMessage(text);
    }

    /**
     * 安全创建AI消息，防止 text null 异常
     * @param text 消息文本
     * @return AiMessage 对象
     */
    private AiMessage safeCreateAiMessage(String text) {
        if (StrUtil.isBlank(text)) {
            log.warn("AI消息内容为空，使用默认内容");
            return new AiMessage("AI消息内容为空");
        }
        return new AiMessage(text);
    }

    /**
     * 安全创建系统消息，防止 text null 异常
     * @param text 消息文本
     * @return SystemMessage 对象
     */
    private SystemMessage safeCreateSystemMessage(String text) {
        if (StrUtil.isBlank(text)) {
            log.warn("系统消息内容为空，使用默认内容");
            return new SystemMessage("系统消息内容为空");
        }
        return new SystemMessage(text);
    }

    @Override
    public void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        log.info("[LC4J-{}] streamChat with model {}", provider, model);
        try {
            if (streamingChatModel == null) {
                onError.accept(new RuntimeException("流式聊天模型未初始化"));
                return;
            }
            
            // 构建增强的系统提示（包含数据库信息）
            String enhancedSystemPrompt = buildEnhancedSystemPrompt("");
            
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(enhancedSystemPrompt)) {
                messages.add(SystemMessage.from(enhancedSystemPrompt));
            }
            messages.add(UserMessage.from(message));
            
            // 构建聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(buildToolSpecifications())
                .build();
            
            // 执行流式聊天
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    // 直接处理响应内容
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 检查是否有工具调用需要处理
                    if (completeResponse != null && completeResponse.aiMessage() != null) {
                        AiMessage aiMessage = completeResponse.aiMessage();
                        if (aiMessage.hasToolExecutionRequests()) {
                            // 处理工具调用
                            handleToolCalls(aiMessage, messages, onToken, onComplete, onError);
                            return;
                        }
                    }
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
            
            // 构建增强的系统提示（包含数据库信息）
            String enhancedSystemPrompt = buildEnhancedSystemPrompt(systemPrompt);
            
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(enhancedSystemPrompt)) {
                messages.add(SystemMessage.from(enhancedSystemPrompt));
            }
            messages.add(UserMessage.from(message));
            
            // 构建聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(buildToolSpecifications())
                .build();
            
            // 执行流式聊天
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    // 直接处理响应内容
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 检查是否有工具调用需要处理
                    if (completeResponse != null && completeResponse.aiMessage() != null) {
                        AiMessage aiMessage = completeResponse.aiMessage();
                        if (aiMessage.hasToolExecutionRequests()) {
                            // 处理工具调用
                            handleToolCalls(aiMessage, messages, onToken, onComplete, onError);
                            return;
                        }
                    }
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


    @Override
    public void streamChatWithModelConfig(String message, String systemPrompt, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        if (StrUtil.isNotBlank(systemPrompt)) {
            streamChatWithSystem(systemPrompt, message, onToken, onComplete, onError);
        } else {
            streamChat(message, onToken, onComplete, onError);
        }
    }

    /**
     * 构建增强的系统提示，包含数据库表结构信息
     */
    private String buildEnhancedSystemPrompt(String originalPrompt) {
        try {
            // 获取AI可访问的表列表
            Row configRow = Db.selectOneBySql("SELECT config_value FROM sys_config WHERE config_key = ?", "ai.database.allowed_tables");
            String allowedTables = configRow != null ? configRow.getString("config_value") : null;
            if (StrUtil.isBlank(allowedTables)) {
                return originalPrompt;
            }

            StringBuilder enhancedPrompt = new StringBuilder();
            enhancedPrompt.append(originalPrompt).append("\n\n");
            
            enhancedPrompt.append("# 🛠️ 工具调用能力说明\n");
            enhancedPrompt.append("你现在具备了强大的工具调用能力！请严格按照以下指南使用工具：\n\n");
            
            enhancedPrompt.append("## 📋 可用工具列表\n");
            enhancedPrompt.append("1. **database_query** - 执行数据库查询\n");
            enhancedPrompt.append("2. **get_workflow_list** - 获取工作流列表\n");
            enhancedPrompt.append("3. **add_workflow** - 创建新工作流\n");
            enhancedPrompt.append("4. **update_workflow** - 更新现有工作流\n\n");
            
            enhancedPrompt.append("## 🎯 工具调用核心原则\n");
            enhancedPrompt.append("1. **精确匹配**：工具名称必须完全匹配，区分大小写\n");
            enhancedPrompt.append("2. **参数完整**：所有必需参数都必须提供，格式正确\n");
            enhancedPrompt.append("3. **显式指定**：database_query 工具必须在用户提示词中显式指定才能使用，非显式指定不能使用\n");
            enhancedPrompt.append("4. **数据驱动**：涉及数据查询时，必须使用 database_query 工具\n");
            enhancedPrompt.append("5. **安全第一**：只能执行 SELECT 查询，严禁修改操作\n");
            enhancedPrompt.append("6. **性能优化**：所有查询必须使用 LIMIT 限制结果数量\n\n");
            
            enhancedPrompt.append("=== 数据库表结构信息 ===\n");
            enhancedPrompt.append("以下是你可以查询的数据库表详细结构信息。请仔细阅读每个表的字段定义、数据类型、业务含义和约束条件：\n\n");

            // 解析允许的表列表
            String[] tables = allowedTables.split(",");
            for (String tableName : tables) {
                tableName = tableName.trim();
                if (StrUtil.isNotBlank(tableName)) {
                    String tableStructure = getTableStructure(tableName);
                    enhancedPrompt.append(tableStructure);
                }
            }

            enhancedPrompt.append("=== 表关系说明 ===\n");
            enhancedPrompt.append("**主要表关系：**\n");
            enhancedPrompt.append("- **重要提醒：请严格根据上述表结构中的实际字段进行查询，不要假设字段存在**\n");
            enhancedPrompt.append("- 常见审计字段（如果表中存在）：create_time、update_time、create_by、update_by\n");
            enhancedPrompt.append("- 常见删除标识字段（如果表中存在）：del_flag（'0'=正常，'2'=删除）\n\n");

            enhancedPrompt.append("=== 查询规则和示例 ===\n");
            enhancedPrompt.append("**重要规则：**\n");
            enhancedPrompt.append("1. **必须严格根据上述表结构中显示的实际字段进行查询，不要使用不存在的字段**\n");
            enhancedPrompt.append("2. 只能执行SELECT查询语句，严禁执行任何修改数据的操作\n");
            enhancedPrompt.append("3. 查询时必须注意性能，建议使用LIMIT限制结果数量（如：LIMIT 10）\n");
            enhancedPrompt.append("4. 字段名请使用反引号包围，如：`user_name`、`role_id`\n");
            enhancedPrompt.append("5. 使用databaseQuery工具来执行SQL查询（必须在用户提示词中显式指定才能使用）\n");
            enhancedPrompt.append("6. 查询条件中的字符串值请使用单引号，如：WHERE `status` = '0'\n");
            enhancedPrompt.append("7. 注意区分字段的数据类型，数字类型不需要引号，字符串类型需要引号\n\n");

            enhancedPrompt.append("**常用查询模式（仅在表中存在相应字段时使用）：**\n");
            enhancedPrompt.append("- 查询正常状态的记录（如果有status和del_flag字段）：WHERE `status` = '0' AND `del_flag` = '0'\n");
            enhancedPrompt.append("- 查询未删除的记录（如果有del_flag字段）：WHERE `del_flag` = '0'\n");
            enhancedPrompt.append("- 按时间排序（如果有create_time字段）：ORDER BY `create_time` DESC\n");
            enhancedPrompt.append("- 模糊查询（根据实际字段名）：WHERE `字段名` LIKE '%关键词%'\n\n");

            enhancedPrompt.append("**查询示例（仅供参考，实际查询必须根据上述表结构中的实际字段）：**\n");
            enhancedPrompt.append("```sql\n");
            
            enhancedPrompt.append("-- 查询部门层级结构\n");
            enhancedPrompt.append("SELECT `dept_id`, `dept_name`, `parent_id`, `ancestors`, `order_num` \n");
            enhancedPrompt.append("FROM `sys_dept` \n");
            enhancedPrompt.append("WHERE `del_flag` = '0' AND `status` = '0' \n");
            enhancedPrompt.append("ORDER BY `order_num` ASC LIMIT 50;\n\n");
            
            enhancedPrompt.append("-- 查询菜单权限结构\n");
            enhancedPrompt.append("SELECT `menu_id`, `menu_name`, `parent_id`, `menu_type`, `path`, `visible` \n");
            enhancedPrompt.append("FROM `sys_menu` \n");
            enhancedPrompt.append("WHERE `del_flag` = '0' AND `status` = '0' \n");
            enhancedPrompt.append("ORDER BY `parent_id`, `order_num` LIMIT 100;\n");
            enhancedPrompt.append("```\n\n");
            
            enhancedPrompt.append("## 重要提醒\n");
            enhancedPrompt.append("- **必须使用LIMIT**：所有查询都必须添加LIMIT子句，建议不超过100条记录\n");
            enhancedPrompt.append("- **字段名使用反引号**：所有字段名和表名都要用反引号包围，如 `user_name`\n");
            enhancedPrompt.append("- **字符串值用单引号**：字符串值必须用单引号包围，如 '0', 'admin'\n");
            enhancedPrompt.append("- **注意删除标志**：查询时通常需要过滤 `del_flag` = '0' 的正常数据\n");
            enhancedPrompt.append("- **状态字段含义**：'0' 通常表示正常/启用，'1' 表示停用/禁用\n");
            enhancedPrompt.append("- **关联查询优化**：使用适当的JOIN类型，注意性能影响\n\n");

            // 添加工作流功能说明
            enhancedPrompt.append("# AI工作流管理能力说明\n");
            enhancedPrompt.append("你现在具备了AI工作流管理能力！可以帮助用户管理和执行AI工作流，实现复杂的多步骤AI任务自动化。\n\n");
            
            enhancedPrompt.append("## 工作流核心概念\n");
            enhancedPrompt.append("**工作流（Workflow）**：由多个AI步骤组成的自动化任务流程，每个步骤可以配置不同的AI模型和提示词\n");
            enhancedPrompt.append("**工作流步骤（Workflow Step）**：工作流中的单个AI处理节点，包含系统提示词、用户提示词、输入输出变量等配置\n");
            enhancedPrompt.append("**顺序执行**：步骤按照设定的顺序依次执行，前一步的输出可作为后一步的输入\n");
            enhancedPrompt.append("**变量传递**：通过输入变量名和输出变量名实现步骤间的数据传递\n\n");
            
            enhancedPrompt.append("## 工作流类型\n");
            enhancedPrompt.append("- **sequential（顺序工作流）**：最常用的类型，步骤按顺序依次执行\n");
            enhancedPrompt.append("- **langchain4j_agent（LangChain4j代理）**：基于LangChain4j框架的智能代理工作流\n");
            enhancedPrompt.append("- **conditional（条件工作流）**：支持条件分支的工作流\n");
            enhancedPrompt.append("- **loop（循环工作流）**：支持循环执行的工作流\n\n");
            
            enhancedPrompt.append("## 支持的工具类型\n");
            enhancedPrompt.append("- **database_query**：执行SQL查询获取数据\n");
            enhancedPrompt.append("- **blog_save**：保存中文博客文章\n");
            enhancedPrompt.append("- **blog_en_save**：保存英文博客文章\n");
            enhancedPrompt.append("- **social_media_article_save**：保存自媒体文章，支持中英文双语内容和多平台发布\n");
            enhancedPrompt.append("- **github_trending**：获取GitHub今日首次上榜热门仓库信息\n");
            enhancedPrompt.append("- **oss_file_read**：通过OSS URL获取远程文件内容，支持README文档等文件的读取\n");
            enhancedPrompt.append("- **github_repo_tree**：通过GitHub API获取指定仓库的文件目录结构，支持递归查看和分支选择\n");
            enhancedPrompt.append("- **github_file_content**：通过GitHub API获取指定仓库中特定文件的完整内容，支持代码文件、配置文件等\n\n");
            
            enhancedPrompt.append("## 工作流数据结构\n");
            enhancedPrompt.append("**ai_workflow表字段说明：**\n");
            enhancedPrompt.append("- `id`：工作流唯一标识\n");
            enhancedPrompt.append("- `workflow_name`：工作流名称\n");
            enhancedPrompt.append("- `workflow_description`：工作流描述\n");
            enhancedPrompt.append("- `workflow_type`：工作流类型（sequential/langchain4j_agent/conditional/loop）\n");
            enhancedPrompt.append("- `workflow_version`：版本号\n");
            enhancedPrompt.append("- `enabled`：启用状态（1=启用，0=禁用）\n");
            enhancedPrompt.append("- `status`：状态（0=正常，1=停用）\n");
            enhancedPrompt.append("- `config_json`：额外配置参数（JSON格式）\n\n");
            
            enhancedPrompt.append("**ai_workflow_step表字段说明：**\n");
            enhancedPrompt.append("- `id`：步骤唯一标识\n");
            enhancedPrompt.append("- `workflow_id`：所属工作流ID\n");
            enhancedPrompt.append("- `step_name`：步骤名称\n");
            enhancedPrompt.append("- `step_description`：步骤描述\n");
            enhancedPrompt.append("- `step_order`：执行顺序（数字越小越先执行）\n");
            enhancedPrompt.append("- `model_config_id`：使用的AI模型配置ID（工作流默认使用deepseek配置ID=19）\n");
            enhancedPrompt.append("- `system_prompt`：系统提示词\n");
            enhancedPrompt.append("- `user_prompt`：用户提示词（支持变量占位符，如：{{input_variable}}）\n");
            enhancedPrompt.append("- `input_variable`：输入变量名（从前一步或外部输入获取）\n");
            enhancedPrompt.append("- `output_variable`：输出变量名（供后续步骤使用）\n");
            enhancedPrompt.append("- `tool_type`：工具类型（使用英文工具名称，如：database_query、blog_save、blog_en_save、social_media_article_save、github_trending、oss_file_read、github_repo_tree、github_file_content等，多个工具用逗号分隔）\n");
            enhancedPrompt.append("- `tool_enabled`：工具启用状态（1=启用，0=禁用）\n");
            enhancedPrompt.append("- `enabled`：启用状态（1=启用，0=禁用）\n\n");
            
            enhancedPrompt.append("## 工作流管理工具\n");
            enhancedPrompt.append("你可以使用以下工具来管理工作流：\n");
            enhancedPrompt.append("1. **getWorkflowList**：获取系统中配置的工作流列表，包括名称和步骤信息\n");
            enhancedPrompt.append("2. **addWorkflow**：新增工作流，包括工作流基本信息和步骤配置\n");
            enhancedPrompt.append("3. **updateWorkflow**：修改现有工作流，可以更新工作流信息和步骤配置\n");
            enhancedPrompt.append("4. **updateWorkflowStep**：修改工作流中的单个步骤，无需重新配置整个工作流，支持更新步骤名称、描述、顺序、提示词、变量配置、工具配置等\n");
            enhancedPrompt.append("5. **getWorkflowStep**：获取工作流中特定步骤的详细信息，用于查看步骤的当前配置，便于进行精确修改\n\n");
            
            enhancedPrompt.append("## 工作流使用场景示例\n");
            enhancedPrompt.append("- **内容创作流程**：文案生成 → 内容优化 → 格式调整 → 质量检查\n");
            enhancedPrompt.append("- **数据分析流程**：数据查询 → 数据清洗 → 统计分析 → 报告生成\n");
            enhancedPrompt.append("- **代码审查流程**：代码分析 → 问题识别 → 优化建议 → 文档生成\n");
            enhancedPrompt.append("- **客服处理流程**：问题分类 → 知识库查询 → 答案生成 → 质量评估\n");
            enhancedPrompt.append("- **项目文档分析流程**：GitHub仓库目录获取 → 关键文件识别 → 文件内容读取 → 项目总结生成\n");
            enhancedPrompt.append("- **开源项目调研流程**：GitHub趋势获取 → 项目README读取 → 代码结构分析 → 技术评估报告\n");
            enhancedPrompt.append("- **技术文档整理流程**：OSS文档读取 → 内容结构化 → 知识点提取 → 学习指南生成\n\n");
            
            enhancedPrompt.append("## 工作流最佳实践\n");
            enhancedPrompt.append("1. **合理设计步骤顺序**：确保前后步骤的逻辑关系正确\n");
            enhancedPrompt.append("2. **明确变量传递**：为每个步骤设置清晰的输入输出变量名，并遵循以下规则：\n");
            enhancedPrompt.append("   - 第一步：输入变量可以为空（从外部获取数据），但输出变量不能为空\n");
            enhancedPrompt.append("   - 后续步骤：输入变量和输出变量都不能为空，必须明确指定变量名\n");
            enhancedPrompt.append("   - 变量名使用有意义的英文命名，如：user_input, analysis_result, final_report\n");
            enhancedPrompt.append("   - 确保前一步的输出变量名与后一步的输入变量名匹配，实现数据正确传递\n");
            enhancedPrompt.append("3. **优化提示词**：为每个步骤编写专门的系统提示词和用户提示词\n");
            enhancedPrompt.append("4. **选择合适模型**：工作流默认使用deepseek模型配置(ID=19)，确保所有步骤统一使用此配置\n");
            enhancedPrompt.append("5. **工具配置要求**：如果步骤需要调用工具，必须同时配置tool_type和tool_enabled字段，tool_type必须使用英文工具名称（如database_query、blog_save等），不能使用中文名称\n");
            enhancedPrompt.append("6. **用户提示词工具调用规范**：在用户提示词中必须**显式指定工具名称**，AI才能正确调用相应的工具\n");
            enhancedPrompt.append("   - ✅ **正确写法**：\"请使用 github_trending 查询今天上榜的热门仓库信息，选择2-3个最有意思的项目...\"\n");
            enhancedPrompt.append("   - ❌ **错误写法**：\"请分析今天的GitHub热门项目，选择2-3个最有意思的项目...\"\n");
            enhancedPrompt.append("7. **测试验证**：创建工作流后进行充分测试，确保各步骤正常运行\n\n");
            
            // 添加工具调用示例
            enhancedPrompt.append("## 🔧 工具调用示例\n");
            enhancedPrompt.append("以下是正确的工具调用示例，请严格按照此格式调用工具：\n\n");
            
            enhancedPrompt.append("### 1. 数据库查询示例\n");
            enhancedPrompt.append("```\n");
            enhancedPrompt.append("用户问：\"请使用 database_query 查询部门信息\"\n");
            enhancedPrompt.append("正确调用：database_query\n");
            enhancedPrompt.append("参数：{\"sql\": \"SELECT `dept_id`, `dept_name`, `parent_id` FROM `sys_dept` WHERE `del_flag` = '0' LIMIT 10\"}\n");
            enhancedPrompt.append("\n");
            enhancedPrompt.append("用户问：\"查询部门信息\"（未显式指定工具）\n");
            enhancedPrompt.append("错误做法：不能调用 database_query 工具\n");
            enhancedPrompt.append("正确做法：提示用户需要明确指定使用 database_query 工具\n");
            enhancedPrompt.append("```\n\n");
            
            enhancedPrompt.append("### 2. 工作流管理示例\n");
            enhancedPrompt.append("```\n");
            enhancedPrompt.append("用户问：\"显示所有工作流\"\n");
            enhancedPrompt.append("正确调用：get_workflow_list\n");
            enhancedPrompt.append("参数：{}\n");
            enhancedPrompt.append("```\n\n");
            
            enhancedPrompt.append("### 3. 创建工作流示例\n");
            enhancedPrompt.append("```\n");
            enhancedPrompt.append("用户问：\"创建一个数据分析工作流\"\n");
            enhancedPrompt.append("正确调用：add_workflow\n");
            enhancedPrompt.append("参数：{\n");
            enhancedPrompt.append("  \"name\": \"数据分析工作流\",\n");
            enhancedPrompt.append("  \"description\": \"用于数据分析的工作流\",\n");
            enhancedPrompt.append("  \"type\": \"sequential\",\n");
            enhancedPrompt.append("  \"steps\": [...]\n");
            enhancedPrompt.append("}\n");
            enhancedPrompt.append("```\n\n");
            
            enhancedPrompt.append("## ⚠️ 重要提醒\n");
            enhancedPrompt.append("- **database_query 工具使用限制**：只有当用户在提示词中显式指定使用 database_query 或明确要求查询数据库时才能使用，否则不能使用\n");
            enhancedPrompt.append("- 工具名称必须完全匹配：database_query、get_workflow_list、add_workflow、update_workflow\n");
            enhancedPrompt.append("- 参数格式必须是有效的JSON\n");
            enhancedPrompt.append("- SQL查询必须使用反引号包围字段名和表名\n");
            enhancedPrompt.append("- 所有查询都必须包含LIMIT子句\n");
            enhancedPrompt.append("- 当用户明确要求查询数据时，使用database_query工具获取实际数据\n\n");

            return enhancedPrompt.toString();
        } catch (Exception e) {
            log.error("构建增强系统提示失败: {}", e.getMessage(), e);
            return originalPrompt;
        }
    }

    /**
     * 获取表结构信息（结构化格式）
     */
    private String getTableStructure(String tableName) {
        try {
            StringBuilder structure = new StringBuilder();
            
            // 获取表注释
            Row tableInfo = Db.selectOneBySql(
                "SELECT table_comment FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", 
                tableName
            );
            String tableComment = tableInfo != null ? tableInfo.getString("table_comment") : "";
            
            structure.append("**表名：").append(tableName);
            if (StrUtil.isNotBlank(tableComment)) {
                structure.append("（").append(tableComment).append("）");
            }
            structure.append("**\n");
            
            // 获取字段信息
            List<Row> columns = Db.selectListBySql(
                "SELECT column_name, data_type, is_nullable, column_default, column_comment, " +
                "column_key, extra FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position", 
                tableName
            );
            
            if (columns.isEmpty()) {
                structure.append("  无法获取字段信息\n");
                return structure.toString();
            }
            
            structure.append("字段列表：\n");
            for (Row column : columns) {
                String columnName = column.getString("column_name");
                String dataType = column.getString("data_type");
                String isNullable = column.getString("is_nullable");
                String columnDefault = column.getString("column_default");
                String columnComment = column.getString("column_comment");
                String columnKey = column.getString("column_key");
                String extra = column.getString("extra");
                
                structure.append("  - `").append(columnName).append("` (").append(dataType).append(")");
                
                // 添加主键标识
                if ("PRI".equals(columnKey)) {
                    structure.append(" [主键]");
                }
                
                // 添加自增标识
                if (StrUtil.isNotBlank(extra) && extra.contains("auto_increment")) {
                    structure.append(" [自增]");
                }
                
                // 添加非空标识
                if ("NO".equals(isNullable)) {
                    structure.append(" [非空]");
                }
                
                // 添加默认值
                if (StrUtil.isNotBlank(columnDefault)) {
                    structure.append(" [默认值: ").append(columnDefault).append("]");
                }
                
                // 添加字段注释和业务含义
                if (StrUtil.isNotBlank(columnComment)) {
                    structure.append(" - ").append(columnComment);
                }
                
                // 添加常见字段的业务含义说明
                addFieldBusinessMeaning(structure, columnName, dataType);
                
                structure.append("\n");
            }
            
            structure.append("\n");
            return structure.toString();
            
        } catch (Exception e) {
            log.error("获取表 {} 结构信息失败: {}", tableName, e.getMessage(), e);
            return "**表名：" + tableName + "**\n  获取表结构失败: " + e.getMessage() + "\n\n";
        }
    }

    /**
     * 为常见字段添加业务含义说明
     */
    private void addFieldBusinessMeaning(StringBuilder structure, String columnName, String dataType) {
        if (columnName == null) return;
        
        String lowerName = columnName.toLowerCase();
        
        // 状态字段
        if ("status".equals(lowerName)) {
            structure.append(" [状态: '0'=正常, '1'=停用]");
        }
        // 删除标志
        else if ("del_flag".equals(lowerName)) {
            structure.append(" [删除标志: '0'=正常, '2'=删除]");
        }
        // 性别字段
        else if ("sex".equals(lowerName)) {
            structure.append(" [性别: '0'=男, '1'=女, '2'=未知]");
        }
        // 用户类型
        else if ("user_type".equals(lowerName)) {
            structure.append(" [用户类型: '00'=系统用户]");
        }
        // 菜单类型
        else if ("menu_type".equals(lowerName)) {
            structure.append(" [菜单类型: 'M'=目录, 'C'=菜单, 'F'=按钮]");
        }
        // 是否框架
        else if ("is_frame".equals(lowerName)) {
            structure.append(" [是否外链: '0'=否, '1'=是]");
        }
        // 是否缓存
        else if ("is_cache".equals(lowerName)) {
            structure.append(" [是否缓存: '0'=缓存, '1'=不缓存]");
        }
        // 显示状态
        else if ("visible".equals(lowerName)) {
            structure.append(" [显示状态: '0'=显示, '1'=隐藏]");
        }
        // 角色权限
        else if ("data_scope".equals(lowerName)) {
            structure.append(" [数据范围: '1'=全部, '2'=自定义, '3'=本部门, '4'=本部门及以下, '5'=仅本人]");
        }
        // 通知类型
        else if ("notice_type".equals(lowerName)) {
            structure.append(" [公告类型: '1'=通知, '2'=公告]");
        }
        // 操作类型
        else if ("oper_type".equals(lowerName)) {
            structure.append(" [操作类型: 数字代码对应不同操作]");
        }
        // 业务类型
        else if ("business_type".equals(lowerName)) {
            structure.append(" [业务类型: 数字代码对应不同业务]");
        }
        // 时间字段说明
        else if (lowerName.contains("time") && ("datetime".equals(dataType) || "timestamp".equals(dataType))) {
            if ("create_time".equals(lowerName)) {
                structure.append(" [创建时间]");
            } else if ("update_time".equals(lowerName)) {
                structure.append(" [更新时间]");
            } else if ("login_date".equals(lowerName)) {
                structure.append(" [最后登录时间]");
            }
        }
        // 创建者和更新者
        else if ("create_by".equals(lowerName)) {
            structure.append(" [创建者]");
        }
        else if ("update_by".equals(lowerName)) {
            structure.append(" [更新者]");
        }
        // 排序字段
        else if (lowerName.contains("sort") || lowerName.contains("order")) {
            structure.append(" [排序字段: 数字越小越靠前]");
        }
        // IP地址
        else if (lowerName.contains("ip")) {
            structure.append(" [IP地址]");
        }
        // 备注字段
        else if ("remark".equals(lowerName)) {
            structure.append(" [备注信息]");
        }
    }



    /**
     * 执行安全的数据库查询
     */
    private static String executeDatabaseQuery(String sql) {
        try {
            if (StrUtil.isBlank(sql)) {
                return "SQL语句不能为空";
            }

            // 安全检查
            if (!isSafeSql(sql)) {
                return "安全检查失败：只允许执行SELECT查询语句";
            }

            // 限制查询结果数量
            int limit = 100;

            // 检查SQL是否已包含LIMIT子句
            String normalizedSql = sql.trim().toLowerCase();
            if (!normalizedSql.contains("limit")) {
                sql = sql.trim();
                if (!sql.endsWith(";")) {
                    sql += " LIMIT " + limit;
                } else {
                    sql = sql.substring(0, sql.length() - 1) + " LIMIT " + limit + ";";
                }
            }

            // 执行查询
            List<Row> rows = Db.selectListBySql(sql);
            
            if (rows.isEmpty()) {
                return "查询结果为空";
            }

            // 构建结果JSON
            Map<String, Object> result = new HashMap<>();
            result.put("total", rows.size());
            result.put("limit", limit);
            result.put("data",rows);

            return JSONUtil.toJsonStr(result);

        } catch (Exception e) {
            log.error("执行数据库查询失败: {}", e.getMessage(), e);
            return "查询执行失败: " + e.getMessage();
        }
    }

    /**
     * 检查SQL是否安全（只允许SELECT语句）
     */
    private static boolean isSafeSql(String sql) {
        if (StrUtil.isBlank(sql)) {
            return false;
        }

        String normalizedSql = sql.trim().toLowerCase();
        
        // 检查是否以SELECT开头
        if (!normalizedSql.startsWith("select")) {
            return false;
        }

        // 检查是否包含危险操作
        if (DANGEROUS_SQL_PATTERN.matcher(normalizedSql).matches()) {
            return false;
        }

        return true;
    }

    /**
     * 获取工作流列表和步骤信息
     */
    private static String getWorkflowList() {
        try {
            IAiWorkflowService workflowService = SpringUtils.getBean(IAiWorkflowService.class);
            IAiWorkflowStepService stepService = SpringUtils.getBean(IAiWorkflowStepService.class);
            
            // 获取所有启用的工作流
            List<AiWorkflow> workflows = workflowService.listByEnabled("1");
            
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> workflowList = new ArrayList<>();
            
            for (AiWorkflow workflow : workflows) {
                Map<String, Object> workflowInfo = new HashMap<>();
                workflowInfo.put("id", workflow.getId());
                workflowInfo.put("name", workflow.getName());
                workflowInfo.put("description", workflow.getDescription());
                workflowInfo.put("type", workflow.getType());
                workflowInfo.put("version", workflow.getVersion());
                workflowInfo.put("status", workflow.getStatus());
                
                // 获取工作流步骤
                List<AiWorkflowStep> steps = stepService.selectByWorkflowId(workflow.getId());
                List<Map<String, Object>> stepList = new ArrayList<>();
                
                for (AiWorkflowStep step : steps) {
                    Map<String, Object> stepInfo = new HashMap<>();
                    stepInfo.put("id", step.getId());
                    stepInfo.put("stepName", step.getStepName());
                    stepInfo.put("description", step.getDescription());
                    stepInfo.put("stepOrder", step.getStepOrder());
                    stepInfo.put("systemPrompt", step.getSystemPrompt());
                    stepInfo.put("userPrompt", step.getUserPrompt());
                    stepInfo.put("inputVariable", step.getInputVariable());
                    stepInfo.put("outputVariable", step.getOutputVariable());
                    stepInfo.put("enabled", step.getEnabled());
                    stepInfo.put("toolTypes", step.getToolTypes());
                    stepInfo.put("toolEnabled", step.getToolEnabled());
                    stepList.add(stepInfo);
                }
                
                workflowInfo.put("steps", stepList);
                workflowList.add(workflowInfo);
            }
            
            result.put("success", true);
            result.put("count", workflowList.size());
            result.put("workflows", workflowList);
            
            return JSONUtil.toJsonStr(result);
            
        } catch (Exception e) {
            log.error("获取工作流列表失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "获取工作流列表失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }

    /**
     * 验证工作流步骤的变量配置
     * 规则：第一步的输入变量可以为空，但输出变量不能为空
     *      后续步骤的输入输出变量都不能为空
     *      
     * @param steps 工作流步骤列表
     * @throws RuntimeException 如果变量配置不符合规则
     */
    private static void validateWorkflowSteps(List<Map<String, Object>> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> stepData = steps.get(i);
            String stepName = (String) stepData.get("stepName");
            String inputVariable = (String) stepData.get("inputVariable");
            String outputVariable = (String) stepData.get("outputVariable");
            
            // 第一步的特殊验证
            if (i == 0) {
                // 第一步的输出变量不能为空
                if (StrUtil.isBlank(outputVariable)) {
                    throw new RuntimeException("第一步 '" + stepName + "' 的输出变量名不能为空");
                }
            } else {
                // 后续步骤的输入变量不能为空
                if (StrUtil.isBlank(inputVariable)) {
                    throw new RuntimeException("步骤 '" + stepName + "' 的输入变量名不能为空");
                }
                // 后续步骤的输出变量也不能为空
                if (StrUtil.isBlank(outputVariable)) {
                    throw new RuntimeException("步骤 '" + stepName + "' 的输出变量名不能为空");
                }
            }
        }
    }

    /**
     * 新增工作流
     */
    private static String addWorkflow(String name, String description, String type, List<Map<String, Object>> steps) {
        try {
            IAiWorkflowService workflowService = SpringUtils.getBean(IAiWorkflowService.class);
            IAiWorkflowStepService stepService = SpringUtils.getBean(IAiWorkflowStepService.class);
            
            // 创建工作流
            AiWorkflow workflow = new AiWorkflow();
            workflow.setName(name);
            workflow.setDescription(description);
            workflow.setType(type != null ? type : "sequential");
            workflow.setVersion("1.0");
            workflow.setEnabled("1");
            workflow.setStatus("0");
            workflow.setDelFlag("0");
            
            boolean workflowSaved = workflowService.save(workflow);
            if (!workflowSaved) {
                throw new RuntimeException("保存工作流失败");
            }
            
            // 创建工作流步骤
            if (steps != null && !steps.isEmpty()) {
                // 验证步骤变量配置
                validateWorkflowSteps(steps);
                
                for (int i = 0; i < steps.size(); i++) {
                    Map<String, Object> stepData = steps.get(i);
                    
                    AiWorkflowStep step = new AiWorkflowStep();
                    step.setWorkflowId(workflow.getId());
                    step.setStepName((String) stepData.get("stepName"));
                    step.setDescription((String) stepData.get("description"));
                    step.setStepOrder(i + 1);
                    // 固定使用deepseek配置ID为19
                    step.setModelConfigId(19L);
                    step.setSystemPrompt((String) stepData.get("systemPrompt"));
                    step.setUserPrompt((String) stepData.get("userPrompt"));
                    step.setInputVariable((String) stepData.get("inputVariable"));
                    step.setOutputVariable((String) stepData.get("outputVariable"));
                    step.setEnabled("1");
                    step.setStatus("0");
                    step.setDelFlag("0");
                    
                    // 设置工具配置
                    step.setToolTypes((String) stepData.get("toolType"));
                    step.setToolEnabled((String) stepData.get("toolEnabled"));
                    
                    // 如果配置了工具类型，必须配置工具启用状态
                    String toolType = (String) stepData.get("toolType");
                    if (StrUtil.isNotBlank(toolType)) {
                        String toolEnabled = (String) stepData.get("toolEnabled");
                        if (StrUtil.isBlank(toolEnabled)) {
                            step.setToolEnabled("Y"); // 默认启用工具
                        }
                    }
                    
                    stepService.save(step);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workflowId", workflow.getId());
            result.put("message", "工作流创建成功");
            
            return JSONUtil.toJsonStr(result);
            
        } catch (Exception e) {
            log.error("新增工作流失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "新增工作流失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }

    /**
     * 修改工作流
     */
    private static String updateWorkflow(Long workflowId, String name, String description, String type, List<Map<String, Object>> steps) {
        try {
            IAiWorkflowService workflowService = SpringUtils.getBean(IAiWorkflowService.class);
            IAiWorkflowStepService stepService = SpringUtils.getBean(IAiWorkflowStepService.class);
            
            // 更新工作流基本信息
            AiWorkflow workflow = workflowService.getById(workflowId);
            if (workflow == null) {
                throw new RuntimeException("工作流不存在");
            }
            
            if (name != null) workflow.setName(name);
            if (description != null) workflow.setDescription(description);
            if (type != null) workflow.setType(type);
            
            boolean workflowUpdated = workflowService.updateById(workflow);
            if (!workflowUpdated) {
                throw new RuntimeException("更新工作流失败");
            }
            
            // 更新工作流步骤（先删除原有步骤，再添加新步骤）
            if (steps != null) {
                // 验证步骤变量配置
                validateWorkflowSteps(steps);
                
                // 删除原有步骤
                List<AiWorkflowStep> existingSteps = stepService.selectByWorkflowId(workflowId);
                for (AiWorkflowStep existingStep : existingSteps) {
                    existingStep.setDelFlag("1");
                    stepService.updateById(existingStep);
                }
                
                // 添加新步骤
                for (int i = 0; i < steps.size(); i++) {
                    Map<String, Object> stepData = steps.get(i);
                    
                    AiWorkflowStep step = new AiWorkflowStep();
                    step.setWorkflowId(workflowId);
                    step.setStepName((String) stepData.get("stepName"));
                    step.setDescription((String) stepData.get("description"));
                    step.setStepOrder(i + 1);
                    // 固定使用deepseek配置ID为19
                    step.setModelConfigId(19L);
                    step.setSystemPrompt((String) stepData.get("systemPrompt"));
                    step.setUserPrompt((String) stepData.get("userPrompt"));
                    step.setInputVariable((String) stepData.get("inputVariable"));
                    step.setOutputVariable((String) stepData.get("outputVariable"));
                    step.setEnabled("1");
                    step.setStatus("0");
                    step.setDelFlag("0");
                    
                    // 设置工具配置
                    step.setToolTypes((String) stepData.get("toolType"));
                    step.setToolEnabled((String) stepData.get("toolEnabled"));
                    
                    // 如果配置了工具类型，必须配置工具启用状态
                    String toolType = (String) stepData.get("toolType");
                    if (StrUtil.isNotBlank(toolType)) {
                        String toolEnabled = (String) stepData.get("toolEnabled");
                        if (StrUtil.isBlank(toolEnabled)) {
                            step.setToolEnabled("Y"); // 默认启用工具
                        }
                    }
                    
                    stepService.save(step);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workflowId", workflowId);
            result.put("message", "工作流更新成功");
            
            return JSONUtil.toJsonStr(result);
            
        } catch (Exception e) {
            log.error("修改工作流失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "修改工作流失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }

    /**
     * 修改单个工作流步骤
     */
    private static String updateWorkflowStep(Long stepId, String stepName, String description, Integer stepOrder,
                                           String systemPrompt, String userPrompt, String inputVariable, 
                                           String outputVariable, String toolType, String toolEnabled, String enabled) {
        try {
            IAiWorkflowStepService stepService = SpringUtils.getBean(IAiWorkflowStepService.class);
            
            // 获取现有步骤
            AiWorkflowStep step = stepService.getById(stepId);
            if (step == null) {
                throw new RuntimeException("工作流步骤不存在");
            }
            
            // 更新步骤信息（只更新非空字段）
            if (StrUtil.isNotBlank(stepName)) {
                step.setStepName(stepName);
            }
            if (StrUtil.isNotBlank(description)) {
                step.setDescription(description);
            }
            if (stepOrder != null) {
                step.setStepOrder(stepOrder);
            }
            if (StrUtil.isNotBlank(systemPrompt)) {
                step.setSystemPrompt(systemPrompt);
            }
            if (StrUtil.isNotBlank(userPrompt)) {
                step.setUserPrompt(userPrompt);
            }
            if (inputVariable != null) { // 允许设置为空字符串
                step.setInputVariable(inputVariable);
            }
            if (StrUtil.isNotBlank(outputVariable)) {
                step.setOutputVariable(outputVariable);
            }
            if (toolType != null) { // 允许设置为空字符串
                step.setToolTypes(toolType);
            }
            if (StrUtil.isNotBlank(toolEnabled)) {
                step.setToolEnabled(toolEnabled);
            }
            if (StrUtil.isNotBlank(enabled)) {
                step.setEnabled(enabled);
            }
            
            // 如果配置了工具类型，确保工具启用状态正确设置
            if (StrUtil.isNotBlank(step.getToolTypes()) && StrUtil.isBlank(step.getToolEnabled())) {
                step.setToolEnabled("Y"); // 默认启用工具
            }
            
            // 验证步骤配置的合理性
            if (StrUtil.isBlank(step.getOutputVariable())) {
                throw new RuntimeException("输出变量名不能为空");
            }
            
            // 更新步骤
            boolean updated = stepService.updateById(step);
            if (!updated) {
                throw new RuntimeException("更新工作流步骤失败");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("stepId", stepId);
            result.put("message", "工作流步骤更新成功");
            result.put("stepInfo", Map.of(
                "stepName", step.getStepName(),
                "description", step.getDescription(),
                "stepOrder", step.getStepOrder(),
                "inputVariable", step.getInputVariable(),
                "outputVariable", step.getOutputVariable(),
                "toolType", step.getToolTypes(),
                "toolEnabled", step.getToolEnabled(),
                "enabled", step.getEnabled()
            ));
            
            return JSONUtil.toJsonStr(result);
            
        } catch (Exception e) {
            log.error("修改工作流步骤失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "修改工作流步骤失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }

    /**
     * 获取单个工作流步骤详情
     */
    private static String getWorkflowStep(Long stepId) {
        try {
            IAiWorkflowStepService stepService = SpringUtils.getBean(IAiWorkflowStepService.class);
            IAiWorkflowService workflowService = SpringUtils.getBean(IAiWorkflowService.class);
            
            // 获取步骤信息
            AiWorkflowStep step = stepService.getById(stepId);
            if (step == null) {
                throw new RuntimeException("工作流步骤不存在");
            }
            
            // 获取所属工作流信息
            AiWorkflow workflow = workflowService.getById(step.getWorkflowId());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> stepInfo = new HashMap<>();
            stepInfo.put("stepId", step.getId());
            stepInfo.put("workflowId", step.getWorkflowId());
            stepInfo.put("workflowName", workflow.getName());
            stepInfo.put("stepName", step.getStepName());
            stepInfo.put("description", step.getDescription());
            stepInfo.put("stepOrder", step.getStepOrder());
            stepInfo.put("modelConfigId", step.getModelConfigId());
            stepInfo.put("systemPrompt", step.getSystemPrompt());
            stepInfo.put("userPrompt", step.getUserPrompt());
            stepInfo.put("inputVariable", step.getInputVariable());
            stepInfo.put("outputVariable", step.getOutputVariable());
            stepInfo.put("toolType", step.getToolTypes());
            stepInfo.put("toolEnabled", step.getToolEnabled());
            stepInfo.put("enabled", step.getEnabled());
            stepInfo.put("status", step.getStatus());
            stepInfo.put("createTime", step.getCreateTime());
            stepInfo.put("updateTime", step.getUpdateTime());
            
            result.put("stepInfo", stepInfo);
            
            return JSONUtil.toJsonStr(result);
            
        } catch (Exception e) {
            log.error("获取工作流步骤详情失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "获取工作流步骤详情失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }

    /**
     * 处理工具调用
     */
    private void handleToolCalls(AiMessage aiMessage, List<ChatMessage> messages, 
                                Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 添加AI消息到对话历史
            messages.add(aiMessage);
            
            // 使用统一的工具规范构建方法
            List<ToolSpecification> toolSpecs = buildToolSpecifications();
            
            // 执行工具调用
            aiMessage.toolExecutionRequests().forEach(toolRequest -> {
                String toolName = toolRequest.name();
                String arguments = toolRequest.arguments();
                String toolId = toolRequest.id();
                
                log.info("开始执行工具调用 - 工具名称: {}, 工具ID: {}, 参数: {}", toolName, toolId, arguments);
                
                try {
                    String result;
                    Map<String, Object> args = parseToolArguments(arguments);
                    
                    log.debug("解析后的工具参数: {}", args);
                    
                    switch (toolName) {
                        case "database_query":
                            String sql = (String) args.get("sql");
                            if (sql == null || sql.trim().isEmpty()) {
                                throw new IllegalArgumentException("SQL查询语句不能为空");
                            }
                            log.debug("执行数据库查询: {}", sql);
                            result = executeDatabaseQuery(sql);
                            log.debug("数据库查询结果长度: {} 字符", result.length());
                            break;
                        case "get_workflow_list":
                            log.debug("获取工作流列表");
                            result = getWorkflowList();
                            log.debug("工作流列表结果长度: {} 字符", result.length());
                            break;
                        case "add_workflow":
                            String name = (String) args.get("name");
                            String description = (String) args.get("description");
                            String type = (String) args.get("type");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> steps = (List<Map<String, Object>>) args.get("steps");
                            
                            if (name == null || name.trim().isEmpty()) {
                                throw new IllegalArgumentException("工作流名称不能为空");
                            }
                            if (type == null || type.trim().isEmpty()) {
                                throw new IllegalArgumentException("工作流类型不能为空");
                            }
                            
                            log.debug("添加工作流 - 名称: {}, 类型: {}, 步骤数量: {}", name, type, steps != null ? steps.size() : 0);
                            result = addWorkflow(name, description, type, steps);
                            log.debug("添加工作流结果: {}", result);
                            break;
                        case "update_workflow":
                            Object workflowIdObj = args.get("workflow_id");
                            if (workflowIdObj == null) {
                                throw new IllegalArgumentException("工作流ID不能为空");
                            }
                            
                            Long workflowId = Long.valueOf(workflowIdObj.toString());
                            String updateName = (String) args.get("name");
                            String updateDescription = (String) args.get("description");
                            String updateType = (String) args.get("type");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> updateSteps = (List<Map<String, Object>>) args.get("steps");
                            
                            log.debug("更新工作流 - ID: {}, 名称: {}, 类型: {}", workflowId, updateName, updateType);
                            result = updateWorkflow(workflowId, updateName, updateDescription, updateType, updateSteps);
                            log.debug("更新工作流结果: {}", result);
                            break;
                        case "update_workflow_step":
                            Object stepIdObj = args.get("stepId");
                            if (stepIdObj == null) {
                                throw new IllegalArgumentException("步骤ID不能为空");
                            }
                            
                            Long stepId = Long.valueOf(stepIdObj.toString());
                            String stepName = (String) args.get("stepName");
                            String stepDescription = (String) args.get("description");
                            Object stepOrderObj = args.get("stepOrder");
                            String systemPrompt = (String) args.get("systemPrompt");
                            String userPrompt = (String) args.get("userPrompt");
                            String inputVariable = (String) args.get("inputVariable");
                            String outputVariable = (String) args.get("outputVariable");
                            String toolType = (String) args.get("toolType");
                            String toolEnabled = (String) args.get("toolEnabled");
                            String enabled = (String) args.get("enabled");
                            
                            Integer stepOrder = null;
                            if (stepOrderObj != null) {
                                stepOrder = Integer.valueOf(stepOrderObj.toString());
                            }
                            
                            log.debug("更新工作流步骤 - ID: {}, 名称: {}, 顺序: {}", stepId, stepName, stepOrder);
                            result = updateWorkflowStep(stepId, stepName, stepDescription, stepOrder, 
                                                      systemPrompt, userPrompt, inputVariable, outputVariable, 
                                                      toolType, toolEnabled, enabled);
                            log.debug("更新工作流步骤结果: {}", result);
                            break;
                        case "get_workflow_step":
                            Object getStepIdObj = args.get("stepId");
                            if (getStepIdObj == null) {
                                throw new IllegalArgumentException("步骤ID不能为空");
                            }
                            
                            Long getStepId = Long.valueOf(getStepIdObj.toString());
                            
                            log.debug("获取工作流步骤详情 - ID: {}", getStepId);
                            result = getWorkflowStep(getStepId);
                            log.debug("获取工作流步骤详情结果: {}", result);
                            break;
                        default:
                            log.warn("未知的工具名称: {}", toolName);
                            result = "错误: 未知的工具 '" + toolName + "'。可用工具: database_query, get_workflow_list, add_workflow, update_workflow, update_workflow_step, get_workflow_step";
                    }
                    
                    // 添加工具执行结果到对话历史
                    messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                    
                    // 发送工具执行结果给用户
                    onToken.accept("\n[工具执行成功] " + toolName + ": " + result + "\n");
                    log.info("工具调用成功 - 工具名称: {}, 工具ID: {}, 结果长度: {} 字符", toolName, toolId, result.length());
                    
                } catch (IllegalArgumentException e) {
                    log.warn("工具调用参数错误 - 工具名称: {}, 工具ID: {}, 错误: {}", toolName, toolId, e.getMessage());
                    String errorResult = "参数错误: " + e.getMessage();
                    messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                    onToken.accept("\n[工具参数错误] " + toolName + ": " + errorResult + "\n");
                } catch (Exception e) {
                    log.error("工具调用执行失败 - 工具名称: {}, 工具ID: {}, 参数: {}, 错误: {}", 
                             toolName, toolId, arguments, e.getMessage(), e);
                    String errorResult = "工具执行失败: " + e.getMessage() + 
                                       (e.getCause() != null ? " (原因: " + e.getCause().getMessage() + ")" : "");
                    messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                    onToken.accept("\n[工具执行错误] " + toolName + ": " + errorResult + "\n");
                }
            });
            
            // 构建聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(toolSpecs)
                .build();
            
            // 继续对话，让AI基于工具结果生成最终回复
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 检查是否还有更多工具调用
                    if (completeResponse != null && completeResponse.aiMessage() != null) {
                        AiMessage newAiMessage = completeResponse.aiMessage();
                        if (newAiMessage.hasToolExecutionRequests()) {
                            // 递归处理更多工具调用
                            handleToolCalls(newAiMessage, messages, onToken, onComplete, onError);
                            return;
                        }
                    }
                    onComplete.run();
                }
                
                @Override
                public void onError(Throwable error) {
                    onError.accept(error);
                }
            });
            
        } catch (Exception e) {
            log.error("处理工具调用失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("工具调用处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 构建所有可用的工具规范
     */
    private List<ToolSpecification> buildToolSpecifications() {
        List<ToolSpecification> toolSpecs = new ArrayList<>();
        
        // 获取AI可访问的表列表
        String allowedTablesDescription = "执行数据库查询并返回结果。";
        try {
            Row configRow = Db.selectOneBySql("SELECT config_value FROM sys_config WHERE config_key = ?", "ai.database.allowed_tables");
            String allowedTables = configRow != null ? configRow.getString("config_value") : null;
            if (StrUtil.isNotBlank(allowedTables)) {
                allowedTablesDescription += "允许查询的表包括：" + allowedTables + "。";
            }
        } catch (Exception e) {
            log.warn("获取允许访问的表列表失败: {}", e.getMessage());
        }
        
        // 数据库查询工具
        ToolSpecification databaseQueryToolSpec = ToolSpecification.builder()
            .name("database_query")
            .description(allowedTablesDescription)
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("sql", "要执行的SQL查询语句，必须是SELECT语句")
                .required("sql")
                .build())
            .build();
        toolSpecs.add(databaseQueryToolSpec);
        
        // 获取工作流列表工具
        ToolSpecification getWorkflowListToolSpec = ToolSpecification.builder()
            .name("get_workflow_list")
            .description("获取系统中所有已启用的工作流列表及其步骤信息。用于查看现有工作流配置。")
            .parameters(JsonObjectSchema.builder().build())
            .build();
        toolSpecs.add(getWorkflowListToolSpec);
        
        // 添加工作流工具
        ToolSpecification addWorkflowToolSpec = ToolSpecification.builder()
            .name("add_workflow")
            .description("创建新的工作流，包括工作流基本信息和步骤配置。用于自动化复杂的AI任务流程。")
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("name", "工作流名称")
                .addStringProperty("description", "工作流描述")
                .addStringProperty("type", "工作流类型，推荐使用 sequential")
                .addProperty("steps", JsonArraySchema.builder()
                    .items(JsonObjectSchema.builder()
                        .addStringProperty("stepName", "步骤名称")
                        .addStringProperty("description", "步骤描述")
                        .addNumberProperty("stepOrder", "步骤顺序，从1开始")
                        .addStringProperty("systemPrompt", "系统提示词")
                        .addStringProperty("userPrompt", "用户提示词，支持变量占位符如{{input_variable}}")
                        .addStringProperty("inputVariable", "输入变量名，第一步可为空")
                        .addStringProperty("outputVariable", "输出变量名，不能为空")
                        .addStringProperty("toolType", "工具类型，使用英文工具名称，如database_query、blog_save等，多个工具用逗号分隔")
                        .addStringProperty("toolEnabled", "工具启用状态，Y=启用，N=不启用")
                        .build())
                    .build())
                .required("name", "description", "type", "steps")
                .build())
            .build();
        toolSpecs.add(addWorkflowToolSpec);
        
        // 修改工作流工具
        ToolSpecification updateWorkflowToolSpec = ToolSpecification.builder()
            .name("update_workflow")
            .description("修改现有工作流的信息和步骤配置。用于更新已存在的工作流。")
            .parameters(JsonObjectSchema.builder()
                .addNumberProperty("workflowId", "要修改的工作流ID")
                .addStringProperty("name", "工作流名称")
                .addStringProperty("description", "工作流描述")
                .addStringProperty("type", "工作流类型")
                .addProperty("steps", JsonArraySchema.builder()
                    .items(JsonObjectSchema.builder()
                        .addStringProperty("stepName", "步骤名称")
                        .addStringProperty("description", "步骤描述")
                        .addNumberProperty("stepOrder", "步骤顺序，从1开始")
                        .addStringProperty("systemPrompt", "系统提示词")
                        .addStringProperty("userPrompt", "用户提示词，支持变量占位符如{{input_variable}}")
                        .addStringProperty("inputVariable", "输入变量名，第一步可为空")
                        .addStringProperty("outputVariable", "输出变量名，不能为空")
                        .addStringProperty("toolType", "工具类型，使用英文工具名称，如database_query、blog_save等，多个工具用逗号分隔")
                        .addStringProperty("toolEnabled", "工具启用状态，Y=启用，N=不启用")
                        .build())
                    .build())
                .required("workflowId", "name", "description", "type", "steps")
                .build())
            .build();
        toolSpecs.add(updateWorkflowToolSpec);
        
        // 修改单个工作流步骤工具
        ToolSpecification updateWorkflowStepToolSpec = ToolSpecification.builder()
            .name("update_workflow_step")
            .description("修改工作流中的单个步骤配置。用于更新已存在工作流中的特定步骤，而不需要重新配置整个工作流。")
            .parameters(JsonObjectSchema.builder()
                .addNumberProperty("stepId", "要修改的步骤ID")
                .addStringProperty("stepName", "步骤名称")
                .addStringProperty("description", "步骤描述")
                .addNumberProperty("stepOrder", "步骤顺序，从1开始")
                .addStringProperty("systemPrompt", "系统提示词")
                .addStringProperty("userPrompt", "用户提示词，支持变量占位符如{{input_variable}}")
                .addStringProperty("inputVariable", "输入变量名，第一步可为空")
                .addStringProperty("outputVariable", "输出变量名，不能为空")
                .addStringProperty("toolType", "工具类型，使用英文工具名称，如database_query、blog_save等，多个工具用逗号分隔")
                .addStringProperty("toolEnabled", "工具启用状态，Y=启用，N=不启用")
                .addStringProperty("enabled", "步骤启用状态，1=启用，0=禁用")
                .required("stepId")
                .build())
            .build();
        toolSpecs.add(updateWorkflowStepToolSpec);
        
        // 获取工作流步骤详情工具
        ToolSpecification getWorkflowStepToolSpec = ToolSpecification.builder()
            .name("get_workflow_step")
            .description("获取工作流中特定步骤的详细信息。用于查看步骤的当前配置，便于进行精确修改。")
            .parameters(JsonObjectSchema.builder()
                .addNumberProperty("stepId", "要查询的步骤ID")
                .required("stepId")
                .build())
            .build();
        toolSpecs.add(getWorkflowStepToolSpec);
        
        return toolSpecs;
    }
    
    /**
     * 解析工具参数
     */
    private Map<String, Object> parseToolArguments(String arguments) {
        try {
            if (StrUtil.isBlank(arguments)) {
                log.debug("工具参数为空，返回空Map");
                return new HashMap<>();
            }
            
            log.debug("开始解析工具参数: {}", arguments);
            Map<String, Object> result = objectMapper.readValue(arguments, Map.class);
            log.debug("工具参数解析成功，包含 {} 个参数", result.size());
            return result;
            
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("工具参数JSON格式错误 - 参数: {}, 错误位置: 行{} 列{}, 错误: {}", 
                     arguments, e.getLocation().getLineNr(), e.getLocation().getColumnNr(), e.getMessage());
            throw new IllegalArgumentException("JSON格式错误: " + e.getMessage() + 
                                             " (位置: 行" + e.getLocation().getLineNr() + 
                                             " 列" + e.getLocation().getColumnNr() + ")");
        } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
            log.error("工具参数JSON映射错误 - 参数: {}, 错误: {}", arguments, e.getMessage());
            throw new IllegalArgumentException("JSON映射错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("解析工具参数时发生未知错误 - 参数: {}, 错误类型: {}, 错误: {}", 
                     arguments, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new IllegalArgumentException("参数解析失败: " + e.getMessage());
        }
    }

    @Override
    public void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 构建消息列表
            List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
            
            // 添加系统消息（总是构建增强系统消息）
            String enhancedSystemPrompt = buildEnhancedSystemPrompt(StrUtil.isNotBlank(systemPrompt) ? systemPrompt : "");
            messages.add(new SystemMessage(enhancedSystemPrompt));
            
            // 添加聊天历史
            if (chatHistory != null && !chatHistory.isEmpty()) {
                for (AiChatMessage historyMessage : chatHistory) {
                    String role = safeGetMessageRole(historyMessage);
                    switch (role) {
                        case "user":
                            messages.add(safeCreateUserMessage(historyMessage.getMessageContent()));
                            break;
                        case "assistant":
                            messages.add(safeCreateAiMessage(historyMessage.getMessageContent()));
                            break;
                        case "system":
                            messages.add(safeCreateSystemMessage(historyMessage.getMessageContent()));
                            break;
                        default:
                            log.warn("未知的消息角色: {}", role);
                            break;
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(new UserMessage(message));
            
            // 构建工具规范列表
            List<ToolSpecification> toolSpecs = buildToolSpecifications();
            
            // 构建聊天请求
            dev.langchain4j.model.chat.request.ChatRequest chatRequest = dev.langchain4j.model.chat.request.ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(toolSpecs)
                .build();
            
            // 执行流式聊天
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 检查是否有工具调用请求
                    if (completeResponse != null && completeResponse.aiMessage() != null) {
                        AiMessage aiMessage = completeResponse.aiMessage();
                        if (aiMessage.hasToolExecutionRequests()) {
                            // 处理工具调用
                            handleToolCalls(aiMessage, messages, onToken, onComplete, onError);
                            return;
                        }
                    }
                    onComplete.run();
                }
                
                @Override
                public void onError(Throwable error) {
                    onError.accept(error);
                }
            });
            
        } catch (Exception e) {
            log.error("带历史的流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("带历史的流式聊天失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithHistory(String message, String systemPrompt, List<AiChatMessage> chatHistory, 
                                    Consumer<String> onToken, BiConsumer<String, String> onToolCall, 
                                    BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            
            // 添加系统消息（总是构建增强系统消息）
            String enhancedSystemPrompt = buildEnhancedSystemPrompt(StrUtil.isNotBlank(systemPrompt) ? systemPrompt : "");
            messages.add(new SystemMessage(enhancedSystemPrompt));
            
            // 添加聊天历史
            if (chatHistory != null && !chatHistory.isEmpty()) {
                for (AiChatMessage historyMessage : chatHistory) {
                    String role = safeGetMessageRole(historyMessage);
                    switch (role) {
                        case "user":
                            messages.add(safeCreateUserMessage(historyMessage.getMessageContent()));
                            break;
                        case "assistant":
                            messages.add(safeCreateAiMessage(historyMessage.getMessageContent()));
                            break;
                        case "system":
                            messages.add(safeCreateSystemMessage(historyMessage.getMessageContent()));
                            break;
                        default:
                            log.warn("未知的消息角色: {}", role);
                            break;
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(new UserMessage(message));
            
            // 构建工具规范列表
            List<ToolSpecification> toolSpecs = buildToolSpecifications();
            
            // 构建聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(toolSpecs)
                .build();
            
            // 执行流式聊天
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 检查是否有工具调用请求
                    if (completeResponse != null && completeResponse.aiMessage() != null) {
                        AiMessage aiMessage = completeResponse.aiMessage();
                        if (aiMessage.hasToolExecutionRequests()) {
                            // 处理工具调用（带回调）
                            handleToolCallsWithCallbacks(aiMessage, messages, onToken, onToolCall, onToolResult, onComplete, onError);
                            return;
                        }
                    }
                    onComplete.run();
                }
                
                @Override
                public void onError(Throwable error) {
                    onError.accept(error);
                }
            });
            
        } catch (Exception e) {
            log.error("带历史的流式聊天失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("带历史的流式聊天失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithModelConfig(String message, String systemPrompt, 
                                        Consumer<String> onToken, BiConsumer<String, String> onToolCall, 
                                        BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError) {
        streamChatWithHistory(message, systemPrompt, null, onToken, onToolCall, onToolResult, onComplete, onError);
    }

    /**
     * 处理工具调用（带回调）
     */
    private void handleToolCallsWithCallbacks(AiMessage aiMessage, List<dev.langchain4j.data.message.ChatMessage> messages, 
                                            Consumer<String> onToken, BiConsumer<String, String> onToolCall, 
                                            BiConsumer<String, String> onToolResult, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 添加AI消息到对话历史
            messages.add(aiMessage);
            
            // 构建统一的工具规范列表
            List<ToolSpecification> toolSpecs = buildToolSpecifications();
            
            // 执行工具调用
            aiMessage.toolExecutionRequests().forEach(toolRequest -> {
                try {
                    String toolName = toolRequest.name();
                    String arguments = toolRequest.arguments();
                    String toolId = toolRequest.id();
                    
                    log.debug("开始执行工具调用 - 工具ID: {}, 工具名称: {}, 参数: {}", toolId, toolName, arguments);
                    
                    // 发送工具调用事件
                    onToolCall.accept(toolName, arguments);
                    
                    // 解析参数
                    Map<String, Object> args = parseToolArguments(arguments);
                    log.debug("解析后的参数: {}", args);
                    
                    String result;
                    switch (toolName) {
                        case "database_query":
                            String sql = (String) args.get("sql");
                            if (sql == null || sql.trim().isEmpty()) {
                                throw new IllegalArgumentException("缺少必要参数: sql");
                            }
                            log.debug("执行SQL查询: {}", sql);
                            result = executeDatabaseQuery(sql);
                            break;
                            
                        case "get_workflow_list":
                            log.debug("获取工作流列表");
                            result = getWorkflowList();
                            break;
                            
                        case "add_workflow":
                            String name = (String) args.get("name");
                            String description = (String) args.get("description");
                            String type = (String) args.get("type");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> steps = (List<Map<String, Object>>) args.get("steps");
                            
                            // 参数验证
                            if (name == null || name.trim().isEmpty()) {
                                throw new IllegalArgumentException("缺少必要参数: name");
                            }
                            if (description == null || description.trim().isEmpty()) {
                                throw new IllegalArgumentException("缺少必要参数: description");
                            }
                            if (type == null || type.trim().isEmpty()) {
                                throw new IllegalArgumentException("缺少必要参数: type");
                            }
                            if (steps == null || steps.isEmpty()) {
                                throw new IllegalArgumentException("缺少必要参数: steps 或 steps不能为空");
                            }
                            
                            log.debug("添加工作流 - 名称: {}, 类型: {}, 步骤数: {}", name, type, steps.size());
                            result = addWorkflow(name, description, type, steps);
                            break;
                            
                        case "update_workflow":
                            // 支持两种命名方式：workflowId和workflow_id
                            Object workflowIdObj = args.get("workflowId");
                            if (workflowIdObj == null) {
                                workflowIdObj = args.get("workflow_id");
                            }
                            if (workflowIdObj == null) {
                                throw new IllegalArgumentException("缺少必要参数: workflowId 或 workflow_id");
                            }
                            Long workflowId = Long.valueOf(workflowIdObj.toString());
                            String updateName = (String) args.get("name");
                            String updateDescription = (String) args.get("description");
                            String updateType = (String) args.get("type");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> updateSteps = (List<Map<String, Object>>) args.get("steps");
                            
                            log.debug("更新工作流 - ID: {}, 名称: {}", workflowId, updateName);
                            result = updateWorkflow(workflowId, updateName, updateDescription, updateType, updateSteps);
                            break;
                            
                        case "update_workflow_step":
                            Object stepIdObj = args.get("stepId");
                            if (stepIdObj == null) {
                                throw new IllegalArgumentException("缺少必要参数: stepId");
                            }
                            Long stepId = Long.valueOf(stepIdObj.toString());
                            String stepName = (String) args.get("stepName");
                            String stepDescription = (String) args.get("description");
                            Object stepOrderObj = args.get("stepOrder");
                            Integer stepOrder = stepOrderObj != null ? Integer.valueOf(stepOrderObj.toString()) : null;
                            String systemPrompt = (String) args.get("systemPrompt");
                            String userPrompt = (String) args.get("userPrompt");
                            String inputVariable = (String) args.get("inputVariable");
                            String outputVariable = (String) args.get("outputVariable");
                            String toolType = (String) args.get("toolType");
                            Object toolEnabledObj = args.get("toolEnabled");
                            String toolEnabled = toolEnabledObj != null ? toolEnabledObj.toString() : null;
                            Object enabledObj = args.get("enabled");
                            String enabled = enabledObj != null ? enabledObj.toString() : null;
                            
                            log.debug("更新工作流步骤 - 步骤ID: {}, 步骤名称: {}", stepId, stepName);
                            result = updateWorkflowStep(stepId, stepName, stepDescription, stepOrder, 
                                systemPrompt, userPrompt, inputVariable, outputVariable, 
                                toolType, toolEnabled, enabled);
                            break;
                            
                        case "get_workflow_step":
                            Object getStepIdObj = args.get("stepId");
                            if (getStepIdObj == null) {
                                throw new IllegalArgumentException("缺少必要参数: stepId");
                            }
                            Long getStepId = Long.valueOf(getStepIdObj.toString());
                            
                            log.debug("获取工作流步骤详情 - 步骤ID: {}", getStepId);
                            result = getWorkflowStep(getStepId);
                            break;
                            
                        default:
                            log.warn("未知的工具名称: {}", toolName);
                            result = "未知的工具: " + toolName;
                            break;
                    }
                    
                    log.debug("工具调用成功 - 工具ID: {}, 结果长度: {}", toolId, result != null ? result.length() : 0);
                    
                    // 发送工具结果事件
                    onToolResult.accept(toolName, result);
                    
                    // 添加工具执行结果到对话历史
                    messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                    
                } catch (IllegalArgumentException e) {
                    log.error("工具调用参数错误 - 工具: {}, 错误: {}", toolRequest.name(), e.getMessage());
                    String errorResult = "参数错误: " + e.getMessage();
                    onToolResult.accept(toolRequest.name(), errorResult);
                    messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                } catch (Exception e) {
                    log.error("工具调用执行失败 - 工具: {}, 错误: {}", toolRequest.name(), e.getMessage(), e);
                    String errorResult = "工具执行失败: " + e.getMessage();
                    onToolResult.accept(toolRequest.name(), errorResult);
                    messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                }
            });
            
            // 构建聊天请求
            dev.langchain4j.model.chat.request.ChatRequest chatRequest = dev.langchain4j.model.chat.request.ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(toolSpecs)
                .build();
            
            // 继续对话，让AI基于工具结果生成最终回复
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null) {
                        onToken.accept(partialResponse);
                    }
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 检查是否还有更多工具调用
                    if (completeResponse != null && completeResponse.aiMessage() != null) {
                        AiMessage newAiMessage = completeResponse.aiMessage();
                        if (newAiMessage.hasToolExecutionRequests()) {
                            // 递归处理更多工具调用
                            handleToolCallsWithCallbacks(newAiMessage, messages, onToken, onToolCall, onToolResult, onComplete, onError);
                            return;
                        }
                    }
                    onComplete.run();
                }
                
                @Override
                public void onError(Throwable error) {
                    onError.accept(error);
                }
            });
            
        } catch (Exception e) {
            log.error("处理工具调用失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("工具调用处理失败: " + e.getMessage()));
        }
    }
}