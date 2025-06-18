package com.ruoyi.framework.manager.factory;

import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.project.monitor.domain.SysLogininfor;
import com.ruoyi.project.monitor.domain.SysOperLog;
import com.ruoyi.project.monitor.service.ISysLogininforService;
import com.ruoyi.project.monitor.service.ISysOperLogService;
import eu.bitwalker.useragentutils.UserAgent;

import cn.hutool.core.util.StrUtil;

/**
 * 异步工厂（产生任务用）
 * 
 * @author ruoyi
 */
public class AsyncFactory
{
    private static final Logger sys_user_logger = LoggerFactory.getLogger("sys-user");

    /**
     * 记录登录信息
     * 
     * @param username 用户名
     * @param status 状态
     * @param message 消息
     * @param args 列表
     * @return 任务task
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message,
            final Object... args)
    {
        sys_user_logger.info("创建登录日志任务: username={}, status={}, message={}", username, status, message);
        
        // 在这里获取客户端信息,因为这时还在请求上下文中
        final UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));
        final String ip = IpUtils.getIpAddr();
        final String address = AddressUtils.getRealAddressByIP(ip);
        
        return new TimerTask()
        {
            @Override
            public void run()
            {
                try {
                    sys_user_logger.info("开始执行登录日志记录任务");

                    // 封装对象
                    SysLogininfor logininfor = new SysLogininfor();
                    logininfor.setUserName(username);
                    logininfor.setIpaddr(ip);
                    logininfor.setLoginLocation(address);
                    logininfor.setBrowser(userAgent.getBrowser().getName());
                    logininfor.setOs(userAgent.getOperatingSystem().getName());
                    logininfor.setMsg(message);
                    // 日志状态
                    if (StrUtil.equalsAny(status, Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER))
                    {
                        logininfor.setStatus(Constants.SUCCESS);
                    }
                    else if (Constants.LOGIN_FAIL.equals(status))
                    {
                        logininfor.setStatus(Constants.FAIL);
                    }

                    // 插入数据
                    ISysLogininforService logininforService = SpringUtils.getBean(ISysLogininforService.class);
                    sys_user_logger.info("准备插入登录日志: {}", logininfor);
                    logininforService.insertLogininfor(logininfor);
                    sys_user_logger.info("登录日志记录完成");
                } catch (Exception e) {
                    sys_user_logger.error("记录登录日志失败", e);
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * 操作日志记录
     * 
     * @param operLog 操作日志信息
     * @return 任务task
     */
    public static TimerTask recordOper(final SysOperLog operLog)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                // 远程查询操作地点
                operLog.setOperLocation(AddressUtils.getRealAddressByIP(operLog.getOperIp()));
                SpringUtils.getBean(ISysOperLogService.class).insertOperlog(operLog);
            }
        };
    }
}
