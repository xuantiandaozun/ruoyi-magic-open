package com.ruoyi.project.bill.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillUserProfileService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户扩展信息Controller
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Tag(name = "用户扩展信息")
@RestController
@RequestMapping("/bill/userProfile")
public class BillUserProfileController extends BaseController {

    @Autowired
    private IBillUserProfileService billUserProfileService;

    /**
     * 获取用户扩展信息（根据userId）
     */
    @Operation(summary = "获取用户扩展信息")
    @SaCheckPermission("bill:userProfile:query")
    @GetMapping("/user/{userId}")
    public AjaxResult getByUserId(@PathVariable Long userId) {
        BillUserProfile profile = billUserProfileService.selectByUserId(userId);
        if (profile == null) {
            // 如果没有扩展信息，返回空对象
            profile = new BillUserProfile();
            profile.setUserId(userId);
        }
        return success(profile);
    }

    /**
     * 获取用户扩展信息详细信息
     */
    @Operation(summary = "获取用户扩展信息详细信息")
    @SaCheckPermission("bill:userProfile:query")
    @GetMapping(value = "/{profileId}")
    public AjaxResult getInfo(@PathVariable Long profileId) {
        return success(billUserProfileService.getById(profileId));
    }

    /**
     * 保存或更新用户扩展信息
     */
    @Operation(summary = "保存或更新用户扩展信息")
    @SaCheckPermission("bill:userProfile:edit")
    @Log(title = "用户扩展信息", businessType = BusinessType.UPDATE)
    @PostMapping("/saveOrUpdate")
    public AjaxResult saveOrUpdate(@RequestBody BillUserProfile billUserProfile) {
        return toAjax(billUserProfileService.saveOrUpdateByUserId(billUserProfile) ? 1 : 0);
    }

    /**
     * 修改用户扩展信息
     */
    @Operation(summary = "修改用户扩展信息")
    @SaCheckPermission("bill:userProfile:edit")
    @Log(title = "用户扩展信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BillUserProfile billUserProfile) {
        return toAjax(billUserProfileService.updateById(billUserProfile) ? 1 : 0);
    }

    /**
     * 更新用户默认账户
     */
    @Operation(summary = "更新用户默认账户")
    @SaCheckPermission("bill:userProfile:edit")
    @Log(title = "用户扩展信息", businessType = BusinessType.UPDATE)
    @PutMapping("/defaultAccount")
    public AjaxResult updateDefaultAccount(
            @RequestParam Long userId,
            @RequestParam Long accountId) {
        BillUserProfile profile = billUserProfileService.selectByUserId(userId);
        if (profile == null) {
            return error("用户扩展信息不存在");
        }
        profile.setDefaultAccountId(accountId);
        return toAjax(billUserProfileService.updateById(profile) ? 1 : 0);
    }

    /**
     * 更新提醒设置
     */
    @Operation(summary = "更新提醒设置")
    @SaCheckPermission("bill:userProfile:edit")
    @Log(title = "用户扩展信息", businessType = BusinessType.UPDATE)
    @PutMapping("/remindSettings")
    public AjaxResult updateRemindSettings(@RequestBody BillUserProfile billUserProfile) {
        BillUserProfile profile = billUserProfileService.selectByUserId(billUserProfile.getUserId());
        if (profile == null) {
            return error("用户扩展信息不存在");
        }

        // 只更新提醒相关字段
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
