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
import org.springframework.web.bind.annotation.RequestParam;
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
import com.ruoyi.project.bill.service.IBillFamilyService;

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
        // 生成邀请码
        String inviteCode = billFamilyService.generateInviteCode();
        billFamily.setFamilyCode(inviteCode);
        billFamily.setMemberCount(1); // 创建者为第一个成员

        return toAjax(billFamilyService.save(billFamily) ? 1 : 0);
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
    public AjaxResult joinFamily(
            @RequestParam String familyCode,
            @RequestParam Long userId) {
        BillFamily family = billFamilyService.selectByInviteCode(familyCode);
        if (family == null) {
            return error("邀请码无效或家庭组不存在");
        }

        // 更新成员数量
        billFamilyService.updateMemberCount(family.getFamilyId(), 1);

        return success("加入家庭组成功", family);
    }

    /**
     * 退出家庭组
     */
    @Operation(summary = "退出家庭组")
    @Log(title = "家庭组", businessType = BusinessType.UPDATE)
    @PostMapping("/leave")
    public AjaxResult leaveFamily(
            @RequestParam Long familyId,
            @RequestParam Long userId) {
        BillFamily family = billFamilyService.getById(familyId);
        if (family == null) {
            return error("家庭组不存在");
        }

        // 检查是否是创建者
        if (userId.equals(family.getCreatorId())) {
            return error("创建者不能退出,请先转让家庭组或解散家庭组");
        }

        // 更新成员数量
        billFamilyService.updateMemberCount(familyId, -1);

        return success("退出家庭组成功");
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
