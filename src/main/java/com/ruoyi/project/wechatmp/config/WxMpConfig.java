package com.ruoyi.project.wechatmp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ruoyi.project.ai.service.IAiService;

import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;

@Configuration
@ConditionalOnProperty(prefix = "wechat.mp", name = "enabled", havingValue = "true")
public class WxMpConfig {

    @Bean
    public WxMpService wxMpService(WechatMpProperties properties) {
        WxMpServiceImpl wxMpService = new WxMpServiceImpl();
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(properties.getAppId());
        config.setSecret(properties.getAppSecret());
        config.setToken(properties.getToken());
        config.setAesKey(properties.getAesKey());
        wxMpService.setWxMpConfigStorage(config);
        return wxMpService;
    }

    @Bean
    public WechatMpMessageHandler wechatMpMessageHandler(
            IAiService aiService,
            WechatMpProperties properties) {
        return new WechatMpMessageHandler(aiService, properties);
    }

    @Bean
    public WxMpMessageRouter wxMpMessageRouter(
            WxMpService wxMpService,
            WechatMpMessageHandler messageHandler) {
        WxMpMessageRouter router = new WxMpMessageRouter(wxMpService);
        router.rule()
                .handler(messageHandler)
                .end();
        return router;
    }
}
