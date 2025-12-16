package com.ruoyi.project.bill.controller;

import java.util.Arrays;
import java.util.List;

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
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.bill.domain.BillCategory;
import com.ruoyi.project.bill.service.IBillCategoryService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 账单分类Controller
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Tag(name = "账单分类")
@RestController
@RequestMapping("/bill/category")
public class BillCategoryController extends BaseController {
    @Autowired
    private IBillCategoryService billCategoryService;

    /**
     * 查询账单分类列表
     */
    @Operation(summary = "查询账单分类列表")
    @SaCheckPermission("bill:category:list")
    @GetMapping("/list")
    public TableDataInfo list(BillCategory billCategory) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = buildFlexQueryWrapper(billCategory);

        Page<BillCategory> page = billCategoryService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 查询分类树（不分页）
     */
    @Operation(summary = "查询分类树")
    @SaCheckPermission("bill:category:list")
    @GetMapping("/tree/{categoryType}")
    public AjaxResult tree(@PathVariable String categoryType) {
        List<BillCategory> list = billCategoryService.selectCategoryTree(categoryType);
        return success(list);
    }

    /**
     * 查询一级分类列表
     */
    @Operation(summary = "查询一级分类列表")
    @SaCheckPermission("bill:category:list")
    @GetMapping("/top/{categoryType}")
    public AjaxResult topList(@PathVariable String categoryType) {
        List<BillCategory> list = billCategoryService.selectTopCategoryList(categoryType);
        return success(list);
    }

    /**
     * 导出账单分类列表
     */
    @Operation(summary = "导出账单分类列表")
    @SaCheckPermission("bill:category:export")
    @Log(title = "账单分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BillCategory billCategory) {
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billCategory);
        List<BillCategory> list = billCategoryService.list(queryWrapper);
        MagicExcelUtil<BillCategory> util = new MagicExcelUtil<>(BillCategory.class);
        util.exportExcel(response, list, "账单分类数据");
    }

    /**
     * 获取账单分类详细信息
     */
    @Operation(summary = "获取账单分类详细信息")
    @SaCheckPermission("bill:category:query")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId) {
        return success(billCategoryService.getById(categoryId));
    }

    /**
     * 新增账单分类
     */
    @Operation(summary = "新增账单分类")
    @SaCheckPermission("bill:category:add")
    @Log(title = "账单分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BillCategory billCategory) {
        return toAjax(billCategoryService.save(billCategory) ? 1 : 0);
    }

    /**
     * 修改账单分类
     */
    @Operation(summary = "修改账单分类")
    @SaCheckPermission("bill:category:edit")
    @Log(title = "账单分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BillCategory billCategory) {
        return toAjax(billCategoryService.updateById(billCategory) ? 1 : 0);
    }

    /**
     * 删除账单分类
     */
    @Operation(summary = "删除账单分类")
    @SaCheckPermission("bill:category:remove")
    @Log(title = "账单分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds) {
        return toAjax(billCategoryService.removeByIds(Arrays.asList(categoryIds)) ? categoryIds.length : 0);
    }
}
