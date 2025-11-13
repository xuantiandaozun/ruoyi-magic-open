package com.ruoyi.project.ai.tool.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.domain.AiWorkflow;
import com.ruoyi.project.ai.domain.AiWorkflowStep;
import com.ruoyi.project.ai.service.IAiWorkflowService;
import com.ruoyi.project.ai.service.IAiWorkflowStepService;
import com.ruoyi.project.ai.tool.LangChain4jTool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的工作流管理工具
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Component
public class WorkflowManagementLangChain4jTool implements LangChain4jTool {
    
    @Autowired
    private IAiWorkflowService workflowService;
    
    @Autowired
    private IAiWorkflowStepService stepService;
    
    @Override
    public String getToolName() {
        return "workflow_management";
    }
    
    @Override
    public String getToolDescription() {
        return "管理工作流，包括获取工作流列表、创建新工作流、更新工作流、管理工作流步骤等操作";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("operation", "操作类型：get_workflow_list、add_workflow、update_workflow、update_workflow_step、get_workflow_step")
                .addStringProperty("name", "工作流名称")
                .addStringProperty("description", "工作流描述")
                .addStringProperty("type", "工作流类型，推荐使用 sequential")
                .addNumberProperty("workflowId", "工作流ID，用于更新操作")
                .addNumberProperty("stepId", "步骤ID，用于步骤操作")
                .addNumberProperty("stepOrder", "步骤顺序，从1开始")
                .addStringProperty("stepName", "步骤名称")
                .addStringProperty("stepDescription", "步骤描述")
                .addStringProperty("systemPrompt", "系统提示词")
                .addStringProperty("userPrompt", "用户提示词，支持变量占位符如{{input_variable}}")
                .addStringProperty("inputVariable", "输入变量名，第一步可为空")
                .addStringProperty("outputVariable", "输出变量名，不能为空")
                .addStringProperty("toolType", "工具类型，使用英文工具名称，如database_query、blog_save等，多个工具用逗号分隔")
                .addStringProperty("toolEnabled", "工具启用状态，Y=启用，N=不启用")
                .addStringProperty("enabled", "步骤启用状态，1=启用，0=禁用")
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
                .required("operation")
                .build())
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        String operation = (String) parameters.get("operation");
        if (StrUtil.isBlank(operation)) {
            return "操作类型不能为空";
        }
        
        switch (operation) {
            case "get_workflow_list":
                return getWorkflowList();
            case "add_workflow":
                return addWorkflow(parameters);
            case "update_workflow":
                return updateWorkflow(parameters);
            case "update_workflow_step":
                return updateWorkflowStep(parameters);
            case "get_workflow_step":
                return getWorkflowStep(parameters);
            default:
                return "不支持的操作类型: " + operation + "。支持的操作：get_workflow_list、add_workflow、update_workflow、update_workflow_step、get_workflow_step";
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return false;
        }
        
        String operation = (String) parameters.get("operation");
        if (StrUtil.isBlank(operation)) {
            return false;
        }
        
        switch (operation) {
            case "get_workflow_list":
                return true;
            case "add_workflow":
                return validateAddWorkflowParams(parameters);
            case "update_workflow":
                return validateUpdateWorkflowParams(parameters);
            case "update_workflow_step":
                return validateUpdateWorkflowStepParams(parameters);
            case "get_workflow_step":
                return parameters.containsKey("stepId") && parameters.get("stepId") != null;
            default:
                return false;
        }
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 获取工作流列表：
           {"operation": "get_workflow_list"}
        
        2. 创建工作流：
           {"operation": "add_workflow", "name": "数据分析工作流", "description": "用于数据分析的工作流", "type": "sequential", "steps": [...]}
        
        3. 更新工作流：
           {"operation": "update_workflow", "workflowId": 1, "name": "新工作流名称", "description": "新描述", "type": "sequential", "steps": [...]}
        
        4. 更新工作流步骤：
           {"operation": "update_workflow_step", "stepId": 1, "stepName": "新步骤名称", "description": "新描述"}
        
