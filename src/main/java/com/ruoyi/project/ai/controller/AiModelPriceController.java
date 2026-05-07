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
import com.ruoyi.project.ai.domain.AiModelPrice;
import com.ruoyi.project.ai.service.IAiModelPriceService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI模型价格")
@RestController
@RequestMapping("/ai/modelPrice")
public class AiModelPriceController extends BaseController {

    @Autowired
    private IAiModelPriceService aiModelPriceService;

    @Operation(summary = "查询AI模型价格列表")
    @SaCheckPermission("ai:modelPrice:list")
    @GetMapping("/list")
    public TableDataInfo list(AiModelPrice query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiModelPrice> page = aiModelPriceService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);
        return getDataTable(page);
    }

    @Operation(summary = "获取AI模型价格详情")
    @SaCheckPermission("ai:modelPrice:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(aiModelPriceService.getById(id));
    }

    @Operation(summary = "新增AI模型价格")
    @SaCheckPermission("ai:modelPrice:add")
    @Log(title = "AI模型价格", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiModelPrice entity) {
        return toAjax(aiModelPriceService.save(entity));
    }

    @Operation(summary = "修改AI模型价格")
    @SaCheckPermission("ai:modelPrice:edit")
    @Log(title = "AI模型价格", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiModelPrice entity) {
        return toAjax(aiModelPriceService.updateById(entity));
    }

    @Operation(summary = "删除AI模型价格")
    @SaCheckPermission("ai:modelPrice:remove")
    @Log(title = "AI模型价格", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiModelPriceService.removeByIds(Arrays.asList(ids)));
    }
}
