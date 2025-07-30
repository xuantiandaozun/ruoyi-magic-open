package com.ruoyi.framework.config.magic;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.script.MagicScriptContext;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;

/**
 * magic-api 接口鉴权拦截器，与sa-token打通
 */
@Component
public class CustomRequestInterceptor implements RequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(CustomRequestInterceptor.class);

    /**
     * 接口请求之前
     */
    @Override
    public Object preHandle(ApiInfo info, MagicScriptContext context, MagicHttpServletRequest request, MagicHttpServletResponse response) throws Exception {
        String apiName = info.getName();
        logger.info("请求接口：{}", apiName);

        // 检查是否为匿名接口
        String anonymous = info.getOptionValue(Options.ANONYMOUS);
        if ("true".equals(anonymous)) {
            logger.info("接口 {} 为匿名接口，跳过权限验证", apiName);
            return null;
        }

        // 检查是否需要登录
        String requireLogin = info.getOptionValue(Options.REQUIRE_LOGIN);
        if ("true".equals(requireLogin)||StrUtil.isBlank(requireLogin)) {
            StpUtil.checkLogin();
            logger.info("接口 {} 需要登录，当前用户ID：{}", apiName, StpUtil.getLoginId());
        }

        // 检查角色权限
        String role = info.getOptionValue(Options.ROLE);
        if (role != null && !role.trim().isEmpty()) {
            StpUtil.checkRole(role);
            logger.info("接口 {} 需要角色：{}，权限验证通过", apiName, role);
        }

        // 检查具体权限
        String permission = info.getOptionValue(Options.PERMISSION);
        if (permission != null && !permission.trim().isEmpty()) {
            StpUtil.checkPermission(permission);
            logger.info("接口 {} 需要权限：{}，权限验证通过", apiName, permission);
        }

        return null; // 返回null继续执行后续操作
    }

    /**
     * 接口执行之后
     */
    @Override
    public Object postHandle(ApiInfo info, MagicScriptContext context, Object value, MagicHttpServletRequest request, MagicHttpServletResponse response) throws Exception {
        logger.info("接口 {} 执行完毕", info.getName());
        return null; // 返回null继续执行后续拦截器
    }

    /**
     * 接口执行完毕之后执行（包括异常情况）
     */
    @Override
    public void afterCompletion(ApiInfo info, MagicScriptContext context, Object returnValue, MagicHttpServletRequest request, MagicHttpServletResponse response, Throwable throwable) {
        if (throwable != null) {
            logger.error("接口 {} 执行异常", info.getName(), throwable);
        } else {
            logger.debug("接口 {} 执行完成", info.getName());
        }
    }
}