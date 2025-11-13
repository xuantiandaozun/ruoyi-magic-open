package com.ruoyi.project.blogapi.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.domain.BlogEn;
import com.ruoyi.project.article.mapper.BlogEnMapper;
import com.ruoyi.project.article.mapper.BlogMapper;
import com.ruoyi.project.blogapi.domain.vo.BlogEnListVO;
import com.ruoyi.project.blogapi.domain.vo.BlogListVO;
import com.ruoyi.project.blogapi.service.IBlogApiService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 博客网页API Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Service
public class BlogApiServiceImpl implements IBlogApiService
{
    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private BlogEnMapper blogEnMapper;

    /**
     * 分页查询中文博客列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param category 分类（可选）
     * @param tag 标签（可选）
     * @param keyword 关键词搜索（可选）
     * @return 博客列表分页结果
     */
    @Override
    public Page<BlogListVO> listPublishedBlogs(Integer pageNum, Integer pageSize, String category, String tag, String keyword)
    {
        // 构建查询条件，查询除 content 外的所有字段
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("blog_id", "title", "summary", "cover_image", "category", "tags", 
                   "status", "is_top", "is_original", "view_count", "like_count", "comment_count", 
                   "publish_time", "feishu_doc_token", "feishu_doc_name", "feishu_sync_status", 
                   "feishu_last_sync_time", "sort_order", "last_read_time", "create_by", "create_time", 
                   "update_by", "update_time", "remark")
            .from("blog")
            .where("del_flag = '0'"); // 未删除

        // 分类筛选
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.and("category = ?", category);
        }

        // 标签筛选
        if (StrUtil.isNotBlank(tag)) {
            queryWrapper.and("FIND_IN_SET(?, tags) > 0", tag);
        }

