package com.ruoyi.project.bill.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.bill.domain.BillBudget;
import com.ruoyi.project.bill.mapper.BillBudgetMapper;
import com.ruoyi.project.bill.service.IBillBudgetService;

/**
 * 预算Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Service
public class BillBudgetServiceImpl extends ServiceImpl<BillBudgetMapper, BillBudget> implements IBillBudgetService {
    @Override
    public List<BillBudget> selectBudgetList(Long userId, Integer year, Integer month) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId)
                .eq("budget_year", year)
                .eq("budget_month", month);

        return this.list(queryWrapper);
    }

    @Override
    public boolean updateActualAmount(Long budgetId) {
        // TODO: 实现实际支出金额更新逻辑
        return false;
    }

    @Override
    public String checkAndUpdateBudgetStatus(Long budgetId) {
        BillBudget budget = this.getById(budgetId);
        if (budget == null)
            return null;

        String status = "0"; // 默认正常

        if (budget.getActualAmount().compareTo(budget.getBudgetAmount()) > 0) {
            status = "2"; // 已超支
        } else if (budget.getActualAmount().compareTo(budget.getBudgetAmount()) == 0) {
            status = "1"; // 已完成
        }

        budget.setStatus(status);
        this.updateById(budget);

        return status;
    }

    @Override
    public List<BillBudget> selectByUserIdAndDate(Long userId, Integer year, Integer month) {
        return selectBudgetList(userId, year, month);
    }

    @Override
    public List<BillBudget> selectByFamilyIdAndDate(Long familyId, Integer year, Integer month) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("family_id", familyId);

        if (year != null) {
            queryWrapper.eq("budget_year", year);
        }
        if (month != null) {
            queryWrapper.eq("budget_month", month);
        }

        return this.list(queryWrapper);
    }

    @Override
    public String checkBudgetStatus(Long budgetId) {
        BillBudget budget = this.getById(budgetId);
        if (budget == null) {
            return null;
        }

        if (budget.getActualAmount().compareTo(budget.getBudgetAmount()) > 0) {
            return "2"; // 已超支
        } else if (budget.getActualAmount().compareTo(budget.getBudgetAmount()) == 0) {
            return "1"; // 已完成
        }

        return "0"; // 正常
    }
}
