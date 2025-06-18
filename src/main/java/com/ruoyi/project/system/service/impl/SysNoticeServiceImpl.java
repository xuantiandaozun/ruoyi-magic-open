package com.ruoyi.project.system.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.system.domain.SysNotice;
import com.ruoyi.project.system.mapper.SysNoticeMapper;
import com.ruoyi.project.system.service.ISysNoticeService;

import cn.hutool.core.util.ObjectUtil;

/**
 * 公告 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements ISysNoticeService
{
    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_notice")
            .where(new QueryColumn("notice_title").like(notice.getNoticeTitle(), ObjectUtil.isNotEmpty(notice.getNoticeTitle())))
            .and(new QueryColumn("notice_type").eq(notice.getNoticeType(), ObjectUtil.isNotNull(notice.getNoticeType())))
            .and(new QueryColumn("create_by").like(notice.getCreateBy(), ObjectUtil.isNotEmpty(notice.getCreateBy())))
            .orderBy(new QueryColumn("notice_id").desc());
        return list(queryWrapper);
    }
}
