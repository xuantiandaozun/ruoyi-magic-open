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
import com.ruoyi.project.ai.domain.AiUsageQuota;
import com.ruoyi.project.ai.service.IAiUsageQuotaService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI用户额度")
@RestController
@RequestMapping("/ai/usageQuota")
public class AiUsageQuotaController extends BaseController {

    @Autowired
    private IAiUsageQuotaService aiUsageQuotaService;

    @Operation(summary = "查询AI用户额度列表")
    @SaCheckPermission("ai:usageQuota:list")
    @GetMapping("/list")
    public TableDataInfo list(AiUsageQuota query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiUsageQuota> page = aiUsageQuotaService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取AI用户额度详情")
    @SaCheckPermission("ai:usageQuota:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(aiUsageQuotaService.getById(id));
    }

    @Operation(summary = "新增AI用户额度")
    @SaCheckPermission("ai:usageQuota:add")
    @Log(title = "AI用户额度", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiUsageQuota entity) {
        return toAjax(aiUsageQuotaService.save(entity));
    }

    @Operation(summary = "修改AI用户额度")
    @SaCheckPermission("ai:usageQuota:edit")
    @Log(title = "AI用户额度", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiUsageQuota entity) {
        return toAjax(aiUsageQuotaService.updateById(entity));
    }

    @Operation(summary = "删除AI用户额度")
    @SaCheckPermission("ai:usageQuota:remove")
    @Log(title = "AI用户额度", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiUsageQuotaService.removeByIds(Arrays.asList(ids)));
    }
}
