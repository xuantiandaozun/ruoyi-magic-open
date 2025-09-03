package com.ruoyi.project.article.controller;

import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.project.article.domain.SocialMediaArticle;
import com.ruoyi.project.article.service.ISocialMediaArticleService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableSupport;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 自媒体文章Controller
 * 
 * @author ruoyi
 * @date 2025-09-02 16:42:31
 */
@Tag(name = "自媒体文章")
@RestController
@RequestMapping("/article/socialArticle")
public class SocialMediaArticleController extends BaseController
{
    @Autowired
    private ISocialMediaArticleService socialMediaArticleService;

    /**
     * 查询自媒体文章列表
     */
    @Operation(summary = "查询自媒体文章列表")
    @SaCheckPermission("article:socialArticle:list")
    @GetMapping("/list")
    public TableDataInfo list(SocialMediaArticle socialMediaArticle)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(socialMediaArticle);
        
        // 使用 MyBatisFlex 的分页方法
        Page<SocialMediaArticle> page = socialMediaArticleService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出自媒体文章列表
     */
    @Operation(summary = "导出自媒体文章列表")
    @SaCheckPermission("article:socialArticle:export")
    @Log(title = "自媒体文章", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SocialMediaArticle socialMediaArticle)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<SocialMediaArticle> list = socialMediaArticleService.list(queryWrapper);
        MagicExcelUtil<SocialMediaArticle> util = new MagicExcelUtil<>(SocialMediaArticle.class);
        util.exportExcel(response, list, "自媒体文章数据");
    }

    /**
     * 获取自媒体文章详细信息
     */
    @Operation(summary = "获取自媒体文章详细信息")
    @SaCheckPermission("article:socialArticle:query")
    @GetMapping(value = "/{articleId}")
    public AjaxResult getInfo(@PathVariable("articleId") String articleId)
    {
        return success(socialMediaArticleService.getById(articleId));
    }

    /**
     * 新增自媒体文章
     */
    @Operation(summary = "新增自媒体文章")
    @SaCheckPermission("article:socialArticle:add")
    @Log(title = "自媒体文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SocialMediaArticle socialMediaArticle)
    {
        return toAjax(socialMediaArticleService.save(socialMediaArticle) ? 1 : 0);
    }

    /**
     * 修改自媒体文章
     */
    @Operation(summary = "修改自媒体文章")
    @SaCheckPermission("article:socialArticle:edit")
    @Log(title = "自媒体文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SocialMediaArticle socialMediaArticle)
    {
        return toAjax(socialMediaArticleService.updateById(socialMediaArticle) ? 1 : 0);
    }

    /**
     * 删除自媒体文章
     */
    @Operation(summary = "删除自媒体文章")
    @SaCheckPermission("article:socialArticle:remove")
    @Log(title = "自媒体文章", businessType = BusinessType.DELETE)
	@DeleteMapping("/{articleIds}")
    public AjaxResult remove(@PathVariable String[] articleIds)
    {
        return toAjax(socialMediaArticleService.removeByIds(Arrays.asList(articleIds)) ? articleIds.length : 0);
    }
}
