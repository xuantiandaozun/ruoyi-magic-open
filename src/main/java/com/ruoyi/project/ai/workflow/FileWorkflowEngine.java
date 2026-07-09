package com.ruoyi.project.ai.workflow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.util.PromptVariableProcessor;
import com.ruoyi.project.ai.util.ToolResultProcessor;
import com.ruoyi.project.ai.workflow.definition.FileWorkflowDefinition;
import com.ruoyi.project.ai.workflow.definition.FileWorkflowStepDefinition;
import com.ruoyi.project.ai.workflow.handler.BlogCoverWorkflowHandler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件化工作流执行引擎，不依赖废弃的工作流数据表。
 */
@Slf4j
@Service
public class FileWorkflowEngine {

    private final FileWorkflowDefinitionLoader definitionLoader;
    private final AiGateway aiGateway;
    private final BlogCoverWorkflowHandler blogCoverWorkflowHandler;

    public FileWorkflowEngine(FileWorkflowDefinitionLoader definitionLoader, AiGateway aiGateway,
            BlogCoverWorkflowHandler blogCoverWorkflowHandler) {
        this.definitionLoader = definitionLoader;
        this.aiGateway = aiGateway;
        this.blogCoverWorkflowHandler = blogCoverWorkflowHandler;
    }

    public boolean supports(String workflowKey) {
        return definitionLoader.supports(workflowKey);
    }

    public boolean supportsLegacyWorkflowId(Long workflowId) {
        return definitionLoader.supportsLegacyWorkflowId(workflowId);
    }

    public Object listWorkflows() {
        return definitionLoader.listWorkflowSummaries();
    }

    public void reload() {
        definitionLoader.load();
    }

    public Map<String, Object> executeByKey(String workflowKey, Map<String, Object> inputData) {
        FileWorkflowDefinition definition = definitionLoader.findByKey(workflowKey)
                .orElseThrow(() -> new ServiceException("文件化工作流不存在: " + workflowKey));
        return execute(definition, inputData);
    }

    public Map<String, Object> executeByLegacyWorkflowId(Long workflowId, Map<String, Object> inputData) {
        FileWorkflowDefinition definition = definitionLoader.findByLegacyWorkflowId(workflowId)
                .orElseThrow(() -> new ServiceException("文件化工作流不存在，legacyWorkflowId=" + workflowId));
        return execute(definition, inputData);
    }

    private Map<String, Object> execute(FileWorkflowDefinition definition, Map<String, Object> inputData) {
        if (definition.getSteps() == null || definition.getSteps().isEmpty()) {
            throw new ServiceException("文件化工作流没有配置步骤: " + definition.getId());
        }

        Map<String, Object> context = new LinkedHashMap<>();
        if (inputData != null) {
            context.putAll(inputData);
        }
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        context.put("today", today);
        context.put("current_date", today);
        context.put("current_weekday", LocalDate.now().getDayOfWeek().toString());

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("workflowKey", definition.getId());
        output.put("workflowName", definition.getName());
        output.put("workflowVersion", definition.getVersion());
        output.put("workflowHash", definitionHash(definition));

        try {
            for (FileWorkflowStepDefinition step : definition.getSteps()) {
                executeStep(definition, step, context, output);
            }
            output.put("status", "completed");
            return output;
        } catch (Exception e) {
            output.put("status", "failed");
            output.put("errorMessage", e.getMessage());
            log.error("文件化工作流执行失败: workflow={}, output={}", definition.getId(), JSONUtil.toJsonStr(output), e);
            throw e instanceof ServiceException ? (ServiceException) e : new ServiceException(e.getMessage());
        }
    }

