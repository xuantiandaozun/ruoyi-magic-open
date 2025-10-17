package com.ruoyi.project.ai.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.ruoyi.project.ai.domain.AiWorkflowScheduleLog;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleLogService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI工作流定时调度日志 控制器
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Tag(name = "AI工作流定时调度日志")
@RestController
@RequestMapping("/ai/workflow/schedule/log")
public class AiWorkflowScheduleLogController extends BaseController {

    @Autowired
    private IAiWorkflowScheduleLogService scheduleLogService;

    /**
     * 分页查询调度日志列表
     */
    @Operation(summary = "查询调度日志列表")
    @SaCheckPermission("ai:workflow:schedule:log:list")
    @GetMapping("/list")
    public TableDataInfo list(AiWorkflowScheduleLog query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<AiWorkflowScheduleLog> page = Page.of(pageDomain.getPageNum(), pageDomain.getPageSize());
        
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule_log")
            .where("del_flag = '0'")
            .orderBy("create_time desc");
        
        // 添加查询条件
        if (query.getScheduleId() != null) {
            qw.and("schedule_id = {0}", query.getScheduleId());
        }
        if (query.getWorkflowId() != null) {
            qw.and("workflow_id = {0}", query.getWorkflowId());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            qw.and("status = {0}", query.getStatus());
        }
        if (query.getTriggerType() != null && !query.getTriggerType().isEmpty()) {
            qw.and("trigger_type = {0}", query.getTriggerType());
        }
        
        Page<AiWorkflowScheduleLog> result = scheduleLogService.page(page, qw);
        return getDataTable(result);
    }

    /**
     * 根据调度ID查询日志列表
     */
    @Operation(summary = "根据调度ID查询日志列表")
    @SaCheckPermission("ai:workflow:schedule:log:list")
    @GetMapping("/schedule/{scheduleId}")
    public AjaxResult listByScheduleId(@PathVariable Long scheduleId) {
        List<AiWorkflowScheduleLog> logs = scheduleLogService.listByScheduleId(scheduleId);
        return success(logs);
    }

    /**
     * 根据工作流ID查询日志列表
     */
    @Operation(summary = "根据工作流ID查询日志列表")
    @SaCheckPermission("ai:workflow:schedule:log:list")
    @GetMapping("/workflow/{workflowId}")
    public AjaxResult listByWorkflowId(@PathVariable Long workflowId) {
        List<AiWorkflowScheduleLog> logs = scheduleLogService.listByWorkflowId(workflowId);
        return success(logs);
    }

    /**
     * 根据状态查询日志列表
     */
    @Operation(summary = "根据状态查询日志列表")
    @SaCheckPermission("ai:workflow:schedule:log:list")
    @GetMapping("/status/{status}")
    public AjaxResult listByStatus(@PathVariable String status) {
        List<AiWorkflowScheduleLog> logs = scheduleLogService.listByStatus(status);
        return success(logs);
    }

    /**
     * 根据触发类型查询日志列表
     */
    @Operation(summary = "根据触发类型查询日志列表")
    @SaCheckPermission("ai:workflow:schedule:log:list")
    @GetMapping("/trigger/{triggerType}")
    public AjaxResult listByTriggerType(@PathVariable String triggerType) {
        List<AiWorkflowScheduleLog> logs = scheduleLogService.listByTriggerType(triggerType);
        return success(logs);
    }

    /**
     * 获取日志详情
     */
    @Operation(summary = "获取日志详情")
    @SaCheckPermission("ai:workflow:schedule:log:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        AiWorkflowScheduleLog log = scheduleLogService.getById(id);
        return success(log);
    }

    /**
     * 删除调度日志
     */
    @Operation(summary = "删除调度日志")
    @SaCheckPermission("ai:workflow:schedule:log:remove")
    @Log(title = "AI工作流定时调度日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        boolean result = scheduleLogService.removeByIds(List.of(ids));
        return toAjax(result);
    }

    /**
     * 清理过期日志
     */
    @Operation(summary = "清理过期日志")
    @SaCheckPermission("ai:workflow:schedule:log:remove")
    @Log(title = "AI工作流定时调度日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/clean")
    public AjaxResult cleanExpiredLogs(@RequestParam(defaultValue = "30") int days) {
        int count = scheduleLogService.cleanExpiredLogs(days);
        return success("成功清理 " + count + " 条过期日志");
    }

    /**
     * 获取日志统计信息
     */
    @Operation(summary = "获取日志统计信息")
    @SaCheckPermission("ai:workflow:schedule:log:list")
    @GetMapping("/statistics")
    public AjaxResult getStatistics(@RequestParam(required = false) Long scheduleId,
                                   @RequestParam(required = false) Long workflowId,
                                   @RequestParam(defaultValue = "7") int days) {
        // 这里可以实现统计逻辑，比如成功率、失败率、平均执行时间等
        // 为了简化，这里返回基本统计信息
        
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule_log")
            .where("del_flag = '0'")
            .and("create_time >= DATE_SUB(NOW(), INTERVAL {0} DAY)", days);
        
        if (scheduleId != null) {
            qw.and("schedule_id = {0}", scheduleId);
        }
        if (workflowId != null) {
            qw.and("workflow_id = {0}", workflowId);
        }
        
        // 总执行次数
        long totalCount = scheduleLogService.count(qw);
        
        // 成功次数
        QueryWrapper successQw = qw.clone().and("status = '0'");
        long successCount = scheduleLogService.count(successQw);
        
        // 失败次数
        QueryWrapper failQw = qw.clone().and("status = '1'");
        long failCount = scheduleLogService.count(failQw);
        
        // 运行中次数
        QueryWrapper runningQw = qw.clone().and("status = '2'");
        long runningCount = scheduleLogService.count(runningQw);
        
        // 计算成功率
        double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
        
        return success()
            .put("totalCount", totalCount)
            .put("successCount", successCount)
            .put("failCount", failCount)
            .put("runningCount", runningCount)
            .put("successRate", Math.round(successRate * 100.0) / 100.0)
            .put("days", days);
    }

    /**
     * 获取最近执行日志
     */
    @Operation(summary = "获取最近执行日志")
    @SaCheckPermission("ai:workflow:schedule:log:list")
    @GetMapping("/recent")
    public AjaxResult getRecentLogs(@RequestParam(defaultValue = "10") int limit) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule_log")
            .where("del_flag = '0'")
            .orderBy("create_time desc")
            .limit(limit);
        
        List<AiWorkflowScheduleLog> logs = scheduleLogService.list(qw);
        return success(logs);
    }
}