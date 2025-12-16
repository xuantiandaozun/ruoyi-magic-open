package com.ruoyi.project.bill.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import com.ruoyi.project.bill.domain.BillRecord;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillRecordService;
import com.ruoyi.project.bill.service.IBillUserProfileService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 账单记录Controller
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Tag(name = "账单记录")
@RestController
@RequestMapping("/bill/record")
public class BillRecordController extends BaseController {
    @Autowired
    private IBillRecordService billRecordService;

    @Autowired
    private IBillUserProfileService billUserProfileService;

    /**
     * 查询账单记录列表
     */
    @Operation(summary = "查询账单记录列表")
    @SaCheckPermission("bill:record:list")
    @GetMapping("/list")
    public TableDataInfo list(BillRecord billRecord) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = buildFlexQueryWrapper(billRecord);

        Page<BillRecord> page = billRecordService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 查询用户统计数据
     */
    @Operation(summary = "查询用户统计数据")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/statistics")
    public AjaxResult statistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = getUserId();

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        Map<String, BigDecimal> statistics = billRecordService.selectStatisticsByDateRange(userId, start, end);
        return success(statistics);
    }

    /**
     * 查询家庭组统计数据
     */
    @Operation(summary = "查询家庭组统计数据")
    @SaCheckPermission("bill:record:query")
    @GetMapping("/familyStatistics")
    public AjaxResult familyStatistics(
            @RequestParam Long familyId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        Map<String, BigDecimal> statistics = billRecordService.selectFamilyStatisticsByDateRange(familyId, start, end);
        return success(statistics);
    }

    /**
     * 导出账单记录列表
     */
    @Operation(summary = "导出账单记录列表")
    @SaCheckPermission("bill:record:export")
    @Log(title = "账单记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BillRecord billRecord) {
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billRecord);
        List<BillRecord> list = billRecordService.list(queryWrapper);
        MagicExcelUtil<BillRecord> util = new MagicExcelUtil<>(BillRecord.class);
        util.exportExcel(response, list, "账单记录数据");
    }

    /**
     * 获取账单记录详细信息
     */
    @Operation(summary = "获取账单记录详细信息")
    @SaCheckPermission("bill:record:query")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable Long recordId) {
        return success(billRecordService.getById(recordId));
    }

    /**
     * 新增账单记录
     */
    @Operation(summary = "新增账单记录")
    @SaCheckPermission("bill:record:add")
    @Log(title = "账单记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BillRecord billRecord) {
        // 设置当前登录用户ID
        billRecord.setUserId(getUserId());

        // 设置家庭组ID（如果用户有家庭组）
        BillUserProfile userProfile = billUserProfileService.selectByUserId(getUserId());
        if (userProfile != null && userProfile.getFamilyId() != null) {
            billRecord.setFamilyId(userProfile.getFamilyId());
        } else {
            // 用户没有家庭组，设置为0表示个人记账
            billRecord.setFamilyId(0L);
        }

        return toAjax(billRecordService.save(billRecord) ? 1 : 0);
    }

    /**
     * 修改账单记录
     */
    @Operation(summary = "修改账单记录")
    @SaCheckPermission("bill:record:edit")
    @Log(title = "账单记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BillRecord billRecord) {
        return toAjax(billRecordService.updateById(billRecord) ? 1 : 0);
    }

    /**
     * 删除账单记录
     */
    @Operation(summary = "删除账单记录")
    @SaCheckPermission("bill:record:remove")
    @Log(title = "账单记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds) {
        return toAjax(billRecordService.removeByIds(Arrays.asList(recordIds)) ? recordIds.length : 0);
    }
}
