package com.ruoyi.project.miniapp.controller.bill;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.bill.domain.BillCategory;
import com.ruoyi.project.bill.service.IBillCategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序-账单分类")
@RestController
@RequestMapping("/miniapp/bill/category")
public class MiniAppBillCategoryController extends BillMiniAppBaseController {

    @Autowired
    private IBillCategoryService billCategoryService;

    @Operation(summary = "查询分类树")
    @GetMapping("/tree/{categoryType}")
    public AjaxResult tree(@PathVariable String categoryType) {
        List<BillCategory> list = billCategoryService.selectCategoryTree(categoryType);
        return success(list);
    }

    @Operation(summary = "根据类型获取分类列表")
    @GetMapping("/byType")
    public AjaxResult byType(@RequestParam String categoryType) {
        List<BillCategory> list = billCategoryService.selectCategoryTree(categoryType);
        return success(list);
    }

    @Operation(summary = "查询一级分类列表")
    @GetMapping("/top/{categoryType}")
    public AjaxResult topList(@PathVariable String categoryType) {
        List<BillCategory> list = billCategoryService.selectTopCategoryList(categoryType);
        return success(list);
    }

    @Operation(summary = "获取账单分类详细信息")
    @GetMapping("/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId) {
        return success(billCategoryService.getById(categoryId));
    }

    @Operation(summary = "新增账单分类")
    @PostMapping
    public AjaxResult add(@RequestBody BillCategory billCategory) {
        return toAjax(billCategoryService.save(billCategory) ? 1 : 0);
    }

    @Operation(summary = "修改账单分类")
    @PutMapping
    public AjaxResult edit(@RequestBody BillCategory billCategory) {
        return toAjax(billCategoryService.updateById(billCategory) ? 1 : 0);
    }

    @Operation(summary = "删除账单分类")
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds) {
        return toAjax(billCategoryService.removeByIds(Arrays.asList(categoryIds)) ? categoryIds.length : 0);
    }
}
