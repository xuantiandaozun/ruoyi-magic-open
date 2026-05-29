package com.ruoyi.project.miniapp.controller.bill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.bill.service.IBillRecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序-账单统计")
@RestController
@RequestMapping("/miniapp/bill/record/stat")
public class MiniAppBillRecordStatController extends BillMiniAppBaseController {

    @Autowired
    private IBillRecordService billRecordService;

    @Operation(summary = "首页概览统计")
    @GetMapping("/overview")
    public AjaxResult overview() {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");
        Long userId = (Long) scope.get("userId");

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        Map<String, BigDecimal> todayStats;
        Map<String, BigDecimal> monthStats;
        Map<String, BigDecimal> yearStats;

        if (isFamilyMode) {
            todayStats = billRecordService.selectFamilyStatisticsByDateRange(queryId, userId, today, today);
            monthStats = billRecordService.selectFamilyStatisticsByDateRange(queryId, userId, monthStart, today);
            yearStats = billRecordService.selectFamilyStatisticsByDateRange(queryId, userId, yearStart, today);
        } else {
            todayStats = billRecordService.selectStatisticsByDateRange(queryId, today, today);
            monthStats = billRecordService.selectStatisticsByDateRange(queryId, monthStart, today);
            yearStats = billRecordService.selectStatisticsByDateRange(queryId, yearStart, today);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("todayExpense", todayStats.getOrDefault("totalExpense", BigDecimal.ZERO));
        result.put("todayIncome", todayStats.getOrDefault("totalIncome", BigDecimal.ZERO));
        result.put("monthExpense", monthStats.getOrDefault("totalExpense", BigDecimal.ZERO));
        result.put("monthIncome", monthStats.getOrDefault("totalIncome", BigDecimal.ZERO));
        result.put("yearExpense", yearStats.getOrDefault("totalExpense", BigDecimal.ZERO));
        result.put("yearIncome", yearStats.getOrDefault("totalIncome", BigDecimal.ZERO));

        BigDecimal yearIncome = (BigDecimal) result.get("yearIncome");
        BigDecimal yearExpense = (BigDecimal) result.get("yearExpense");
        result.put("totalAssets", yearIncome.subtract(yearExpense));
        return success(result);
    }

    @Operation(summary = "分类统计")
    @GetMapping("/category")
    public AjaxResult categoryStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String recordType) {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");
        Long userId = (Long) scope.get("userId");

        LocalDate now = LocalDate.now();
        LocalDate start;
        LocalDate end;

        if (startDate != null && endDate != null) {
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(endDate);
        } else if (month != null) {
            int targetYear = year != null ? year : now.getYear();
            start = LocalDate.of(targetYear, month, 1);
            end = start.withDayOfMonth(start.lengthOfMonth());
        } else if (year != null) {
            start = LocalDate.of(year, 1, 1);
            end = LocalDate.of(year, 12, 31);
        } else {
            start = now.withDayOfMonth(1);
            end = start.withDayOfMonth(start.lengthOfMonth());
        }

        List<Map<String, Object>> statistics;
        if (isFamilyMode) {
            statistics = billRecordService.selectFamilyCategoryStatistics(queryId, userId, recordType, start, end);
        } else {
            statistics = billRecordService.selectCategoryStatistics(queryId, recordType, start, end);
        }
        return success(statistics);
    }

    @Operation(summary = "收支趋势")
    @GetMapping("/trend")
    public AjaxResult trendStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");
        Long userId = (Long) scope.get("userId");

        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        LocalDate start;
        LocalDate end;
        String groupBy;

        if (month != null) {
            start = LocalDate.of(targetYear, month, 1);
            end = start.withDayOfMonth(start.lengthOfMonth());
            groupBy = "day";
        } else {
            start = LocalDate.of(targetYear, 1, 1);
            end = LocalDate.of(targetYear, 12, 31);
            groupBy = "month";
        }

        String scopeSql = isFamilyMode
                ? "(family_id = ? OR (user_id = ? AND (family_id = 0 OR family_id IS NULL)))"
                : "user_id = ?";
        String sql;
        if ("day".equals(groupBy)) {
            sql = "SELECT DAY(record_date) as day, "
                    + "SUM(CASE WHEN record_type = '0' THEN amount ELSE 0 END) as expense, "
                    + "SUM(CASE WHEN record_type = '1' THEN amount ELSE 0 END) as income "
                    + "FROM bill_record WHERE " + scopeSql + " AND record_date BETWEEN ? AND ? "
                    + "AND del_flag = '0' GROUP BY DAY(record_date) ORDER BY day";
        } else {
            sql = "SELECT MONTH(record_date) as month, "
                    + "SUM(CASE WHEN record_type = '0' THEN amount ELSE 0 END) as expense, "
                    + "SUM(CASE WHEN record_type = '1' THEN amount ELSE 0 END) as income "
                    + "FROM bill_record WHERE " + scopeSql + " AND record_date BETWEEN ? AND ? "
                    + "AND del_flag = '0' GROUP BY MONTH(record_date) ORDER BY month";
        }

        List<Row> rows = isFamilyMode
                ? Db.selectListBySql(sql, queryId, userId, start, end)
                : Db.selectListBySql(sql, queryId, start, end);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Row row : rows) {
            Map<String, Object> map = new HashMap<>();
            if ("day".equals(groupBy)) {
                map.put("day", row.get("day"));
            } else {
                map.put("month", row.get("month"));
            }
            map.put("expense", row.get("expense"));
            map.put("income", row.get("income"));
            results.add(map);
        }
        return success(results);
    }

    @Operation(summary = "成员统计")
    @GetMapping("/member/{familyId}")
    public AjaxResult memberStats(
            @PathVariable Long familyId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        Map<String, BigDecimal> statistics = billRecordService.selectFamilyStatisticsByDateRange(familyId, start, end);
        return success(statistics);
    }

    @Operation(summary = "账户统计")
    @GetMapping("/account")
    public AjaxResult accountStats(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            userId = getBillUserId();
        }
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        Map<String, BigDecimal> statistics = billRecordService.selectStatisticsByDateRange(userId, monthStart, today);
        return success(statistics);
    }

    @Operation(summary = "月度统计")
    @GetMapping("/month")
    public AjaxResult monthStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");
        Long userId = (Long) scope.get("userId");

        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();
        LocalDate start = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        Map<String, BigDecimal> statistics;
        if (isFamilyMode) {
            statistics = billRecordService.selectFamilyStatisticsByDateRange(queryId, userId, start, end);
        } else {
            statistics = billRecordService.selectStatisticsByDateRange(queryId, start, end);
        }
        return success(statistics);
    }

    @Operation(summary = "年度统计")
    @GetMapping("/year")
    public AjaxResult yearStats(@RequestParam(required = false) Integer year) {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");
        Long userId = (Long) scope.get("userId");

        int targetYear = year != null ? year : LocalDate.now().getYear();
        LocalDate start = LocalDate.of(targetYear, 1, 1);
        LocalDate end = LocalDate.of(targetYear, 12, 31);

        Map<String, BigDecimal> statistics;
        if (isFamilyMode) {
            statistics = billRecordService.selectFamilyStatisticsByDateRange(queryId, userId, start, end);
        } else {
            statistics = billRecordService.selectStatisticsByDateRange(queryId, start, end);
        }
        return success(statistics);
    }
}
