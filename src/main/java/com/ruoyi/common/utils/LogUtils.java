package com.ruoyi.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类
 * 
 * @author ruoyi
 */
public class LogUtils
{
    public static <T> Logger getLogger(Class<T> clazz)
    {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * 获取业务日志logger
     */
    public static Logger getBusLogger()
    {
        return LoggerFactory.getLogger("sys-user");
    }

    /**
     * 获取异常日志logger
     */
    public static Logger getExceptionLogger()
    {
        return LoggerFactory.getLogger("sys-error");
    }

    /**
     * 获取访问日志logger
     */
    public static Logger getAccessLogger()
    {
        return LoggerFactory.getLogger("sys-access");
    }

    /**
     * 设置请求分析器
     */
    public static String getBlock(Object msg)
    {
        if (msg == null)
        {
            msg = "";
        }
        return "[" + msg.toString() + "]";
    }
}
