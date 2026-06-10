package com.ruoyi.project.wechatmp.config;

import java.util.Map;

import com.ruoyi.project.ai.service.IAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutTextMessage;

@Slf4j
@RequiredArgsConstructor
public class WechatMpMessageHandler implements WxMpMessageHandler {

    private final IAiService aiService;
    private final WechatMpProperties properties;

    @Override
    public WxMpXmlOutMessage handle(
            WxMpXmlMessage wxMessage,
            Map<String, Object> context,
            WxMpService wxMpService,
            WxSessionManager sessionManager) {

        String msgType = wxMessage.getMsgType();
        String fromUser = wxMessage.getFromUser();
        log.info("收到公众号消息: type={}, from={}", msgType, fromUser);

        String replyContent;
        try {
            switch (msgType) {
                case "text":
                    replyContent = handleTextMessage(wxMessage.getContent());
                    break;
                case "image":
                    replyContent = "收到图片，暂无法识别图片内容，请用文字描述您的问题。";
                    break;
                case "voice":
                    replyContent = "收到语音消息，暂不支持语音识别，请用文字发送您的问题。";
                    break;
                case "video":
                case "shortvideo":
                    replyContent = "收到视频消息，暂不支持处理，请用文字描述您的问题。";
                    break;
                case "location":
                    replyContent = "收到位置信息，暂不支持处理，请用文字描述您的问题。";
                    break;
                case "link":
                    replyContent = "收到链接消息，暂不支持处理，请用文字描述您的问题。";
                    break;
                case "event":
                    replyContent = handleEvent(wxMessage);
                    if (replyContent == null) {
                        return null;
                    }
                    break;
                default:
                    replyContent = "暂不支持此消息类型，请用文字描述您的问题。";
                    break;
            }
        } catch (Exception e) {
            log.error("处理公众号消息异常: from={}, type={}", fromUser, msgType, e);
            replyContent = "处理消息时出现异常，请稍后重试。";
        }

        if (replyContent == null || replyContent.isBlank()) {
            return null;
        }

        WxMpXmlOutTextMessage textMessage = WxMpXmlOutMessage
                .TEXT()
                .content(replyContent)
                .fromUser(wxMessage.getToUser())
                .toUser(fromUser)
                .build();
        return textMessage;
    }

    private String handleTextMessage(String content) {
        if (content == null || content.isBlank()) {
            return "请输入您的问题。";
        }
        Long modelConfigId = properties.getAiModelConfigId();
        String systemPrompt = properties.getAiSystemPrompt();
        return aiService.chatWithModelConfig(content, systemPrompt, modelConfigId);
    }

    private String handleEvent(WxMpXmlMessage wxMessage) {
        String event = wxMessage.getEvent();
        if ("subscribe".equalsIgnoreCase(event)) {
            return "欢迎关注！我是AI智能助手，有任何问题请直接发送文字，我会尽快为您解答。";
        }
        return null;
    }
}
