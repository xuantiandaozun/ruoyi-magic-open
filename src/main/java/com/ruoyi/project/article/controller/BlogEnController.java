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
import com.ruoyi.project.article.domain.BlogEn;
import com.ruoyi.project.article.service.IBlogEnService;
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
 * 英文博客Controller
 * 
 * @author ruoyi
 * @date 2025-08-26 15:05:51
 */
@Tag(name = "英文博客")
@RestController
@RequestMapping("/article/enBlog")
public class BlogEnController extends BaseController
{
    @Autowired
    private IBlogEnService blogEnService;

    /**
     * 查询英文博客列表
     */
    @Operation(summary = "查询英文博客列表")
    @SaCheckPermission("article:enBlog:list")
    @GetMapping("/list")
    public TableDataInfo list(BlogEn blogEn)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(blogEn);
        
        // 使用 MyBatisFlex 的分页方法
        Page<BlogEn> page = blogEnService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出英文博客列表
     */
    @Operation(summary = "导出英文博客列表")
    @SaCheckPermission("article:enBlog:export")
    @Log(title = "英文博客", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BlogEn blogEn)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<BlogEn> list = blogEnService.list(queryWrapper);
        MagicExcelUtil<BlogEn> util = new MagicExcelUtil<>(BlogEn.class);
        util.exportExcel(response, list, "英文博客数据");
    }

    /**
     * 获取英文博客详细信息
     */
    @Operation(summary = "获取英文博客详细信息")
    @SaCheckPermission("article:enBlog:query")
    @GetMapping(value = "/{blogId}")
    public AjaxResult getInfo(@PathVariable("blogId") String blogId)
    {
        return success(blogEnService.getById(blogId));
    }

    /**
     * 新增英文博客
     */
    @Operation(summary = "新增英文博客")
    @SaCheckPermission("article:enBlog:add")
    @Log(title = "英文博客", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogEn blogEn)
    {
        return toAjax(blogEnService.save(blogEn) ? 1 : 0);
    }

    /**
     * 修改英文博客
     */
    @Operation(summary = "修改英文博客")
    @SaCheckPermission("article:enBlog:edit")
    @Log(title = "英文博客", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogEn blogEn)
    {
        return toAjax(blogEnService.updateById(blogEn) ? 1 : 0);
    }

    /**
     * 删除英文博客
     */
    @Operation(summary = "删除英文博客")
    @SaCheckPermission("article:enBlog:remove")
    @Log(title = "英文博客", businessType = BusinessType.DELETE)
	@DeleteMapping("/{blogIds}")
    public AjaxResult remove(@PathVariable String[] blogIds)
    {
        return toAjax(blogEnService.removeByIds(Arrays.asList(blogIds)) ? blogIds.length : 0);
    }
}
