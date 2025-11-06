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
import com.ruoyi.project.ai.domain.AiCoverGenerationRecord;
import com.ruoyi.project.ai.service.IAiCoverGenerationRecordService;
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
    
    @Autowired
    private IAiCoverGenerationRecordService aiCoverService;
    
    @Autowired
    private IBlogEnService blogEnService;
    


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

    /**
     * 自动配图 - 为没有封面的博客自动设置封面
     */
    @Operation(summary = "自动配图")
    @SaCheckPermission("article:blog:edit")
    @Log(title = "自动配图", businessType = BusinessType.UPDATE)
    @PostMapping("/autoAssignCovers")
    public AjaxResult autoAssignCovers()
    {
        try {
            // 1. 查询所有没有设置封面的博客
            QueryWrapper queryWrapper = QueryWrapper.create()
                .from("blog").as("b")
                .select("b.*")
                .where("b.cover_image IS NULL OR b.cover_image = ''")
                .and("b.del_flag = '0'")
                .orderBy("b.create_time", true);
            
            List<Blog> blogsWithoutCover = blogService.list(queryWrapper);
            
            if (blogsWithoutCover.isEmpty()) {
                return success("所有博客都已有封面，无需处理");
            }
            
            int successCount = 0;
            int totalCount = blogsWithoutCover.size();
            
            // 通用关键词列表
            String[] genericKeywords = {"github", "开源", "项目", "代码", "编程", "技术", "开发"};
            
            for (Blog blog : blogsWithoutCover) {
                try {
                    AiCoverGenerationRecord selectedCover = null;
                    
                    // 2. 优先使用分类查询封面
                    if (blog.getCategory() != null && !blog.getCategory().trim().isEmpty()) {
                        List<AiCoverGenerationRecord> coversByCategory = aiCoverService.listByCategory(blog.getCategory().trim());
                        if (!coversByCategory.isEmpty()) {
                            selectedCover = coversByCategory.get(0);
                        }
                    }
                    
                    // 3. 如果分类没找到，使用标签查询
                    if (selectedCover == null && blog.getTags() != null && !blog.getTags().trim().isEmpty()) {
                        String[] tags = blog.getTags().split(",");
                        for (String tag : tags) {
                            tag = tag.trim();
                            if (!tag.isEmpty()) {
                                List<AiCoverGenerationRecord> coversByTag = aiCoverService.listByPrompt(tag);
                                if (!coversByTag.isEmpty()) {
                                    selectedCover = coversByTag.get(0);
                                    break;
                                }
                            }
                        }
                    }
                    
                    // 4. 如果都没找到，使用通用关键词查询
                    if (selectedCover == null) {
                        for (String keyword : genericKeywords) {
                            List<AiCoverGenerationRecord> coversByKeyword = aiCoverService.listByPrompt(keyword);
                            if (!coversByKeyword.isEmpty()) {
                                selectedCover = coversByKeyword.get(0);
                                break;
                            }
                        }
                    }
                    
                    // 5. 如果找到封面，更新博客
                    if (selectedCover != null) {
                        blog.setCoverImage(selectedCover.getImageUrl());
                        blogService.updateById(blog);
                        
                        // 同时更新关联的英文博客封面
                        try {
                            QueryWrapper enQueryWrapper = QueryWrapper.create()
                                .from("blog_en")
                                .where("zh_blog_id", blog.getBlogId())
                                .and("del_flag", "0");
                            
                            BlogEn enBlog = blogEnService.getOne(enQueryWrapper);
                            if (enBlog != null) {
                                enBlog.setCoverImage(selectedCover.getImageUrl());
                                blogEnService.updateById(enBlog);
                            }
                        } catch (Exception enError) {
                            System.err.println("更新英文博客 " + blog.getBlogId() + " 封面失败: " + enError.getMessage());
                        }
                        
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    // 单个博客处理失败不影响其他博客
                    System.err.println("处理博客 " + blog.getBlogId() + " 时出错: " + e.getMessage());
                }
            }
            
            String message = String.format("自动配图完成！共处理 %d 个博客，成功设置 %d 个封面", totalCount, successCount);
            return success(message);
            
        } catch (Exception e) {
            return error("自动配图失败: " + e.getMessage());
        }
    }
    

    

    

}
