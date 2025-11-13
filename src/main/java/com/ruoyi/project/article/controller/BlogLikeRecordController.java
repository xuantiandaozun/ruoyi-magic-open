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
import com.ruoyi.project.article.domain.BlogLikeRecord;
import com.ruoyi.project.article.service.IBlogLikeRecordService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 博客点赞记录Controller
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Tag(name = "博客点赞记录")
@RestController
@RequestMapping("/article/likeRecord")
public class BlogLikeRecordController extends BaseController
{
    @Autowired
    private IBlogLikeRecordService blogLikeRecordService;

    /**
     * 查询博客点赞记录列表
     */
    @Operation(summary = "查询博客点赞记录列表")
    @SaCheckPermission("article:likeRecord:list")
    @GetMapping("/list")
    public TableDataInfo list(BlogLikeRecord blogLikeRecord)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper，包含左连接查询博客标题
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("l.*", "b.title as blogTitle")
            .from("blog_like_record").as("l")
            .leftJoin("blog").as("b").on("l.blog_id = b.blog_id");
        
        // 添加查询条件
        if (blogLikeRecord.getBlogId() != null) {
            queryWrapper.and("l.blog_id = {0}", blogLikeRecord.getBlogId());
        }
        if (blogLikeRecord.getUserId() != null) {
            queryWrapper.and("l.user_id = {0}", blogLikeRecord.getUserId());
        }
        if (StrUtil.isNotBlank(blogLikeRecord.getUserIp())) {
            queryWrapper.and("l.user_ip = {0}", blogLikeRecord.getUserIp());
        }
        if (StrUtil.isNotBlank(blogLikeRecord.getLikeStatus())) {
            queryWrapper.and("l.like_status = {0}", blogLikeRecord.getLikeStatus());
        }
        
        queryWrapper.and("l.del_flag = '0'");
        queryWrapper.orderBy("l.create_time", false);
        
        // 使用 MyBatisFlex 的分页方法
        Page<BlogLikeRecord> page = blogLikeRecordService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出博客点赞记录列表
     */
    @Operation(summary = "导出博客点赞记录列表")
    @SaCheckPermission("article:likeRecord:export")
    @Log(title = "博客点赞记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BlogLikeRecord blogLikeRecord)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("l.*", "b.title as blogTitle")
            .from("blog_like_record").as("l")
            .leftJoin("blog").as("b").on("l.blog_id = b.blog_id")
            .where("l.del_flag = '0'");
        
        List<BlogLikeRecord> list = blogLikeRecordService.list(queryWrapper);
        MagicExcelUtil<BlogLikeRecord> util = new MagicExcelUtil<>(BlogLikeRecord.class);
        util.exportExcel(response, list, "博客点赞记录数据");
    }

    /**
     * 获取博客点赞记录详细信息
     */
    @Operation(summary = "获取博客点赞记录详细信息")
    @SaCheckPermission("article:likeRecord:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogLikeRecordService.getById(id));
    }

    /**
     * 新增博客点赞记录
     */
    @Operation(summary = "新增博客点赞记录")
    @SaCheckPermission("article:likeRecord:add")
    @Log(title = "博客点赞记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogLikeRecord blogLikeRecord)
    {
        return toAjax(blogLikeRecordService.save(blogLikeRecord) ? 1 : 0);
    }

    /**
     * 修改博客点赞记录
     */
    @Operation(summary = "修改博客点赞记录")
    @SaCheckPermission("article:likeRecord:edit")
    @Log(title = "博客点赞记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogLikeRecord blogLikeRecord)
    {
        return toAjax(blogLikeRecordService.updateById(blogLikeRecord) ? 1 : 0);
    }

    /**
     * 删除博客点赞记录
     */
    @Operation(summary = "删除博客点赞记录")
    @SaCheckPermission("article:likeRecord:remove")
    @Log(title = "博客点赞记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(blogLikeRecordService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
}
