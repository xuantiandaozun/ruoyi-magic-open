package com.ruoyi.project.system.controller;

import java.util.Arrays;

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
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.system.domain.SysOauthAccount;
import com.ruoyi.project.system.service.ISysOauthAccountService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "第三方账号绑定")
@RestController
@RequestMapping("/system/oauthAccount")
public class SysOauthAccountController extends BaseController {

    @Autowired
    private ISysOauthAccountService sysOauthAccountService;

    @Operation(summary = "查询第三方账号绑定列表")
    @SaCheckPermission("system:oauthAccount:list")
    @GetMapping("/list")
    public TableDataInfo list(SysOauthAccount query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<SysOauthAccount> page = sysOauthAccountService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取第三方账号绑定详情")
    @SaCheckPermission("system:oauthAccount:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(sysOauthAccountService.getById(id));
    }

    @Operation(summary = "新增第三方账号绑定")
    @SaCheckPermission("system:oauthAccount:add")
    @Log(title = "第三方账号绑定", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysOauthAccount entity) {
        return toAjax(sysOauthAccountService.save(entity));
    }

    @Operation(summary = "修改第三方账号绑定")
    @SaCheckPermission("system:oauthAccount:edit")
    @Log(title = "第三方账号绑定", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysOauthAccount entity) {
        return toAjax(sysOauthAccountService.updateById(entity));
    }

    @Operation(summary = "删除第三方账号绑定")
    @SaCheckPermission("system:oauthAccount:remove")
    @Log(title = "第三方账号绑定", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(sysOauthAccountService.removeByIds(Arrays.asList(ids)));
    }
}
