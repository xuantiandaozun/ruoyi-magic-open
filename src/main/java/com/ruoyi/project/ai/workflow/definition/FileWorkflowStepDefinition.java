package com.ruoyi.project.ai.workflow.definition;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 文件化工作流步骤定义。
 */
@Data
public class FileWorkflowStepDefinition {

    private String id;

    private String name;

    private String prompt;

    /**
     * 确定性步骤处理器名称。非空时跳过 LLM，直接执行对应 handler（如 blog_cover）。
     */
    private String handler;

    private Long modelConfigId;

    private List<String> tools = new ArrayList<>();

    private List<String> input = new ArrayList<>();

    private String output;

    private Integer retry = 0;

    private String emptyPolicy = "fail";

    private String failurePolicy = "fail";
}
