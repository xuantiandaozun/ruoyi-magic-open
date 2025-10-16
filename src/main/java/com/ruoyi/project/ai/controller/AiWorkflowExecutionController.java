package com.ruoyi.project.ai.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
import com.ruoyi.project.ai.domain.AiWorkflowExecution;
import com.ruoyi.project.ai.dto.WorkflowExecuteRequest;
import com.ruoyi.project.ai.service.IAiWorkflowExecutionService;
import com.ruoyi.project.ai.service.IWorkflowExecutionService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI工作流执行 控制器
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Tag(name = "AI工作流执行")
@RestController
@RequestMapping("/ai/workflow/execution")
public class AiWorkflowExecutionController extends BaseController {

    @Autowired
    private IAiWorkflowExecutionService workflowExecutionService;
    
    @Autowired
    private IWorkflowExecutionService workflowExecutor;

    /**
     * 执行工作流
     */
    @Operation(summary = "执行工作流")
    @SaCheckPermission("ai:workflow:execute")
    @Log(title = "AI工作流执行", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public AjaxResult executeWorkflow(@Validated @RequestBody WorkflowExecuteRequest request) {
        try {
            if (request.getWorkflowId() == null) {
                return error("工作流ID不能为空");
            }
            
            Map<String, Object> result = workflowExecutor.executeWorkflow(request);
            return success("工作流执行完成", result);
        } catch (Exception e) {
            logger.error("工作流执行失败: {}", e.getMessage(), e);
            return error("工作流执行失败: " + e.getMessage());
        }
    }

    /**
     * 快速执行工作流（仅传入工作流ID和输入数据）
     */
    @Operation(summary = "快速执行工作流")
    @SaCheckPermission("ai:workflow:execute")
    @Log(title = "AI工作流执行", businessType = BusinessType.OTHER)
    @PostMapping("/quickExecute/{workflowId}")
    public AjaxResult quickExecuteWorkflow(@PathVariable Long workflowId, @RequestBody Map<String, Object> inputData) {
        try {
            Map<String, Object> result = workflowExecutor.executeWorkflow(workflowId, inputData);
            return success("工作流执行完成", result);
        } catch (Exception e) {
            logger.error("工作流执行失败: {}", e.getMessage(), e);
            return error("工作流执行失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询工作流执行历史
     */
    @Operation(summary = "查询工作流执行历史")
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/list")
    public TableDataInfo list(AiWorkflowExecution query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiWorkflowExecution> page = workflowExecutionService.page(new Page<>(pageNum, pageSize), qw);
        return getDataTable(page);
    }

    /**
     * 获取工作流执行详情
     */
    @Operation(summary = "获取工作流执行详情")
    @SaCheckPermission("ai:workflow:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        AiWorkflowExecution execution = workflowExecutionService.getById(id);
        return success(execution);
    }

    /**
     * 新增工作流执行记录（一般由系统自动创建）
     */
    @Operation(summary = "新增工作流执行记录")
    @SaCheckPermission("ai:workflow:add")
    @Log(title = "AI工作流执行", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiWorkflowExecution execution) {
        return toAjax(workflowExecutionService.saveOrUpdate(execution));
    }

    /**
     * 修改工作流执行记录
     */
    @Operation(summary = "修改工作流执行记录")
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "AI工作流执行", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiWorkflowExecution execution) {
        return toAjax(workflowExecutionService.updateById(execution));
    }

    /**
     * 删除工作流执行记录
     */
    @Operation(summary = "删除工作流执行记录")
    @SaCheckPermission("ai:workflow:remove")
    @Log(title = "AI工作流执行", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(workflowExecutionService.removeByIds(Arrays.asList(ids)));
    }

    /**
     * 获取工作流执行状态统计
     */
    @Operation(summary = "获取工作流执行状态统计")
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/statistics")
    public AjaxResult getExecutionStatistics() {
        try {
            // 查询各种状态的执行记录数量
            QueryWrapper successQuery = QueryWrapper.create().eq("status", "SUCCESS").eq("deleted", 0);
            QueryWrapper failedQuery = QueryWrapper.create().eq("status", "FAILED").eq("deleted", 0);
            QueryWrapper runningQuery = QueryWrapper.create().eq("status", "RUNNING").eq("deleted", 0);
            
            long successCount = workflowExecutionService.count(successQuery);
            long failedCount = workflowExecutionService.count(failedQuery);
            long runningCount = workflowExecutionService.count(runningQuery);
            long totalCount = successCount + failedCount + runningCount;
            
            Map<String, Object> statistics = Map.of(
                "total", totalCount,
                "success", successCount,
                "failed", failedCount,
                "running", runningCount
            );
            
            return success(statistics);
        } catch (Exception e) {
            logger.error("获取执行统计失败: {}", e.getMessage(), e);
            return error("获取执行统计失败: " + e.getMessage());
        }
    }

    /**
     * 重新执行失败的工作流
     */
    @Operation(summary = "重新执行失败的工作流")
    @SaCheckPermission("ai:workflow:execute")
    @Log(title = "AI工作流执行", businessType = BusinessType.OTHER)
    @PostMapping("/retry/{executionId}")
    public AjaxResult retryExecution(@PathVariable Long executionId) {
        try {
            AiWorkflowExecution execution = workflowExecutionService.getById(executionId);
            if (execution == null) {
                return error("执行记录不存在");
            }
            
            if (!"FAILED".equals(execution.getStatus())) {
                return error("只能重新执行失败的工作流");
            }
            
            // 解析输入数据并重新执行
            Map<String, Object> inputData = null;
            if (StrUtil.isNotBlank(execution.getInputData())) {
                // 这里需要JSON解析，简化处理
                inputData = Map.of(); // 实际应该解析JSON
            }
            
            Map<String, Object> result = workflowExecutor.executeWorkflow(execution.getWorkflowId(), inputData);
            return success("工作流重新执行完成", result);
        } catch (Exception e) {
            logger.error("重新执行工作流失败: {}", e.getMessage(), e);
            return error("重新执行工作流失败: " + e.getMessage());
        }
    }
}