        // 关键词搜索（标题或摘要）
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and("(title LIKE ? OR summary LIKE ?)", 
                "%" + keyword + "%", "%" + keyword + "%");
        }

        // 排序：置顶优先，然后按发布时间倒序
        queryWrapper.orderBy("is_top", false)
                    .orderBy("publish_time", false);

        // 分页查询
        Page<Blog> blogPage = blogMapper.paginate(new Page<>(pageNum, pageSize), queryWrapper);

        // 转换为VO对象
        List<BlogListVO> voList = blogPage.getRecords().stream()
            .map(blog -> BeanUtil.copyProperties(blog, BlogListVO.class))
            .collect(Collectors.toList());

        // 构建返回的分页结果
        Page<BlogListVO> voPage = new Page<>();
        voPage.setRecords(voList);
        voPage.setPageNumber(blogPage.getPageNumber());
        voPage.setPageSize(blogPage.getPageSize());
        voPage.setTotalRow(blogPage.getTotalRow());
        voPage.setTotalPage(blogPage.getTotalPage());

        return voPage;
    }

    /**
     * 分页查询英文博客列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param category 分类（可选）
     * @param tag 标签（可选）
     * @param keyword 关键词搜索（可选）
     * @return 英文博客列表分页结果
     */
    @Override
    public Page<BlogEnListVO> listPublishedBlogsEn(Integer pageNum, Integer pageSize, String category, String tag, String keyword)
    {
        // 构建查询条件，查询除 content 外的所有字段
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("blog_id", "title", "summary", "cover_image", "category", "tags", 
                   "status", "is_top", "is_original", "view_count", "like_count", "comment_count", 
                   "publish_time", "zh_blog_id", "feishu_doc_token", "feishu_doc_name", 
                   "feishu_sync_status", "feishu_last_sync_time", "sort_order", "last_read_time", 
                   "create_by", "create_time", "update_by", "update_time", "remark")
            .from("blog_en")
            .where("del_flag = '0'"); // 未删除

        // 分类筛选
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.and("category = ?", category);
        }

        // 标签筛选
        if (StrUtil.isNotBlank(tag)) {
            queryWrapper.and("FIND_IN_SET(?, tags) > 0", tag);
        }

        // 关键词搜索（标题或摘要）
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and("(title LIKE ? OR summary LIKE ?)", 
                "%" + keyword + "%", "%" + keyword + "%");
        }

        // 排序：置顶优先，然后按发布时间倒序
        queryWrapper.orderBy("is_top", false)
                    .orderBy("publish_time", false);

        // 分页查询
        Page<BlogEn> blogPage = blogEnMapper.paginate(new Page<>(pageNum, pageSize), queryWrapper);

        // 转换为VO对象
        List<BlogEnListVO> voList = blogPage.getRecords().stream()
            .map(blog -> BeanUtil.copyProperties(blog, BlogEnListVO.class))
            .collect(Collectors.toList());

        // 构建返回的分页结果
        Page<BlogEnListVO> voPage = new Page<>();
        voPage.setRecords(voList);
        voPage.setPageNumber(blogPage.getPageNumber());
        voPage.setPageSize(blogPage.getPageSize());
        voPage.setTotalRow(blogPage.getTotalRow());
        voPage.setTotalPage(blogPage.getTotalPage());

        return voPage;
    }

    /**
     * 根据博客ID获取中文博客详情
     * 每次获取成功后会更新最后查看时间和查看次数
     * 
     * @param blogId 博客ID
     * @return 博客详情
     */
    @Override
    public Blog getBlogDetail(String blogId)
    {
        if (StrUtil.isBlank(blogId)) {
            return null;
        }

        // 查询博客详情
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("blog")
            .where("blog_id = ?", blogId)
            .and("del_flag = '0'");
        
        Blog blog = blogMapper.selectOneByQuery(queryWrapper);
        
        if (ObjectUtil.isNotNull(blog)) {
            // 更新最后查看时间
            blog.setLastReadTime(LocalDateTime.now());
            
            // 更新查看次数
            Integer currentViewCount = 0;
            if (StrUtil.isNotBlank(blog.getViewCount())) {
                try {
                    currentViewCount = Integer.parseInt(blog.getViewCount());
                } catch (NumberFormatException e) {
                    currentViewCount = 0;
                }
            }
            blog.setViewCount(String.valueOf(currentViewCount + 1));
            
            // 保存更新
            blogMapper.update(blog);
        }
        
        return blog;
    }

    /**
     * 根据博客ID获取英文博客详情
     * 每次获取成功后会更新最后查看时间和查看次数
     * 
     * @param blogId 博客ID
     * @return 英文博客详情
     */
    @Override
    public BlogEn getBlogEnDetail(String blogId)
    {
        if (StrUtil.isBlank(blogId)) {
            return null;
        }

        // 查询英文博客详情
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("blog_en")
            .where("blog_id = ?", blogId)
            .and("del_flag = '0'");
        
        BlogEn blogEn = blogEnMapper.selectOneByQuery(queryWrapper);
        
        if (ObjectUtil.isNotNull(blogEn)) {
            // 更新最后查看时间
            blogEn.setLastReadTime(LocalDateTime.now());
            
            // 更新查看次数
            Integer currentViewCount = 0;
            if (StrUtil.isNotBlank(blogEn.getViewCount())) {
                try {
                    currentViewCount = Integer.parseInt(blogEn.getViewCount());
                } catch (NumberFormatException e) {
                    currentViewCount = 0;
                }
            }
            blogEn.setViewCount(String.valueOf(currentViewCount + 1));
            
            // 保存更新
            blogEnMapper.update(blogEn);
        }
        
        return blogEn;
    }

    /**
     * 获取推荐博客列表（中文）
     * 根据查看次数和点赞次数排序
     * 
     * @param limit 返回数量限制
     * @return 推荐博客列表
     */
    @Override
    public List<BlogListVO> getRecommendedBlogs(Integer limit)
    {
        // 默认返回10条
        int size = (limit != null && limit > 0) ? limit : 10;
        
        // 构建查询条件，查询除 content 外的所有字段
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("blog_id", "title", "summary", "cover_image", "category", "tags", 
                   "status", "is_top", "is_original", "view_count", "like_count", "comment_count", 
                   "publish_time", "feishu_doc_token", "feishu_doc_name", "feishu_sync_status", 
                   "feishu_last_sync_time", "sort_order", "last_read_time", "create_by", "create_time", 
                   "update_by", "update_time", "remark")
            .from("blog")
            .where("del_flag = '0'") // 未删除
            .and("status = '1'"); // 已发布
        
        // 按查看次数和点赞次数降序排序
        queryWrapper.orderBy("CAST(view_count AS UNSIGNED)", false)
                    .orderBy("CAST(like_count AS UNSIGNED)", false)
                    .orderBy("publish_time", false)
                    .limit(size);
        
        // 查询数据
        List<Blog> blogList = blogMapper.selectListByQuery(queryWrapper);
        
        // 转换为VO对象
        return blogList.stream()
            .map(blog -> BeanUtil.copyProperties(blog, BlogListVO.class))
            .collect(Collectors.toList());
    }

    /**
     * 获取推荐博客列表（英文）
     * 根据查看次数和点赞次数排序
     * 
     * @param limit 返回数量限制
     * @return 推荐英文博客列表
     */
    @Override
    public List<BlogEnListVO> getRecommendedBlogsEn(Integer limit)
    {
        // 默认返回10条
        int size = (limit != null && limit > 0) ? limit : 10;
        
        // 构建查询条件，查询除 content 外的所有字段
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("blog_id", "title", "summary", "cover_image", "category", "tags", 
                   "status", "is_top", "is_original", "view_count", "like_count", "comment_count", 
                   "publish_time", "zh_blog_id", "feishu_doc_token", "feishu_doc_name", 
                   "feishu_sync_status", "feishu_last_sync_time", "sort_order", "last_read_time", 
                   "create_by", "create_time", "update_by", "update_time", "remark")
            .from("blog_en")
            .where("del_flag = '0'") // 未删除
            .and("status = '1'"); // 已发布
        
        // 按查看次数和点赞次数降序排序
        queryWrapper.orderBy("CAST(view_count AS UNSIGNED)", false)
                    .orderBy("CAST(like_count AS UNSIGNED)", false)
                    .orderBy("publish_time", false)
                    .limit(size);
        
        // 查询数据
        List<BlogEn> blogList = blogEnMapper.selectListByQuery(queryWrapper);
        
        // 转换为VO对象
        return blogList.stream()
            .map(blog -> BeanUtil.copyProperties(blog, BlogEnListVO.class))
            .collect(Collectors.toList());
    }
}
