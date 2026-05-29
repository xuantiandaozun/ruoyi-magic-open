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

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.bill.domain.BillBudget;
import com.ruoyi.project.bill.service.IBillBudgetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序-预算管理")
@RestController
@RequestMapping("/miniapp/bill/budget")
public class MiniAppBillBudgetController extends BillMiniAppBaseController {

    @Autowired
    private IBillBudgetService billBudgetService;

    @Operation(summary = "查询预算列表")
    @GetMapping("/list")
    public TableDataInfo list(BillBudget billBudget) {
        billBudget.setUserId(getBillUserId());
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billBudget);
        Page<BillBudget> page = billBudgetService.page(
                new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "查询用户预算列表")
    @GetMapping("/user")
    public AjaxResult listByUser(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (userId == null) {
            userId = getBillUserId();
        }
        List<BillBudget> list = billBudgetService.selectByUserIdAndDate(userId, year, month);
        return success(list);
    }

    @Operation(summary = "查询家庭组预算列表")
    @GetMapping("/family")
    public AjaxResult listByFamily(
            @RequestParam Long familyId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        List<BillBudget> list = billBudgetService.selectByFamilyIdAndDate(familyId, year, month);
        return success(list);
    }

    @Operation(summary = "获取预算详细信息")
    @GetMapping("/{budgetId}")
    public AjaxResult getInfo(@PathVariable Long budgetId) {
        return success(billBudgetService.getById(budgetId));
    }

    @Operation(summary = "新增预算")
    @PostMapping
    public AjaxResult add(@RequestBody BillBudget billBudget) {
        billBudget.setUserId(getBillUserId());
        List<BillBudget> existingBudgets = billBudgetService.selectByUserIdAndDate(
                billBudget.getUserId(), billBudget.getBudgetYear(), billBudget.getBudgetMonth());
        if (billBudget.getCategoryId() != null) {
            boolean exists = existingBudgets.stream()
                    .anyMatch(b -> b.getCategoryId() != null && b.getCategoryId().equals(billBudget.getCategoryId()));
            if (exists) {
                return error("该分类的预算已存在");
            }
        } else {
            boolean exists = existingBudgets.stream().anyMatch(b -> b.getCategoryId() == null);
            if (exists) {
                return error("总预算已存在");
            }
        }
        return toAjax(billBudgetService.save(billBudget) ? 1 : 0);
    }

    @Operation(summary = "修改预算")
    @PutMapping
    public AjaxResult edit(@RequestBody BillBudget billBudget) {
        return toAjax(billBudgetService.updateById(billBudget) ? 1 : 0);
    }

    @Operation(summary = "删除预算")
    @DeleteMapping("/{budgetIds}")
    public AjaxResult remove(@PathVariable Long[] budgetIds) {
        return toAjax(billBudgetService.removeByIds(Arrays.asList(budgetIds)) ? budgetIds.length : 0);
    }

    @Operation(summary = "检查预算状态")
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

    @Operation(summary = "获取预算进度")
    @GetMapping("/progress/{budgetId}")
    public AjaxResult progress(@PathVariable Long budgetId) {
        BillBudget budget = billBudgetService.getById(budgetId);
        if (budget == null) {
            return error("预算不存在");
        }
        double progress = budget.getActualAmount()
                .divide(budget.getBudgetAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new java.math.BigDecimal("100"))
                .doubleValue();
        return success()
                .put("budget", budget)
                .put("progress", progress)
                .put("remaining", budget.getBudgetAmount().subtract(budget.getActualAmount()));
    }

    @Operation(summary = "刷新预算实际金额")
    @PutMapping("/refreshActual/{budgetId}")
    public AjaxResult refreshActual(@PathVariable Long budgetId) {
        return toAjax(billBudgetService.updateActualAmount(budgetId) ? 1 : 0);
    }
}
