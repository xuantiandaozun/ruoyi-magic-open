package com.ruoyi.project.ai.strategy.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ruoyi.project.ai.strategy.AiClientStrategy;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatFunction;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.completion.chat.ChatTool;
import com.volcengine.ark.runtime.model.completion.chat.ChatToolCall;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.service.ArkService;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;


/**
 * 豆包SDK策略实现（使用火山引擎 ArkService）
 */
public class DoubaoSdkClientStrategy implements AiClientStrategy {

    private static final Logger log = LoggerFactory.getLogger(DoubaoSdkClientStrategy.class);

    private final String apiKey;
    private final String endpoint; // ArkService 内部使用默认域名，保留以备扩展
    private final String model;
    private final String imageModel;

    // SQL安全检查的正则表达式
    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
        "(?i).*(drop|delete|truncate|alter|create|insert|update|grant|revoke|exec|execute|xp_|sp_).*"
    );

    public DoubaoSdkClientStrategy(String apiKey, String endpoint, String model, String imageModel) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.imageModel = imageModel;
    }

    private ArkService buildService() {
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        return ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String chat(String message) {
        try {
            ArkService service = buildService();
            List<ChatMessage> chatMessages = new ArrayList<>();
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(message)
                    .build();
            chatMessages.add(userMessage);
            
            ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages);
            return service.createChatCompletion(builder.build())
                    .getChoices().get(0).getMessage().getContent().toString();
        } catch (Exception e) {
            log.error("豆包SDK chat 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("豆包SDK chat 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithSystem(String systemPrompt, String message, boolean returnJson) {
        try {
            ArkService service = buildService();
            List<ChatMessage> messagesForReqList = new ArrayList<>();
            ChatMessage systemMessage = ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt)
                    .build();
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(message)
                    .build();
            messagesForReqList.add(systemMessage);
            messagesForReqList.add(userMessage);
            ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messagesForReqList);
            return service.createChatCompletion(builder.build())
                    .getChoices().get(0).getMessage().getContent().toString();
        } catch (Exception e) {
            log.error("豆包SDK chatWithSystem 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("豆包SDK chatWithSystem 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithHistory(List<String> messages) {
        try {
            ArkService service = buildService();
            List<ChatMessage> chatMessages = new ArrayList<>();
            for (String msg : messages) {
                chatMessages.add(ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(msg)
                        .build());
            }
            ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages);
            return service.createChatCompletion(builder.build())
                    .getChoices().get(0).getMessage().getContent().toString();
        } catch (Exception e) {
            log.error("豆包SDK chatWithHistory 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("豆包SDK chatWithHistory 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatVision(String message, List<String> imageUrls) {
        // Ark SDK 目前图文能力通常通过 Hutool DoubaoService；保留占位，直接串联文本返回
        return chat(message);
    }

    @Override
    public String generateImage(String prompt, String size, Double guidanceScale, Integer seed, Boolean watermark) {
        try {
            ArkService service = buildService();
            GenerateImagesRequest.Builder requestBuilder = GenerateImagesRequest.builder()
                    .model(StrUtil.isNotBlank(imageModel) ? imageModel : model)
                    .prompt(prompt);
            if (StrUtil.isNotBlank(size)) {
                requestBuilder.size(size);
            }
            if (guidanceScale != null) {
                requestBuilder.guidanceScale(guidanceScale);
            }
            if (seed != null) {
                requestBuilder.seed(seed);
            }
            if (watermark != null) {
                requestBuilder.watermark(watermark);
            }
            ImagesResponse response = service.generateImages(requestBuilder.build());
            return response.getData().get(0).getUrl();
        } catch (Exception e) {
            log.error("豆包SDK generateImage 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("豆包SDK generateImage 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String embeddingText(String[] texts) {
        // Ark SDK 未直接提供，返回占位或交由通用实现
        throw new UnsupportedOperationException("豆包SDK暂不支持 embeddingText，请使用通用实现");
    }

    @Override
    public String embeddingVision(String text, String imageUrl) {
        throw new UnsupportedOperationException("豆包SDK暂不支持 embeddingVision，请使用通用实现");
    }

    @Override
    public String batchChat(String prompt) {
        // 可复用 chat
        return chat(prompt);
    }

    @Override
    public String createVideoTask(String prompt, String imageUrl) {
        // 暂未统一封装，返回占位
        throw new UnsupportedOperationException("豆包SDK暂不支持 createVideoTask 封装，请保留原实现或使用通用实现");
    }

    @Override
    public String getVideoTaskStatus(String taskId) {
        throw new UnsupportedOperationException("豆包SDK暂不支持 getVideoTaskStatus 封装，请保留原实现或使用通用实现");
    }

    @Override
    public String tokenization(String[] texts) {
        // 简单分词占位
        return String.join(" ", texts);
    }

    @Override
    public String createContext(List<String> messages) {
        // 简易上下文占位：返回拼接hash
        return Integer.toHexString(String.join("|", messages).hashCode());
    }

    @Override
    public String chatWithContext(String message, String contextId) {
        // 简易占位：忽略context，直接chat
        return chat(message);
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public void streamChat(String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            ArkService service = buildService();
            List<ChatMessage> chatMessages = new ArrayList<>();
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(message)
                    .build();
            chatMessages.add(userMessage);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages)
                    .stream(true) // 启用流式响应
                    .build();
            
            // 使用豆包SDK的流式接口
            service.streamChatCompletion(request)
                    .doOnNext(chunk -> {
                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            String content = chunk.getChoices().get(0).getMessage().getContent().toString();
                            if (content != null && !content.isEmpty()) {
                                onToken.accept(content);
                            }
                        }
                    })
                    .doOnComplete(() -> onComplete.run())
                    .doOnError(throwable -> onError.accept(throwable))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("豆包SDK streamChat 调用失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("豆包SDK streamChat 调用失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithSystem(String systemPrompt, String message, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            ArkService service = buildService();
            List<ChatMessage> messagesForReqList = new ArrayList<>();
            
            // 构建增强的系统消息，包含数据库表结构信息
            String enhancedSystemPrompt = buildEnhancedSystemPrompt(systemPrompt);
            
            ChatMessage systemMessage = ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(enhancedSystemPrompt)
                    .build();
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(message)
                    .build();
            messagesForReqList.add(systemMessage);
            messagesForReqList.add(userMessage);
            
            // 准备工具定义
            List<ChatTool> tools = new ArrayList<>();
            tools.add(createDatabaseQueryTool());
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messagesForReqList)
                    .tools(tools)
                    .stream(true) // 启用流式响应
                    .build();
            
            // 使用豆包SDK的流式接口
            service.streamChatCompletion(request)
                    .doOnNext(chunk -> {
                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            var choice = chunk.getChoices().get(0);
                            var chatMessage = choice.getMessage();
                            
                            // 处理普通文本响应
                            if (chatMessage.getContent() != null && !chatMessage.getContent().toString().isEmpty()) {
                                onToken.accept(chatMessage.getContent().toString());
                            }
                            
                            // 处理工具调用
                            if (chatMessage.getToolCalls() != null && !chatMessage.getToolCalls().isEmpty()) {
                                handleToolCalls(chatMessage.getToolCalls(), service, messagesForReqList, tools, onToken, onComplete, onError);
                                return; // 工具调用处理中，不执行onComplete
                            }
                        }
                    })
                    .doOnComplete(() -> onComplete.run())
                    .doOnError(throwable -> onError.accept(throwable))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("豆包SDK streamChatWithSystem 调用失败: {}", e.getMessage(), e);
            onError.accept(new RuntimeException("豆包SDK streamChatWithSystem 调用失败: " + e.getMessage()));
        }
    }

    @Override
    public void streamChatWithModelConfig(String message, String systemPrompt, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        // 对于DoubaoSdkClientStrategy，直接委托给streamChatWithSystem方法
        // 因为模型配置已经在构造函数中设置了
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
            enhancedPrompt.append("=== 数据库信息 ===\n");
            enhancedPrompt.append("你可以查询以下数据库表的数据：\n\n");

            // 解析允许的表列表
            String[] tables = allowedTables.split(",");
            for (String tableName : tables) {
                tableName = tableName.trim();
                if (StrUtil.isNotBlank(tableName)) {
                    String tableStructure = getTableStructure(tableName);
                    enhancedPrompt.append(tableStructure).append("\n");
                }
            }

            enhancedPrompt.append("\n注意事项：\n");
            enhancedPrompt.append("1. 只能执行SELECT查询语句\n");
            enhancedPrompt.append("2. 禁止执行任何修改数据的操作（INSERT、UPDATE、DELETE等）\n");
            enhancedPrompt.append("3. 查询时请注意性能，避免全表扫描\n");
            enhancedPrompt.append("4. 使用database_query工具来执行SQL查询\n\n");

            return enhancedPrompt.toString();
        } catch (Exception e) {
            log.error("构建增强系统提示失败: {}", e.getMessage(), e);
            return originalPrompt;
        }
    }

    /**
     * 获取表结构信息
     */
    private String getTableStructure(String tableName) {
        try {
            // 查询表结构
            String sql = "DESCRIBE " + tableName;
            List<Row> columns = Db.selectListBySql(sql);

            StringBuilder structure = new StringBuilder();
            structure.append("表名: ").append(tableName).append("\n");
            structure.append("字段信息:\n");

            for (Row column : columns) {
                String field = column.getString("Field");
                String type = column.getString("Type");
                String nullable = column.getString("Null");
                String key = column.getString("Key");
                String defaultValue = column.getString("Default");
                String extra = column.getString("Extra");

                structure.append("  - ").append(field)
                         .append(" (").append(type).append(")");
                
                if ("NO".equals(nullable)) {
                    structure.append(" NOT NULL");
                }
                if ("PRI".equals(key)) {
                    structure.append(" PRIMARY KEY");
                }
                if (StrUtil.isNotBlank(extra)) {
                    structure.append(" ").append(extra);
                }
                structure.append("\n");
            }

            return structure.toString();
        } catch (Exception e) {
            log.error("获取表 {} 结构失败: {}", tableName, e.getMessage(), e);
            return "表名: " + tableName + " (获取结构失败)\n";
        }
    }

    /**
     * 创建数据库查询工具
     */
    private ChatTool createDatabaseQueryTool() {
        return new ChatTool(
            "function",
            new ChatFunction.Builder()
                .name("database_query")
                .description("执行数据库查询，只支持SELECT语句")
                .parameters(new DatabaseQueryParameters(
                    "object",
                    new HashMap<String, Object>() {{
                        put("sql", new HashMap<String, String>() {{
                            put("type", "string");
                            put("description", "要执行的SQL查询语句，只支持SELECT语句");
                        }});
                        put("limit", new HashMap<String, String>() {{
                            put("type", "integer");
                            put("description", "查询结果数量限制，默认100，最大1000");
                        }});
                    }},
                    Arrays.asList("sql") // sql是必需参数
                ))
                .build()
        );
    }

    /**
     * 执行安全的数据库查询
     */
    private String executeDatabaseQuery(Map<String, Object> arguments) {
        try {
            String sql = (String) arguments.get("sql");
            Integer limit = arguments.get("limit") != null ? 
                Integer.parseInt(arguments.get("limit").toString()) : 100;

            if (StrUtil.isBlank(sql)) {
                return "SQL语句不能为空";
            }

            // 安全检查
            if (!isSafeSql(sql)) {
                return "安全检查失败：只允许执行SELECT查询语句";
            }

            // 限制查询结果数量
            if (limit > 1000) limit = 1000;
            if (limit < 1) limit = 100;

            // 如果SQL中没有LIMIT，自动添加
            if (!sql.toUpperCase().contains("LIMIT")) {
                sql += " LIMIT " + limit;
            }

            log.info("执行数据库查询: {}", sql);

            // 执行查询
            List<Row> rows = Db.selectListBySql(sql);

            // 转换为JSON格式
            List<Object> result = new ArrayList<>();
            for (Row row : rows) {
                result.add(row.toCamelKeysMap());
            }

            String jsonResult = JSONUtil.toJsonStr(result);
            log.info("数据库查询成功，返回{}条记录", rows.size());
            
            return "查询成功，返回 " + rows.size() + " 条记录：\n" + jsonResult;

        } catch (Exception e) {
            log.error("执行数据库查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 检查SQL是否安全（只允许SELECT语句）
     */
    private boolean isSafeSql(String sql) {
        if (StrUtil.isBlank(sql)) {
            return false;
        }

        // 转换为小写并去除多余空格
        String cleanSql = sql.trim().toLowerCase();

        // 检查是否以SELECT开头
        if (!cleanSql.startsWith("select")) {
            return false;
        }

        // 检查是否包含危险的SQL关键字
        if (DANGEROUS_SQL_PATTERN.matcher(cleanSql).matches()) {
            return false;
        }

        // 检查是否包含分号（防止SQL注入）
        if (cleanSql.contains(";") && !cleanSql.endsWith(";")) {
            return false;
        }

        return true;
    }

    /**
     * 处理工具调用
     */
    private void handleToolCalls(List<ChatToolCall> toolCalls, ArkService service, List<ChatMessage> messages, 
                                List<ChatTool> tools, Consumer<String> onMessage, Runnable onComplete, 
                                Consumer<Throwable> onError) {
        try {
            // 添加助手消息
            ChatMessage assistantMessage = ChatMessage.builder()
                    .role(ChatMessageRole.ASSISTANT)
                    .toolCalls(toolCalls)
                    .build();
            messages.add(assistantMessage);

            // 执行每个工具调用
            for (ChatToolCall toolCall : toolCalls) {
                String functionName = toolCall.getFunction().getName();
                String argumentsJson = toolCall.getFunction().getArguments();
                
                log.info("执行工具调用: {} with arguments: {}", functionName, argumentsJson);
                
                // 解析参数
                Map<String, Object> arguments = new HashMap<>();
                if (StrUtil.isNotBlank(argumentsJson)) {
                    try {
                        arguments = JSONUtil.toBean(argumentsJson, Map.class);
                    } catch (Exception e) {
                        log.error("解析工具参数失败: {}", e.getMessage());
                    }
                }
                
                // 执行工具函数
                String result = executeToolFunction(functionName, arguments);
                
                // 添加工具执行结果消息
                ChatMessage toolMessage = ChatMessage.builder()
                        .role(ChatMessageRole.TOOL)
                        .content(result)
                        .toolCallId(toolCall.getId())
                        .build();
                messages.add(toolMessage);
            }

            // 继续对话
            ChatCompletionRequest followUpRequest = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .tools(tools)
                    .stream(true)
                    .build();
            
            service.streamChatCompletion(followUpRequest)
                    .doOnNext(chunk -> {
                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            var choice = chunk.getChoices().get(0);
                            var chatMessage = choice.getMessage();
                            
                            // 处理普通文本响应
                            if (chatMessage.getContent() != null && !chatMessage.getContent().toString().isEmpty()) {
                                onMessage.accept(chatMessage.getContent().toString());
                            }
                            
                            // 处理工具调用
                            if (chatMessage.getToolCalls() != null && !chatMessage.getToolCalls().isEmpty()) {
                                handleToolCalls(chatMessage.getToolCalls(), service, messages, tools, onMessage, onComplete, onError);
                                return; // 工具调用处理中，不执行onComplete
                            }
                        }
                    })
                    .doOnComplete(() -> onComplete.run())
                    .doOnError(throwable -> onError.accept(throwable))
                    .subscribe();
            
        } catch (Exception e) {
            log.error("处理工具调用失败: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }

    /**
     * 执行具体的工具函数
     */
    private String executeToolFunction(String functionName, Map<String, Object> arguments) {
        try {
            switch (functionName) {
                case "database_query":
                    return executeDatabaseQuery(arguments);
                default:
                    return "未知的工具函数: " + functionName;
            }
        } catch (Exception e) {
            log.error("执行工具函数 {} 失败: {}", functionName, e.getMessage(), e);
            return "执行工具函数失败: " + e.getMessage();
        }
    }

    /**
     * 数据库查询参数类
     */
    public static class DatabaseQueryParameters {
        public String type;
        public Map<String, Object> properties;
        public List<String> required;

        public DatabaseQueryParameters(String type, Map<String, Object> properties, List<String> required) {
            this.type = type;
            this.properties = properties;
            this.required = required;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
        public List<String> getRequired() { return required; }
        public void setRequired(List<String> required) { this.required = required; }
    }
}