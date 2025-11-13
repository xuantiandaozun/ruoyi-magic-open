package com.ruoyi.project.blogapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aspectj.lang.annotation.Anonymous;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.domain.BlogComment;
import com.ruoyi.project.article.service.IBlogCommentService;
import com.ruoyi.project.article.service.IBlogService;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 博客公开API Controller
 * 提供给博客前端的公开API接口（点赞、评论等）
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Tag(name = "博客公开API")
@Anonymous
@RestController
@RequestMapping("/api/blog/articles")
public class BlogPublicApiController extends BaseController
{
    @Autowired
    private IBlogService blogService;
    
    @Autowired
    private IBlogCommentService blogCommentService;

    /**
     * 博客点赞接口
     * 
     * @param blogId 博客ID
     * @param action 操作类型：like（点赞）或unlike（取消点赞）
     * @return 操作结果
     */
    @Operation(summary = "博客点赞")
    @PostMapping("/like")
    public AjaxResult like(
        @RequestParam(required = false) String blogId,
        @RequestParam(required = false, defaultValue = "like") String action
    )
    {
        if (StrUtil.isBlank(blogId)) {
            return error("博客ID不能为空");
        }
        
        Blog blog = blogService.getById(blogId);
        if (ObjectUtil.isNull(blog)) {
            return error("博客不存在");
        }
        
        try {
            int currentLikeCount = 0;
            if (StrUtil.isNotBlank(blog.getLikeCount())) {
                try {
                    currentLikeCount = Integer.parseInt(blog.getLikeCount());
                } catch (NumberFormatException e) {
                    currentLikeCount = 0;
                }
            }
            
            if ("like".equals(action)) {
                blog.setLikeCount(String.valueOf(currentLikeCount + 1));
            } else if ("unlike".equals(action)) {
                blog.setLikeCount(String.valueOf(Math.max(currentLikeCount - 1, 0)));
            } else {
                return error("操作类型不正确，应为like或unlike");
            }
            
            blogService.updateById(blog);
            return success("操作成功");
        } catch (Exception e) {
            return error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 获取博客评论列表
     * 
     * @param blogId 博客ID
     * @param page 页码，默认1
     * @param size 每页数量，默认10
     * @return 评论列表分页数据
     */
    @Operation(summary = "获取博客评论列表")
    @GetMapping("/comments")
    public AjaxResult getComments(
        @RequestParam String blogId,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size
    )
    {
        if (StrUtil.isBlank(blogId)) {
            return error("博客ID不能为空");
        }
        
        Blog blog = blogService.getById(blogId);
        if (ObjectUtil.isNull(blog)) {
            return error("博客不存在");
        }
        
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("blog_id", blogId)
                .eq("del_flag", "0")
                .isNull("parent_id")  // 只查询主评论
                .orderBy("create_time", false);
            
            Page<BlogComment> pageResult = blogCommentService.page(
                new Page<>(page, size),
                queryWrapper
            );
            
            // 为每个主评论加载回复
            if (pageResult.getRecords() != null) {
                pageResult.getRecords().forEach(comment -> {
                    QueryWrapper replyQuery = QueryWrapper.create()
                        .eq("parent_id", comment.getId())
                        .eq("status", "1")
                        .eq("del_flag", "0")
                        .orderBy("create_time", true);
                    
                    java.util.List<BlogComment> replies = blogCommentService.list(replyQuery);
                    comment.setReplies(replies);
                });
            }
            
            // 构建返回数据格式
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("rows", pageResult.getRecords());
            result.put("total", pageResult.getTotalRow());
            result.put("size", pageResult.getPageSize());
            result.put("page", pageResult.getPageNumber());
            
            return success(result);
        } catch (Exception e) {
            return error("获取评论列表失败：" + e.getMessage());
        }
    }

    /**
     * 提交博客评论或回复
     * 
     * @param blogId 博客ID
     * @param content 评论内容
     * @param nickname 昵称
     * @param email 邮箱（可选）
     * @param website 网站（可选）
     * @param parentId 父评论ID（可选，用于回复）
     * @return 操作结果
     */
    @Operation(summary = "提交博客评论")
    @PostMapping("/comment")
    public AjaxResult submitComment(
        @RequestParam String blogId,
        @RequestParam String content,
        @RequestParam String nickname,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String website,
        @RequestParam(required = false) Long parentId
    )
    {
        if (StrUtil.isBlank(blogId)) {
            return error("博客ID不能为空");
        }
        
        if (StrUtil.isBlank(content)) {
            return error("评论内容不能为空");
        }
        
        if (StrUtil.isBlank(nickname)) {
            return error("昵称不能为空");
        }
        
        Blog blog = blogService.getById(blogId);
        if (ObjectUtil.isNull(blog)) {
            return error("博客不存在");
        }
        
        try {
            BlogComment comment = new BlogComment();
            try {
                comment.setBlogId(Long.parseLong(blogId));
            } catch (NumberFormatException e) {
                return error("博客ID格式不正确");
            }
            comment.setContent(content);
            comment.setNickname(nickname);
            comment.setEmail(email);
            comment.setWebsite(website);
            comment.setParentId(parentId);
            comment.setStatus("0");  // 新评论默认待审核
            comment.setDelFlag("0");
            
            blogCommentService.save(comment);
            
            return success("评论提交成功，待审核");
        } catch (Exception e) {
            return error("提交评论失败：" + e.getMessage());
        }
    }
}
