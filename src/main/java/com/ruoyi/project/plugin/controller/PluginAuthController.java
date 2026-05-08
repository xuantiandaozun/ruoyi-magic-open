package com.ruoyi.project.plugin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.ai.service.IAiQuotaCheckService;
import com.ruoyi.project.system.domain.dto.GoogleOAuthLoginResult;
import com.ruoyi.project.system.service.IGoogleOAuthService;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 插件用户认证接口（无需已登录状态）
 * <p>
 * 所有接口均不需要系统 RBAC 权限，Sa-Token 白名单中需放行 /plugin/auth/**
 */
@Slf4j
@Tag(name = "插件-认证")
@RestController
@RequestMapping("/plugin/auth")
public class PluginAuthController {

    private static final String PRODUCT_TYPE_PLUGIN = "plugin";
    private static final String USER_TIER_FREE = "free";

    @Autowired
    private IGoogleOAuthService googleOAuthService;

    @Autowired
    private IAiQuotaCheckService quotaCheckService;

    /**
     * Google OAuth 登录 / 注册
     * <p>
     * 插件前端通过 Google Identity Services 获取 id_token（credential），
     * 直接 POST 到此接口完成登录。
     *
     * @param body {"idToken": "..."}
     */
    @Operation(summary = "Google 登录")
    @PostMapping("/google/login")
    public AjaxResult googleLogin(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (StrUtil.isBlank(idToken)) {
            return AjaxResult.error("idToken 不能为空");
        }
        GoogleOAuthLoginResult result = googleOAuthService.loginWithIdToken(idToken);
        return AjaxResult.success(result);
    }

    /**
     * 获取当前登录用户信息及配额状态（需已登录）
     */
    @Operation(summary = "获取用户信息及配额")
    @GetMapping("/userinfo")
    public AjaxResult userinfo() {
        if (!StpUtil.isLogin()) {
            return AjaxResult.error(401, "未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        IAiQuotaCheckService.QuotaUsageInfo usage =
                quotaCheckService.getUsageInfo(userId, USER_TIER_FREE, PRODUCT_TYPE_PLUGIN);

        Map<String, Object> data = Map.of(
                "userId", userId,
                "userTier", USER_TIER_FREE,
                "todayUsedRequests", usage.getTodayUsedRequests(),
                "todayRequestLimit", usage.getRequestLimit(),
                "remainingRequests", usage.getRemainingRequests(),
                "todayUsedTokens", usage.getTodayUsedTokens(),
                "tokenLimit", usage.getTokenLimit()
        );
        return AjaxResult.success(data);
    }

    /**
     * 查询当前用户今日配额使用情况（需已登录）
     * <p>
     * 插件可轮询此接口刷新显示剩余次数，无副作用（不消耗配额）。
     */
    @Operation(summary = "查询今日配额使用情况")
    @GetMapping("/quota")
    public AjaxResult quota() {
        if (!StpUtil.isLogin()) {
            return AjaxResult.error(401, "未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        IAiQuotaCheckService.QuotaUsageInfo usage =
                quotaCheckService.getUsageInfo(userId, USER_TIER_FREE, PRODUCT_TYPE_PLUGIN);

        Map<String, Object> data = Map.of(
                "todayUsedRequests", usage.getTodayUsedRequests(),
                "requestLimit", usage.getRequestLimit(),
                "remainingRequests", usage.getRemainingRequests(),
                "todayUsedTokens", usage.getTodayUsedTokens(),
                "tokenLimit", usage.getTokenLimit(),
                "remainingTokens", Math.max(0L, usage.getTokenLimit() - usage.getTodayUsedTokens())
        );
        return AjaxResult.success(data);
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public AjaxResult logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return AjaxResult.success("已退出登录");
    }
}
