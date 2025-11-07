package com.ruoyi.project.ai.strategy.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.project.ai.domain.AiChatMessage;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.strategy.AiClientStrategy;
import com.ruoyi.project.ai.tool.LangChain4jToolRegistry;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
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
    private final Integer toolCallDelay; // 工具调用后延时（毫秒）
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    
    // 注入工具注册器
    private final LangChain4jToolRegistry toolRegistry;

    // 速率限制重试配置
    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final long INITIAL_RETRY_DELAY_MS = 3000; // 3秒
    private static final double RETRY_MULTIPLIER = 2.0;
    private static final long MAX_RETRY_DELAY_MS = 60000; // 60秒

    public LangChainGenericClientStrategy(String provider, String model, String endpoint, String apiKey) {
        this.provider = provider;
        this.model = model;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.toolCallDelay = null; // 不设置默认延时
        this.chatModel = buildChatModel();
        this.streamingChatModel = buildStreamingChatModel();
        // 初始化工具注册器
        this.toolRegistry = SpringUtils.getBean(LangChain4jToolRegistry.class);
    }

    public LangChainGenericClientStrategy(AiModelConfig config) {
        this.provider = config.getProvider();
        this.model = config.getModel();
        this.endpoint = config.getEndpoint();
        this.apiKey = config.getApiKey();
        this.toolCallDelay = config.getToolCallDelay(); // 直接使用配置值，可能为空
        this.chatModel = buildChatModel();
        this.streamingChatModel = buildStreamingChatModel();
        // 初始化工具注册器
        this.toolRegistry = SpringUtils.getBean(LangChain4jToolRegistry.class);
    }

    /**
     * 检查是否为速率限制错误
     */
    private boolean isRateLimitError(Throwable error) {
        if (error == null) return false;
        String message = error.getMessage();
        if (message == null) return false;
        
        return message.toLowerCase().contains("rate limit") ||
               message.toLowerCase().contains("rate_limit") ||
               message.toLowerCase().contains("too many requests") ||
               message.toLowerCase().contains("quota exceeded") ||
               message.toLowerCase().contains("rpm") ||
               message.toLowerCase().contains("tpm");
    }

    /**
     * 计算重试延迟时间（指数退避 + 随机抖动）
     */
    private long calculateRetryDelay(int attemptNumber) {
        long baseDelay = (long) (INITIAL_RETRY_DELAY_MS * Math.pow(RETRY_MULTIPLIER, attemptNumber - 1));
        // 添加随机抖动，避免多个请求同时重试
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.min(1000, baseDelay / 4));
        long totalDelay = Math.min(baseDelay + jitter, MAX_RETRY_DELAY_MS);
        
        log.info("[LC4J-{}] Rate limit retry attempt {}, delay: {}ms", provider, attemptNumber, totalDelay);
        return totalDelay;
    }

    /**
     * 带重试的流式聊天执行器
     */
    private void executeStreamChatWithRetry(ChatRequest chatRequest, 
                                          Consumer<String> onToken, 
                                          Runnable onComplete, 
                                          Consumer<Throwable> onError,
                                          int attemptNumber) {
        
        if (attemptNumber > MAX_RETRY_ATTEMPTS) {
            onError.accept(new RuntimeException("达到最大重试次数，请求失败"));
            return;
        }

        streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
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
                        handleToolCallsWithRetry(aiMessage, chatRequest.messages(), onToken, onComplete, onError, 1);
                        return;
                    }
                }
                onComplete.run();
            }
            
            @Override
            public void onError(Throwable error) {
                if (isRateLimitError(error) && attemptNumber <= MAX_RETRY_ATTEMPTS) {
                    log.warn("[LC4J-{}] Rate limit error on attempt {}: {}", provider, attemptNumber, error.getMessage());
                    
                    // 异步延迟重试
                    long delay = calculateRetryDelay(attemptNumber);
                    CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            executeStreamChatWithRetry(chatRequest, onToken, onComplete, onError, attemptNumber + 1);
                        });
                } else {
                    onError.accept(error);
                }
            }
        });
    }

    /**
     * 带重试的工具调用处理器
     */
    private void handleToolCallsWithRetry(AiMessage aiMessage, List<ChatMessage> messages, 
                                        Consumer<String> onToken, Runnable onComplete, 
                                        Consumer<Throwable> onError, int attemptNumber) {
        try {
            handleToolCalls(aiMessage, messages, onToken, onComplete, onError);
        } catch (Exception e) {
            if (isRateLimitError(e) && attemptNumber <= MAX_RETRY_ATTEMPTS) {
                log.warn("[LC4J-{}] Rate limit error in tool call on attempt {}: {}", provider, attemptNumber, e.getMessage());
                
                long delay = calculateRetryDelay(attemptNumber);
                CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        handleToolCallsWithRetry(aiMessage, messages, onToken, onComplete, onError, attemptNumber + 1);
                    });
            } else {
                onError.accept(e);
            }
        }
    }

    /**
     * 执行带工具调用回调的流式聊天重试
     */
    private void executeStreamChatWithCallbacksRetry(ChatRequest chatRequest, 
                                                   List<dev.langchain4j.data.message.ChatMessage> messages,
                                                   Consumer<String> onToken, 
                                                   BiConsumer<String, String> onToolCall,
                                                   BiConsumer<String, String> onToolResult,
                                                   Runnable onComplete, 
                                                   Consumer<Throwable> onError,
                                                   int attemptNumber) {
        if (attemptNumber >= MAX_RETRY_ATTEMPTS) {
            onError.accept(new RuntimeException("流式聊天重试次数已达上限"));
            return;
        }

        try {
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
                    if (isRateLimitError(error)) {
                        long delay = calculateRetryDelay(attemptNumber);
                        log.warn("[LC4J-provider] Rate limit retry attempt {}, delay: {}ms", attemptNumber + 1, delay);
                        log.warn("[LC4J-provider] Rate limit error on attempt {}: {}", attemptNumber + 1, error.getMessage());
                        CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .execute(() -> executeStreamChatWithCallbacksRetry(chatRequest, messages, onToken, onToolCall, onToolResult, onComplete, onError, attemptNumber + 1));
                    } else {
                        onError.accept(error);
                    }
                }
            });
        } catch (Exception e) {
            if (isRateLimitError(e)) {
                long delay = calculateRetryDelay(attemptNumber);
                log.warn("[LC4J-provider] Rate limit retry attempt {}, delay: {}ms", attemptNumber + 1, delay);
                log.warn("[LC4J-provider] Rate limit error on attempt {}: {}", attemptNumber + 1, e.getMessage());
                CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .execute(() -> executeStreamChatWithCallbacksRetry(chatRequest, messages, onToken, onToolCall, onToolResult, onComplete, onError, attemptNumber + 1));
            } else {
                onError.accept(e);
            }
        }
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
            
            // 使用带重试的执行器
            executeStreamChatWithRetry(chatRequest, onToken, onComplete, onError, 1);
                    
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
            
            // 使用带重试的执行器
            executeStreamChatWithRetry(chatRequest, onToken, onComplete, onError, 1);
                    
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
            String[] tables = allowedTables != null ? allowedTables.split(",") : new String[0];
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
     * 解析工具参数
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
                String result;
                
                log.info("开始执行工具调用 - 工具名称: {}, 工具ID: {}, 参数: {}", toolName, toolId, arguments);
                
                try {
                    // 使用ToolRegistry统一执行工具
                    result = toolRegistry.executeTool(toolName, arguments);
                    log.debug("工具调用成功 - 工具名称: {}, 结果长度: {} 字符", toolName, result.length());
                    
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
            
            // 工具调用成功后添加延时，避免触发API速率限制
            if (toolCallDelay != null && toolCallDelay > 0) {
                try {
                    Thread.sleep(toolCallDelay); // 使用配置的延时
                    log.debug("工具调用完成后延时{}毫秒，避免API速率限制", toolCallDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("延时被中断: {}", e.getMessage());
                }
            } else {
                log.debug("工具调用延时未配置或为0，跳过延时");
            }
            
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
        return toolRegistry.getAllToolSpecifications();
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
            
            // 执行流式聊天（带重试）
            executeStreamChatWithCallbacksRetry(chatRequest, messages, onToken, onToolCall, onToolResult, onComplete, onError, 1);
            
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
                    
                    // 使用ToolRegistry统一执行工具
                    String result = toolRegistry.executeTool(toolName, arguments);
                    
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
            
            // 工具调用成功后添加延时，避免触发API速率限制
            if (toolCallDelay != null && toolCallDelay > 0) {
                try {
                    Thread.sleep(toolCallDelay); // 使用配置的延时
                    log.debug("工具调用完成后延时{}毫秒，避免API速率限制", toolCallDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("延时被中断: {}", e.getMessage());
                }
            } else {
                log.debug("工具调用延时未配置或为0，跳过延时");
            }
            
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