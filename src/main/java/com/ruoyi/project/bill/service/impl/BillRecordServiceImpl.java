package com.ruoyi.project.bill.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.bill.domain.BillRecord;
import com.ruoyi.project.bill.mapper.BillRecordMapper;
import com.ruoyi.project.bill.service.IBillRecordService;

/**
 * 账单记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Service
public class BillRecordServiceImpl extends ServiceImpl<BillRecordMapper, BillRecord> implements IBillRecordService {
        @Override
        public Map<String, BigDecimal> selectStatisticsByDateRange(Long userId, LocalDate startDate,
                        LocalDate endDate) {
                Map<String, BigDecimal> result = new HashMap<>();

                // 查询收入
                QueryWrapper incomeQuery = QueryWrapper.create()
                                .select("SUM(amount) as total")
                                .from("bill_record")
                                .eq("user_id", userId)
                                .eq("record_type", "1")
                                .where("record_date BETWEEN ? AND ?", startDate, endDate);

                BigDecimal totalIncome = getMapper().selectObjectByQueryAs(incomeQuery, BigDecimal.class);

                // 查询支出
                QueryWrapper expenseQuery = QueryWrapper.create()
                                .select("SUM(amount) as total")
                                .from("bill_record")
                                .eq("user_id", userId)
                                .eq("record_type", "0")
                                .where("record_date BETWEEN ? AND ?", startDate, endDate);

                BigDecimal totalExpense = getMapper().selectObjectByQueryAs(expenseQuery, BigDecimal.class);

                result.put("totalIncome", totalIncome != null ? totalIncome : BigDecimal.ZERO);
                result.put("totalExpense", totalExpense != null ? totalExpense : BigDecimal.ZERO);
                result.put("balance", result.get("totalIncome").subtract(result.get("totalExpense")));

                return result;
        }

        @Override
        public Map<String, BigDecimal> selectFamilyStatisticsByDateRange(Long familyId, LocalDate startDate,
                        LocalDate endDate) {
                Map<String, BigDecimal> result = new HashMap<>();

                // 查询收入
                QueryWrapper incomeQuery = QueryWrapper.create()
                                .select("SUM(amount) as total")
                                .from("bill_record")
                                .eq("family_id", familyId)
                                .eq("record_type", "1")
                                .where("record_date BETWEEN ? AND ?", startDate, endDate);

                BigDecimal totalIncome = getMapper().selectObjectByQueryAs(incomeQuery, BigDecimal.class);

                // 查询支出
                QueryWrapper expenseQuery = QueryWrapper.create()
                                .select("SUM(amount) as total")
                                .from("bill_record")
                                .eq("family_id", familyId)
                                .eq("record_type", "0")
                                .where("record_date BETWEEN ? AND ?", startDate, endDate);

                BigDecimal totalExpense = getMapper().selectObjectByQueryAs(expenseQuery, BigDecimal.class);

                result.put("totalIncome", totalIncome != null ? totalIncome : BigDecimal.ZERO);
                result.put("totalExpense", totalExpense != null ? totalExpense : BigDecimal.ZERO);
                result.put("balance", result.get("totalIncome").subtract(result.get("totalExpense")));

                return result;
        }

        @Override
        public List<Map<String, Object>> selectCategoryStatistics(Long userId, String recordType, LocalDate startDate,
                        LocalDate endDate) {
                // TODO: 实现分类统计逻辑，需要关联分类表
                return null;
        }
}
