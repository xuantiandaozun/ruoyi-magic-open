package com.ruoyi.project.article.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.service.IBlogService;
import com.ruoyi.project.feishu.domain.FeishuDoc;
import com.ruoyi.project.feishu.service.IFeishuDocService;
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
 * 文章列表Controller
 * 
 * @author ruoyi
 * @date 2025-08-05 16:49:23
 */
@Tag(name = "文章列表")
@RestController
@RequestMapping("/article/blog")
public class BlogController extends BaseController
{
    @Autowired
    private IBlogService blogService;
    
    @Autowired
    private IFeishuDocService feishuDocService;

    /**
     * 查询文章列表列表
     */
    @Operation(summary = "查询文章列表列表")
    @SaCheckPermission("article:blog:list")
    @GetMapping("/list")
    public TableDataInfo list(Blog blog)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(blog);
        
        // 使用 MyBatisFlex 的分页方法
        Page<Blog> page = blogService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出文章列表列表
     */
    @Operation(summary = "导出文章列表列表")
    @SaCheckPermission("article:blog:export")
    @Log(title = "文章列表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Blog blog)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<Blog> list = blogService.list(queryWrapper);
        MagicExcelUtil<Blog> util = new MagicExcelUtil<>(Blog.class);
        util.exportExcel(response, list, "文章列表数据");
    }

    /**
     * 获取文章列表详细信息
     */
    @Operation(summary = "获取文章列表详细信息")
    @SaCheckPermission("article:blog:query")
    @GetMapping(value = "/{blogId}")
    public AjaxResult getInfo(@PathVariable("blogId") String blogId)
    {
        return success(blogService.getById(blogId));
    }

    /**
     * 新增文章列表
     */
    @Operation(summary = "新增文章列表")
    @SaCheckPermission("article:blog:add")
    @Log(title = "文章列表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Blog blog)
    {
        return toAjax(blogService.save(blog) ? 1 : 0);
    }

    /**
     * 修改文章列表
     */
    @Operation(summary = "修改文章列表")
    @SaCheckPermission("article:blog:edit")
    @Log(title = "文章列表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Blog blog)
    {
        return toAjax(blogService.updateById(blog) ? 1 : 0);
    }

    /**
     * 删除文章列表
     */
    @Operation(summary = "删除文章列表")
    @SaCheckPermission("article:blog:remove")
    @Log(title = "文章列表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{blogIds}")
    public AjaxResult remove(@PathVariable String[] blogIds)
    {
        return toAjax(blogService.removeByIds(Arrays.asList(blogIds)) ? blogIds.length : 0);
    }
    
    /**
     * 获取飞书文档选项列表
     */
    @Operation(summary = "获取飞书文档选项列表")
    @SaCheckPermission("article:blog:list")
    @GetMapping("/feishu-docs")
    public AjaxResult getFeishuDocOptions()
    {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("is_folder", 0) // 只获取文档，不包括文件夹
                .orderBy("name", true); // 按名称排序
            
            List<FeishuDoc> feishuDocs = feishuDocService.list(queryWrapper);
            
            // 转换为前端需要的格式
            List<Object> options = feishuDocs.stream()
                .map(doc -> {
                    java.util.Map<String, Object> option = new java.util.HashMap<>();
                    option.put("token", doc.getToken());
                    option.put("name", doc.getName());
                    option.put("type", doc.getType());
                    option.put("url", doc.getUrl());
                    return option;
                })
                .collect(Collectors.toList());
            
            return success(options);
        } catch (Exception e) {
            return error("获取飞书文档列表失败: " + e.getMessage());
        }
    }
}
