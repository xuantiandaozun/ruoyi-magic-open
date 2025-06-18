package com.ruoyi.common.utils.http;

import jakarta.servlet.http.HttpServletRequest;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;

/**
 * 获取客户端浏览器和操作系统信息
 */
public class UserAgentUtils {
    /**
     * 获取用户代理对象
     */
    public static UserAgent getUserAgent(HttpServletRequest request) {
        return UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
    }

    /**
     * 获取系统
     */
    public static String getOs(HttpServletRequest request) {
        UserAgent userAgent = getUserAgent(request);
        OperatingSystem os = userAgent.getOperatingSystem();
        return os.getName();
    }

    /**
     * 获取浏览器
     */
    public static String getBrowser(HttpServletRequest request) {
        UserAgent userAgent = getUserAgent(request);
        Browser browser = userAgent.getBrowser();
        return browser.getName();
    }
} 