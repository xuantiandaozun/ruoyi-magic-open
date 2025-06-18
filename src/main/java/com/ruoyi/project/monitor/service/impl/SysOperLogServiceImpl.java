package com.ruoyi.project.monitor.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.monitor.domain.SysOperLog;
import com.ruoyi.project.monitor.mapper.SysOperLogMapper;
import com.ruoyi.project.monitor.service.ISysOperLogService;

/**
 * 操作日志 服务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService
{
    /**
     * 新增操作日志
     * 
     * @param operLog 操作日志对象
     */
    @Override
    public void insertOperlog(SysOperLog operLog)
    {
        save(operLog);
    }

    /**
     * 查询系统操作日志集合
     * 
     * @param operLog 操作日志对象
     * @return 操作日志集合
     */
    @Override
    public List<SysOperLog> selectOperLogList(SysOperLog operLog)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_oper_log")
            .where(new QueryColumn("title").like(operLog.getTitle(), operLog.getTitle() != null))
            .and(new QueryColumn("business_type").eq(operLog.getBusinessType(), operLog.getBusinessType() != null))
            .and(new QueryColumn("status").eq(operLog.getStatus(), operLog.getStatus() != null))
            .and(new QueryColumn("oper_name").like(operLog.getOperName(), operLog.getOperName() != null))
            .and(new QueryColumn("oper_time").ge(operLog.getParams().get("beginTime"), operLog.getParams().get("beginTime") != null))
            .and(new QueryColumn("oper_time").le(operLog.getParams().get("endTime"), operLog.getParams().get("endTime") != null))
            .orderBy(new QueryColumn("oper_id").desc());
        return list(queryWrapper);
    }

    /**
     * 批量删除系统操作日志
     * 
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    @Override
    public int deleteOperLogByIds(Long[] operIds)
    {
        return removeByIds(Arrays.asList(operIds)) ? 1 : 0;
    }

    /**
     * 查询操作日志详细
     * 
     * @param operId 操作ID
     * @return 操作日志对象
     */
    @Override
    public SysOperLog selectOperLogById(Long operId)
    {
        return getById(operId);
    }

    /**
     * 清空操作日志
     */
    @Override
    public void cleanOperLog()
    {
        QueryWrapper queryWrapper = QueryWrapper.create().from("sys_oper_log");
        remove(queryWrapper);
    }
}
