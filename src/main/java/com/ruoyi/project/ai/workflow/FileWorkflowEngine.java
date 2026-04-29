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
import com.ruoyi.project.ai.domain.AiWorkflowStepRun;
import com.ruoyi.project.ai.domain.AiWorkflowExecution;
import com.ruoyi.project.ai.service.IAiWorkflowStepRunService;
import com.ruoyi.project.ai.service.IAiWorkflowExecutionService;
import com.ruoyi.project.ai.util.PromptVariableProcessor;
import com.ruoyi.project.ai.util.ToolResultProcessor;
import com.ruoyi.project.ai.workflow.definition.FileWorkflowDefinition;
import com.ruoyi.project.ai.workflow.definition.FileWorkflowStepDefinition;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * V3文件化工作流执行引擎。
 */
@Slf4j
@Service
public class FileWorkflowEngine {

    private final FileWorkflowDefinitionLoader definitionLoader;
    private final AiGateway aiGateway;
    private final IAiWorkflowExecutionService executionService;
    private final IAiWorkflowStepRunService stepRunService;

    public FileWorkflowEngine(FileWorkflowDefinitionLoader definitionLoader,
            AiGateway aiGateway,
            IAiWorkflowExecutionService executionService,
            IAiWorkflowStepRunService stepRunService) {
        this.definitionLoader = definitionLoader;
        this.aiGateway = aiGateway;
        this.executionService = executionService;
        this.stepRunService = stepRunService;
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
        Long legacyWorkflowId = definition.getLegacyWorkflowIds() != null && !definition.getLegacyWorkflowIds().isEmpty()
                ? definition.getLegacyWorkflowIds().get(0)
                : null;
        return execute(definition, legacyWorkflowId, inputData);
    }

    public Map<String, Object> executeByLegacyWorkflowId(Long workflowId, Map<String, Object> inputData) {
        FileWorkflowDefinition definition = definitionLoader.findByLegacyWorkflowId(workflowId)
                .orElseThrow(() -> new ServiceException("文件化工作流不存在，legacyWorkflowId=" + workflowId));
        return execute(definition, workflowId, inputData);
    }

    private Map<String, Object> execute(FileWorkflowDefinition definition, Long legacyWorkflowId,
            Map<String, Object> inputData) {
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

        AiWorkflowExecution execution = new AiWorkflowExecution();
        execution.setWorkflowId(legacyWorkflowId);
        execution.setStatus("running");
        execution.setInputData(JSONUtil.toJsonStr(inputData));
        executionService.save(execution);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("workflowKey", definition.getId());
        output.put("workflowName", definition.getName());
        output.put("workflowVersion", definition.getVersion());
        output.put("workflowHash", definitionHash(definition));
        output.put("executionId", execution.getId());

        try {
            for (FileWorkflowStepDefinition step : definition.getSteps()) {
                executeStep(definition, step, context, output, execution.getId());
            }

            execution.setStatus("completed");
            execution.setOutputData(JSONUtil.toJsonStr(output));
            executionService.updateById(execution);
            return output;
        } catch (Exception e) {
            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            execution.setOutputData(JSONUtil.toJsonStr(output));
            executionService.updateById(execution);
            throw e instanceof ServiceException ? (ServiceException) e : new ServiceException(e.getMessage());
        }
    }

    private void executeStep(FileWorkflowDefinition definition, FileWorkflowStepDefinition step,
            Map<String, Object> context, Map<String, Object> output, Long executionId) {
        long start = System.currentTimeMillis();
        int maxAttempts = Math.max(1, (step.getRetry() == null ? 0 : step.getRetry()) + 1);
        Exception lastError = null;
        AiWorkflowStepRun stepRun = createStepRun(definition, step, context, executionId);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                stepRun.setAttemptCount(attempt);
                updateStepRun(stepRun);
                String promptTemplate = definitionLoader.loadPrompt(step.getPrompt());
                String userPrompt = PromptVariableProcessor.processVariables(promptTemplate, context);
                String systemPrompt = buildSystemPrompt(definition, step);
                Long modelConfigId = step.getModelConfigId() != null ? step.getModelConfigId() : definition.getModelConfigId();
                if (modelConfigId == null) {
                    throw new ServiceException("步骤未配置模型: " + step.getId());
                }

                log.info("执行文件化工作流步骤: workflow={}, step={}, attempt={}/{}",
                        definition.getId(), step.getId(), attempt, maxAttempts);
                WorkflowRunContext.set(WorkflowRunContext.Context.builder()
                        .executionId(executionId)
                        .stepRunId(stepRun.getId())
                        .workflowKey(definition.getId())
                        .stepKey(step.getId())
                        .build());
                String result;
                try {
                    result = aiGateway.chat(modelConfigId, systemPrompt, userPrompt, step.getTools());
                } finally {
                    WorkflowRunContext.clear();
                }
                handleStepResult(step, context, output, result, System.currentTimeMillis() - start, attempt);
                stepRun.setStatus("completed");
                stepRun.setDurationMs(System.currentTimeMillis() - start);
                stepRun.setOutputSnapshot(truncate(result, 12000));
                updateStepRun(stepRun);
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
            stepRun.setStatus("skipped");
            stepRun.setDurationMs(System.currentTimeMillis() - start);
            stepRun.setErrorMessage(lastError != null ? truncate(lastError.getMessage(), 2000) : null);
            updateStepRun(stepRun);
            output.put(step.getId(), Map.of(
                    "status", "skipped",
                    "message", lastError != null ? lastError.getMessage() : "步骤失败但已跳过"));
            return;
        }
        stepRun.setStatus("failed");
        stepRun.setDurationMs(System.currentTimeMillis() - start);
        stepRun.setErrorMessage(lastError != null ? truncate(lastError.getMessage(), 2000) : null);
        updateStepRun(stepRun);
        throw new ServiceException("文件化工作流步骤失败: " + step.getName() + ", "
                + (lastError != null ? lastError.getMessage() : "未知错误"));
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

    private AiWorkflowStepRun createStepRun(FileWorkflowDefinition definition, FileWorkflowStepDefinition step,
            Map<String, Object> context, Long executionId) {
        AiWorkflowStepRun stepRun = new AiWorkflowStepRun();
        stepRun.setExecutionId(executionId);
        stepRun.setWorkflowKey(definition.getId());
        stepRun.setStepKey(step.getId());
        stepRun.setStepName(step.getName());
        stepRun.setStepOrder(definition.getSteps().indexOf(step) + 1);
        stepRun.setModelConfigId(step.getModelConfigId() != null ? step.getModelConfigId() : definition.getModelConfigId());
        stepRun.setStatus("running");
        stepRun.setAttemptCount(0);
        stepRun.setInputSnapshot(truncate(JSONUtil.toJsonStr(context), 12000));
        try {
            stepRunService.save(stepRun);
        } catch (Exception e) {
            log.warn("步骤执行记录写入失败，继续执行主流程: {}", e.getMessage());
        }
        return stepRun;
    }

    private void updateStepRun(AiWorkflowStepRun stepRun) {
        if (stepRun == null || stepRun.getId() == null) {
            return;
        }
        try {
            stepRunService.updateById(stepRun);
        } catch (Exception e) {
            log.warn("步骤执行记录更新失败，继续执行主流程: {}", e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
