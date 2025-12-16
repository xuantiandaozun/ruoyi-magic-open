package com.ruoyi.project.bill.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.bill.domain.BillRecord;

/**
 * 账单记录Service接口
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
public interface IBillRecordService extends IService<BillRecord> {
    /**
     * 查询用户指定日期范围的账单统计
     * 
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计数据（totalIncome: 总收入, totalExpense: 总支出, balance: 结余）
     */
    Map<String, BigDecimal> selectStatisticsByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询家庭组指定日期范围的账单统计
     * 
     * @param familyId  家庭组ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计数据
     */
    Map<String, BigDecimal> selectFamilyStatisticsByDateRange(Long familyId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询分类统计
     * 
     * @param userId     用户ID
     * @param recordType 记录类型（0支出 1收入）
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 分类统计列表
     */
    java.util.List<Map<String, Object>> selectCategoryStatistics(Long userId, String recordType, LocalDate startDate,
            LocalDate endDate);
}
