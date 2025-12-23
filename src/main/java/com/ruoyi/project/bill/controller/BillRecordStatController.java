package com.ruoyi.project.bill.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillRecordService;
import com.ruoyi.project.bill.service.IBillUserProfileService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 账单统计Controller
 * 
 * @author ruoyi
 * @date 2025-12-16
 */
@Tag(name = "账单统计")
@RestController
@RequestMapping("/bill/record/stat")
public class BillRecordStatController extends BaseController {
    @Autowired
    private IBillRecordService billRecordService;

    @Autowired
    private IBillUserProfileService billUserProfileService;

    /**
     * 获取查询范围信息（个人或家庭组）
     * 返回Map包含：isFamilyMode(是否家庭组模式), queryId(查询ID), queryType("user"或"family")
     */
    private Map<String, Object> getQueryScope() {
        Map<String, Object> scope = new HashMap<>();
        Long userId = getUserId();

        // 查询用户的家庭组信息
        BillUserProfile userProfile = billUserProfileService.selectByUserId(userId);

        if (userProfile != null && userProfile.getFamilyId() != null && userProfile.getFamilyId() > 0) {
            // 用户属于家庭组，统计家庭组数据
            scope.put("isFamilyMode", true);
            scope.put("queryId", userProfile.getFamilyId());
            scope.put("queryType", "family");
        } else {
            // 用户不属于家庭组，统计个人数据
            scope.put("isFamilyMode", false);
            scope.put("queryId", userId);
            scope.put("queryType", "user");
        }

        return scope;
    }

    /**
     * 首页概览统计
     */
    @Operation(summary = "首页概览统计")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/overview")
    public AjaxResult overview() {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");

        // 今日统计
        LocalDate today = LocalDate.now();
        Map<String, BigDecimal> todayStats;

        // 本月统计
        LocalDate monthStart = today.withDayOfMonth(1);
        Map<String, BigDecimal> monthStats;

        // 本年统计
        LocalDate yearStart = today.withDayOfYear(1);
        Map<String, BigDecimal> yearStats;

        if (isFamilyMode) {
            // 家庭组模式：统计家庭组数据
            todayStats = billRecordService.selectFamilyStatisticsByDateRange(queryId, today, today);
            monthStats = billRecordService.selectFamilyStatisticsByDateRange(queryId, monthStart, today);
            yearStats = billRecordService.selectFamilyStatisticsByDateRange(queryId, yearStart, today);
        } else {
            // 个人模式：统计个人数据
            todayStats = billRecordService.selectStatisticsByDateRange(queryId, today, today);
            monthStats = billRecordService.selectStatisticsByDateRange(queryId, monthStart, today);
            yearStats = billRecordService.selectStatisticsByDateRange(queryId, yearStart, today);
        }

        Map<String, Object> result = new HashMap<>();

        // 今日数据
        result.put("todayExpense", todayStats.getOrDefault("totalExpense", BigDecimal.ZERO));
        result.put("todayIncome", todayStats.getOrDefault("totalIncome", BigDecimal.ZERO));

        // 本月数据
        result.put("monthExpense", monthStats.getOrDefault("totalExpense", BigDecimal.ZERO));
        result.put("monthIncome", monthStats.getOrDefault("totalIncome", BigDecimal.ZERO));

        // 本年数据
        result.put("yearExpense", yearStats.getOrDefault("totalExpense", BigDecimal.ZERO));
        result.put("yearIncome", yearStats.getOrDefault("totalIncome", BigDecimal.ZERO));

        // 总资产（本年收入 - 本年支出）
        BigDecimal yearIncome = (BigDecimal) result.get("yearIncome");
        BigDecimal yearExpense = (BigDecimal) result.get("yearExpense");
        result.put("totalAssets", yearIncome.subtract(yearExpense));

        return success(result);
    }

