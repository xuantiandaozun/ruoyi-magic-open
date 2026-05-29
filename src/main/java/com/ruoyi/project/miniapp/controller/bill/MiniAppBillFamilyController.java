package com.ruoyi.project.miniapp.controller.bill;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.bill.domain.BillFamily;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillFamilyService;
import com.ruoyi.project.bill.service.IBillRecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序-家庭组")
@RestController
@RequestMapping("/miniapp/bill/family")
public class MiniAppBillFamilyController extends BillMiniAppBaseController {

    @Autowired
    private IBillFamilyService billFamilyService;

    @Autowired
    private IBillRecordService billRecordService;

    @Operation(summary = "获取家庭组详细信息")
    @GetMapping("/{familyId}")
    public AjaxResult getInfo(@PathVariable Long familyId) {
        return success(billFamilyService.getById(familyId));
    }

    @Operation(summary = "根据邀请码查询家庭组")
    @GetMapping("/code/{familyCode}")
    public AjaxResult getByCode(@PathVariable String familyCode) {
        BillFamily family = billFamilyService.selectByInviteCode(familyCode);
        if (family == null) {
            return error("邀请码无效或家庭组不存在");
        }
        return success(family);
    }

    @Operation(summary = "新增家庭组")
    @PostMapping
    public AjaxResult add(@RequestBody BillFamily billFamily) {
        Long userId = getBillUserId();
        String inviteCode = billFamilyService.generateInviteCode();
        billFamily.setFamilyCode(inviteCode);
        billFamily.setCreatorId(userId);
        billFamily.setMemberCount(1);

        boolean saved = billFamilyService.save(billFamily);
        if (saved) {
            BillUserProfile profile = requireBillProfile();
            profile.setFamilyId(billFamily.getFamilyId());
            profile.setFamilyRole("2");
            billUserProfileService.saveOrUpdateByUserId(profile);
            billRecordService.migratePersonalRecordsToFamily(userId, billFamily.getFamilyId());
        }
        return toAjax(saved ? 1 : 0);
    }

    @Operation(summary = "加入家庭组")
    @PostMapping("/join")
    public AjaxResult joinFamily(@RequestBody Map<String, String> params) {
        String familyCode = params.get("familyCode");
        if (familyCode == null || familyCode.trim().isEmpty()) {
            return error("邀请码不能为空");
        }

        Long userId = getBillUserId();
        BillFamily family = billFamilyService.selectByInviteCode(familyCode);
        if (family == null) {
            return error("邀请码无效或家庭组不存在");
        }

        BillUserProfile existProfile = billUserProfileService.selectByMiniUserId(userId);
        if (existProfile != null && existProfile.getFamilyId() != null) {
            return error("您已经在其他家庭组中，请先退出后再加入");
        }

        BillUserProfile profile = existProfile != null ? existProfile : requireBillProfile();
        profile.setFamilyId(family.getFamilyId());
        profile.setFamilyRole("1");
        billUserProfileService.saveOrUpdateByUserId(profile);
        billRecordService.migratePersonalRecordsToFamily(userId, family.getFamilyId());
        billFamilyService.updateMemberCount(family.getFamilyId(), 1);
        return success("加入家庭组成功", family);
    }

    @Operation(summary = "退出家庭组")
    @PostMapping("/leave/{familyId}")
    public AjaxResult leaveFamily(@PathVariable Long familyId) {
        Long userId = getBillUserId();
        BillFamily family = billFamilyService.getById(familyId);
        if (family == null) {
            return error("家庭组不存在");
        }
        if (userId.equals(family.getCreatorId())) {
            return error("创建者不能退出,请先转让家庭组或解散家庭组");
        }

        BillUserProfile profile = billUserProfileService.selectByMiniUserId(userId);
        if (profile != null) {
            profile.setFamilyId(null);
            profile.setFamilyRole(null);
            billUserProfileService.updateById(profile);
        }
        billFamilyService.updateMemberCount(familyId, -1);
        return success("退出家庭组成功");
    }

    @Operation(summary = "获取家庭组成员列表")
    @GetMapping("/members/{familyId}")
    public AjaxResult getMembers(@PathVariable Long familyId) {
        BillFamily family = billFamilyService.getById(familyId);
        if (family == null) {
            return error("家庭组不存在");
        }
        List<BillUserProfile> members = billUserProfileService.selectByFamilyId(familyId);
        return success(members);
    }

    @Operation(summary = "移除家庭组成员")
    @DeleteMapping("/{familyId}/member/{userId}")
    public AjaxResult removeMember(@PathVariable Long familyId, @PathVariable Long userId) {
        BillFamily family = billFamilyService.getById(familyId);
        if (family == null) {
            return error("家庭组不存在");
        }
        if (userId.equals(family.getCreatorId())) {
            return error("不能移除创建者");
        }

        BillUserProfile profile = billUserProfileService.selectByUserId(userId);
        if (profile == null) {
            profile = billUserProfileService.selectByMiniUserId(userId);
        }
        if (profile != null) {
            profile.setFamilyId(null);
            profile.setFamilyRole(null);
            billUserProfileService.updateById(profile);
        }
        billFamilyService.updateMemberCount(familyId, -1);
        return success("成员已移除");
    }

    @Operation(summary = "重新生成邀请码")
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
