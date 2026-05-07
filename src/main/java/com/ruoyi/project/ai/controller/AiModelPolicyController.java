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
import com.ruoyi.project.ai.domain.AiModelPolicy;
import com.ruoyi.project.ai.service.IAiModelPolicyService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI模型策略")
@RestController
@RequestMapping("/ai/modelPolicy")
public class AiModelPolicyController extends BaseController {

    @Autowired
    private IAiModelPolicyService aiModelPolicyService;

    @Operation(summary = "查询AI模型策略列表")
    @SaCheckPermission("ai:modelPolicy:list")
    @GetMapping("/list")
    public TableDataInfo list(AiModelPolicy query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiModelPolicy> page = aiModelPolicyService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取AI模型策略详情")
    @SaCheckPermission("ai:modelPolicy:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(aiModelPolicyService.getById(id));
    }

    @Operation(summary = "新增AI模型策略")
    @SaCheckPermission("ai:modelPolicy:add")
    @Log(title = "AI模型策略", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiModelPolicy entity) {
        return toAjax(aiModelPolicyService.save(entity));
    }

    @Operation(summary = "修改AI模型策略")
    @SaCheckPermission("ai:modelPolicy:edit")
    @Log(title = "AI模型策略", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiModelPolicy entity) {
        return toAjax(aiModelPolicyService.updateById(entity));
    }

    @Operation(summary = "删除AI模型策略")
    @SaCheckPermission("ai:modelPolicy:remove")
    @Log(title = "AI模型策略", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiModelPolicyService.removeByIds(Arrays.asList(ids)));
    }
}
