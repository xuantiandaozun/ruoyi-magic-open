package com.ruoyi.project.miniapp.util;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;

public class MiniAppSecurityUtils {

    private MiniAppSecurityUtils() {
    }

    public static MiniAppLoginUser getLoginUser() {
        if (!MiniAppStpUtil.isLogin()) {
            throw new ServiceException("用户未登录", HttpStatus.UNAUTHORIZED);
        }
        Object value = MiniAppStpUtil.getSession().get(MiniAppStpUtil.LOGIN_USER_KEY);
        MiniAppLoginUser loginUser = JSON.to(MiniAppLoginUser.class, value);
        if (loginUser == null) {
            throw new ServiceException("获取小程序用户信息失败", HttpStatus.UNAUTHORIZED);
        }
        return loginUser;
    }
}
