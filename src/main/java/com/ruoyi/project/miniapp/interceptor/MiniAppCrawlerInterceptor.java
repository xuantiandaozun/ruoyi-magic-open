package com.ruoyi.project.miniapp.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.miniapp.domain.MiniApp;
import com.ruoyi.project.miniapp.service.IMiniAppService;
import com.ruoyi.project.miniapp.util.MiniAppCrawlerContext;
import com.ruoyi.project.miniapp.util.MiniAppWxCrawlerSignatureUtil;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MiniAppCrawlerInterceptor implements HandlerInterceptor {

    private static final String HEADER_TIMESTAMP = "X-WXApp-Crawler-Timestamp";
    private static final String HEADER_NONCE = "X-WXApp-Crawler-Nonce";
    private static final String HEADER_SIGNATURE = "X-WXApp-Crawler-Signature";
    private static final String CRAWLER_USER_AGENT = "mpcrawler";

    private final IMiniAppService miniAppService;

    public MiniAppCrawlerInterceptor(IMiniAppService miniAppService) {
        this.miniAppService = miniAppService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        MiniAppCrawlerContext.clear();
        if (!isCrawlerRequest(request)) {
            return true;
        }

        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String signature = request.getHeader(HEADER_SIGNATURE);
        if (verifyWithAnyEnabledApp(timestamp, nonce, signature)) {
            MiniAppCrawlerContext.markVerified();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        MiniAppCrawlerContext.clear();
    }

    private boolean isCrawlerRequest(HttpServletRequest request) {
        String userAgent = StrUtil.nullToDefault(request.getHeader("User-Agent"), "");
        if (userAgent.toLowerCase().contains(CRAWLER_USER_AGENT)) {
            return true;
        }
        return StrUtil.isAllNotBlank(
                request.getHeader(HEADER_TIMESTAMP),
                request.getHeader(HEADER_NONCE),
                request.getHeader(HEADER_SIGNATURE));
    }

    private boolean verifyWithAnyEnabledApp(String timestamp, String nonce, String signature) {
        QueryWrapper qw = QueryWrapper.create()
                .from("mini_app")
                .where("enabled = 'Y'")
                .and("status = '0'")
                .and("del_flag = '0'")
                .and("token is not null")
                .and("token <> ''");
        for (MiniApp miniApp : miniAppService.list(qw)) {
            if (MiniAppWxCrawlerSignatureUtil.verify(miniApp.getToken(), timestamp, nonce, signature)) {
                return true;
            }
        }
        return false;
    }
}
