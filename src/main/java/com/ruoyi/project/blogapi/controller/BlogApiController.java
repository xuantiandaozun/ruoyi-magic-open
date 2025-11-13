package com.ruoyi.project.blogapi.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.ruoyi.framework.aspectj.lang.annotation.Anonymous;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.domain.BlogEn;
import com.ruoyi.project.blogapi.domain.vo.BlogEnListVO;
import com.ruoyi.project.blogapi.domain.vo.BlogListVO;
import com.ruoyi.project.blogapi.service.IBlogApiService;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 博客网页API Controller
 * 提供给博客前端展示的公开API接口（免登录访问）
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Tag(name = "博客网页API")
@Anonymous
@RestController
@RequestMapping("/api/blog")
public class BlogApiController extends BaseController
{
    @Autowired
    private IBlogApiService blogApiService;

    /**
     * 分页获取中文博客文案列表
     * 
     * @param pageNum 页码，默认1
     * @param pageSize 每页数量，默认10
     * @param category 分类（可选）
     * @param tag 标签（可选）
     * @param keyword 关键词（可选）
     * @return 博客列表分页数据
     */
    @Operation(summary = "分页获取中文博客文案列表")
    @GetMapping("/list")
    public AjaxResult list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) String keyword
    )
    {
        Page<BlogListVO> page = blogApiService.listPublishedBlogs(pageNum, pageSize, category, tag, keyword);
        return success(page);
    }

    /**
     * 分页获取英文博客文案列表
     * 
     * @param pageNum 页码，默认1
     * @param pageSize 每页数量，默认10
     * @param category 分类（可选）
     * @param tag 标签（可选）
     * @param keyword 关键词（可选）
     * @return 英文博客列表分页数据
     */
    @Operation(summary = "分页获取英文博客文案列表")
    @GetMapping("/listEn")
    public AjaxResult listEn(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) String keyword
    )
    {
        Page<BlogEnListVO> page = blogApiService.listPublishedBlogsEn(pageNum, pageSize, category, tag, keyword);
        return success(page);
    }

    /**
     * 根据博客ID获取中文博客详情
     * 每次获取成功后会更新最后查看时间和查看次数
     * 
     * @param blogId 博客ID
     * @return 博客详情
     */
    @Operation(summary = "获取中文博客详情")
    @GetMapping("/detail/{blogId}")
    public AjaxResult detail(@PathVariable String blogId)
    {
        Blog blog = blogApiService.getBlogDetail(blogId);
        if (ObjectUtil.isNull(blog)) {
            return error("博客不存在或已被删除");
        }
        return success(blog);
    }

    /**
     * 根据博客ID获取英文博客详情
     * 每次获取成功后会更新最后查看时间和查看次数
     * 
     * @param blogId 博客ID
     * @return 英文博客详情
     */
    @Operation(summary = "获取英文博客详情")
    @GetMapping("/detailEn/{blogId}")
    public AjaxResult detailEn(@PathVariable String blogId)
    {
        BlogEn blogEn = blogApiService.getBlogEnDetail(blogId);
        if (ObjectUtil.isNull(blogEn)) {
            return error("博客不存在或已被删除");
        }
        return success(blogEn);
    }

    /**
     * 获取推荐博客列表（中文）
     * 根据查看次数和点赞次数排序
     * 
     * @param limit 返回数量限制，默认10
     * @return 推荐博客列表
     */
    @Operation(summary = "获取推荐博客列表")
    @GetMapping("/recommended")
    public AjaxResult recommended(@RequestParam(defaultValue = "10") Integer limit)
    {
        List<BlogListVO> blogs = blogApiService.getRecommendedBlogs(limit);
        return success(blogs);
    }

    /**
     * 获取推荐博客列表（英文）
     * 根据查看次数和点赞次数排序
     * 
     * @param limit 返回数量限制，默认10
     * @return 推荐英文博客列表
     */
    @Operation(summary = "获取推荐英文博客列表")
    @GetMapping("/recommendedEn")
    public AjaxResult recommendedEn(@RequestParam(defaultValue = "10") Integer limit)
    {
        List<BlogEnListVO> blogs = blogApiService.getRecommendedBlogsEn(limit);
        return success(blogs);
    }
}
