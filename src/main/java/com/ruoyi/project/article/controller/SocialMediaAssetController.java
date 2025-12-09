package com.ruoyi.project.article.controller;

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
import com.ruoyi.framework.aspectj.lang.annotation.Anonymous;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.article.domain.SocialMediaAsset;
import com.ruoyi.project.article.service.ISocialMediaAssetService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 自媒体素材Controller
 * 
 * @author ruoyi
 * @date 2025-12-09
 */
@Tag(name = "自媒体素材")
@RestController
@RequestMapping("/article/social-media-asset")
public class SocialMediaAssetController extends BaseController
{
    @Autowired
    private ISocialMediaAssetService socialMediaAssetService;

    /**
     * 查询自媒体素材列表
     */
    @Operation(summary = "查询自媒体素材列表")
    @SaCheckPermission("article:social-media-asset:list")
    @GetMapping("/list")
    public TableDataInfo list(SocialMediaAsset socialMediaAsset)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(socialMediaAsset);
        
        // 使用 MyBatisFlex 的分页方法
        Page<SocialMediaAsset> page = socialMediaAssetService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出自媒体素材列表
     */
    @Operation(summary = "导出自媒体素材列表")
    @SaCheckPermission("article:social-media-asset:export")
    @Log(title = "自媒体素材", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SocialMediaAsset socialMediaAsset)
    {
        QueryWrapper queryWrapper = buildFlexQueryWrapper(socialMediaAsset);
        
        List<SocialMediaAsset> list = socialMediaAssetService.list(queryWrapper);
        MagicExcelUtil<SocialMediaAsset> util = new MagicExcelUtil<>(SocialMediaAsset.class);
        util.exportExcel(response, list, "自媒体素材数据");
    }

    /**
     * 获取自媒体素材详情
     */
    @Operation(summary = "获取自媒体素材详情")
    @SaCheckPermission("article:social-media-asset:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        SocialMediaAsset socialMediaAsset = socialMediaAssetService.getById(id);
        return success(socialMediaAsset);
    }

    /**
     * 新增自媒体素材
     */
    @Operation(summary = "新增自媒体素材")
    @SaCheckPermission("article:social-media-asset:add")
    @Log(title = "自媒体素材", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SocialMediaAsset socialMediaAsset)
    {
        boolean result = socialMediaAssetService.save(socialMediaAsset);
        return result ? success() : error("新增失败");
    }

    /**
     * 修改自媒体素材
     */
    @Operation(summary = "修改自媒体素材")
    @SaCheckPermission("article:social-media-asset:edit")
    @Log(title = "自媒体素材", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SocialMediaAsset socialMediaAsset)
    {
        boolean result = socialMediaAssetService.updateById(socialMediaAsset);
        return result ? success() : error("修改失败");
    }

    /**
     * 删除自媒体素材
     */
    @Operation(summary = "删除自媒体素材")
    @SaCheckPermission("article:social-media-asset:remove")
    @Log(title = "自媒体素材", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        boolean result = socialMediaAssetService.removeByIds(Arrays.asList(ids));
        return result ? success() : error("删除失败");
    }

    /**
     * 修改素材状态
     */
    @Operation(summary = "修改素材状态")
    @SaCheckPermission("article:social-media-asset:edit")
    @Log(title = "自媒体素材", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult updateStatus(@PathVariable("id") Long id, @RequestBody SocialMediaAsset socialMediaAsset)
    {
        socialMediaAsset.setId(id);
        boolean result = socialMediaAssetService.updateById(socialMediaAsset);
        return result ? success() : error("状态修改失败");
    }

    /**
     * 更新素材指标快照
     */
    @Operation(summary = "更新素材指标快照")
    @SaCheckPermission("article:social-media-asset:edit")
    @Log(title = "自媒体素材", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/metrics")
    public AjaxResult updateMetrics(@PathVariable("id") Long id, @RequestBody SocialMediaAsset socialMediaAsset)
    {
        socialMediaAsset.setId(id);
        boolean result = socialMediaAssetService.updateById(socialMediaAsset);
        return result ? success() : error("指标更新失败");
    }

    /**
     * 爬虫/脚本上传素材
     */
    @Operation(summary = "爬虫/脚本上传素材")
    @Anonymous
    @Log(title = "自媒体素材", businessType = BusinessType.INSERT)
    @PostMapping("/upload/script")
    public AjaxResult uploadByScript(@RequestBody SocialMediaAsset socialMediaAsset)
    {
        if (socialMediaAsset == null) {
            return error("素材数据不能为空");
        }
        
        if (cn.hutool.core.util.StrUtil.isBlank(socialMediaAsset.getPlatform())) {
            return error("平台不能为空");
        }
        
        try {
            socialMediaAsset.setCaptureMethod("spider");
            socialMediaAsset.setCaptureTime(new java.time.LocalDateTime[]{java.time.LocalDateTime.now()}[0]);
            boolean result = socialMediaAssetService.save(socialMediaAsset);
            
            if (result) {
                return success("素材上传成功", socialMediaAsset);
            } else {
                return error("素材上传失败");
            }
        } catch (Exception e) {
            return error("素材上传异常: " + e.getMessage());
        }
    }
}
