package com.ruoyi.project.system.service.impl;

import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.project.monitor.domain.SysUserOnline;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.project.system.service.ISysUserOnlineService;

/**
 * 在线用户 服务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysUserOnlineServiceImpl implements ISysUserOnlineService
{
    /**
     * 通过登录地址查询信息
     * 
     * @param ipaddr 登录地址
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user)
    {
        if (StrUtil.equals(ipaddr, user.getIpaddr()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过用户名称查询信息
     * 
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user)
    {
        if (StrUtil.equals(userName, user.getUser().getUserName()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过登录地址/用户名称查询信息
     * 
     * @param ipaddr 登录地址
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user)
    {
        if (StrUtil.equals(ipaddr, user.getIpaddr()) && StrUtil.equals(userName, user.getUser().getUserName()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 设置在线用户信息
     * 
     * @param user 用户信息
     * @return 在线用户
     */
    @Override
    public SysUserOnline loginUserToUserOnline(LoginUser user)
    {
        if (ObjectUtil.isNull(user) || ObjectUtil.isNull(user.getUser()))
        {
            return null;
        }
        SysUserOnline online = new SysUserOnline();
        online.setTokenId(StpUtil.getTokenValue());
        online.setUserName(user.getUser().getUserName());
        online.setIpaddr(user.getIpaddr());
        online.setLoginLocation(user.getLoginLocation());
        online.setBrowser(user.getBrowser());
        online.setOs(user.getOs());
        online.setLoginTime(user.getUser().getLoginDate() != null ? user.getUser().getLoginDate().getTime() : null);
        if (ObjectUtil.isNotNull(user.getUser().getDept()))
        {
            online.setDeptName(user.getUser().getDept().getDeptName());
        }
        return online;
    }
}
