package com.ruoyi.project.miniapp.controller.admin;

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
import com.ruoyi.project.miniapp.domain.MiniSubscribeTemplate;
import com.ruoyi.project.miniapp.service.IMiniSubscribeTemplateService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 订阅消息模板管理 Controller
 */
@Tag(name = "订阅消息模板管理")
@RestController
@RequestMapping("/manage/miniSubscribeTemplate")
public class MiniSubscribeTemplateManageController extends BaseController {

    @Autowired
    private IMiniSubscribeTemplateService subscribeTemplateService;

    @Operation(summary = "查询订阅消息模板列表")
    @SaCheckPermission("manage:miniSubscribeTemplate:list")
    @GetMapping("/list")
    public TableDataInfo list(MiniSubscribeTemplate template) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        QueryWrapper queryWrapper = buildFlexQueryWrapper(template);
        Page<MiniSubscribeTemplate> page = subscribeTemplateService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "获取订阅消息模板详情")
    @SaCheckPermission("manage:miniSubscribeTemplate:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(subscribeTemplateService.getById(id));
    }

    @Operation(summary = "新增订阅消息模板")
    @SaCheckPermission("manage:miniSubscribeTemplate:add")
    @Log(title = "订阅消息模板管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MiniSubscribeTemplate template) {
        return toAjax(subscribeTemplateService.save(template) ? 1 : 0);
    }

    @Operation(summary = "修改订阅消息模板")
    @SaCheckPermission("manage:miniSubscribeTemplate:edit")
    @Log(title = "订阅消息模板管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MiniSubscribeTemplate template) {
        return toAjax(subscribeTemplateService.updateById(template) ? 1 : 0);
    }

    @Operation(summary = "删除订阅消息模板")
    @SaCheckPermission("manage:miniSubscribeTemplate:remove")
    @Log(title = "订阅消息模板管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(subscribeTemplateService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
}
