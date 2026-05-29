package com.ruoyi.project.miniapp.controller.bill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.bill.domain.BillRecord;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillRecordService;
import com.ruoyi.project.miniapp.domain.dto.BillRecordAnalyzeRequest;
import com.ruoyi.project.miniapp.service.impl.MiniAppBillRecordAiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "小程序-账单记录")
@RestController
@RequestMapping("/miniapp/bill/record")
public class MiniAppBillRecordController extends BillMiniAppBaseController {

    @Autowired
    private IBillRecordService billRecordService;

    @Autowired
    private MiniAppBillRecordAiService billRecordAiService;

    @Operation(summary = "查询账单记录列表")
    @GetMapping("/list")
    public TableDataInfo list(BillRecord billRecord) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Long userId = getBillUserId();
        BillUserProfile userProfile = billUserProfileService.selectByMiniUserId(userId);
        if (userProfile == null) {
            userProfile = requireBillProfile();
        }

        QueryWrapper queryWrapper;
        if (userProfile.getFamilyId() != null && userProfile.getFamilyId() > 0) {
            Long familyId = userProfile.getFamilyId();
            billRecord.setFamilyId(null);
            billRecord.setUserId(null);
            queryWrapper = buildFlexQueryWrapper(billRecord);
            queryWrapper.and("(family_id = ? OR (user_id = ? AND (family_id = 0 OR family_id IS NULL)))",
                    familyId, userId);
        } else {
            billRecord.setUserId(userId);
            billRecord.setFamilyId(0L);
            queryWrapper = buildFlexQueryWrapper(billRecord);
        }
        Page<BillRecord> page = billRecordService.page(
                new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "查询用户统计数据")
    @GetMapping("/statistics")
    public AjaxResult statistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = getBillUserId();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        Map<String, BigDecimal> statistics = billRecordService.selectStatisticsByDateRange(userId, start, end);
        return success(statistics);
    }

    @Operation(summary = "获取账单记录详细信息")
    @GetMapping("/{recordId}")
    public AjaxResult getInfo(@PathVariable Long recordId) {
        return success(billRecordService.getById(recordId));
    }

    @Operation(summary = "AI 解析记账文本")
    @PostMapping("/analyze")
    public AjaxResult analyze(@Valid @RequestBody BillRecordAnalyzeRequest request) {
        return success(billRecordAiService.analyze(getBillUserId(), request));
    }

    @Operation(summary = "AI 识别记账图片")
    @PostMapping("/analyze-image")
    public AjaxResult analyzeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "remark", required = false) String remark) {
        return success(billRecordAiService.analyzeImage(getBillUserId(), file, remark));
    }

    @Operation(summary = "新增账单记录")
    @PostMapping
    public AjaxResult add(@RequestBody BillRecord billRecord) {
        Long userId = getBillUserId();
        billRecord.setUserId(userId);

        BillUserProfile userProfile = requireBillProfile();
        if (userProfile.getFamilyId() != null && userProfile.getFamilyId() > 0) {
            billRecord.setFamilyId(userProfile.getFamilyId());
        } else {
            billRecord.setFamilyId(0L);
        }

        return toAjax(billRecordService.save(billRecord) ? 1 : 0);
    }

    @Operation(summary = "修改账单记录")
    @PutMapping
    public AjaxResult edit(@RequestBody BillRecord billRecord) {
        return toAjax(billRecordService.updateById(billRecord) ? 1 : 0);
    }

    @Operation(summary = "删除账单记录")
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds) {
        return toAjax(billRecordService.removeByIds(Arrays.asList(recordIds)) ? recordIds.length : 0);
    }
}