    private void executeStep(FileWorkflowDefinition definition, FileWorkflowStepDefinition step,
            Map<String, Object> context, Map<String, Object> output) {
        long start = System.currentTimeMillis();
        int maxAttempts = Math.max(1, (step.getRetry() == null ? 0 : step.getRetry()) + 1);
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("执行文件化工作流步骤: workflow={}, step={}, handler={}, attempt={}/{}",
                        definition.getId(), step.getId(), step.getHandler(), attempt, maxAttempts);
                String result = executeStepBody(definition, step, context);
                handleStepResult(step, context, output, result, System.currentTimeMillis() - start, attempt);
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("文件化工作流步骤失败: workflow={}, step={}, attempt={}/{}, error={}",
                        definition.getId(), step.getId(), attempt, maxAttempts, e.getMessage());
                if (attempt >= maxAttempts) {
                    break;
                }
            }
        }

        if ("continue".equalsIgnoreCase(step.getFailurePolicy())) {
            output.put(step.getId(), Map.of(
                    "status", "skipped",
                    "message", lastError != null ? lastError.getMessage() : "步骤失败但已跳过"));
            return;
        }
        throw new ServiceException("文件化工作流步骤失败: " + step.getName() + ", "
                + (lastError != null ? lastError.getMessage() : "未知错误"));
    }

    private String executeStepBody(FileWorkflowDefinition definition, FileWorkflowStepDefinition step,
            Map<String, Object> context) {
        if (StrUtil.isNotBlank(step.getHandler())) {
            return executeDeterministicHandler(step, context);
        }

        String promptTemplate = definitionLoader.loadPrompt(step.getPrompt());
        String userPrompt = PromptVariableProcessor.processVariables(promptTemplate, context);
        String systemPrompt = buildSystemPrompt(definition, step);
        Long modelConfigId = step.getModelConfigId() != null ? step.getModelConfigId() : definition.getModelConfigId();
        if (modelConfigId == null) {
            throw new ServiceException("步骤未配置模型: " + step.getId());
        }
        return aiGateway.chat(modelConfigId, systemPrompt, userPrompt, step.getTools());
    }

    private String executeDeterministicHandler(FileWorkflowStepDefinition step, Map<String, Object> context) {
        String handler = StrUtil.trim(step.getHandler());
        if ("blog_cover".equalsIgnoreCase(handler)) {
            return blogCoverWorkflowHandler.execute(context);
        }
        throw new ServiceException("未知的确定性步骤处理器: " + handler);
    }

    private void handleStepResult(FileWorkflowStepDefinition step, Map<String, Object> context,
            Map<String, Object> output, String result, long durationMs, int attempt) {
        if (ToolResultProcessor.isEmpty(result)) {
            if ("continue".equalsIgnoreCase(step.getEmptyPolicy())) {
                output.put(step.getId(), Map.of(
                        "status", "empty",
                        "message", ToolResultProcessor.getMessage(result),
                        "durationMs", durationMs,
                        "attempt", attempt));
                return;
            }
            throw new ServiceException("步骤返回空数据: " + step.getName() + ", "
                    + ToolResultProcessor.getMessage(result));
        }

        if ("TOOL_EXECUTION_FAILED".equals(StrUtil.trim(result))) {
            if ("continue".equalsIgnoreCase(step.getFailurePolicy())) {
                output.put(step.getId(), Map.of(
                        "status", "tool_failed",
                        "durationMs", durationMs,
                        "attempt", attempt));
                return;
            }
            throw new ServiceException("步骤工具执行失败: " + step.getName());
        }

        Map<String, Object> stepOutput = new HashMap<>();
        stepOutput.put("status", "completed");
        stepOutput.put("durationMs", durationMs);
        stepOutput.put("attempt", attempt);
        stepOutput.put("result", result);
        if (StrUtil.isNotBlank(step.getHandler())) {
            stepOutput.put("handler", step.getHandler());
        }
        output.put(step.getId(), stepOutput);

        if (StrUtil.isNotBlank(step.getOutput())) {
            context.put(step.getOutput(), result);
            output.put(step.getOutput(), result);
        }
    }

    private String buildSystemPrompt(FileWorkflowDefinition definition, FileWorkflowStepDefinition step) {
        return "你是文件化AI工作流中的一个可靠步骤执行器。\n"
                + "工作流: " + definition.getName() + " (" + definition.getId() + ")\n"
                + "当前步骤: " + step.getName() + " (" + step.getId() + ")\n"
                + "请优先完成当前步骤目标。工具返回空数据时，根据提示词中的降级要求继续完成可完成的部分。";
    }

    private String definitionHash(FileWorkflowDefinition definition) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(JSONUtil.toJsonStr(definition).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(hash.length, 8); i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
