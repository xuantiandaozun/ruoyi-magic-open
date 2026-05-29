package com.ruoyi.project.miniapp.controller.bill;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.bill.domain.BillUserProfile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序-用户扩展信息")
@RestController
@RequestMapping("/miniapp/bill/userProfile")
public class MiniAppBillUserProfileController extends BillMiniAppBaseController {

    @Operation(summary = "获取当前用户扩展信息")
    @GetMapping("/me")
    public AjaxResult me() {
        BillUserProfile profile = requireBillProfile();
        return success(profile);
    }

    @Operation(summary = "获取用户扩展信息")
    @GetMapping("/user/{userId}")
    public AjaxResult getByUserId(@org.springframework.web.bind.annotation.PathVariable Long userId) {
        if (!getBillUserId().equals(userId)) {
            userId = getBillUserId();
        }
        BillUserProfile profile = billUserProfileService.selectByMiniUserId(userId);
        if (profile == null) {
            profile = requireBillProfile();
        }
        return success(profile);
    }

    @Operation(summary = "保存或更新用户扩展信息")
    @PostMapping("/saveOrUpdate")
    public AjaxResult saveOrUpdate(@RequestBody BillUserProfile billUserProfile) {
        billUserProfile.setMiniUserId(getBillUserId());
        billUserProfile.setUserId(getBillUserId());
        return toAjax(billUserProfileService.saveOrUpdateByUserId(billUserProfile) ? 1 : 0);
    }

    @Operation(summary = "更新用户默认账户")
    @PutMapping("/defaultAccount")
    public AjaxResult updateDefaultAccount(@RequestParam Long accountId) {
        BillUserProfile profile = requireBillProfile();
        profile.setDefaultAccountId(accountId);
        return toAjax(billUserProfileService.updateById(profile) ? 1 : 0);
    }

    @Operation(summary = "更新提醒设置")
    @PutMapping("/remindSettings")
    public AjaxResult updateRemindSettings(@RequestBody BillUserProfile billUserProfile) {
        BillUserProfile profile = requireBillProfile();
        if (billUserProfile.getBudgetAlertEnabled() != null) {
            profile.setBudgetAlertEnabled(billUserProfile.getBudgetAlertEnabled());
        }
        if (billUserProfile.getDailyRemindEnabled() != null) {
            profile.setDailyRemindEnabled(billUserProfile.getDailyRemindEnabled());
        }
        if (billUserProfile.getDailyRemindTime() != null) {
            profile.setDailyRemindTime(billUserProfile.getDailyRemindTime());
        }
        return toAjax(billUserProfileService.updateById(profile) ? 1 : 0);
    }
}
