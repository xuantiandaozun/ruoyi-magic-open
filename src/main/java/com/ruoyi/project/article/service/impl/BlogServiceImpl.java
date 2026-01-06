package com.ruoyi.project.article.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.mapper.BlogMapper;
import com.ruoyi.project.article.service.IBlogService;

/**
 * 文章列表Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-08-05 16:49:23
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
