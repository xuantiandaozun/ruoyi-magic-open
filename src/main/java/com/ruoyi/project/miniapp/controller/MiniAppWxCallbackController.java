package com.ruoyi.project.miniapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.project.miniapp.service.impl.MiniAppWxCallbackService;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaMessage;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信小程序消息推送回调（用于 mediaCheckAsync 异步结果）
 *
 * 在微信公众平台配置服务器地址，例如：
 * https://your-domain/api/miniapp/wx/callback/yizhou-doc-translate
 */
@Hidden
@Slf4j
@RestController
@RequestMapping("/miniapp/wx/callback")
public class MiniAppWxCallbackController {

    private final MiniAppWxCallbackService callbackService;

    public MiniAppWxCallbackController(MiniAppWxCallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @GetMapping("/{appCode}")
    public String verify(
            @PathVariable String appCode,
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        WxMaService wxMaService = callbackService.resolveService(appCode);
        if (wxMaService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }
        log.warn("小程序回调签名校验失败: appCode={}", appCode);
        return "error";
    }

    @PostMapping("/{appCode}")
    public String receiveMessage(
            @PathVariable String appCode,
            @RequestBody String requestBody,
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "encrypt_type", required = false) String encryptType) {
        WxMaService wxMaService = callbackService.resolveService(appCode);
        if (!wxMaService.checkSignature(timestamp, nonce, signature)) {
            log.warn("小程序消息推送签名校验失败: appCode={}", appCode);
            return "error";
        }

        WxMaMessage message = callbackService.parseMessage(
                appCode, requestBody, timestamp, nonce, msgSignature, encryptType);
        log.info("收到小程序消息推送: appCode={}, event={}, traceId={}",
                appCode, message.getEvent(), message.getTraceId());
        callbackService.handleMessage(message);
        return "success";
    }
}
