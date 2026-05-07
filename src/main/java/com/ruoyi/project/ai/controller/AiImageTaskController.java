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
import com.ruoyi.project.ai.domain.AiImageTask;
import com.ruoyi.project.ai.service.IAiImageTaskService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI生图任务")
@RestController
@RequestMapping("/ai/imageTask")
public class AiImageTaskController extends BaseController {

    @Autowired
    private IAiImageTaskService aiImageTaskService;

    @Operation(summary = "查询AI生图任务列表")
    @SaCheckPermission("ai:imageTask:list")
    @GetMapping("/list")
    public TableDataInfo list(AiImageTask query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiImageTask> page = aiImageTaskService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取AI生图任务详情")
    @SaCheckPermission("ai:imageTask:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(aiImageTaskService.getById(id));
    }

    @Operation(summary = "新增AI生图任务")
    @SaCheckPermission("ai:imageTask:add")
    @Log(title = "AI生图任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiImageTask entity) {
        return toAjax(aiImageTaskService.save(entity));
    }

    @Operation(summary = "修改AI生图任务")
    @SaCheckPermission("ai:imageTask:edit")
    @Log(title = "AI生图任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiImageTask entity) {
        return toAjax(aiImageTaskService.updateById(entity));
    }

    @Operation(summary = "删除AI生图任务")
    @SaCheckPermission("ai:imageTask:remove")
    @Log(title = "AI生图任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiImageTaskService.removeByIds(Arrays.asList(ids)));
    }
}