    /**
     * 分类统计
     */
    @Operation(summary = "分类统计")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/category")
    public AjaxResult categoryStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String recordType) {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");

        // 设置时间范围
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        LocalDate start = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 调用Service获取分类统计
        java.util.List<Map<String, Object>> statistics;
        if (isFamilyMode) {
            statistics = billRecordService.selectFamilyCategoryStatistics(
                    queryId, recordType, start, end);
        } else {
            statistics = billRecordService.selectCategoryStatistics(
                    queryId, recordType, start, end);
        }

        return success(statistics);
    }

    /**
     * 收支趋势
     */
    @Operation(summary = "收支趋势")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/trend")
    public AjaxResult trendStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");

        // 设置时间范围
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();

        LocalDate start, end;
        String groupBy;

        if (month != null) {
            // 按月查询，按天分组
            start = LocalDate.of(targetYear, month, 1);
            end = start.withDayOfMonth(start.lengthOfMonth());
            groupBy = "day";
        } else {
            // 按年查询，按月分组
            start = LocalDate.of(targetYear, 1, 1);
            end = LocalDate.of(targetYear, 12, 31);
            groupBy = "month";
        }

        // 执行趋势统计SQL
        String sql;
        String queryField = isFamilyMode ? "family_id" : "user_id";

        if ("day".equals(groupBy)) {
            sql = "SELECT " +
                    "DAY(record_date) as day, " +
                    "SUM(CASE WHEN record_type = '0' THEN amount ELSE 0 END) as expense, " +
                    "SUM(CASE WHEN record_type = '1' THEN amount ELSE 0 END) as income " +
                    "FROM bill_record " +
                    "WHERE " + queryField + " = ? " +
                    "AND record_date BETWEEN ? AND ? " +
                    "AND del_flag = '0' " +
                    "GROUP BY DAY(record_date) " +
                    "ORDER BY day";
        } else {
            sql = "SELECT " +
                    "MONTH(record_date) as month, " +
                    "SUM(CASE WHEN record_type = '0' THEN amount ELSE 0 END) as expense, " +
                    "SUM(CASE WHEN record_type = '1' THEN amount ELSE 0 END) as income " +
                    "FROM bill_record " +
                    "WHERE " + queryField + " = ? " +
                    "AND record_date BETWEEN ? AND ? " +
                    "AND del_flag = '0' " +
                    "GROUP BY MONTH(record_date) " +
                    "ORDER BY month";
        }

        // 使用Db执行SQL查询
        java.util.List<com.mybatisflex.core.row.Row> rows = com.mybatisflex.core.row.Db.selectListBySql(sql,
                queryId, start, end);

        // 转换为Map列表
        java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (com.mybatisflex.core.row.Row row : rows) {
            Map<String, Object> map = new java.util.HashMap<>();
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

    /**
     * 成员统计
     */
    @Operation(summary = "成员统计")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/member/{familyId}")
    public AjaxResult memberStats(
            @RequestParam Long familyId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        Map<String, BigDecimal> statistics = billRecordService.selectFamilyStatisticsByDateRange(familyId, start, end);
        return success(statistics);
    }

    /**
     * 账户统计
     */
    @Operation(summary = "账户统计")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/account")
    public AjaxResult accountStats(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            userId = getUserId();
        }

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        Map<String, BigDecimal> statistics = billRecordService.selectStatisticsByDateRange(userId, monthStart, today);
        return success(statistics);
    }

    /**
     * 月度统计
     */
    @Operation(summary = "月度统计")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/month")
    public AjaxResult monthStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");

        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        LocalDate start = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        Map<String, BigDecimal> statistics;
        if (isFamilyMode) {
            statistics = billRecordService.selectFamilyStatisticsByDateRange(queryId, start, end);
        } else {
            statistics = billRecordService.selectStatisticsByDateRange(queryId, start, end);
        }
        return success(statistics);
    }

    /**
     * 年度统计
     */
    @Operation(summary = "年度统计")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/year")
    public AjaxResult yearStats(@RequestParam(required = false) Integer year) {
        Map<String, Object> scope = getQueryScope();
        boolean isFamilyMode = (boolean) scope.get("isFamilyMode");
        Long queryId = (Long) scope.get("queryId");

        int targetYear = year != null ? year : LocalDate.now().getYear();

        LocalDate start = LocalDate.of(targetYear, 1, 1);
        LocalDate end = LocalDate.of(targetYear, 12, 31);

        Map<String, BigDecimal> statistics;
        if (isFamilyMode) {
            statistics = billRecordService.selectFamilyStatisticsByDateRange(queryId, start, end);
        } else {
            statistics = billRecordService.selectStatisticsByDateRange(queryId, start, end);
        }
        return success(statistics);
    }
}
