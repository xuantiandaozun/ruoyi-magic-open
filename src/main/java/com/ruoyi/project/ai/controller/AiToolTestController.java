package com.ruoyi.project.ai.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.ai.tool.LangChain4jToolRegistry;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI工具测试控制器
 * 用于测试AI工具的调用
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Tag(name = "AI工具测试")
@RestController
@RequestMapping("/ai/tool/test")
public class AiToolTestController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AiToolTestController.class);

    @Autowired
    private LangChain4jToolRegistry toolRegistry;

    /**
     * 执行工具调用
     * 
     * @param request 请求参数
     * @return 执行结果
     */
    @Operation(summary = "执行工具调用")
    @SaIgnore
    @PostMapping("/execute")
    public AjaxResult executeTool(@RequestBody ToolExecuteRequest request) {
        try {
            // 参数校验
            if (StrUtil.isBlank(request.getToolName())) {
                return error("工具名称不能为空");
            }

            // 检查工具是否存在
            if (!toolRegistry.hasToolByName(request.getToolName())) {
                return error("未找到工具: " + request.getToolName());
            }

            log.info("开始执行工具测试 - 工具名称: {}, 参数: {}", request.getToolName(), request.getParameters());

            // 将参数Map转换为JSON字符串
            String arguments = convertParametersToJson(request.getParameters());

            // 执行工具
            String result = toolRegistry.executeTool(request.getToolName(), arguments);

            log.info("工具执行成功 - 工具名称: {}, 结果长度: {} 字符", request.getToolName(), result.length());

            // 构建返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("toolName", request.getToolName());
            data.put("result", result);
            data.put("parameters", request.getParameters());

            return success(data);

        } catch (Exception e) {
            log.error("工具执行失败: {}", e.getMessage(), e);
            return error("工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有可用工具列表
     * 
     * @return 工具列表
     */
    @Operation(summary = "获取所有可用工具列表")
    @SaCheckPermission("ai:tool:test")
    @GetMapping("/list")
    public AjaxResult listTools() {
        try {
            List<Map<String, Object>> toolsInfo = toolRegistry.getAllToolsInfo();
            return success(toolsInfo);
        } catch (Exception e) {
            log.error("获取工具列表失败: {}", e.getMessage(), e);
            return error("获取工具列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取工具详细信息
     * 
     * @param request 请求参数
     * @return 工具信息
     */
    @Operation(summary = "获取工具详细信息")
    @SaCheckPermission("ai:tool:test")
    @PostMapping("/info")
    public AjaxResult getToolInfo(@RequestBody ToolInfoRequest request) {
        try {
            if (StrUtil.isBlank(request.getToolName())) {
                return error("工具名称不能为空");
            }

            Map<String, Object> toolInfo = toolRegistry.getToolInfo(request.getToolName());
            if (toolInfo == null) {
                return error("未找到工具: " + request.getToolName());
            }

            return success(toolInfo);
        } catch (Exception e) {
            log.error("获取工具信息失败: {}", e.getMessage(), e);
            return error("获取工具信息失败: " + e.getMessage());
        }
    }

    /**
     * 将参数Map转换为JSON字符串
     */
    private String convertParametersToJson(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }
        
        try {
            return cn.hutool.json.JSONUtil.toJsonStr(parameters);
        } catch (Exception e) {
            log.error("转换参数为JSON失败: {}", e.getMessage(), e);
            throw new RuntimeException("参数格式错误: " + e.getMessage());
        }
    }

    /**
     * 工具执行请求
     */
    public static class ToolExecuteRequest {
        /** 工具名称 */
        private String toolName;
        
        /** 工具参数 */
        private Map<String, Object> parameters;

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    /**
     * 工具信息请求
     */
    public static class ToolInfoRequest {
        /** 工具名称 */
        private String toolName;

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }
    }
}
