package com.ruoyi.project.bill.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
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
                // 使用Db.selectListBySql执行原生SQL查询
                String sql = "SELECT " +
                                "c.category_id AS categoryId, " +
                                "c.category_name AS categoryName, " +
                                "c.icon AS categoryIcon, " +
                                "c.color AS categoryColor, " +
                                "SUM(r.amount) AS amount, " +
                                "COUNT(r.record_id) AS count " +
                                "FROM bill_record r " +
                                "LEFT JOIN bill_category c ON r.category_id = c.category_id " +
                                "WHERE r.user_id = ? " +
                                "AND r.record_type = ? " +
                                "AND r.record_date BETWEEN ? AND ? " +
                                "AND r.del_flag = '0' " +
                                "GROUP BY c.category_id, c.category_name, c.icon, c.color " +
                                "ORDER BY amount DESC";

                // 使用Db执行SQL查询
                List<Row> rows = Db.selectListBySql(sql,
                                userId, recordType, startDate, endDate);

                if (rows == null || rows.isEmpty()) {
                        return new java.util.ArrayList<>();
                }

                // 将Row转换为Map
                List<Map<String, Object>> results = new java.util.ArrayList<>();
                for (com.mybatisflex.core.row.Row row : rows) {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("categoryId", row.get("categoryId"));
                        map.put("categoryName", row.get("categoryName"));
                        map.put("categoryIcon", row.get("categoryIcon"));
                        map.put("categoryColor", row.get("categoryColor"));
                        map.put("amount", row.get("amount"));
                        map.put("count", row.get("count"));
                        results.add(map);
                }

                // 计算总金额
                BigDecimal totalAmount = results.stream()
                                .map(item -> (BigDecimal) item.get("amount"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 计算百分比
                for (Map<String, Object> item : results) {
                        BigDecimal amount = (BigDecimal) item.get("amount");
                        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal percent = amount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                                                .multiply(new BigDecimal("100"));
                                item.put("percent", percent.setScale(2, java.math.RoundingMode.HALF_UP));
                        } else {
                                item.put("percent", BigDecimal.ZERO);
                        }
                }

                return results;
        }

        @Override
        public List<Map<String, Object>> selectFamilyCategoryStatistics(Long familyId, String recordType,
                        LocalDate startDate,
                        LocalDate endDate) {
                // 使用Db.selectListBySql执行原生SQL查询（按家庭组ID查询）
                String sql = "SELECT " +
                                "c.category_id AS categoryId, " +
                                "c.category_name AS categoryName, " +
                                "c.icon AS categoryIcon, " +
                                "c.color AS categoryColor, " +
                                "SUM(r.amount) AS amount, " +
                                "COUNT(r.record_id) AS count " +
                                "FROM bill_record r " +
                                "LEFT JOIN bill_category c ON r.category_id = c.category_id " +
                                "WHERE r.family_id = ? " +
                                "AND r.record_type = ? " +
                                "AND r.record_date BETWEEN ? AND ? " +
                                "AND r.del_flag = '0' " +
                                "GROUP BY c.category_id, c.category_name, c.icon, c.color " +
                                "ORDER BY amount DESC";

                // 使用Db执行SQL查询
                List<Row> rows = Db.selectListBySql(sql,
                                familyId, recordType, startDate, endDate);

                if (rows == null || rows.isEmpty()) {
                        return new java.util.ArrayList<>();
                }

                // 将Row转换为Map
                List<Map<String, Object>> results = new java.util.ArrayList<>();
                for (com.mybatisflex.core.row.Row row : rows) {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("categoryId", row.get("categoryId"));
                        map.put("categoryName", row.get("categoryName"));
                        map.put("categoryIcon", row.get("categoryIcon"));
                        map.put("categoryColor", row.get("categoryColor"));
                        map.put("amount", row.get("amount"));
                        map.put("count", row.get("count"));
                        results.add(map);
                }

                // 计算总金额
                BigDecimal totalAmount = results.stream()
                                .map(item -> (BigDecimal) item.get("amount"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 计算百分比
                for (Map<String, Object> item : results) {
                        BigDecimal amount = (BigDecimal) item.get("amount");
                        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal percent = amount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                                                .multiply(new BigDecimal("100"));
                                item.put("percent", percent.setScale(2, java.math.RoundingMode.HALF_UP));
                        } else {
                                item.put("percent", BigDecimal.ZERO);
                        }
                }

                return results;
        }
}
