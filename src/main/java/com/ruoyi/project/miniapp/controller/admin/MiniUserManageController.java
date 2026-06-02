package com.ruoyi.project.miniapp.controller.admin;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.ruoyi.project.miniapp.domain.MiniUser;
import com.ruoyi.project.miniapp.service.IMiniUserService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 小程序用户管理 Controller
 */
@Tag(name = "小程序用户管理")
@RestController
@RequestMapping("/manage/miniuser")
public class MiniUserManageController extends BaseController {

    @Autowired
    private IMiniUserService miniUserService;

    @Operation(summary = "查询小程序用户列表")
    @SaCheckPermission("manage:miniuser:list")
    @GetMapping("/list")
    public TableDataInfo list(MiniUser miniUser) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        QueryWrapper queryWrapper = buildFlexQueryWrapper(miniUser);
        Page<MiniUser> page = miniUserService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "获取小程序用户详情")
    @SaCheckPermission("manage:miniuser:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(miniUserService.getById(id));
    }

    @Operation(summary = "修改小程序用户状态")
    @SaCheckPermission("manage:miniuser:edit")
    @Log(title = "小程序用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody MiniUser miniUser) {
        return toAjax(miniUserService.updateById(miniUser) ? 1 : 0);
    }

    @Operation(summary = "删除小程序用户(逻辑删除)")
    @SaCheckPermission("manage:miniuser:remove")
    @Log(title = "小程序用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(miniUserService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
}
