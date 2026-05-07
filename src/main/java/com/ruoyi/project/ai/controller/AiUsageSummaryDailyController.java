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
import com.ruoyi.project.ai.domain.AiUsageSummaryDaily;
import com.ruoyi.project.ai.service.IAiUsageSummaryDailyService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI每日用量汇总")
@RestController
@RequestMapping("/ai/usageSummaryDaily")
public class AiUsageSummaryDailyController extends BaseController {

    @Autowired
    private IAiUsageSummaryDailyService aiUsageSummaryDailyService;

    @Operation(summary = "查询AI每日用量汇总列表")
    @SaCheckPermission("ai:usageSummaryDaily:list")
    @GetMapping("/list")
    public TableDataInfo list(AiUsageSummaryDaily query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query).orderBy("summary_date desc");
        Page<AiUsageSummaryDaily> page = aiUsageSummaryDailyService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取AI每日用量汇总详情")
    @SaCheckPermission("ai:usageSummaryDaily:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(aiUsageSummaryDailyService.getById(id));
    }

    @Operation(summary = "新增AI每日用量汇总")
    @SaCheckPermission("ai:usageSummaryDaily:add")
    @Log(title = "AI每日用量汇总", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiUsageSummaryDaily entity) {
        return toAjax(aiUsageSummaryDailyService.save(entity));
    }

    @Operation(summary = "修改AI每日用量汇总")
    @SaCheckPermission("ai:usageSummaryDaily:edit")
    @Log(title = "AI每日用量汇总", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiUsageSummaryDaily entity) {
        return toAjax(aiUsageSummaryDailyService.updateById(entity));
    }

    @Operation(summary = "删除AI每日用量汇总")
    @SaCheckPermission("ai:usageSummaryDaily:remove")
    @Log(title = "AI每日用量汇总", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiUsageSummaryDailyService.removeByIds(Arrays.asList(ids)));
    }
}
