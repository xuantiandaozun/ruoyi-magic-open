package com.ruoyi.project.bill.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.bill.domain.BillReminder;
import com.ruoyi.project.bill.service.IBillReminderService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 提醒设置Controller
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Tag(name = "提醒设置")
@RestController
@RequestMapping("/bill/reminder")
public class BillReminderController extends BaseController {

    @Autowired
    private IBillReminderService billReminderService;

    /**
     * 查询提醒列表
     */
    @Operation(summary = "查询提醒列表")
    @SaCheckPermission("bill:reminder:list")
    @GetMapping("/list")
    public TableDataInfo list(BillReminder billReminder) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = buildFlexQueryWrapper(billReminder);

        Page<BillReminder> page = billReminderService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 查询用户提醒列表（不分页）
     */
    @Operation(summary = "查询用户提醒列表")
    @SaCheckPermission("bill:reminder:query")
    @GetMapping("/user/{userId}")
    public AjaxResult listByUser(@PathVariable Long userId) {
        List<BillReminder> list = billReminderService.selectByUserId(userId);
        return success(list);
    }

    /**
     * 查询启用的提醒列表
     */
    @Operation(summary = "查询启用的提醒列表")
    @SaCheckPermission("bill:reminder:query")
    @GetMapping("/enabled/{userId}")
    public AjaxResult listEnabled(@PathVariable Long userId) {
        List<BillReminder> list = billReminderService.selectEnabledByUserId(userId);
        return success(list);
    }

    /**
     * 导出提醒列表
     */
    @Operation(summary = "导出提醒列表")
    @SaCheckPermission("bill:reminder:export")
    @Log(title = "提醒设置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BillReminder billReminder) {
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billReminder);
        List<BillReminder> list = billReminderService.list(queryWrapper);
        MagicExcelUtil<BillReminder> util = new MagicExcelUtil<>(BillReminder.class);
        util.exportExcel(response, list, "提醒设置数据");
    }

    /**
     * 获取提醒详细信息
     */
    @Operation(summary = "获取提醒详细信息")
    @SaCheckPermission("bill:reminder:query")
    @GetMapping(value = "/{reminderId}")
    public AjaxResult getInfo(@PathVariable Long reminderId) {
        return success(billReminderService.getById(reminderId));
    }

    /**
     * 新增提醒
     */
    @Operation(summary = "新增提醒")
    @SaCheckPermission("bill:reminder:add")
    @Log(title = "提醒设置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BillReminder billReminder) {
        // 默认启用
        if (billReminder.getEnabled() == null) {
            billReminder.setEnabled("1");
        }
        return toAjax(billReminderService.save(billReminder) ? 1 : 0);
    }

    /**
     * 修改提醒
     */
    @Operation(summary = "修改提醒")
    @SaCheckPermission("bill:reminder:edit")
    @Log(title = "提醒设置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BillReminder billReminder) {
        return toAjax(billReminderService.updateById(billReminder) ? 1 : 0);
    }

    /**
     * 删除提醒
     */
    @Operation(summary = "删除提醒")
    @SaCheckPermission("bill:reminder:remove")
    @Log(title = "提醒设置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reminderIds}")
    public AjaxResult remove(@PathVariable Long[] reminderIds) {
        return toAjax(billReminderService.removeByIds(Arrays.asList(reminderIds)) ? reminderIds.length : 0);
    }

    /**
     * 启用提醒
     */
    @Operation(summary = "启用提醒")
    @SaCheckPermission("bill:reminder:edit")
    @Log(title = "提醒设置", businessType = BusinessType.UPDATE)
    @PutMapping("/enable/{reminderId}")
    public AjaxResult enable(@PathVariable Long reminderId) {
        return toAjax(billReminderService.enableReminder(reminderId, true) ? 1 : 0);
    }

    /**
     * 禁用提醒
     */
    @Operation(summary = "禁用提醒")
    @SaCheckPermission("bill:reminder:edit")
    @Log(title = "提醒设置", businessType = BusinessType.UPDATE)
    @PutMapping("/disable/{reminderId}")
    public AjaxResult disable(@PathVariable Long reminderId) {
        return toAjax(billReminderService.enableReminder(reminderId, false) ? 1 : 0);
    }

    /**
     * 切换提醒状态
     */
    @Operation(summary = "切换提醒状态")
    @SaCheckPermission("bill:reminder:edit")
    @Log(title = "提醒设置", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{reminderId}")
    public AjaxResult toggle(@PathVariable Long reminderId) {
        BillReminder reminder = billReminderService.getById(reminderId);
        if (reminder == null) {
            return error("提醒不存在");
        }

        boolean enable = "0".equals(reminder.getEnabled());
        return toAjax(billReminderService.enableReminder(reminderId, enable) ? 1 : 0);
    }

    /**
     * 批量启用提醒
     */
    @Operation(summary = "批量启用提醒")
    @SaCheckPermission("bill:reminder:edit")
    @Log(title = "提醒设置", businessType = BusinessType.UPDATE)
    @PutMapping("/batchEnable")
    public AjaxResult batchEnable(@RequestBody Long[] reminderIds) {
        int count = 0;
        for (Long reminderId : reminderIds) {
            if (billReminderService.enableReminder(reminderId, true)) {
                count++;
            }
        }
        return toAjax(count);
    }

    /**
     * 批量禁用提醒
     */
    @Operation(summary = "批量禁用提醒")
    @SaCheckPermission("bill:reminder:edit")
    @Log(title = "提醒设置", businessType = BusinessType.UPDATE)
    @PutMapping("/batchDisable")
    public AjaxResult batchDisable(@RequestBody Long[] reminderIds) {
        int count = 0;
        for (Long reminderId : reminderIds) {
            if (billReminderService.enableReminder(reminderId, false)) {
                count++;
            }
        }
        return toAjax(count);
    }

    /**
     * 根据类型查询提醒
     */
    @Operation(summary = "根据类型查询提醒")
    @SaCheckPermission("bill:reminder:query")
    @GetMapping("/type/{userId}/{reminderType}")
    public AjaxResult listByType(
            @PathVariable Long userId,
            @PathVariable String reminderType) {
        List<BillReminder> list = billReminderService.selectByUserId(userId);
        list = list.stream()
                .filter(r -> reminderType.equals(r.getReminderType()))
                .toList();
        return success(list);
    }

    /**
     * 创建默认提醒（为新用户创建）
     */
    @Operation(summary = "创建默认提醒")
    @SaCheckPermission("bill:reminder:add")
    @Log(title = "提醒设置", businessType = BusinessType.INSERT)
    @PostMapping("/createDefault/{userId}")
    public AjaxResult createDefaultReminders(@PathVariable Long userId) {
        // 创建默认的每日记账提醒
        BillReminder dailyReminder = new BillReminder();
        dailyReminder.setUserId(userId);
        dailyReminder.setReminderType("0"); // 记账提醒
        dailyReminder.setReminderName("每日记账提醒");
        dailyReminder.setReminderTime(java.time.LocalTime.parse("21:00:00"));
        dailyReminder.setRepeatType("0"); // 每天
        dailyReminder.setEnabled("1");

        boolean success = billReminderService.save(dailyReminder);

        return toAjax(success ? 1 : 0);
    }
}
