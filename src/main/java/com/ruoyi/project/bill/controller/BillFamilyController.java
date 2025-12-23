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
import com.ruoyi.project.bill.domain.BillFamily;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillFamilyService;
import com.ruoyi.project.bill.service.IBillUserProfileService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 家庭组Controller
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Tag(name = "家庭组管理")
@RestController
@RequestMapping("/bill/family")
public class BillFamilyController extends BaseController {

    @Autowired
    private IBillFamilyService billFamilyService;

    @Autowired
    private IBillUserProfileService billUserProfileService;

    /**
     * 查询家庭组列表
     */
    @Operation(summary = "查询家庭组列表")
    @SaCheckPermission("bill:family:list")
    @GetMapping("/list")
    public TableDataInfo list(BillFamily billFamily) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = buildFlexQueryWrapper(billFamily);

        Page<BillFamily> page = billFamilyService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出家庭组列表
     */
    @Operation(summary = "导出家庭组列表")
    @SaCheckPermission("bill:family:export")
    @Log(title = "家庭组", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BillFamily billFamily) {
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billFamily);
        List<BillFamily> list = billFamilyService.list(queryWrapper);
        MagicExcelUtil<BillFamily> util = new MagicExcelUtil<>(BillFamily.class);
        util.exportExcel(response, list, "家庭组数据");
    }

    /**
     * 获取家庭组详细信息
     */
    @Operation(summary = "获取家庭组详细信息")
    @SaCheckPermission("bill:family:query")
    @GetMapping(value = "/{familyId}")
    public AjaxResult getInfo(@PathVariable Long familyId) {
        return success(billFamilyService.getById(familyId));
    }

    /**
     * 根据邀请码查询家庭组
     */
    @Operation(summary = "根据邀请码查询家庭组")
    @GetMapping("/code/{familyCode}")
    public AjaxResult getByCode(@PathVariable String familyCode) {
        BillFamily family = billFamilyService.selectByInviteCode(familyCode);
        if (family == null) {
            return error("邀请码无效或家庭组不存在");
        }
        return success(family);
    }

    /**
     * 新增家庭组
     */
    @Operation(summary = "新增家庭组")
    @SaCheckPermission("bill:family:add")
    @Log(title = "家庭组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BillFamily billFamily) {
        // 获取当前用户ID
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();

        // 生成邀请码
        String inviteCode = billFamilyService.generateInviteCode();
        billFamily.setFamilyCode(inviteCode);
        billFamily.setCreatorId(userId); // 设置创建者ID
        billFamily.setMemberCount(1); // 创建者为第一个成员

        boolean saved = billFamilyService.save(billFamily);
        if (saved) {
            // 更新创建者的用户扩展信息，关联到该家庭组
            BillUserProfile profile = billUserProfileService.selectByUserId(userId);
            if (profile == null) {
                profile = new BillUserProfile();
                profile.setUserId(userId);
            }
            profile.setFamilyId(billFamily.getFamilyId());
            profile.setFamilyRole("2"); // 创建者为管理员
            billUserProfileService.saveOrUpdateByUserId(profile);
        }

        return toAjax(saved ? 1 : 0);
    }

    /**
     * 修改家庭组
     */
    @Operation(summary = "修改家庭组")
    @SaCheckPermission("bill:family:edit")
    @Log(title = "家庭组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BillFamily billFamily) {
        return toAjax(billFamilyService.updateById(billFamily) ? 1 : 0);
    }

    /**
     * 删除家庭组
     */
    @Operation(summary = "删除家庭组")
    @SaCheckPermission("bill:family:remove")
    @Log(title = "家庭组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{familyIds}")
    public AjaxResult remove(@PathVariable Long[] familyIds) {
        return toAjax(billFamilyService.removeByIds(Arrays.asList(familyIds)) ? familyIds.length : 0);
    }

    /**
     * 加入家庭组（通过邀请码）
     */
    @Operation(summary = "加入家庭组")
    @Log(title = "家庭组", businessType = BusinessType.UPDATE)
    @PostMapping("/join")
    public AjaxResult joinFamily(@RequestBody java.util.Map<String, String> params) {
        String familyCode = params.get("familyCode");
        if (familyCode == null || familyCode.trim().isEmpty()) {
            return error("邀请码不能为空");
        }

        // 获取当前登录用户ID
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();

        BillFamily family = billFamilyService.selectByInviteCode(familyCode);
        if (family == null) {
            return error("邀请码无效或家庭组不存在");
        }

        // 检查用户是否已经在其他家庭组
        BillUserProfile existProfile = billUserProfileService.selectByUserId(userId);
        if (existProfile != null && existProfile.getFamilyId() != null) {
            return error("您已经在其他家庭组中，请先退出后再加入");
        }

        // 更新用户的家庭组信息
        BillUserProfile profile = existProfile != null ? existProfile : new BillUserProfile();
        if (existProfile == null) {
            profile.setUserId(userId);
        }
        profile.setFamilyId(family.getFamilyId());
        profile.setFamilyRole("1"); // 普通成员
        billUserProfileService.saveOrUpdateByUserId(profile);

        // 更新成员数量
        billFamilyService.updateMemberCount(family.getFamilyId(), 1);

        return success("加入家庭组成功", family);
    }

    /**
     * 退出家庭组
     */
    @Operation(summary = "退出家庭组")
    @Log(title = "家庭组", businessType = BusinessType.UPDATE)
    @PostMapping("/leave/{familyId}")
    public AjaxResult leaveFamily(@PathVariable Long familyId) {
        // 获取当前登录用户ID
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();

        BillFamily family = billFamilyService.getById(familyId);
        if (family == null) {
            return error("家庭组不存在");
        }

        // 检查是否是创建者
        if (userId.equals(family.getCreatorId())) {
            return error("创建者不能退出,请先转让家庭组或解散家庭组");
        }

        // 清除用户的家庭组信息
        BillUserProfile profile = billUserProfileService.selectByUserId(userId);
        if (profile != null) {
            profile.setFamilyId(null);
            profile.setFamilyRole(null);
            billUserProfileService.updateById(profile);
        }

        // 更新成员数量
        billFamilyService.updateMemberCount(familyId, -1);

        return success("退出家庭组成功");
    }

    /**
     * 获取家庭组成员列表
     */
    @Operation(summary = "获取家庭组成员列表")
    @GetMapping("/members/{familyId}")
    public AjaxResult getMembers(@PathVariable Long familyId) {
        BillFamily family = billFamilyService.getById(familyId);
        if (family == null) {
            return error("家庭组不存在");
        }

        // 查询该家庭组的所有成员
        List<BillUserProfile> members = billUserProfileService.selectByFamilyId(familyId);

        return success(members);
    }

    /**
     * 移除家庭组成员
     */
    @Operation(summary = "移除家庭组成员")
    @Log(title = "家庭组", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{familyId}/member/{userId}")
    public AjaxResult removeMember(
            @PathVariable Long familyId,
            @PathVariable Long userId) {
        BillFamily family = billFamilyService.getById(familyId);
        if (family == null) {
            return error("家庭组不存在");
        }

        // 检查是否是创建者
        if (userId.equals(family.getCreatorId())) {
            return error("不能移除创建者");
        }

        // 清除用户的家庭组信息
        BillUserProfile profile = billUserProfileService.selectByUserId(userId);
        if (profile != null) {
            profile.setFamilyId(null);
            profile.setFamilyRole(null);
            billUserProfileService.updateById(profile);
        }

        // 更新成员数量
        billFamilyService.updateMemberCount(familyId, -1);

        return success("成员已移除");
    }

    /**
     * 重新生成邀请码
     */
    @Operation(summary = "重新生成邀请码")
    @SaCheckPermission("bill:family:edit")
    @Log(title = "家庭组", businessType = BusinessType.UPDATE)
    @PutMapping("/regenerateCode/{familyId}")
    public AjaxResult regenerateCode(@PathVariable Long familyId) {
        BillFamily family = billFamilyService.getById(familyId);
        if (family == null) {
            return error("家庭组不存在");
        }

        String newCode = billFamilyService.generateInviteCode();
        family.setFamilyCode(newCode);
        billFamilyService.updateById(family);

        return success("邀请码已重新生成", newCode);
    }
}
