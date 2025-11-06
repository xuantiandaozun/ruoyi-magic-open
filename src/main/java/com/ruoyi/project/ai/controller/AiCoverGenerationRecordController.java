package com.ruoyi.project.ai.controller;

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
import com.ruoyi.project.ai.domain.AiCoverGenerationRecord;
import com.ruoyi.project.ai.service.IAiCoverGenerationRecordService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AI生图记录Controller
 * 
 * @author ruoyi
 */
@Tag(name = "AI生图记录")
@RestController
@RequestMapping("/ai/coverGenerationRecord")
public class AiCoverGenerationRecordController extends BaseController {
    @Autowired
    private IAiCoverGenerationRecordService aiCoverGenerationRecordService;

    /**
     * 查询AI生图记录列表
     */
    @Operation(summary = "查询AI生图记录列表")
    @SaCheckPermission("ai:coverGenerationRecord:list")
    @GetMapping("/list")
    public TableDataInfo list(AiCoverGenerationRecord aiCoverGenerationRecord) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        // 使用连表查询获取博客名称
        QueryWrapper qw = buildFlexQueryWrapper(aiCoverGenerationRecord);
        qw.select("u.*", "b.title as blogName")
          .from("ai_cover_generation_record").as("u")
          .leftJoin("blog").as("b").on("u.blog_id = b.blog_id")
          .where("u.del_flag = 0");
        
        Page<AiCoverGenerationRecord> page = aiCoverGenerationRecordService.page(new Page<>(pageNum, pageSize), qw);
        return getDataTable(page);
    }

    /**
     * 导出AI生图记录列表
     */
    @Operation(summary = "导出AI生图记录列表")
    @SaCheckPermission("ai:coverGenerationRecord:export")
    @Log(title = "AI生图记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiCoverGenerationRecord aiCoverGenerationRecord) {
        QueryWrapper qw = buildFlexQueryWrapper(aiCoverGenerationRecord);
        List<AiCoverGenerationRecord> list = aiCoverGenerationRecordService.list(qw);
        MagicExcelUtil<AiCoverGenerationRecord> util = new MagicExcelUtil<>(AiCoverGenerationRecord.class);
        util.exportExcel(response, list, "AI生图记录");
    }

    /**
     * 获取AI生图记录详细信息
     */
    @Operation(summary = "获取AI生图记录详细信息")
    @SaCheckPermission("ai:coverGenerationRecord:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(aiCoverGenerationRecordService.getById(id));
    }

    /**
     * 新增AI生图记录
     */
    @Operation(summary = "新增AI生图记录")
    @SaCheckPermission("ai:coverGenerationRecord:add")
    @Log(title = "AI生图记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiCoverGenerationRecord aiCoverGenerationRecord) {
        return toAjax(aiCoverGenerationRecordService.save(aiCoverGenerationRecord));
    }

    /**
     * 修改AI生图记录
     */
    @Operation(summary = "修改AI生图记录")
    @SaCheckPermission("ai:coverGenerationRecord:edit")
    @Log(title = "AI生图记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiCoverGenerationRecord aiCoverGenerationRecord) {
        return toAjax(aiCoverGenerationRecordService.updateById(aiCoverGenerationRecord));
    }

    /**
     * 删除AI生图记录
     */
    @Operation(summary = "删除AI生图记录")
    @SaCheckPermission("ai:coverGenerationRecord:remove")
    @Log(title = "AI生图记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiCoverGenerationRecordService.removeByIds(Arrays.asList(ids)));
    }

    /**
     * 根据博客ID查询生成记录
     */
    @Operation(summary = "根据博客ID查询生成记录")
    @SaCheckPermission("ai:coverGenerationRecord:list")
    @GetMapping("/listByBlogId/{blogId}")
    public AjaxResult listByBlogId(@PathVariable("blogId") Long blogId) {
        return success(aiCoverGenerationRecordService.listByBlogId(blogId));
    }

    /**
     * 根据分类查询记录
     */
    @Operation(summary = "根据分类查询记录")
    @SaCheckPermission("ai:coverGenerationRecord:list")
    @GetMapping("/listByCategory/{category}")
    public AjaxResult listByCategory(@PathVariable("category") String category) {
        return success(aiCoverGenerationRecordService.listByCategory(category));
    }

    /**
     * 查询失败的生成记录
     */
    @Operation(summary = "查询失败的生成记录")
    @SaCheckPermission("ai:coverGenerationRecord:list")
    @GetMapping("/listFailedRecords")
    public AjaxResult listFailedRecords() {
        return success(aiCoverGenerationRecordService.listFailedRecords());
    }

    /**
     * 查询可复用的通用封面
     */
    @Operation(summary = "查询可复用的通用封面")
    @SaCheckPermission("ai:coverGenerationRecord:list")
    @GetMapping("/listReusableGenericCovers/{category}")
    public AjaxResult listReusableGenericCovers(@PathVariable("category") String category) {
        return success(aiCoverGenerationRecordService.listReusableGenericCovers(category));
    }

    /**
     * 查询成功但未被使用的记录
     */
    @Operation(summary = "查询成功但未被使用的记录")
    @SaCheckPermission("ai:coverGenerationRecord:list")
    @GetMapping("/listUnusedRecords")
    public AjaxResult listUnusedRecords() {
        return success(aiCoverGenerationRecordService.listUnusedRecords());
    }
}