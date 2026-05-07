package com.ruoyi.project.ai.controller;

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
import com.ruoyi.project.ai.domain.AiProductApp;
import com.ruoyi.project.ai.service.IAiProductAppService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI产品应用")
@RestController
@RequestMapping("/ai/productApp")
public class AiProductAppController extends BaseController {

    @Autowired
    private IAiProductAppService aiProductAppService;

    @Operation(summary = "查询AI产品应用列表")
    @SaCheckPermission("ai:productApp:list")
    @GetMapping("/list")
    public TableDataInfo list(AiProductApp query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiProductApp> page = aiProductAppService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取AI产品应用详情")
    @SaCheckPermission("ai:productApp:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(aiProductAppService.getById(id));
    }

    @Operation(summary = "新增AI产品应用")
    @SaCheckPermission("ai:productApp:add")
    @Log(title = "AI产品应用", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiProductApp entity) {
        return toAjax(aiProductAppService.save(entity));
    }

    @Operation(summary = "修改AI产品应用")
    @SaCheckPermission("ai:productApp:edit")
    @Log(title = "AI产品应用", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiProductApp entity) {
        return toAjax(aiProductAppService.updateById(entity));
    }

    @Operation(summary = "删除AI产品应用")
    @SaCheckPermission("ai:productApp:remove")
    @Log(title = "AI产品应用", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiProductAppService.removeByIds(Arrays.asList(ids)));
    }
}
