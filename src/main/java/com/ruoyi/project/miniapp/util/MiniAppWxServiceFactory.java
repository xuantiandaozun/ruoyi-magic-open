package com.ruoyi.project.miniapp.util;

import org.springframework.stereotype.Component;

import com.ruoyi.project.miniapp.domain.MiniApp;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;

@Component
public class MiniAppWxServiceFactory {

    public WxMaService getService(MiniApp miniApp) {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(miniApp.getAppId());
        config.setSecret(miniApp.getAppSecret());
        config.setToken(miniApp.getToken());
        config.setAesKey(miniApp.getAesKey());

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
