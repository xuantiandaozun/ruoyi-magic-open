package com.ruoyi.project.ai.workflow;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.workflow.definition.FileWorkflowDefinition;
import com.ruoyi.project.ai.workflow.definition.FileWorkflowStepDefinition;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 从 classpath 加载文件化工作流定义。
 */
@Slf4j
@Component
public class FileWorkflowDefinitionLoader {

    private static final String WORKFLOW_PATTERN = "classpath*:ai-workflows/*.yaml";

    private final Map<String, FileWorkflowDefinition> workflows = new LinkedHashMap<>();
    private final Map<Long, String> legacyIdIndex = new LinkedHashMap<>();

    @PostConstruct
    public void load() {
        workflows.clear();
        legacyIdIndex.clear();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(WORKFLOW_PATTERN);
            Yaml yaml = new Yaml();
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Object loaded = yaml.load(inputStream);
                    if (!(loaded instanceof Map)) {
                        continue;
                    }
                    FileWorkflowDefinition definition = toDefinition((Map<?, ?>) loaded);
                    if (StrUtil.isBlank(definition.getId())) {
                        throw new ServiceException("文件化工作流缺少id: " + resource.getFilename());
                    }
                    workflows.put(definition.getId(), definition);
                    for (Long legacyWorkflowId : definition.getLegacyWorkflowIds()) {
                        legacyIdIndex.put(legacyWorkflowId, definition.getId());
                    }
                    log.info("加载文件化工作流: id={}, name={}, steps={}",
                            definition.getId(), definition.getName(), definition.getSteps().size());
                }
            }
        } catch (Exception e) {
            throw new ServiceException("加载文件化工作流失败: " + e.getMessage());
        }
    }

    public boolean supports(String workflowKey) {
        return StrUtil.isNotBlank(workflowKey) && workflows.containsKey(workflowKey);
    }

    public boolean supportsLegacyWorkflowId(Long workflowId) {
        return workflowId != null && legacyIdIndex.containsKey(workflowId);
    }

    public Optional<FileWorkflowDefinition> findByKey(String workflowKey) {
        return Optional.ofNullable(workflows.get(workflowKey));
    }

    public Optional<FileWorkflowDefinition> findByLegacyWorkflowId(Long workflowId) {
        String workflowKey = legacyIdIndex.get(workflowId);
        return findByKey(workflowKey);
    }

    public List<Map<String, Object>> listWorkflowSummaries() {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (FileWorkflowDefinition definition : workflows.values()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", definition.getId());
            summary.put("name", definition.getName());
            summary.put("version", definition.getVersion());
            summary.put("modelConfigId", definition.getModelConfigId());
            summary.put("legacyWorkflowIds", definition.getLegacyWorkflowIds());
            summary.put("stepCount", definition.getSteps() != null ? definition.getSteps().size() : 0);
            summaries.add(summary);
        }
        return summaries;
    }

    public List<Map<String, Object>> listManagementSummaries() {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (FileWorkflowDefinition definition : workflows.values()) {
            Map<String, Object> summary = toManagementSummary(definition);
            summaries.add(summary);
        }
        return summaries;
    }

    public Map<String, Object> getManagementDetail(String idOrKey) {
        FileWorkflowDefinition definition = null;
        if (StrUtil.isNotBlank(idOrKey)) {
            definition = workflows.get(idOrKey);
            if (definition == null) {
                try {
                    definition = findByLegacyWorkflowId(Long.valueOf(idOrKey)).orElse(null);
                } catch (NumberFormatException ignored) {
                    // key lookup already attempted
                }
            }
        }
        if (definition == null) {
            return null;
        }
        Map<String, Object> detail = toManagementSummary(definition);
        List<Map<String, Object>> steps = new ArrayList<>();
        int order = 1;
        for (FileWorkflowStepDefinition step : definition.getSteps()) {
            Map<String, Object> stepInfo = new LinkedHashMap<>();
            stepInfo.put("id", step.getId());
            stepInfo.put("stepName", step.getName());
            stepInfo.put("stepOrder", order++);
            stepInfo.put("prompt", step.getPrompt());
            stepInfo.put("tools", step.getTools());
            stepInfo.put("input", step.getInput());
            stepInfo.put("output", step.getOutput());
            stepInfo.put("retry", step.getRetry());
            stepInfo.put("emptyPolicy", step.getEmptyPolicy());
            stepInfo.put("failurePolicy", step.getFailurePolicy());
            steps.add(stepInfo);
        }
        detail.put("steps", steps);
        return detail;
    }

    private Map<String, Object> toManagementSummary(FileWorkflowDefinition definition) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Long legacyId = definition.getLegacyWorkflowIds() != null && !definition.getLegacyWorkflowIds().isEmpty()
                ? definition.getLegacyWorkflowIds().get(0)
                : null;
        summary.put("id", legacyId != null ? legacyId : definition.getId());
        summary.put("workflowKey", definition.getId());
        summary.put("name", definition.getName());
        summary.put("description", "文件化工作流：" + definition.getId());
        summary.put("type", "file");
        summary.put("version", definition.getVersion());
        summary.put("enabled", "1");
        summary.put("status", "0");
        summary.put("modelConfigId", definition.getModelConfigId());
        summary.put("legacyWorkflowIds", definition.getLegacyWorkflowIds());
        summary.put("stepCount", definition.getSteps() != null ? definition.getSteps().size() : 0);
        return summary;
    }

    public String loadPrompt(String promptPath) {
        if (StrUtil.isBlank(promptPath)) {
            return "";
        }
        String normalizedPath = promptPath.startsWith("/") ? promptPath.substring(1) : promptPath;
        Resource resource = new PathMatchingResourcePatternResolver()
                .getResource("classpath:ai-prompts/" + normalizedPath);
        try {
            if (!resource.exists()) {
                throw new ServiceException("提示词文件不存在: " + promptPath);
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ServiceException("读取提示词文件失败: " + promptPath + ", " + e.getMessage());
        }
    }

    private FileWorkflowDefinition toDefinition(Map<?, ?> source) {
        FileWorkflowDefinition definition = new FileWorkflowDefinition();
        definition.setId(asString(source.get("id")));
        definition.setName(asString(source.get("name")));
        definition.setVersion(asString(source.get("version")));
        definition.setModelConfigId(asLong(source.get("modelConfigId")));
        definition.setOnFailure(defaultString(source.get("onFailure"), "stop"));
        definition.setLegacyWorkflowIds(asLongList(source.get("legacyWorkflowIds")));

        List<FileWorkflowStepDefinition> steps = new ArrayList<>();
        Object rawSteps = source.get("steps");
        if (rawSteps instanceof List) {
            for (Object rawStep : (List<?>) rawSteps) {
                if (rawStep instanceof Map) {
                    steps.add(toStepDefinition((Map<?, ?>) rawStep));
                }
            }
        }
        definition.setSteps(steps);
        return definition;
    }

    private FileWorkflowStepDefinition toStepDefinition(Map<?, ?> source) {
        FileWorkflowStepDefinition step = new FileWorkflowStepDefinition();
        step.setId(asString(source.get("id")));
        step.setName(asString(source.get("name")));
        step.setPrompt(asString(source.get("prompt")));
        step.setModelConfigId(asLong(source.get("modelConfigId")));
        step.setTools(asStringList(source.get("tools")));
        step.setInput(asStringList(source.get("input")));
        step.setOutput(asString(source.get("output")));
        step.setRetry(asInteger(source.get("retry"), 0));
        step.setEmptyPolicy(defaultString(source.get("emptyPolicy"), "fail"));
        step.setFailurePolicy(defaultString(source.get("failurePolicy"), "fail"));
        return step;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultString(Object value, String defaultValue) {
        String text = asString(value);
        return StrUtil.isBlank(text) ? defaultValue : text;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Integer asInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return Collections.singletonList(String.valueOf(value));
    }

    private List<Long> asLongList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                result.add(asLong(item));
            }
            return result;
        }
        result.add(asLong(value));
        return result;
    }
}
