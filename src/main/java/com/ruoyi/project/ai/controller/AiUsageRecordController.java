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
import com.ruoyi.project.ai.domain.AiUsageRecord;
import com.ruoyi.project.ai.service.IAiUsageRecordService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI使用明细")
@RestController
@RequestMapping("/ai/usageRecord")
public class AiUsageRecordController extends BaseController {

    @Autowired
    private IAiUsageRecordService aiUsageRecordService;

    @Operation(summary = "查询AI使用明细列表")
    @SaCheckPermission("ai:usageRecord:list")
    @GetMapping("/list")
    public TableDataInfo list(AiUsageRecord query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiUsageRecord> page = aiUsageRecordService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取AI使用明细详情")
    @SaCheckPermission("ai:usageRecord:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(aiUsageRecordService.getById(id));
    }

    @Operation(summary = "新增AI使用明细")
    @SaCheckPermission("ai:usageRecord:add")
    @Log(title = "AI使用明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiUsageRecord entity) {
        return toAjax(aiUsageRecordService.save(entity));
    }

    @Operation(summary = "修改AI使用明细")
    @SaCheckPermission("ai:usageRecord:edit")
    @Log(title = "AI使用明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiUsageRecord entity) {
        return toAjax(aiUsageRecordService.updateById(entity));
    }

    @Operation(summary = "删除AI使用明细")
    @SaCheckPermission("ai:usageRecord:remove")
    @Log(title = "AI使用明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiUsageRecordService.removeByIds(Arrays.asList(ids)));
    }
}
