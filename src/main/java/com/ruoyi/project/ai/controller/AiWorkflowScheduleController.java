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
import com.ruoyi.project.ai.domain.AiWorkflowSchedule;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleService;
import com.ruoyi.project.ai.task.WorkflowScheduleTask;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI工作流定时调度 控制器
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Tag(name = "AI工作流定时调度")
@RestController
@RequestMapping("/ai/workflow/schedule")
public class AiWorkflowScheduleController extends BaseController {

    @Autowired
    private IAiWorkflowScheduleService scheduleService;

    @Autowired
    private WorkflowScheduleTask workflowScheduleTask;

    /**
     * 分页查询调度配置列表
     */
    @Operation(summary = "查询调度配置列表")
    @SaCheckPermission("ai:workflow:schedule:list")
    @GetMapping("/list")
    public TableDataInfo list(AiWorkflowSchedule query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<AiWorkflowSchedule> page = Page.of(pageDomain.getPageNum(), pageDomain.getPageSize());
        
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_schedule")
            .orderBy("create_time desc");
        
        Page<AiWorkflowSchedule> result = scheduleService.page(page, qw);
        return getDataTable(result);
    }

    /**
     * 根据工作流ID查询调度配置列表
     */
    @Operation(summary = "根据工作流ID查询调度配置列表")
    @SaCheckPermission("ai:workflow:schedule:list")
    @GetMapping("/workflow/{workflowId}")
    public AjaxResult listByWorkflowId(@PathVariable Long workflowId) {
        List<AiWorkflowSchedule> schedules = scheduleService.listByWorkflowId(workflowId);
        return success(schedules);
    }

    /**
     * 获取调度配置详情
     */
    @Operation(summary = "获取调度配置详情")
    @SaCheckPermission("ai:workflow:schedule:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        AiWorkflowSchedule schedule = scheduleService.getById(id);
        return success(schedule);
    }

    /**
     * 新增调度配置
     */
    @Operation(summary = "新增调度配置")
    @SaCheckPermission("ai:workflow:schedule:add")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiWorkflowSchedule schedule) {
        // 设置默认值
        if (schedule.getEnabled() == null) {
            schedule.setEnabled("N");
        }
        if (schedule.getStatus() == null) {
            schedule.setStatus("1"); // 暂停状态
        }
        if (schedule.getMisfirePolicy() == null) {
            schedule.setMisfirePolicy("3"); // 放弃执行
        }
        if (schedule.getConcurrent() == null) {
            schedule.setConcurrent("N"); // 不允许并发
        }
        if (schedule.getRetryCount() == null) {
            schedule.setRetryCount(0);
        }
        if (schedule.getExecutionTimeout() == null) {
            schedule.setExecutionTimeout(3600); // 默认1小时超时
        }
        if (schedule.getPriority() == null) {
            schedule.setPriority(5); // 默认优先级
        }
        
        boolean result = scheduleService.save(schedule);
        return toAjax(result);
    }

    /**
     * 修改调度配置
     */
    @Operation(summary = "修改调度配置")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiWorkflowSchedule schedule) {
        boolean result = scheduleService.updateById(schedule);

        // 更新后无条件触发Quartz同步（内部按enabled/status确定任务状态）
        if (result) {
            AiWorkflowSchedule latest = scheduleService.getById(schedule.getId());
            if (latest != null) {
                scheduleService.startSchedule(latest.getId());
            }
        }

        return toAjax(result);
    }

    /**
     * 删除调度配置
     */
    @Operation(summary = "删除调度配置")
    @SaCheckPermission("ai:workflow:schedule:remove")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        // 先停止所有要删除的调度任务
        Arrays.stream(ids).forEach(id -> {
            try {
                scheduleService.deleteSchedule(id);
            } catch (Exception e) {
                logger.error("删除调度任务失败，ID：{}", id, e);
            }
        });
        
        return success();
    }

    /**
     * 启动调度任务
     */
    @Operation(summary = "启动调度任务")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping("/start/{id}")
    public AjaxResult start(@PathVariable Long id) {
        boolean result = scheduleService.startSchedule(id);
        return result ? success("启动成功") : error("启动失败");
    }

    /**
     * 暂停调度任务
     */
    @Operation(summary = "暂停调度任务")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping("/pause/{id}")
    public AjaxResult pause(@PathVariable Long id) {
        boolean result = scheduleService.pauseSchedule(id);
        return result ? success("暂停成功") : error("暂停失败");
    }

    /**
     * 恢复调度任务
     */
    @Operation(summary = "恢复调度任务")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping("/resume/{id}")
    public AjaxResult resume(@PathVariable Long id) {
        boolean result = scheduleService.resumeSchedule(id);
        return result ? success("恢复成功") : error("恢复失败");
    }

    /**
     * 立即执行一次调度任务
     */
    @Operation(summary = "立即执行一次调度任务")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping("/execute/{id}")
    public AjaxResult executeOnce(@PathVariable Long id) {
        try {
            // 异步执行任务
            workflowScheduleTask.executeManually(id);
            return success("执行任务已启动，请查看执行日志获取结果");
        } catch (Exception e) {
            logger.error("立即执行调度任务失败，ID：{}", id, e);
            return error("执行失败：" + e.getMessage());
        }
    }

    /**
     * 批量启动调度任务
     */
    @Operation(summary = "批量启动调度任务")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping("/start/batch")
    public AjaxResult startBatch(@RequestBody Long[] ids) {
        int successCount = 0;
        for (Long id : ids) {
            try {
                if (scheduleService.startSchedule(id)) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("批量启动调度任务失败，ID：{}", id, e);
            }
        }
        return success("成功启动 " + successCount + " 个调度任务");
    }

    /**
     * 批量暂停调度任务
     */
    @Operation(summary = "批量暂停调度任务")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping("/pause/batch")
    public AjaxResult pauseBatch(@RequestBody Long[] ids) {
        int successCount = 0;
        for (Long id : ids) {
            try {
                if (scheduleService.pauseSchedule(id)) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("批量暂停调度任务失败，ID：{}", id, e);
            }
        }
        return success("成功暂停 " + successCount + " 个调度任务");
    }

    /**
     * 获取启用的调度配置列表
     */
    @Operation(summary = "获取启用的调度配置列表")
    @SaCheckPermission("ai:workflow:schedule:list")
    @GetMapping("/enabled")
    public AjaxResult getEnabledSchedules() {
        List<AiWorkflowSchedule> schedules = scheduleService.listEnabledSchedules();
        return success(schedules);
    }
}