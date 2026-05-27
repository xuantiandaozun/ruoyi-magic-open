package com.ruoyi.project.miniapp.util;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.session.SaSession;

public class MiniAppStpUtil {
    public static final String LOGIN_USER_KEY = "miniapp_login_user";

    private static final StpLogic STP_LOGIC = new StpLogic("miniapp");

    private MiniAppStpUtil() {
    }

    public static void login(Object id) {
        STP_LOGIC.login(id);
    }

    public static boolean isLogin() {
        return STP_LOGIC.isLogin();
    }

    public static void checkLogin() {
        STP_LOGIC.checkLogin();
    }

    public static Long getLoginIdAsLong() {
        return STP_LOGIC.getLoginIdAsLong();
    }

    public static String getTokenValue() {
        return STP_LOGIC.getTokenValue();
    }

    public static SaSession getSession() {
        return STP_LOGIC.getSession();
    }

    public static void logout() {
        STP_LOGIC.logout();
    }
}
