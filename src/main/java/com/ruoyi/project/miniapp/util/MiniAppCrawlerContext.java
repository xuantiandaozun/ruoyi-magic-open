package com.ruoyi.project.miniapp.util;

/**
 * 标记当前 HTTP 请求是否通过微信搜索爬虫签名校验。
 */
public final class MiniAppCrawlerContext {

    private static final ThreadLocal<Boolean> VERIFIED = new ThreadLocal<>();

    private MiniAppCrawlerContext() {
    }

    public static void markVerified() {
        VERIFIED.set(Boolean.TRUE);
    }

    public static boolean isVerified() {
        return Boolean.TRUE.equals(VERIFIED.get());
    }

    public static void clear() {
        VERIFIED.remove();
    }
}
