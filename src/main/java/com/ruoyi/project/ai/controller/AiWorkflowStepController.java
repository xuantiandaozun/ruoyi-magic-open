package com.ruoyi.project.ai.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.ai.domain.AiWorkflowStep;
import com.ruoyi.project.ai.service.IAiWorkflowStepService;
import com.ruoyi.project.ai.util.PromptVariableProcessor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI工作流步骤 控制器
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Tag(name = "AI工作流步骤")
@RestController
@RequestMapping("/ai/workflow/step")
public class AiWorkflowStepController extends BaseController {

    @Autowired
    private IAiWorkflowStepService workflowStepService;

    /**
     * 分页查询工作流步骤列表
     */
    @Operation(summary = "查询工作流步骤列表")
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/list")
    public TableDataInfo list(AiWorkflowStep query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiWorkflowStep> page = workflowStepService.page(new Page<>(pageNum, pageSize), qw);
        return getDataTable(page);
    }

    /**
     * 根据工作流ID查询步骤列表
     */
    @Operation(summary = "根据工作流ID查询步骤列表")
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/listByWorkflowId")
    public AjaxResult listByWorkflowId(@RequestParam Long workflowId) {
        try {
            List<AiWorkflowStep> steps = workflowStepService.selectByWorkflowId(workflowId);
            return success(steps);
        } catch (Exception e) {
            logger.error("查询工作流步骤失败: {}", e.getMessage(), e);
            return error("查询工作流步骤失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流步骤详情
     */
    @Operation(summary = "获取工作流步骤详情")
    @SaCheckPermission("ai:workflow:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        AiWorkflowStep step = workflowStepService.getById(id);
        return success(step);
    }

    /**
     * 新增工作流步骤
     */
    @Operation(summary = "新增工作流步骤")
    @SaCheckPermission("ai:workflow:add")
    @Log(title = "AI工作流步骤", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiWorkflowStep step) {
        return toAjax(workflowStepService.saveOrUpdate(step));
    }

    /**
     * 修改工作流步骤
     */
    @Operation(summary = "修改工作流步骤")
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "AI工作流步骤", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiWorkflowStep step) {
        return toAjax(workflowStepService.updateById(step));
    }

    /**
     * 删除工作流步骤
     */
    @Operation(summary = "删除工作流步骤")
    @SaCheckPermission("ai:workflow:remove")
    @Log(title = "AI工作流步骤", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(workflowStepService.removeByIds(Arrays.asList(ids)));
    }

    /**
     * 启用/禁用工作流步骤
     */
    @Operation(summary = "启用/禁用工作流步骤")
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "AI工作流步骤", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}")
    public AjaxResult toggleStatus(@PathVariable Long id) {
        try {
            AiWorkflowStep step = workflowStepService.getById(id);
            if (step == null) {
                return error("工作流步骤不存在");
            }
            
            // 切换状态：1启用 -> 0禁用，0禁用 -> 1启用
            String newStatus = "1".equals(step.getEnabled()) ? "0" : "1";
            step.setEnabled(newStatus);
            
            boolean result = workflowStepService.updateById(step);
            return toAjax(result);
        } catch (Exception e) {
            logger.error("切换工作流步骤状态失败: {}", e.getMessage(), e);
            return error("切换工作流步骤状态失败: " + e.getMessage());
        }
    }

    /**
     * 调整步骤顺序
     */
    @Operation(summary = "调整步骤顺序")
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "AI工作流步骤", businessType = BusinessType.UPDATE)
    @PutMapping("/reorder/{id}")
    public AjaxResult reorderStep(@PathVariable Long id, @RequestParam Integer newOrder) {
        try {
            AiWorkflowStep step = workflowStepService.getById(id);
            if (step == null) {
                return error("工作流步骤不存在");
            }
            
            step.setStepOrder(newOrder);
            boolean result = workflowStepService.updateById(step);
            return toAjax(result);
        } catch (Exception e) {
            logger.error("调整步骤顺序失败: {}", e.getMessage(), e);
            return error("调整步骤顺序失败: " + e.getMessage());
        }
    }

    /**
     * 批量调整步骤顺序
     */
    @Operation(summary = "批量调整步骤顺序")
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "AI工作流步骤", businessType = BusinessType.UPDATE)
    @PutMapping("/batchReorder")
    public AjaxResult batchReorderSteps(@RequestBody List<AiWorkflowStep> steps) {
        try {
            boolean result = true;
            for (AiWorkflowStep step : steps) {
                if (!workflowStepService.updateById(step)) {
                    result = false;
                    break;
                }
            }
            return toAjax(result);
        } catch (Exception e) {
            logger.error("批量调整步骤顺序失败: {}", e.getMessage(), e);
            return error("批量调整步骤顺序失败: " + e.getMessage());
        }
    }

    /**
     * 验证用户提示词中的变量
     */
    @Operation(summary = "验证用户提示词中的变量")
    @SaCheckPermission("ai:workflow:query")
    @PostMapping("/validatePromptVariables")
    public AjaxResult validatePromptVariables(@RequestBody String userPrompt) {
        try {
            // 检查是否包含变量
            boolean hasVariables = PromptVariableProcessor.hasVariables(userPrompt);
            
            // 提取变量列表
            String[] variableNames = PromptVariableProcessor.extractVariableNames(userPrompt);
            
            // 验证变量格式（这里我们只检查是否有缺失的变量，传入空的变量映射）
            String[] missingVariables = PromptVariableProcessor.validateVariables(userPrompt, null);
            
            // 构建返回结果
            AjaxResult result = success();
            result.put("hasVariables", hasVariables);
            result.put("variables", Arrays.asList(variableNames));
            result.put("missingVariables", Arrays.asList(missingVariables));
            result.put("isValid", missingVariables.length == 0);
            
            return result;
        } catch (Exception e) {
            logger.error("验证用户提示词变量失败: {}", e.getMessage(), e);
            return error("验证用户提示词变量失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用工具类型列表
     */
    @Operation(summary = "获取可用工具类型列表")
    @SaCheckPermission("ai:workflow:query")
    @GetMapping("/toolTypes")
    public AjaxResult getToolTypes() {
        try {
            List<String> toolTypes = Arrays.asList(
                "github_trending",
                "blog_history_query",
                "database_query", 
                "blog_save",
                "blog_en_save",
                "social_media_article_save",
                "oss_file_read"
            );
            return success(toolTypes);
        } catch (Exception e) {
            logger.error("获取工具类型列表失败: {}", e.getMessage(), e);
            return error("获取工具类型列表失败: " + e.getMessage());
        }
    }
}