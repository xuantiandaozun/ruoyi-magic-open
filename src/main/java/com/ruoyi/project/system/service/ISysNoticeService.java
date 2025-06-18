package com.ruoyi.project.system.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.system.domain.SysNotice;
/**
 * 公告 服务层
 * 
 * @author ruoyi
 */
public interface ISysNoticeService extends IService<SysNotice>
{
    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    public List<SysNotice> selectNoticeList(SysNotice notice);
}
