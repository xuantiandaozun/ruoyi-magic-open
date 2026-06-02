package com.ruoyi.project.ai.controller;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.utils.job.CronUtils;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.ai.service.FileWorkflowRunLogService;
import com.ruoyi.project.ai.task.FileWorkflowScheduleTask;
import com.ruoyi.project.ai.workflow.FileWorkflowDefinitionLoader;
import com.ruoyi.project.ai.workflow.definition.FileWorkflowDefinition;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文件化AI工作流查询接口。
 */
@Tag(name = "AI工作流")
@RestController
@RequestMapping("/ai/workflow")
public class AiWorkflowController extends BaseController {

    @Autowired
    private FileWorkflowDefinitionLoader fileWorkflowDefinitionLoader;

    @Autowired
    private FileWorkflowRunLogService runLogService;

    @Autowired
    private FileWorkflowScheduleTask fileWorkflowScheduleTask;

    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private Executor executor;

    /**
     * 查询 yml 配置的工作流列表。
     */
    @Operation(summary = "查询工作流列表")
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/list")
    public TableDataInfo list() {
        List<Map<String, Object>> workflows = fileWorkflowDefinitionLoader.listManagementSummaries();
        TableDataInfo dataInfo = new TableDataInfo();
        dataInfo.setCode(200);
        dataInfo.setMsg("查询成功");
        dataInfo.setRows(workflows);
        dataInfo.setTotal(workflows.size());
        return dataInfo;
    }

    /**
     * 获取 yml 工作流详情。
     */
    @Operation(summary = "获取工作流详情")
    @SaCheckPermission("ai:workflow:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        Map<String, Object> workflow = fileWorkflowDefinitionLoader.getManagementDetail(id);
        if (workflow == null) {
            return error("文件化工作流不存在");
        }
        return success(workflow);
    }

    /**
     * 获取所有启用的 yml 工作流列表。
     */
    @Operation(summary = "获取启用的工作流列表")
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/enabled")
    public AjaxResult getEnabledWorkflows() {
        return success(fileWorkflowDefinitionLoader.listManagementSummaries());
    }

    /**
     * 查看 yml 工作流的定时任务，只读。
     */
    @Operation(summary = "查看工作流定时任务")
    @SaCheckPermission("ai:workflow:schedule:list")
    @GetMapping("/{workflowId}/schedules")
    public AjaxResult listSchedules(@PathVariable String workflowId) {
        FileWorkflowDefinition definition = resolveDefinition(workflowId);
        if (definition == null) {
            return error("文件化工作流不存在");
        }
        return success(List.of(toScheduleView(definition)));
    }

    /**
     * 查看 yml 工作流定时任务统计。
     */
    @Operation(summary = "查看工作流定时任务统计")
    @SaCheckPermission("ai:workflow:schedule:list")
    @GetMapping("/{workflowId}/schedules/statistics")
    public AjaxResult getScheduleStatistics(@PathVariable String workflowId,
            @RequestParam(defaultValue = "7") int days) {
        FileWorkflowDefinition definition = resolveDefinition(workflowId);
        if (definition == null) {
            return error("文件化工作流不存在");
        }
        Map<String, Object> statistics = runLogService.statistics(definition.getId(), getLegacyWorkflowId(definition), days);
        statistics.put("schedule", toScheduleView(definition));
        return success(statistics);
    }

    /**
     * 查看 yml 工作流运行日志。
     */
    @Operation(summary = "查看工作流运行日志")
    @SaCheckPermission("ai:workflow:schedule:list")
    @GetMapping("/{workflowId}/schedules/logs")
    public TableDataInfo listScheduleLogs(@PathVariable String workflowId) {
        FileWorkflowDefinition definition = resolveDefinition(workflowId);
        if (definition == null) {
            TableDataInfo dataInfo = new TableDataInfo();
            dataInfo.setCode(200);
            dataInfo.setMsg("查询成功");
            dataInfo.setRows(List.of());
            dataInfo.setTotal(0);
            return dataInfo;
        }
        PageDomain pageDomain = TableSupport.buildPageRequest();
        List<Map<String, Object>> logs = runLogService.listLogs(definition.getId(), getLegacyWorkflowId(definition),
                pageDomain.getPageNum(), pageDomain.getPageSize());
        TableDataInfo dataInfo = new TableDataInfo();
        dataInfo.setCode(200);
        dataInfo.setMsg("查询成功");
        dataInfo.setRows(logs);
        dataInfo.setTotal(runLogService.countLogs(definition.getId(), getLegacyWorkflowId(definition)));
        return dataInfo;
    }