        5. 获取工作流步骤详情：
           {"operation": "get_workflow_step", "stepId": 1}
        """;
    }
    
    /**
     * 获取工作流列表和步骤信息
     */
    private String getWorkflowList() {
        try {
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
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "获取工作流列表失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }
    
    /**
     * 新增工作流
     */
    private String addWorkflow(Map<String, Object> parameters) {
        try {
            String name = (String) parameters.get("name");
            String description = (String) parameters.get("description");
            String type = (String) parameters.get("type");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) parameters.get("steps");
            
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
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "新增工作流失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }
    
    /**
     * 修改工作流
     */
    private String updateWorkflow(Map<String, Object> parameters) {
        try {
            Object workflowIdObj = parameters.get("workflowId");
            if (workflowIdObj == null) {
                throw new IllegalArgumentException("工作流ID不能为空");
            }
            
            Long workflowId = Long.valueOf(workflowIdObj.toString());
            String name = (String) parameters.get("name");
            String description = (String) parameters.get("description");
            String type = (String) parameters.get("type");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) parameters.get("steps");
            
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
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "修改工作流失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }
    
    /**
     * 修改单个工作流步骤
     */
    private String updateWorkflowStep(Map<String, Object> parameters) {
        try {
            Object stepIdObj = parameters.get("stepId");
            if (stepIdObj == null) {
                throw new IllegalArgumentException("步骤ID不能为空");
            }
            
            Long stepId = Long.valueOf(stepIdObj.toString());
            String stepName = (String) parameters.get("stepName");
            String stepDescription = (String) parameters.get("stepDescription");
            Object stepOrderObj = parameters.get("stepOrder");
            String systemPrompt = (String) parameters.get("systemPrompt");
            String userPrompt = (String) parameters.get("userPrompt");
            String inputVariable = (String) parameters.get("inputVariable");
            String outputVariable = (String) parameters.get("outputVariable");
            String toolType = (String) parameters.get("toolType");
            String toolEnabled = (String) parameters.get("toolEnabled");
            String enabled = (String) parameters.get("enabled");
            
            Integer stepOrder = null;
            if (stepOrderObj != null) {
                stepOrder = Integer.valueOf(stepOrderObj.toString());
            }
            
            // 获取现有步骤
            AiWorkflowStep step = stepService.getById(stepId);
            if (step == null) {
                throw new RuntimeException("工作流步骤不存在");
            }
            
            // 更新步骤信息（只更新非空字段）
            if (StrUtil.isNotBlank(stepName)) {
                step.setStepName(stepName);
            }
            if (StrUtil.isNotBlank(stepDescription)) {
                step.setDescription(stepDescription);
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
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "修改工作流步骤失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }
    
    /**
     * 获取单个工作流步骤详情
     */
    private String getWorkflowStep(Map<String, Object> parameters) {
        try {
            Object stepIdObj = parameters.get("stepId");
            if (stepIdObj == null) {
                throw new IllegalArgumentException("步骤ID不能为空");
            }
            
            Long stepId = Long.valueOf(stepIdObj.toString());
            
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
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "获取工作流步骤详情失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }
    
    /**
     * 验证工作流步骤的变量配置
     */
    private void validateWorkflowSteps(List<Map<String, Object>> steps) {
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
     * 验证添加工作流参数
     */
    private boolean validateAddWorkflowParams(Map<String, Object> parameters) {
        String name = (String) parameters.get("name");
        String description = (String) parameters.get("description");
        String type = (String) parameters.get("type");
        List<?> steps = (List<?>) parameters.get("steps");
        
        return StrUtil.isNotBlank(name) && 
               StrUtil.isNotBlank(description) && 
               StrUtil.isNotBlank(type) && 
               steps != null && !steps.isEmpty();
    }
    
    /**
     * 验证更新工作流参数
     */
    private boolean validateUpdateWorkflowParams(Map<String, Object> parameters) {
        return parameters.containsKey("workflowId") && 
               parameters.get("workflowId") != null &&
               validateAddWorkflowParams(parameters);
    }
    
    /**
     * 验证更新工作流步骤参数
     */
    private boolean validateUpdateWorkflowStepParams(Map<String, Object> parameters) {
        return parameters.containsKey("stepId") && 
               parameters.get("stepId") != null;
    }
}
