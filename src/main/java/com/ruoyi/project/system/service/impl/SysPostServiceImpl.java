package com.ruoyi.project.system.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.system.domain.SysPost;
import com.ruoyi.project.system.domain.SysUserPost;
import com.ruoyi.project.system.mapper.SysPostMapper;
import com.ruoyi.project.system.mapper.SysUserPostMapper;
import com.ruoyi.project.system.service.ISysPostService;

import cn.hutool.core.util.ObjectUtil;

/**
 * 岗位信息 服务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements ISysPostService
{
    @Autowired
    private SysUserPostMapper userPostMapper;

    /**
     * 查询岗位信息集合
     * 
     * @param post 岗位信息
     * @return 岗位信息集合
     */
    @Override
    public List<SysPost> selectPostList(SysPost post)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_post")
            .where(new QueryColumn("post_name").like(post.getPostName(), ObjectUtil.isNotEmpty(post.getPostName())))
            .and(new QueryColumn("post_code").eq(post.getPostCode(), ObjectUtil.isNotEmpty(post.getPostCode())))
            .and(new QueryColumn("status").eq(post.getStatus(), ObjectUtil.isNotNull(post.getStatus())))
            .orderBy(new QueryColumn("post_sort").asc());
        return list(queryWrapper);
    }

    /**
     * 查询所有岗位
     * 
     * @return 岗位列表
     */
    @Override
    public List<SysPost> selectPostAll()
    {
        return list(QueryWrapper.create()
            .from("sys_post")
            .orderBy(new QueryColumn("post_sort").asc()));
    }

    /**
     * 根据用户ID获取岗位选择框列表
     * 
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    @Override
    public List<Long> selectPostListByUserId(Long userId)
    {
        return userPostMapper.selectListByQuery(
            QueryWrapper.create()
                .from("sys_user_post")
                .where(new QueryColumn("user_id").eq(userId))
        ).stream()
        .map(SysUserPost::getPostId)
        .collect(Collectors.toList());
    }

    /**
     * 校验岗位名称是否唯一
     * 
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostNameUnique(SysPost post)
    {
        Long postId = ObjectUtil.isNull(post.getPostId()) ? -1L : post.getPostId();
        
        SysPost info = getOne(QueryWrapper.create()
            .from("sys_post")
            .where(new QueryColumn("post_name").eq(post.getPostName()))
            .limit(1));
            
        if (ObjectUtil.isNotNull(info) && info.getPostId().longValue() != postId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 校验岗位编码是否唯一
     * 
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostCodeUnique(SysPost post)
    {
        Long postId = ObjectUtil.isNull(post.getPostId()) ? -1L : post.getPostId();
        
        SysPost info = getOne(QueryWrapper.create()
            .from("sys_post")
            .where(new QueryColumn("post_code").eq(post.getPostCode()))
            .limit(1));
            
        if (ObjectUtil.isNotNull(info) && info.getPostId().longValue() != postId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 通过岗位ID查询岗位使用数量
     * 
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int countUserPostById(Long postId)
    {
        return Math.toIntExact(userPostMapper.selectCountByQuery(
            QueryWrapper.create()
                .from("sys_user_post")
                .where(new QueryColumn("post_id").eq(postId))
        ));
    }
}
