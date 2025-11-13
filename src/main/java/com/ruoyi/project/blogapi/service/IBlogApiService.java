package com.ruoyi.project.blogapi.service;

import java.util.List;

import com.mybatisflex.core.paginate.Page;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.domain.BlogEn;
import com.ruoyi.project.blogapi.domain.vo.BlogEnListVO;
import com.ruoyi.project.blogapi.domain.vo.BlogListVO;

/**
 * 博客网页API Service接口
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
public interface IBlogApiService
{
    /**
     * 分页查询已发布的中文博客列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param category 分类（可选）
     * @param tag 标签（可选）
     * @param keyword 关键词搜索（可选）
     * @return 博客列表分页结果
     */
    Page<BlogListVO> listPublishedBlogs(Integer pageNum, Integer pageSize, String category, String tag, String keyword);

    /**
     * 分页查询已发布的英文博客列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param category 分类（可选）
     * @param tag 标签（可选）
     * @param keyword 关键词搜索（可选）
     * @return 英文博客列表分页结果
     */
    Page<BlogEnListVO> listPublishedBlogsEn(Integer pageNum, Integer pageSize, String category, String tag, String keyword);

    /**
     * 根据博客ID获取中文博客详情
     * 每次获取成功后会更新最后查看时间和查看次数
     * 
     * @param blogId 博客ID
     * @return 博客详情
     */
    Blog getBlogDetail(String blogId);

    /**
     * 根据博客ID获取英文博客详情
     * 每次获取成功后会更新最后查看时间和查看次数
     * 
     * @param blogId 博客ID
     * @return 英文博客详情
     */
    BlogEn getBlogEnDetail(String blogId);

    /**
     * 获取推荐博客列表（中文）
     * 根据查看次数和点赞次数排序
     * 
     * @param limit 返回数量限制
     * @return 推荐博客列表
     */
    List<BlogListVO> getRecommendedBlogs(Integer limit);

    /**
     * 获取推荐博客列表（英文）
     * 根据查看次数和点赞次数排序
     * 
     * @param limit 返回数量限制
     * @return 推荐英文博客列表
     */
    List<BlogEnListVO> getRecommendedBlogsEn(Integer limit);
}
