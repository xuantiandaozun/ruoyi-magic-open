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
import com.ruoyi.project.ai.domain.AiWorkflow;
import com.ruoyi.project.ai.domain.AiWorkflowStep;
import com.ruoyi.project.ai.dto.AiChatMessage;
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
                        .logResponses(true)
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
            
            // 创建数据库查询工具规范
            JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
                .addStringProperty("sql", "要执行的SQL查询语句")
                .required("sql")
                .build();
            
            ToolSpecification databaseQueryToolSpec = ToolSpecification.builder()
                .name("databaseQuery")
                .description("执行数据库查询并返回结果")
                .parameters(parametersSchema)
                .build();
            
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(enhancedSystemPrompt)) {
                messages.add(SystemMessage.from(enhancedSystemPrompt));
            }
            messages.add(UserMessage.from(message));
            
            // 构建聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(List.of(databaseQueryToolSpec))
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
            
            // 创建数据库查询工具规范
            JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
                .addStringProperty("sql", "要执行的SQL查询语句")
                .required("sql")
                .build();
            
            ToolSpecification databaseQueryToolSpec = ToolSpecification.builder()
                .name("databaseQuery")
                .description("执行数据库查询并返回结果")
                .parameters(parametersSchema)
                .build();
            
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(enhancedSystemPrompt)) {
                messages.add(SystemMessage.from(enhancedSystemPrompt));
            }
            messages.add(UserMessage.from(message));
            
            // 构建聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(List.of(databaseQueryToolSpec))
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
            
            enhancedPrompt.append("# 数据库查询能力说明\n");
            enhancedPrompt.append("你现在具备了数据库查询能力！当用户询问数据相关问题时，你可以使用 `database_query` 工具来执行SQL查询获取准确的数据。\n\n");
            
            enhancedPrompt.append("## 核心原则\n");
            enhancedPrompt.append("1. **数据驱动回答**：当涉及具体数据查询时，必须使用database_query工具获取实际数据\n");
            enhancedPrompt.append("2. **安全第一**：只能执行SELECT查询，严禁任何修改操作\n");
            enhancedPrompt.append("3. **性能优化**：所有查询必须使用LIMIT限制结果数量\n");
            enhancedPrompt.append("4. **准确性保证**：严格按照表结构信息构造SQL语句\n\n");
            
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
            enhancedPrompt.append("- sys_user（用户表）与 sys_role（角色表）通过 sys_user_role 关联\n");
            enhancedPrompt.append("- sys_user（用户表）与 sys_dept（部门表）通过 dept_id 字段关联\n");
            enhancedPrompt.append("- sys_role（角色表）与 sys_menu（菜单表）通过 sys_role_menu 关联\n");
            enhancedPrompt.append("- **重要提醒：请严格根据上述表结构中的实际字段进行查询，不要假设字段存在**\n");
            enhancedPrompt.append("- 常见审计字段（如果表中存在）：create_time、update_time、create_by、update_by\n");
            enhancedPrompt.append("- 常见删除标识字段（如果表中存在）：del_flag（'0'=正常，'2'=删除）\n\n");

            enhancedPrompt.append("=== 查询规则和示例 ===\n");
            enhancedPrompt.append("**重要规则：**\n");
            enhancedPrompt.append("1. **必须严格根据上述表结构中显示的实际字段进行查询，不要使用不存在的字段**\n");
            enhancedPrompt.append("2. 只能执行SELECT查询语句，严禁执行任何修改数据的操作\n");
            enhancedPrompt.append("3. 查询时必须注意性能，建议使用LIMIT限制结果数量（如：LIMIT 10）\n");
            enhancedPrompt.append("4. 字段名请使用反引号包围，如：`user_name`、`role_id`\n");
            enhancedPrompt.append("5. 使用databaseQuery工具来执行SQL查询\n");
            enhancedPrompt.append("6. 查询条件中的字符串值请使用单引号，如：WHERE `status` = '0'\n");
            enhancedPrompt.append("7. 注意区分字段的数据类型，数字类型不需要引号，字符串类型需要引号\n\n");

            enhancedPrompt.append("**常用查询模式（仅在表中存在相应字段时使用）：**\n");
            enhancedPrompt.append("- 查询正常状态的记录（如果有status和del_flag字段）：WHERE `status` = '0' AND `del_flag` = '0'\n");
            enhancedPrompt.append("- 查询未删除的记录（如果有del_flag字段）：WHERE `del_flag` = '0'\n");
            enhancedPrompt.append("- 按时间排序（如果有create_time字段）：ORDER BY `create_time` DESC\n");
            enhancedPrompt.append("- 模糊查询（根据实际字段名）：WHERE `字段名` LIKE '%关键词%'\n\n");

            enhancedPrompt.append("**查询示例（仅供参考，实际查询必须根据上述表结构中的实际字段）：**\n");
            enhancedPrompt.append("```sql\n");
            enhancedPrompt.append("-- 查询前10个正常用户的基本信息（请根据sys_user表的实际字段调整）\n");
            enhancedPrompt.append("SELECT `user_id`, `user_name`, `nick_name`, `email`, `phonenumber` \n");
            enhancedPrompt.append("FROM `sys_user` \n");
            enhancedPrompt.append("WHERE `status` = '0' AND `del_flag` = '0' \n");
            enhancedPrompt.append("ORDER BY `create_time` DESC LIMIT 10;\n\n");

            enhancedPrompt.append("-- 查询所有角色信息\n");
            enhancedPrompt.append("SELECT `role_id`, `role_name`, `role_key`, `role_sort` \n");
            enhancedPrompt.append("FROM `sys_role` \n");
            enhancedPrompt.append("WHERE `del_flag` = '0' \n");
            enhancedPrompt.append("ORDER BY `role_sort`;\n\n");

            enhancedPrompt.append("-- 统计正常用户数量\n");
            enhancedPrompt.append("SELECT COUNT(*) as user_count \n");
            enhancedPrompt.append("FROM `sys_user` \n");
            enhancedPrompt.append("WHERE `status` = '0' AND `del_flag` = '0';\n\n");
            
            enhancedPrompt.append("-- 查询用户及其所属部门信息\n");
            enhancedPrompt.append("SELECT u.`user_name`, u.`nick_name`, d.`dept_name`, d.`leader` \n");
            enhancedPrompt.append("FROM `sys_user` u \n");
            enhancedPrompt.append("LEFT JOIN `sys_dept` d ON u.`dept_id` = d.`dept_id` \n");
            enhancedPrompt.append("WHERE u.`del_flag` = '0' AND d.`del_flag` = '0' \n");
            enhancedPrompt.append("ORDER BY d.`order_num`, u.`user_name` LIMIT 20;\n\n");
            
            enhancedPrompt.append("-- 查询用户的角色分配情况\n");
            enhancedPrompt.append("SELECT u.`user_name`, u.`nick_name`, r.`role_name`, r.`role_key` \n");
            enhancedPrompt.append("FROM `sys_user` u \n");
            enhancedPrompt.append("INNER JOIN `sys_user_role` ur ON u.`user_id` = ur.`user_id` \n");
            enhancedPrompt.append("INNER JOIN `sys_role` r ON ur.`role_id` = r.`role_id` \n");
            enhancedPrompt.append("WHERE u.`del_flag` = '0' AND r.`del_flag` = '0' AND r.`status` = '0' \n");
            enhancedPrompt.append("ORDER BY u.`user_name`, r.`role_sort` LIMIT 30;\n\n");
            
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
            enhancedPrompt.append("- **数据库查询工具**：执行SQL查询获取数据\n");
            enhancedPrompt.append("- **文件操作工具**：读取、写入、处理文件\n");
            enhancedPrompt.append("- **HTTP请求工具**：调用外部API接口\n");
            enhancedPrompt.append("- **GitHub趋势工具**：获取GitHub热门项目信息\n");
            enhancedPrompt.append("- **OSS文件读取工具**：通过OSS URL获取远程文件内容，支持README文档等文件的读取\n");
            enhancedPrompt.append("- **GitHub仓库目录工具**：通过GitHub API获取指定仓库的文件目录结构，支持递归查看和分支选择\n");
            enhancedPrompt.append("- **GitHub文件内容工具**：通过GitHub API获取指定仓库中特定文件的完整内容，支持代码文件、配置文件等\n\n");
            
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
            enhancedPrompt.append("- `model_config_id`：使用的AI模型配置ID\n");
            enhancedPrompt.append("- `system_prompt`：系统提示词\n");
            enhancedPrompt.append("- `user_prompt`：用户提示词（支持变量占位符，如：{{input_variable}}）\n");
            enhancedPrompt.append("- `input_variable`：输入变量名（从前一步或外部输入获取）\n");
            enhancedPrompt.append("- `output_variable`：输出变量名（供后续步骤使用）\n");
            enhancedPrompt.append("- `enabled`：启用状态（1=启用，0=禁用）\n\n");
            
            enhancedPrompt.append("## 工作流管理工具\n");
            enhancedPrompt.append("你可以使用以下工具来管理工作流：\n");
            enhancedPrompt.append("1. **getWorkflowList**：获取系统中配置的工作流列表，包括名称和步骤信息\n");
            enhancedPrompt.append("2. **addWorkflow**：新增工作流，包括工作流基本信息和步骤配置\n");
            enhancedPrompt.append("3. **updateWorkflow**：修改现有工作流，可以更新工作流信息和步骤配置\n\n");
            
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
            enhancedPrompt.append("2. **明确变量传递**：为每个步骤设置清晰的输入输出变量名\n");
            enhancedPrompt.append("3. **优化提示词**：为每个步骤编写专门的系统提示词和用户提示词\n");
            enhancedPrompt.append("4. **选择合适模型**：根据步骤特点选择最适合的AI模型配置\n");
            enhancedPrompt.append("5. **测试验证**：创建工作流后进行充分测试，确保各步骤正常运行\n\n");

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
                    stepInfo.put("enabled", step.getEnabled());
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
            workflow.setStatus("active");
            workflow.setDelFlag("0");
            
            boolean workflowSaved = workflowService.save(workflow);
            if (!workflowSaved) {
                throw new RuntimeException("保存工作流失败");
            }
            
            // 创建工作流步骤
            if (steps != null && !steps.isEmpty()) {
                for (int i = 0; i < steps.size(); i++) {
                    Map<String, Object> stepData = steps.get(i);
                    
                    AiWorkflowStep step = new AiWorkflowStep();
                    step.setWorkflowId(workflow.getId());
                    step.setStepName((String) stepData.get("stepName"));
                    step.setDescription((String) stepData.get("description"));
                    step.setStepOrder(i + 1);
                    step.setSystemPrompt((String) stepData.get("systemPrompt"));
                    step.setUserPrompt((String) stepData.get("userPrompt"));
                    step.setInputVariable((String) stepData.get("inputVariable"));
                    step.setEnabled("1");
                    step.setStatus("active");
                    step.setDelFlag("0");
                    
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
                    step.setSystemPrompt((String) stepData.get("systemPrompt"));
                    step.setUserPrompt((String) stepData.get("userPrompt"));
                    step.setInputVariable((String) stepData.get("inputVariable"));
                    step.setEnabled("1");
                    step.setStatus("active");
                    step.setDelFlag("0");
                    
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
     * 处理工具调用
     */
    private void handleToolCalls(AiMessage aiMessage, List<ChatMessage> messages, 
                                Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            // 添加AI消息到对话历史
            messages.add(aiMessage);
            
            // 创建数据库查询工具规范
            JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
                .addStringProperty("sql", "要执行的SQL查询语句")
                .required("sql")
                .build();
            
            ToolSpecification databaseQueryToolSpec = ToolSpecification.builder()
                .name("databaseQuery")
                .description("执行数据库查询并返回结果")
                .parameters(parametersSchema)
                .build();
            
            // 执行工具调用
            aiMessage.toolExecutionRequests().forEach(toolRequest -> {
                try {
                    String toolName = toolRequest.name();
                    String arguments = toolRequest.arguments();
                    
                    log.info("执行工具调用: {} with arguments: {}", toolName, arguments);
                    
                    String result;
                    if ("databaseQuery".equals(toolName)) {
                        // 解析参数
                        Map<String, Object> args = parseToolArguments(arguments);
                        String sql = (String) args.get("sql");
                        
                        // 执行数据库查询
                        result = executeDatabaseQuery(sql);
                    } else {
                        result = "未知的工具: " + toolName;
                    }
                    
                    // 添加工具执行结果到对话历史
                    messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                    
                    // 发送工具执行结果给用户
                    onToken.accept("\n[工具执行结果] " + result + "\n");
                    
                } catch (Exception e) {
                    log.error("工具调用执行失败: {}", e.getMessage(), e);
                    String errorResult = "工具执行失败: " + e.getMessage();
                    messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                    onToken.accept("\n[工具执行错误] " + errorResult + "\n");
                }
            });
            
            // 构建聊天请求
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(List.of(databaseQueryToolSpec))
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
     * 解析工具参数
     */
    private Map<String, Object> parseToolArguments(String arguments) {
        try {
            if (StrUtil.isBlank(arguments)) {
                return new HashMap<>();
            }
            return objectMapper.readValue(arguments, Map.class);
        } catch (Exception e) {
            log.error("解析工具参数失败: {}", e.getMessage(), e);
            return new HashMap<>();
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
                    switch (historyMessage.getRole().toLowerCase()) {
                        case "user":
                            messages.add(new UserMessage(historyMessage.getContent()));
                            break;
                        case "assistant":
                            messages.add(new AiMessage(historyMessage.getContent()));
                            break;
                        case "system":
                            messages.add(new SystemMessage(historyMessage.getContent()));
                            break;
                        default:
                            log.warn("未知的消息角色: {}", historyMessage.getRole());
                            break;
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(new UserMessage(message));
            
            // 构建工具规范列表
            List<ToolSpecification> toolSpecs = new ArrayList<>();
            
            // 数据库查询工具
            ToolSpecification databaseQueryToolSpec = ToolSpecification.builder()
                .name("database_query")
                .description("执行数据库查询以获取相关信息")
                .parameters(JsonObjectSchema.builder()
                    .addStringProperty("sql", "要执行的SQL查询语句")
                    .required("sql")
                    .build())
                .build();
            toolSpecs.add(databaseQueryToolSpec);
            
            // 获取工作流列表工具
            ToolSpecification getWorkflowListToolSpec = ToolSpecification.builder()
                .name("get_workflow_list")
                .description("获取系统中所有已启用的工作流列表及其步骤信息")
                .parameters(JsonObjectSchema.builder().build())
                .build();
            toolSpecs.add(getWorkflowListToolSpec);
            
            // 添加工作流工具
            ToolSpecification addWorkflowToolSpec = ToolSpecification.builder()
                .name("add_workflow")
                .description("创建新的工作流，包括工作流基本信息和步骤配置")
                .parameters(JsonObjectSchema.builder()
                    .addStringProperty("name", "工作流名称")
                    .addStringProperty("description", "工作流描述")
                    .addStringProperty("type", "工作流类型")
                    .addProperty("steps", dev.langchain4j.model.chat.request.json.JsonArraySchema.builder()
                        .description("工作流步骤列表，每个步骤包含stepName、description、stepOrder、systemPrompt、userPrompt、inputVariable等字段")
                        .build())
                    .required("name", "description", "type", "steps")
                    .build())
                .build();
            toolSpecs.add(addWorkflowToolSpec);
            
            // 修改工作流工具
            ToolSpecification updateWorkflowToolSpec = ToolSpecification.builder()
                .name("update_workflow")
                .description("修改现有工作流的信息和步骤配置")
                .parameters(JsonObjectSchema.builder()
                    .addNumberProperty("workflowId", "要修改的工作流ID")
                    .addStringProperty("name", "工作流名称")
                    .addStringProperty("description", "工作流描述")
                    .addStringProperty("type", "工作流类型")
                    .addProperty("steps", dev.langchain4j.model.chat.request.json.JsonArraySchema.builder()
                        .description("工作流步骤列表，每个步骤包含stepName、description、stepOrder、systemPrompt、userPrompt、inputVariable等字段")
                        .build())
                    .required("workflowId", "name", "description", "type", "steps")
                    .build())
                .build();
            toolSpecs.add(updateWorkflowToolSpec);
            
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
                    switch (historyMessage.getRole().toLowerCase()) {
                        case "user":
                            messages.add(new UserMessage(historyMessage.getContent()));
                            break;
                        case "assistant":
                            messages.add(new AiMessage(historyMessage.getContent()));
                            break;
                        case "system":
                            messages.add(new SystemMessage(historyMessage.getContent()));
                            break;
                        default:
                            log.warn("未知的消息角色: {}", historyMessage.getRole());
                            break;
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(new UserMessage(message));
            
            // 构建工具规范列表
            List<ToolSpecification> toolSpecs = new ArrayList<>();
            
            // 数据库查询工具
            ToolSpecification databaseQueryToolSpec = ToolSpecification.builder()
                .name("database_query")
                .description("执行数据库查询以获取相关信息")
                .parameters(JsonObjectSchema.builder()
                    .addStringProperty("sql", "要执行的SQL查询语句")
                    .required("sql")
                    .build())
                .build();
            toolSpecs.add(databaseQueryToolSpec);
            
            // 获取工作流列表工具
            ToolSpecification getWorkflowListToolSpec = ToolSpecification.builder()
                .name("get_workflow_list")
                .description("获取系统中所有已启用的工作流列表及其步骤信息")
                .parameters(JsonObjectSchema.builder().build())
                .build();
            toolSpecs.add(getWorkflowListToolSpec);
            
            // 添加工作流工具
            ToolSpecification addWorkflowToolSpec = ToolSpecification.builder()
                .name("add_workflow")
                .description("创建新的工作流，包括工作流基本信息和步骤配置")
                .parameters(JsonObjectSchema.builder()
                    .addStringProperty("name", "工作流名称")
                    .addStringProperty("description", "工作流描述")
                    .addStringProperty("type", "工作流类型")
                    .addProperty("steps", JsonArraySchema.builder()
                        .items(JsonObjectSchema.builder()
                            .addStringProperty("stepName", "步骤名称")
                            .addStringProperty("description", "步骤描述")
                            .addNumberProperty("stepOrder", "步骤顺序")
                            .addStringProperty("systemPrompt", "系统提示")
                            .addStringProperty("userPrompt", "用户提示")
                            .addStringProperty("inputVariable", "输入变量")
                            .build())
                        .build())
                    .required("name", "description", "type", "steps")
                    .build())
                .build();
            toolSpecs.add(addWorkflowToolSpec);
            
            // 修改工作流工具
            ToolSpecification updateWorkflowToolSpec = ToolSpecification.builder()
                .name("update_workflow")
                .description("修改现有工作流的信息和步骤配置")
                .parameters(JsonObjectSchema.builder()
                    .addNumberProperty("workflowId", "要修改的工作流ID")
                    .addStringProperty("name", "工作流名称")
                    .addStringProperty("description", "工作流描述")
                    .addStringProperty("type", "工作流类型")
                    .addProperty("steps", JsonArraySchema.builder()
                        .items(JsonObjectSchema.builder()
                            .addStringProperty("stepName", "步骤名称")
                            .addStringProperty("description", "步骤描述")
                            .addNumberProperty("stepOrder", "步骤顺序")
                            .addStringProperty("systemPrompt", "系统提示")
                            .addStringProperty("userPrompt", "用户提示")
                            .addStringProperty("inputVariable", "输入变量")
                            .build())
                        .build())
                    .required("workflowId", "name", "description", "type", "steps")
                    .build())
                .build();
            toolSpecs.add(updateWorkflowToolSpec);
            
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
            
            // 构建工具规范列表
            List<ToolSpecification> toolSpecs = new ArrayList<>();
            
            // 数据库查询工具
            ToolSpecification databaseQueryToolSpec = ToolSpecification.builder()
                .name("database_query")
                .description("执行数据库查询并返回结果")
                .parameters(JsonObjectSchema.builder()
                    .addStringProperty("sql", "要执行的SQL查询语句")
                    .required("sql")
                    .build())
                .build();
            toolSpecs.add(databaseQueryToolSpec);
            
            // 获取工作流列表工具
            ToolSpecification getWorkflowListToolSpec = ToolSpecification.builder()
                .name("get_workflow_list")
                .description("获取系统中所有已启用的工作流列表及其步骤信息")
                .parameters(JsonObjectSchema.builder().build())
                .build();
            toolSpecs.add(getWorkflowListToolSpec);
            
            // 添加工作流工具
            ToolSpecification addWorkflowToolSpec = ToolSpecification.builder()
                .name("add_workflow")
                .description("创建新的工作流，包括工作流基本信息和步骤配置")
                .parameters(JsonObjectSchema.builder()
                    .addStringProperty("name", "工作流名称")
                    .addStringProperty("description", "工作流描述")
                    .addStringProperty("type", "工作流类型")
                    .addProperty("steps", JsonArraySchema.builder()
                        .items(JsonObjectSchema.builder()
                            .addStringProperty("stepName", "步骤名称")
                            .addStringProperty("description", "步骤描述")
                            .addNumberProperty("stepOrder", "步骤顺序")
                            .addStringProperty("systemPrompt", "系统提示")
                            .addStringProperty("userPrompt", "用户提示")
                            .addStringProperty("inputVariable", "输入变量")
                            .build())
                        .build())
                    .required("name", "description", "type", "steps")
                    .build())
                .build();
            toolSpecs.add(addWorkflowToolSpec);
            
            // 修改工作流工具
            ToolSpecification updateWorkflowToolSpec = ToolSpecification.builder()
                .name("update_workflow")
                .description("修改现有工作流的信息和步骤配置")
                .parameters(JsonObjectSchema.builder()
                    .addNumberProperty("workflowId", "要修改的工作流ID")
                    .addStringProperty("name", "工作流名称")
                    .addStringProperty("description", "工作流描述")
                    .addStringProperty("type", "工作流类型")
                    .addProperty("steps", JsonArraySchema.builder()
                        .items(JsonObjectSchema.builder()
                            .addStringProperty("stepName", "步骤名称")
                            .addStringProperty("description", "步骤描述")
                            .addNumberProperty("stepOrder", "步骤顺序")
                            .addStringProperty("systemPrompt", "系统提示")
                            .addStringProperty("userPrompt", "用户提示")
                            .addStringProperty("inputVariable", "输入变量")
                            .build())
                        .build())
                    .required("workflowId", "name", "description", "type", "steps")
                    .build())
                .build();
            toolSpecs.add(updateWorkflowToolSpec);
            
            // 执行工具调用
            aiMessage.toolExecutionRequests().forEach(toolRequest -> {
                try {
                    String toolName = toolRequest.name();
                    String arguments = toolRequest.arguments();
                    
                    log.info("执行工具调用: {} with arguments: {}", toolName, arguments);
                    
                    // 发送工具调用事件
                    onToolCall.accept(toolName, arguments);
                    
                    String result;
                    if ("database_query".equals(toolName)) {
                        // 解析参数
                        Map<String, Object> args = parseToolArguments(arguments);
                        String sql = (String) args.get("sql");
                        
                        // 执行数据库查询
                        result = executeDatabaseQuery(sql);
                    } else if ("get_workflow_list".equals(toolName)) {
                        // 获取工作流列表
                        result = getWorkflowList();
                    } else if ("add_workflow".equals(toolName)) {
                        // 添加工作流
                        Map<String, Object> args = parseToolArguments(arguments);
                        String name = (String) args.get("name");
                        String description = (String) args.get("description");
                        String type = (String) args.get("type");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> steps = (List<Map<String, Object>>) args.get("steps");
                        
                        result = addWorkflow(name, description, type, steps);
                    } else if ("update_workflow".equals(toolName)) {
                        // 修改工作流
                        Map<String, Object> args = parseToolArguments(arguments);
                        Long workflowId = Long.valueOf(args.get("workflowId").toString());
                        String name = (String) args.get("name");
                        String description = (String) args.get("description");
                        String type = (String) args.get("type");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> steps = (List<Map<String, Object>>) args.get("steps");
                        
                        result = updateWorkflow(workflowId, name, description, type, steps);
                    } else {
                        result = "未知的工具: " + toolName;
                    }
                    
                    // 发送工具结果事件
                    onToolResult.accept(toolName, result);
                    
                    // 添加工具执行结果到对话历史
                    messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                    
                } catch (Exception e) {
                    log.error("工具调用执行失败: {}", e.getMessage(), e);
                    String errorResult = "工具执行失败: " + e.getMessage();
                    onToolResult.accept(toolRequest.name(), errorResult);
                    messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                }
            });
            
            // 构建聊天请求
            dev.langchain4j.model.chat.request.ChatRequest chatRequest = dev.langchain4j.model.chat.request.ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(List.of(databaseQueryToolSpec))
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