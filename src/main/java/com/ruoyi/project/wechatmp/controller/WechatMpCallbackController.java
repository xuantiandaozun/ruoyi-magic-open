package com.ruoyi.project.wechatmp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

@Slf4j
@RestController
@RequestMapping("/wechat/mp")
@ConditionalOnProperty(prefix = "wechat.mp", name = "enabled", havingValue = "true")
public class WechatMpCallbackController {

    @Autowired
    private WxMpService wxMpService;

    @Autowired
    private WxMpMessageRouter wxMpMessageRouter;

    @GetMapping("/callback")
    public String verify(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        log.info("微信公众号服务器验证: timestamp={}, nonce={}", timestamp, nonce);
        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }
        log.warn("微信公众号签名验证失败");
        return "error";
    }

    @PostMapping("/callback")
    public String receiveMessage(
            @RequestBody String requestBody,
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "encrypt_type", required = false) String encryptType) {
        log.info("收到公众号消息推送: encrypt_type={}", encryptType);

        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            log.warn("公众号消息签名验证失败");
            return "error";
        }

        WxMpXmlMessage inMessage;
        if ("aes".equals(encryptType)) {
            inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody,
                    wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
        } else {
            inMessage = WxMpXmlMessage.fromXml(requestBody);
        }

        log.info("公众号消息解析完成: msgType={}, fromUser={}", inMessage.getMsgType(), inMessage.getFromUser());

        WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
        if (outMessage == null) {
            return "";
        }

        if ("aes".equals(encryptType)) {
            return outMessage.toEncryptedXml(wxMpService.getWxMpConfigStorage());
        }
        return outMessage.toXml();
    }
}
