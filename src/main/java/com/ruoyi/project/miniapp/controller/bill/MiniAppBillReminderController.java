package com.ruoyi.project.miniapp.controller.bill;

import java.time.LocalTime;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.bill.domain.BillReminder;
import com.ruoyi.project.bill.service.IBillReminderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序-提醒设置")
@RestController
@RequestMapping("/miniapp/bill/reminder")
public class MiniAppBillReminderController extends BillMiniAppBaseController {

    @Autowired
    private IBillReminderService billReminderService;

    @Operation(summary = "查询提醒列表")
    @GetMapping("/list")
    public TableDataInfo list(BillReminder billReminder) {
        billReminder.setUserId(getBillUserId());
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billReminder);
        Page<BillReminder> page = billReminderService.page(
                new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "查询用户提醒列表")
    @GetMapping("/user")
    public AjaxResult listByUser(@RequestParam(required = false) Long userId) {
        if (userId == null || !getBillUserId().equals(userId)) {
            userId = getBillUserId();
        }
        List<BillReminder> list = billReminderService.selectByUserId(userId);
        return success(list);
    }

    @Operation(summary = "查询启用的提醒列表")
    @GetMapping("/enabled")
    public AjaxResult listEnabled(@RequestParam(required = false) Long userId) {
        if (userId == null || !getBillUserId().equals(userId)) {
            userId = getBillUserId();
        }
        List<BillReminder> list = billReminderService.selectEnabledByUserId(userId);
        return success(list);
    }

    @Operation(summary = "获取提醒详细信息")
    @GetMapping("/{reminderId}")
    public AjaxResult getInfo(@PathVariable Long reminderId) {
        return success(billReminderService.getById(reminderId));
    }

    @Operation(summary = "新增提醒")
    @PostMapping
    public AjaxResult add(@RequestBody BillReminder billReminder) {
        billReminder.setUserId(getBillUserId());
        if (billReminder.getEnabled() == null) {
            billReminder.setEnabled("1");
        }
        return toAjax(billReminderService.save(billReminder) ? 1 : 0);
    }

    @Operation(summary = "修改提醒")
    @PutMapping
    public AjaxResult edit(@RequestBody BillReminder billReminder) {
        billReminder.setUserId(getBillUserId());
        return toAjax(billReminderService.updateById(billReminder) ? 1 : 0);
    }

    @Operation(summary = "删除提醒")
    @DeleteMapping("/{reminderIds}")
    public AjaxResult remove(@PathVariable Long[] reminderIds) {
        return toAjax(billReminderService.removeByIds(Arrays.asList(reminderIds)) ? reminderIds.length : 0);
    }

    @Operation(summary = "启用提醒")
    @PutMapping("/enable/{reminderId}")
    public AjaxResult enable(@PathVariable Long reminderId) {
        return toAjax(billReminderService.enableReminder(reminderId, true) ? 1 : 0);
    }

    @Operation(summary = "禁用提醒")
    @PutMapping("/disable/{reminderId}")
    public AjaxResult disable(@PathVariable Long reminderId) {
        return toAjax(billReminderService.enableReminder(reminderId, false) ? 1 : 0);
    }

    @Operation(summary = "切换提醒状态")
    @PutMapping("/toggle/{reminderId}")
    public AjaxResult toggle(@PathVariable Long reminderId) {
        BillReminder reminder = billReminderService.getById(reminderId);
        if (reminder == null) {
            return error("提醒不存在");
        }

        boolean enable = "0".equals(reminder.getEnabled());
        return toAjax(billReminderService.enableReminder(reminderId, enable) ? 1 : 0);
    }

    @Operation(summary = "根据类型查询提醒")
    @GetMapping("/type/{reminderType}")
    public AjaxResult listByType(@PathVariable String reminderType) {
        List<BillReminder> list = billReminderService.selectByUserId(getBillUserId()).stream()
                .filter(reminder -> reminderType.equals(reminder.getReminderType()))
                .toList();
        return success(list);
    }

    @Operation(summary = "创建默认提醒")
    @PostMapping("/createDefault")
    public AjaxResult createDefaultReminders() {
        BillReminder dailyReminder = new BillReminder();
        dailyReminder.setUserId(getBillUserId());
        dailyReminder.setReminderType("0");
        dailyReminder.setReminderName("每日记账提醒");
        dailyReminder.setReminderTime(LocalTime.parse("21:00:00"));
        dailyReminder.setRepeatType("0");
        dailyReminder.setEnabled("1");
        return toAjax(billReminderService.save(dailyReminder) ? 1 : 0);
    }
}
