package com.ruoyi.project.miniapp.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ruoyi.project.miniapp.constant.MiniSubscribeSceneCodes;
import com.ruoyi.project.miniapp.domain.MiniApp;
import com.ruoyi.project.miniapp.domain.MiniSubscribeTemplate;
import com.ruoyi.project.miniapp.domain.MiniUserAuth;
import com.ruoyi.project.miniapp.domain.TranslateDocument;
import com.ruoyi.project.miniapp.domain.TranslateTask;
import com.ruoyi.project.miniapp.domain.dto.SubscribeTemplateFieldConfig;
import com.ruoyi.project.miniapp.service.IMiniAppService;
import com.ruoyi.project.miniapp.service.IMiniSubscribeTemplateService;
import com.ruoyi.project.miniapp.service.IMiniUserAuthService;
import com.ruoyi.project.miniapp.util.MiniAppWxServiceFactory;

import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;

@Slf4j
@Service
public class MiniAppSubscribeMessageService {

    private static final int DEFAULT_THING_MAX_LENGTH = 20;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final IMiniAppService miniAppService;
    private final IMiniUserAuthService miniUserAuthService;
    private final IMiniSubscribeTemplateService subscribeTemplateService;
    private final MiniAppWxServiceFactory wxServiceFactory;

    public MiniAppSubscribeMessageService(IMiniAppService miniAppService,
            IMiniUserAuthService miniUserAuthService,
            IMiniSubscribeTemplateService subscribeTemplateService,
            MiniAppWxServiceFactory wxServiceFactory) {
        this.miniAppService = miniAppService;
        this.miniUserAuthService = miniUserAuthService;
        this.subscribeTemplateService = subscribeTemplateService;
        this.wxServiceFactory = wxServiceFactory;
    }

    public void sendTranslateTaskCompleteNotice(TranslateTask task, TranslateDocument document) {
        if (task == null || document == null) {
            return;
        }

        Map<String, Object> context = new HashMap<>();
        context.put("task", task);
        context.put("document", document);
        sendByScene(task.getMiniAppId(), task.getMiniUserId(), MiniSubscribeSceneCodes.TRANSLATE_TASK_COMPLETE, context);
    }

    public void sendByScene(Long miniAppId, Long miniUserId, String sceneCode, Map<String, Object> context) {
        if (miniAppId == null || miniUserId == null || StrUtil.isBlank(sceneCode)) {
            return;
        }

        try {
            MiniApp miniApp = miniAppService.getById(miniAppId);
            if (miniApp == null || !"0".equals(miniApp.getDelFlag())) {
                log.warn("订阅消息跳过：小程序不存在, miniAppId={}, sceneCode={}", miniAppId, sceneCode);
                return;
            }

            MiniUserAuth auth = miniUserAuthService.getByMiniUserAndApp(miniUserId, miniAppId);
            if (auth == null || StrUtil.isBlank(auth.getOpenid())) {
                log.warn("订阅消息跳过：未找到 openid, miniAppId={}, sceneCode={}", miniAppId, sceneCode);
                return;
            }

            List<MiniSubscribeTemplate> templates = subscribeTemplateService.listEnabledByMiniAppAndScene(miniAppId,
                    sceneCode);
            if (templates.isEmpty()) {
                log.warn("订阅消息跳过：未配置模板, miniAppId={}, sceneCode={}", miniAppId, sceneCode);
                return;
            }

            MiniSubscribeTemplate template = templates.get(0);
            WxMaSubscribeMessage message = buildMessage(auth.getOpenid(), template, context);
            wxServiceFactory.getService(miniApp).getSubscribeService().sendSubscribeMsg(message);
            log.info("订阅消息已发送, miniAppId={}, sceneCode={}, templateId={}, openid={}",
                    miniAppId, sceneCode, template.getTemplateId(), auth.getOpenid());
        } catch (WxErrorException e) {
            log.warn("订阅消息发送失败, miniAppId={}, sceneCode={}, errCode={}, errMsg={}",
                    miniAppId, sceneCode, e.getError().getErrorCode(), e.getError().getErrorMsg());
        } catch (Exception e) {
            log.error("订阅消息发送异常, miniAppId={}, sceneCode={}", miniAppId, sceneCode, e);
        }
    }

    private WxMaSubscribeMessage buildMessage(String openid, MiniSubscribeTemplate template,
            Map<String, Object> context) {
        WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
                .toUser(openid)
                .templateId(template.getTemplateId())
                .page(template.getPagePath())
                .build();

        List<SubscribeTemplateFieldConfig> fieldConfigs = parseFieldConfigs(template.getFieldConfigJson());
        for (SubscribeTemplateFieldConfig fieldConfig : fieldConfigs) {
            if (fieldConfig == null || StrUtil.isBlank(fieldConfig.getFieldName())) {
                continue;
            }
            String value = resolveFieldValue(fieldConfig, context);
            message.addData(new WxMaSubscribeMessage.MsgData(fieldConfig.getFieldName(), value));
        }
        return message;
    }

    private List<SubscribeTemplateFieldConfig> parseFieldConfigs(String fieldConfigJson) {
        if (StrUtil.isBlank(fieldConfigJson)) {
            return List.of();
        }
        return JSON.parseObject(fieldConfigJson, new TypeReference<List<SubscribeTemplateFieldConfig>>() {
        });
    }

    private String resolveFieldValue(SubscribeTemplateFieldConfig fieldConfig, Map<String, Object> context) {
        String valueExpr = StrUtil.nullToDefault(fieldConfig.getValueExpr(), "");
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(valueExpr);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object value = resolvePath(context, matcher.group(1).trim());
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(buffer);

        String result = buffer.toString();
        if (StrUtil.isBlank(result) && StrUtil.isNotBlank(fieldConfig.getDefaultValue())) {
            result = fieldConfig.getDefaultValue();
        }

        int maxLength = fieldConfig.getMaxLength() != null && fieldConfig.getMaxLength() > 0
                ? fieldConfig.getMaxLength()
                : DEFAULT_THING_MAX_LENGTH;
        return StrUtil.maxLength(result, maxLength);
    }

    private Object resolvePath(Map<String, Object> context, String path) {
        int dotIndex = path.indexOf('.');
        if (dotIndex <= 0 || dotIndex >= path.length() - 1) {
            return null;
        }

        String rootKey = path.substring(0, dotIndex);
        String propertyPath = path.substring(dotIndex + 1);
        Object rootObject = context.get(rootKey);
        if (rootObject == null) {
            return null;
        }
        return BeanUtil.getProperty(rootObject, propertyPath);
    }
}