    /**
     * 立即执行 yml 工作流。
     */
    @Operation(summary = "立即执行工作流定时任务")
    @SaCheckPermission("ai:workflow:schedule:execute")
    @PutMapping("/{workflowId}/schedules/{scheduleId}/execute")
    public AjaxResult executeSchedule(@PathVariable String workflowId, @PathVariable String scheduleId) {
        FileWorkflowDefinition definition = resolveDefinition(workflowId);
        if (definition == null) {
            return error("文件化工作流不存在");
        }
        executor.execute(() -> fileWorkflowScheduleTask.executeManual(definition.getId()));
        return success("执行任务已启动，请查看运行日志获取结果");
    }

    /**
     * 释放卡住的工作流锁，并结束 running 状态日志。
     */
    @Operation(summary = "释放工作流执行锁")
    @SaCheckPermission("ai:workflow:schedule:execute")
    @PutMapping("/{workflowId}/schedules/unlock")
    public AjaxResult unlockSchedule(@PathVariable String workflowId) {
        FileWorkflowDefinition definition = resolveDefinition(workflowId);
        if (definition == null) {
            return error("文件化工作流不存在");
        }
        return success(fileWorkflowScheduleTask.forceRelease(definition.getId()));
    }

    /**
     * 兼容旧页面的立即执行入口。
     */
    @Operation(summary = "立即执行工作流定时任务")
    @SaCheckPermission("ai:workflow:schedule:execute")
    @PutMapping("/schedule/execute/{id}")
    public AjaxResult executeScheduleCompat(@PathVariable String id) {
        FileWorkflowDefinition definition = resolveDefinition(id);
        if (definition == null) {
            return error("文件化工作流不存在");
        }
        executor.execute(() -> fileWorkflowScheduleTask.executeManual(definition.getId()));
        return success("执行任务已启动，请查看运行日志获取结果");
    }

    private FileWorkflowDefinition resolveDefinition(String idOrKey) {
        FileWorkflowDefinition definition = fileWorkflowDefinitionLoader.findByKey(idOrKey).orElse(null);
        if (definition != null) {
            return definition;
        }
        try {
            return fileWorkflowDefinitionLoader.findByLegacyWorkflowId(Long.valueOf(idOrKey)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> toScheduleView(FileWorkflowDefinition definition) {
        Map<String, Object> schedule = new LinkedHashMap<>();
        Long legacyWorkflowId = getLegacyWorkflowId(definition);
        schedule.put("id", legacyWorkflowId != null ? legacyWorkflowId : definition.getId());
        schedule.put("workflowId", legacyWorkflowId);
        schedule.put("workflowKey", definition.getId());
        schedule.put("workflowName", definition.getName());
        schedule.put("scheduleName", definition.getName() + " 自动执行");
        schedule.put("cronExpression", definition.getCronExpression());
        schedule.put("enabled", definition.getScheduleEnabled());
        schedule.put("status", "Y".equalsIgnoreCase(definition.getScheduleEnabled()) ? "0" : "1");
        schedule.put("misfirePolicy", definition.getMisfirePolicy());
        schedule.put("concurrent", definition.getConcurrent());
        schedule.put("nextExecutionTime", StrUtil.isNotBlank(definition.getCronExpression())
                ? CronUtils.getNextExecution(definition.getCronExpression())
                : null);
        schedule.put("readonly", true);
        return schedule;
    }

    private Long getLegacyWorkflowId(FileWorkflowDefinition definition) {
        if (definition.getLegacyWorkflowIds() == null || definition.getLegacyWorkflowIds().isEmpty()) {
            return null;
        }
        return definition.getLegacyWorkflowIds().get(0);
    }
}
