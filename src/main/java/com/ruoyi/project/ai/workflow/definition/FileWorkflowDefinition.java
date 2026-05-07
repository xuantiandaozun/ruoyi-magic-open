package com.ruoyi.project.ai.workflow.definition;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 文件化工作流定义。
 */
@Data
public class FileWorkflowDefinition {

    private String id;

    private String name;

    private String version;

    private Long modelConfigId;

    private String onFailure = "stop";

    private String scheduleEnabled = "N";

    private String cronExpression;

    private String misfirePolicy = "3";

    private String concurrent = "1";

    private List<Long> legacyWorkflowIds = new ArrayList<>();

    private List<FileWorkflowStepDefinition> steps = new ArrayList<>();
}
