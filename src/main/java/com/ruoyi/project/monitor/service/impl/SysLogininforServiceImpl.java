package com.ruoyi.project.monitor.service.impl;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.monitor.domain.SysLogininfor;
import com.ruoyi.project.monitor.mapper.SysLogininforMapper;
import com.ruoyi.project.monitor.service.ISysLogininforService;

/**
 * 系统访问日志情况信息 服务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysLogininforServiceImpl extends ServiceImpl<SysLogininforMapper, SysLogininfor> implements ISysLogininforService
{
    private static final Logger log = LoggerFactory.getLogger(SysLogininforServiceImpl.class);

    /**
     * 新增系统登录日志
     * 
     * @param logininfor 访问日志对象
     */
    @Override
    public void insertLogininfor(SysLogininfor logininfor)
    {
        log.info("开始插入登录日志: {}", logininfor);
        try {
            log.info("调用 save 方法");
            save(logininfor);
            log.info("登录日志插入成功");
        } catch (Exception e) {
            log.error("登录日志插入失败", e);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 查询系统登录日志集合
     * 
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_logininfor")
            .where(new QueryColumn("ipaddr").like(logininfor.getIpaddr(), logininfor.getIpaddr() != null))
            .and(new QueryColumn("status").eq(logininfor.getStatus(), logininfor.getStatus() != null))
            .and(new QueryColumn("user_name").like(logininfor.getUserName(), logininfor.getUserName() != null))
            .and(new QueryColumn("login_time").ge(logininfor.getParams().get("beginTime"), logininfor.getParams().get("beginTime") != null))
            .and(new QueryColumn("login_time").le(logininfor.getParams().get("endTime"), logininfor.getParams().get("endTime") != null))
            .orderBy(new QueryColumn("info_id").desc());
        return list(queryWrapper);
    }

    /**
     * 批量删除系统登录日志
     * 
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLogininforByIds(Long[] infoIds)
    {
        return removeByIds(Arrays.asList(infoIds)) ? 1 : 0;
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLogininfor()
    {
        QueryWrapper queryWrapper = QueryWrapper.create().from("sys_logininfor");
        remove(queryWrapper);
    }
}
