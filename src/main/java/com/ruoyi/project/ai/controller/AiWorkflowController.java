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
import com.ruoyi.project.ai.domain.AiWorkflow;
import com.ruoyi.project.ai.domain.AiWorkflowSchedule;
import com.ruoyi.project.ai.service.IAiWorkflowService;
import com.ruoyi.project.ai.service.IAiWorkflowScheduleService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI工作流 控制器
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Tag(name = "AI工作流")
@RestController
@RequestMapping("/ai/workflow")
public class AiWorkflowController extends BaseController {

    @Autowired
    private IAiWorkflowService workflowService;

    @Autowired
    private IAiWorkflowScheduleService scheduleService;

    /**
     * 分页查询工作流列表
     */
    @Operation(summary = "查询工作流列表")
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/list")
    public TableDataInfo list(AiWorkflow query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiWorkflow> page = workflowService.page(new Page<>(pageNum, pageSize), qw);
        return getDataTable(page);
    }

    /**
     * 获取工作流详情
     */
    @Operation(summary = "获取工作流详情")
    @SaCheckPermission("ai:workflow:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        AiWorkflow workflow = workflowService.getById(id);
        return success(workflow);
    }

    /**
     * 新增工作流
     */
    @Operation(summary = "新增工作流")
    @SaCheckPermission("ai:workflow:add")
    @Log(title = "AI工作流", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiWorkflow workflow) {
        return toAjax(workflowService.save(workflow));
    }

    /**
     * 修改工作流
     */
    @Operation(summary = "修改工作流")
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "AI工作流", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiWorkflow workflow) {
        return toAjax(workflowService.updateById(workflow));
    }

    /**
     * 删除工作流
     */
    @Operation(summary = "删除工作流")
    @SaCheckPermission("ai:workflow:remove")
    @Log(title = "AI工作流", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(workflowService.removeByIds(Arrays.asList(ids)));
    }

    /**
     * 启用/禁用工作流
     */
    @Operation(summary = "启用/禁用工作流")
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "AI工作流", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}")
    public AjaxResult toggleStatus(@PathVariable Long id) {
        try {
            AiWorkflow workflow = workflowService.getById(id);
            if (workflow == null) {
                return error("工作流不存在");
            }
            
            // 切换状态：1启用 -> 0禁用，0禁用 -> 1启用
            String newStatus = "1".equals(workflow.getEnabled()) ? "0" : "1";
            workflow.setEnabled(newStatus);
            
            boolean result = workflowService.updateById(workflow);
            return toAjax(result);
        } catch (Exception e) {
            logger.error("切换工作流状态失败: {}", e.getMessage(), e);
            return error("切换工作流状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有启用的工作流列表
     */
    @Operation(summary = "获取启用的工作流列表")
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/enabled")
    public AjaxResult getEnabledWorkflows() {
        try {
            List<AiWorkflow> workflows = workflowService.listByEnabled("1");
            return success(workflows);
        } catch (Exception e) {
            logger.error("获取启用工作流列表失败: {}", e.getMessage(), e);
            return error("获取启用工作流列表失败: " + e.getMessage());
        }
    }

    // ==================== 定时任务管理接口 ====================

    /**
     * 获取工作流的定时调度配置列表
     */
    @Operation(summary = "获取工作流的定时调度配置列表")
    @SaCheckPermission("ai:workflow:schedule:list")
    @GetMapping("/{id}/schedules")
    public AjaxResult getWorkflowSchedules(@PathVariable Long id) {
        try {
            List<AiWorkflowSchedule> schedules = scheduleService.listByWorkflowId(id);
            return success(schedules);
        } catch (Exception e) {
            logger.error("获取工作流定时调度配置失败: {}", e.getMessage(), e);
            return error("获取工作流定时调度配置失败: " + e.getMessage());
        }
    }

    /**
     * 为工作流创建定时调度配置
     */
    @Operation(summary = "为工作流创建定时调度配置")
    @SaCheckPermission("ai:workflow:schedule:add")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/schedule")
    public AjaxResult createSchedule(@PathVariable Long id, @Validated @RequestBody AiWorkflowSchedule schedule) {
        try {
            // 验证工作流是否存在
            AiWorkflow workflow = workflowService.getById(id);
            if (workflow == null) {
                return error("工作流不存在");
            }

            // 设置工作流ID
            schedule.setWorkflowId(id);
            
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
        } catch (Exception e) {
            logger.error("创建工作流定时调度配置失败: {}", e.getMessage(), e);
            return error("创建工作流定时调度配置失败: " + e.getMessage());
        }
    }

    /**
     * 启用工作流的所有定时调度
     */
    @Operation(summary = "启用工作流的所有定时调度")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/schedules/enable")
    public AjaxResult enableAllSchedules(@PathVariable Long id) {
        try {
            List<AiWorkflowSchedule> schedules = scheduleService.listByWorkflowId(id);
            int successCount = 0;
            
            for (AiWorkflowSchedule schedule : schedules) {
                if ("Y".equals(schedule.getEnabled())) {
                    try {
                        if (scheduleService.startSchedule(schedule.getId())) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        logger.error("启用调度任务失败，ID：{}", schedule.getId(), e);
                    }
                }
            }
            
            return success("成功启用 " + successCount + " 个调度任务");
        } catch (Exception e) {
            logger.error("启用工作流所有定时调度失败: {}", e.getMessage(), e);
            return error("启用工作流所有定时调度失败: " + e.getMessage());
        }
    }

    /**
     * 禁用工作流的所有定时调度
     */
    @Operation(summary = "禁用工作流的所有定时调度")
    @SaCheckPermission("ai:workflow:schedule:edit")
    @Log(title = "AI工作流定时调度", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/schedules/disable")
    public AjaxResult disableAllSchedules(@PathVariable Long id) {
        try {
            List<AiWorkflowSchedule> schedules = scheduleService.listByWorkflowId(id);
            int successCount = 0;
            
            for (AiWorkflowSchedule schedule : schedules) {
                try {
                    if (scheduleService.pauseSchedule(schedule.getId())) {
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.error("禁用调度任务失败，ID：{}", schedule.getId(), e);
                }
            }
            
            return success("成功禁用 " + successCount + " 个调度任务");
        } catch (Exception e) {
            logger.error("禁用工作流所有定时调度失败: {}", e.getMessage(), e);
            return error("禁用工作流所有定时调度失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流定时调度统计信息
     */
    @Operation(summary = "获取工作流定时调度统计信息")
    @SaCheckPermission("ai:workflow:schedule:list")
    @GetMapping("/{id}/schedules/statistics")
    public AjaxResult getScheduleStatistics(@PathVariable Long id) {
        try {
            List<AiWorkflowSchedule> schedules = scheduleService.listByWorkflowId(id);
            
            long totalCount = schedules.size();
            long enabledCount = schedules.stream().filter(s -> "Y".equals(s.getEnabled())).count();
            long runningCount = schedules.stream().filter(s -> "0".equals(s.getStatus())).count();
            long pausedCount = schedules.stream().filter(s -> "1".equals(s.getStatus())).count();
            
            return success()
                .put("totalCount", totalCount)
                .put("enabledCount", enabledCount)
                .put("runningCount", runningCount)
                .put("pausedCount", pausedCount);
        } catch (Exception e) {
            logger.error("获取工作流定时调度统计信息失败: {}", e.getMessage(), e);
            return error("获取工作流定时调度统计信息失败: " + e.getMessage());
        }
    }
}