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
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.article.domain.BlogComment;
import com.ruoyi.project.article.service.IBlogCommentService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 博客评论Controller
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Tag(name = "博客评论")
@RestController
@RequestMapping("/article/comment")
public class BlogCommentController extends BaseController
{
    @Autowired
    private IBlogCommentService blogCommentService;

    /**
     * 查询博客评论列表
     */
    @Operation(summary = "查询博客评论列表")
    @SaCheckPermission("article:comment:list")
    @GetMapping("/list")
    public TableDataInfo list(BlogComment blogComment)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper，包含左连接查询博客标题
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("c.*", "b.title as blogTitle")
            .from("blog_comment").as("c")
            .leftJoin("blog").as("b").on("c.blog_id = b.blog_id");
        
        // 添加查询条件
        if (blogComment.getBlogId() != null) {
            queryWrapper.and("c.blog_id = {0}", blogComment.getBlogId());
        }
        if (StrUtil.isNotBlank(blogComment.getNickname())) {
            queryWrapper.and("c.nickname like {0}", "%" + blogComment.getNickname() + "%");
        }
        if (StrUtil.isNotBlank(blogComment.getStatus())) {
            queryWrapper.and("c.status = {0}", blogComment.getStatus());
        }
        if (StrUtil.isNotBlank(blogComment.getContent())) {
            queryWrapper.and("c.content like {0}", "%" + blogComment.getContent() + "%");
        }
        
        queryWrapper.and("c.del_flag = '0'");
        queryWrapper.orderBy("c.create_time", false);
        
        // 使用 MyBatisFlex 的分页方法
        Page<BlogComment> page = blogCommentService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出博客评论列表
     */
    @Operation(summary = "导出博客评论列表")
    @SaCheckPermission("article:comment:export")
    @Log(title = "博客评论", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BlogComment blogComment)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("c.*", "b.title as blogTitle")
            .from("blog_comment").as("c")
            .leftJoin("blog").as("b").on("c.blog_id = b.blog_id")
            .where("c.del_flag = '0'");
        
        List<BlogComment> list = blogCommentService.list(queryWrapper);
        MagicExcelUtil<BlogComment> util = new MagicExcelUtil<>(BlogComment.class);
        util.exportExcel(response, list, "博客评论数据");
    }

    /**
     * 获取博客评论详细信息
     */
    @Operation(summary = "获取博客评论详细信息")
    @SaCheckPermission("article:comment:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogCommentService.getById(id));
    }

    /**
     * 新增博客评论
     */
    @Operation(summary = "新增博客评论")
    @SaCheckPermission("article:comment:add")
    @Log(title = "博客评论", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogComment blogComment)
    {
        return toAjax(blogCommentService.save(blogComment) ? 1 : 0);
    }

    /**
     * 修改博客评论
     */
    @Operation(summary = "修改博客评论")
    @SaCheckPermission("article:comment:edit")
    @Log(title = "博客评论", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogComment blogComment)
    {
        return toAjax(blogCommentService.updateById(blogComment) ? 1 : 0);
    }

    /**
     * 删除博客评论
     */
    @Operation(summary = "删除博客评论")
    @SaCheckPermission("article:comment:remove")
    @Log(title = "博客评论", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(blogCommentService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
    
    /**
     * 审核通过评论
     */
    @Operation(summary = "审核通过评论")
    @SaCheckPermission("article:comment:edit")
    @Log(title = "审核评论", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{ids}")
    public AjaxResult approve(@PathVariable Long[] ids)
    {
        for (Long id : ids) {
            BlogComment comment = blogCommentService.getById(id);
            if (comment != null) {
                comment.setStatus("1");
                blogCommentService.updateById(comment);
            }
        }
        return success("审核通过成功");
    }
    
    /**
     * 拒绝评论
     */
    @Operation(summary = "拒绝评论")
    @SaCheckPermission("article:comment:edit")
    @Log(title = "拒绝评论", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{ids}")
    public AjaxResult reject(@PathVariable Long[] ids)
    {
        for (Long id : ids) {
            BlogComment comment = blogCommentService.getById(id);
            if (comment != null) {
                comment.setStatus("2");
                blogCommentService.updateById(comment);
            }
        }
        return success("拒绝评论成功");
    }
}
