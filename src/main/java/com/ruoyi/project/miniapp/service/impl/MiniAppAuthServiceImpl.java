package com.ruoyi.project.miniapp.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.miniapp.domain.MiniApp;
import com.ruoyi.project.miniapp.domain.MiniUser;
import com.ruoyi.project.miniapp.domain.MiniUserAuth;
import com.ruoyi.project.miniapp.domain.dto.MiniAppLoginRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.service.IMiniAppAuthService;
import com.ruoyi.project.miniapp.service.IMiniAppService;
import com.ruoyi.project.miniapp.service.IMiniUserAuthService;
import com.ruoyi.project.miniapp.service.IMiniUserService;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;
import com.ruoyi.project.miniapp.util.MiniAppStpUtil;
import com.ruoyi.project.miniapp.util.MiniAppWxServiceFactory;

import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import me.chanjar.weixin.common.error.WxErrorException;

@Service
public class MiniAppAuthServiceImpl implements IMiniAppAuthService {

    private final IMiniAppService miniAppService;
    private final IMiniUserService miniUserService;
    private final IMiniUserAuthService miniUserAuthService;
    private final MiniAppWxServiceFactory wxServiceFactory;

    public MiniAppAuthServiceImpl(IMiniAppService miniAppService,
            IMiniUserService miniUserService,
            IMiniUserAuthService miniUserAuthService,
            MiniAppWxServiceFactory wxServiceFactory) {
        this.miniAppService = miniAppService;
        this.miniUserService = miniUserService;
        this.miniUserAuthService = miniUserAuthService;
        this.wxServiceFactory = wxServiceFactory;
    }

    @Override
    @Transactional
    public Map<String, Object> login(MiniAppLoginRequest request) {
        MiniApp miniApp = miniAppService.getEnabledByAppCode(request.getAppCode());
        if (miniApp == null) {
            throw new ServiceException("小程序应用不存在或已停用");
        }

        WxMaJscode2SessionResult sessionResult;
        try {
            sessionResult = wxServiceFactory.getService(miniApp).getUserService().getSessionInfo(request.getCode());
        } catch (WxErrorException e) {
            throw new ServiceException("微信登录失败: " + e.getError().getErrorMsg());
        }

        if (sessionResult == null || sessionResult.getOpenid() == null) {
            throw new ServiceException("微信登录失败，未获取到openid");
        }

        Date now = new Date();
        MiniUserAuth auth = miniUserAuthService.getByMiniAppAndOpenid(miniApp.getId(), sessionResult.getOpenid());
        boolean isNewUser = false;
        MiniUser miniUser;

        if (auth == null) {
            miniUser = new MiniUser();
            miniUser.setNickname("微信用户");
            miniUser.setSourceAppCode(miniApp.getAppCode());
            miniUser.setStatus("0");
            miniUser.setDelFlag("0");
            miniUserService.save(miniUser);

            auth = new MiniUserAuth();
            auth.setMiniUserId(miniUser.getId());
            auth.setMiniAppId(miniApp.getId());
            auth.setPlatform(miniApp.getPlatform());
            auth.setOpenid(sessionResult.getOpenid());
            auth.setUnionid(sessionResult.getUnionid());
            auth.setSessionKey(sessionResult.getSessionKey());
            auth.setRawJson(JSON.toJSONString(sessionResult));
            auth.setBindTime(now);
            auth.setLastLoginTime(now);
            auth.setStatus("0");
            auth.setDelFlag("0");
            miniUserAuthService.save(auth);
            isNewUser = true;
        } else {
            miniUser = miniUserService.getById(auth.getMiniUserId());
            auth.setUnionid(sessionResult.getUnionid());
            auth.setSessionKey(sessionResult.getSessionKey());
            auth.setRawJson(JSON.toJSONString(sessionResult));
            auth.setLastLoginTime(now);
            auth.setStatus("0");
            auth.setDelFlag("0");
            miniUserAuthService.updateById(auth);
        }

        miniUser.setLastLoginTime(now);
        miniUserService.updateById(miniUser);

        MiniAppStpUtil.login(miniUser.getId());
        MiniAppLoginUser loginUser = new MiniAppLoginUser(miniUser.getId(), miniApp.getId(), miniApp.getAppCode(),
                auth.getOpenid());
        MiniAppStpUtil.getSession().set(MiniAppStpUtil.LOGIN_USER_KEY, loginUser);

        Map<String, Object> data = new HashMap<>();
        data.put("token", MiniAppStpUtil.getTokenValue());
        data.put("isNewUser", isNewUser);
        data.put("miniUserId", miniUser.getId());
        data.put("appCode", miniApp.getAppCode());
        data.put("userInfo", miniUser);
        return data;
    }

    @Override
    public Map<String, Object> currentUser(MiniAppLoginUser loginUser) {
        MiniUser miniUser = miniUserService.getById(loginUser.getMiniUserId());
        if (miniUser == null) {
            throw new ServiceException("用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("miniUserId", miniUser.getId());
        data.put("appCode", loginUser.getAppCode());
        data.put("openid", loginUser.getOpenid());
        data.put("userInfo", miniUser);
        return data;
    }

    @Override
    public void logout() {
        MiniAppSecurityUtils.getLoginUser();
        MiniAppStpUtil.logout();
    }
}
