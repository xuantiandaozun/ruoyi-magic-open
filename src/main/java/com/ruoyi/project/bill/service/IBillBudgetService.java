package com.ruoyi.project.bill.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.bill.domain.BillBudget;

/**
 * 预算Service接口
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
public interface IBillBudgetService extends IService<BillBudget> {
    /**
     * 查询用户指定年月的预算列表
     * 
     * @param userId 用户ID
     * @param year   年份
     * @param month  月份
     * @return 预算列表
     */
    List<BillBudget> selectBudgetList(Long userId, Integer year, Integer month);

    /**
     * 查询用户指定年月的预算列表（别名方法）
     * 
     * @param userId 用户ID
     * @param year   年份
     * @param month  月份
     * @return 预算列表
     */
    List<BillBudget> selectByUserIdAndDate(Long userId, Integer year, Integer month);

    /**
     * 查询家庭组指定年月的预算列表
     * 
     * @param familyId 家庭组ID
     * @param year     年份
     * @param month    月份
     * @return 预算列表
     */
    List<BillBudget> selectByFamilyIdAndDate(Long familyId, Integer year, Integer month);

    /**
     * 更新预算的实际支出金额
     * 
     * @param budgetId 预算ID
     * @return 是否成功
     */
    boolean updateActualAmount(Long budgetId);

    /**
     * 检查预算是否超支并更新状态
     * 
     * @param budgetId 预算ID
     * @return 预算状态（0正常 1已完成 2已超支）
     */
    String checkAndUpdateBudgetStatus(Long budgetId);

    /**
     * 检查预算状态（不更新）
     * 
     * @param budgetId 预算ID
     * @return 预算状态（0正常 1已完成 2已超支）
     */
    String checkBudgetStatus(Long budgetId);
}
