package com.ruoyi.project.ai.controller;

import java.util.Arrays;
import java.util.Map;

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
import com.ruoyi.project.ai.domain.AiOpenApiKey;
import com.ruoyi.project.ai.service.IAiOpenApiKeyService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI对外API Key")
@RestController
@RequestMapping("/ai/openApiKey")
public class AiOpenApiKeyController extends BaseController {

    private final IAiOpenApiKeyService openApiKeyService;

    public AiOpenApiKeyController(IAiOpenApiKeyService openApiKeyService) {
        this.openApiKeyService = openApiKeyService;
    }

    @Operation(summary = "查询对外API Key列表")
    @SaCheckPermission("ai:openApiKey:list")
    @GetMapping("/list")
    public TableDataInfo list(AiOpenApiKey query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiOpenApiKey> page = openApiKeyService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取对外API Key详情")
    @SaCheckPermission("ai:openApiKey:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(openApiKeyService.getById(id));
    }

    @Operation(summary = "创建对外API Key")
    @SaCheckPermission("ai:openApiKey:add")
    @Log(title = "AI对外API Key", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiOpenApiKey entity) {
        entity.setCreateBy(getUsername());
        entity.setUpdateBy(getUsername());
        Map<String, Object> result = openApiKeyService.createKey(entity);
        return success(result);
    }

    @Operation(summary = "修改对外API Key")
    @SaCheckPermission("ai:openApiKey:edit")
    @Log(title = "AI对外API Key", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiOpenApiKey entity) {
        entity.setUpdateBy(getUsername());
        return toAjax(openApiKeyService.updateById(entity));
    }

    @Operation(summary = "删除对外API Key")
    @SaCheckPermission("ai:openApiKey:remove")
    @Log(title = "AI对外API Key", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(openApiKeyService.removeByIds(Arrays.asList(ids)));
    }
}
