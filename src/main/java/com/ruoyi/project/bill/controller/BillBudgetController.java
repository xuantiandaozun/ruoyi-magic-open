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
import org.springframework.web.bind.annotation.RequestParam;
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
import com.ruoyi.project.bill.domain.BillBudget;
import com.ruoyi.project.bill.service.IBillBudgetService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 预算管理Controller
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Tag(name = "预算管理")
@RestController
@RequestMapping("/bill/budget")
public class BillBudgetController extends BaseController {

    @Autowired
    private IBillBudgetService billBudgetService;

    /**
     * 查询预算列表
     */
    @Operation(summary = "查询预算列表")
    @SaCheckPermission("bill:budget:list")
    @GetMapping("/list")
    public TableDataInfo list(BillBudget billBudget) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = buildFlexQueryWrapper(billBudget);

        Page<BillBudget> page = billBudgetService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 查询用户预算列表（不分页）
     */
    @Operation(summary = "查询用户预算列表")
    @SaCheckPermission("bill:budget:query")
    @GetMapping("/user/{userId}")
    public AjaxResult listByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        List<BillBudget> list = billBudgetService.selectByUserIdAndDate(userId, year, month);
        return success(list);
    }

    /**
     * 查询家庭组预算列表（不分页）
     */
    @Operation(summary = "查询家庭组预算列表")
    @SaCheckPermission("bill:budget:query")
    @GetMapping("/family/{familyId}")
    public AjaxResult listByFamily(
            @PathVariable Long familyId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        List<BillBudget> list = billBudgetService.selectByFamilyIdAndDate(familyId, year, month);
        return success(list);
    }

    /**
     * 导出预算列表
     */
    @Operation(summary = "导出预算列表")
    @SaCheckPermission("bill:budget:export")
    @Log(title = "预算管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BillBudget billBudget) {
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billBudget);
        List<BillBudget> list = billBudgetService.list(queryWrapper);
        MagicExcelUtil<BillBudget> util = new MagicExcelUtil<>(BillBudget.class);
        util.exportExcel(response, list, "预算数据");
    }

    /**
     * 获取预算详细信息
     */
    @Operation(summary = "获取预算详细信息")
    @SaCheckPermission("bill:budget:query")
    @GetMapping(value = "/{budgetId}")
    public AjaxResult getInfo(@PathVariable Long budgetId) {
        return success(billBudgetService.getById(budgetId));
    }

    /**
     * 新增预算
     */
    @Operation(summary = "新增预算")
    @SaCheckPermission("bill:budget:add")
    @Log(title = "预算管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BillBudget billBudget) {
        // 检查是否已存在相同的预算
        List<BillBudget> existingBudgets = billBudgetService.selectByUserIdAndDate(
                billBudget.getUserId(),
                billBudget.getBudgetYear(),
                billBudget.getBudgetMonth());

        // 检查是否已存在相同分类的预算
        if (billBudget.getCategoryId() != null) {
            boolean exists = existingBudgets.stream()
                    .anyMatch(b -> b.getCategoryId() != null &&
                            b.getCategoryId().equals(billBudget.getCategoryId()));
            if (exists) {
                return error("该分类的预算已存在");
            }
        } else {
            // 检查是否已存在总预算
            boolean exists = existingBudgets.stream()
                    .anyMatch(b -> b.getCategoryId() == null);
            if (exists) {
                return error("总预算已存在");
            }
        }

        return toAjax(billBudgetService.save(billBudget) ? 1 : 0);
    }

    /**
     * 修改预算
     */
    @Operation(summary = "修改预算")
    @SaCheckPermission("bill:budget:edit")
    @Log(title = "预算管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BillBudget billBudget) {
        return toAjax(billBudgetService.updateById(billBudget) ? 1 : 0);
    }

    /**
     * 删除预算
     */
    @Operation(summary = "删除预算")
    @SaCheckPermission("bill:budget:remove")
    @Log(title = "预算管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{budgetIds}")
    public AjaxResult remove(@PathVariable Long[] budgetIds) {
        return toAjax(billBudgetService.removeByIds(Arrays.asList(budgetIds)) ? budgetIds.length : 0);
    }

    /**
     * 检查预算状态（是否超支）
     */
    @Operation(summary = "检查预算状态")
    @SaCheckPermission("bill:budget:query")
    @GetMapping("/checkStatus/{budgetId}")
    public AjaxResult checkStatus(@PathVariable Long budgetId) {
        BillBudget budget = billBudgetService.getById(budgetId);
        if (budget == null) {
            return error("预算不存在");
        }

        String status = billBudgetService.checkBudgetStatus(budgetId);
        budget.setStatus(status);
        billBudgetService.updateById(budget);

        return success(budget);
    }

    /**
     * 批量检查所有预算状态
     */
    @Operation(summary = "批量检查所有预算状态")
    @SaCheckPermission("bill:budget:query")
    @PostMapping("/checkAllStatus")
    public AjaxResult checkAllStatus(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        List<BillBudget> budgets = billBudgetService.selectByUserIdAndDate(userId, year, month);

        int updatedCount = 0;
        for (BillBudget budget : budgets) {
            String newStatus = billBudgetService.checkBudgetStatus(budget.getBudgetId());
            if (!newStatus.equals(budget.getStatus())) {
                budget.setStatus(newStatus);
                if (billBudgetService.updateById(budget)) {
                    updatedCount++;
                }
            }
        }

        return success("已更新" + updatedCount + "个预算状态");
    }

    /**
     * 获取预算执行进度
     */
    @Operation(summary = "获取预算执行进度")
    @SaCheckPermission("bill:budget:query")
    @GetMapping("/progress/{budgetId}")
    public AjaxResult getBudgetProgress(@PathVariable Long budgetId) {
        BillBudget budget = billBudgetService.getById(budgetId);
        if (budget == null) {
            return error("预算不存在");
        }

        // 计算进度百分比
        double progress = budget.getActualAmount()
                .divide(budget.getBudgetAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new java.math.BigDecimal("100"))
                .doubleValue();

        return success()
                .put("budget", budget)
                .put("progress", progress)
                .put("remaining", budget.getBudgetAmount().subtract(budget.getActualAmount()));
    }

    /**
     * 刷新预算实际支出金额（从账单记录重新计算）
     */
    @Operation(summary = "刷新预算实际支出金额")
    @SaCheckPermission("bill:budget:edit")
    @Log(title = "预算管理", businessType = BusinessType.UPDATE)
    @PutMapping("/refreshActual/{budgetId}")
    public AjaxResult refreshActualAmount(@PathVariable Long budgetId) {
        // TODO: 实现从账单记录中统计实际支出
        // 这需要调用BillRecordService来统计对应时间段和分类的支出
        return error("功能待实现");
    }
}
