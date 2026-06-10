package com.ruoyi.project.miniapp.service.impl;

import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.miniapp.domain.MiniApp;
import com.ruoyi.project.miniapp.domain.vo.MiniAppMediaCheckResult;
import com.ruoyi.project.miniapp.service.IMiniAppService;
import com.ruoyi.project.miniapp.util.MiniAppWxServiceFactory;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaMessage;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理微信小程序消息推送（含 mediaCheckAsync 异步结果）
 */
@Slf4j
@Service
public class MiniAppWxCallbackService {

    private static final String EVENT_MEDIA_CHECK = "wxa_media_check";

    private final IMiniAppService miniAppService;
    private final MiniAppWxServiceFactory wxServiceFactory;
    private final MiniAppMediaCheckResultStore mediaCheckResultStore;

    public MiniAppWxCallbackService(IMiniAppService miniAppService,
            MiniAppWxServiceFactory wxServiceFactory,
            MiniAppMediaCheckResultStore mediaCheckResultStore) {
        this.miniAppService = miniAppService;
        this.wxServiceFactory = wxServiceFactory;
        this.mediaCheckResultStore = mediaCheckResultStore;
    }

    public WxMaService resolveService(String appCode) {
        MiniApp miniApp = miniAppService.getEnabledByAppCode(appCode);
        if (miniApp == null) {
            throw new ServiceException("小程序应用不存在或已停用");
        }
        return wxServiceFactory.getService(miniApp);
    }

    public WxMaMessage parseMessage(String appCode, String requestBody, String timestamp, String nonce,
            String msgSignature, String encryptType) {
        WxMaService wxMaService = resolveService(appCode);
        if ("aes".equalsIgnoreCase(encryptType)) {
            return WxMaMessage.fromEncryptedXml(
                    requestBody,
                    wxMaService.getWxMaConfig(),
                    timestamp,
                    nonce,
                    msgSignature);
        }
        return WxMaMessage.fromXml(requestBody);
    }

    public void handleMessage(WxMaMessage message) {
        if (message == null || !EVENT_MEDIA_CHECK.equals(message.getEvent())) {
            return;
        }

        MiniAppMediaCheckResult result = new MiniAppMediaCheckResult();
        result.setTraceId(message.getTraceId());
        if (message.getAllFieldsMap() != null) {
            Object errcode = message.getAllFieldsMap().get("errcode");
            Object errmsg = message.getAllFieldsMap().get("errmsg");
            if (errcode instanceof Number number) {
                result.setErrcode(number.intValue());
            }
            if (errmsg != null) {
                result.setErrmsg(String.valueOf(errmsg));
            }
        }
        if (message.getResult() != null) {
            result.setSuggest(message.getResult().getSuggest());
            result.setLabel(message.getResult().getLabel());
        }
        mediaCheckResultStore.save(result);
        log.info("收到图片异步安全检测结果: traceId={}, suggest={}, errcode={}",
                result.getTraceId(), result.getSuggest(), result.getErrcode());
    }
}
