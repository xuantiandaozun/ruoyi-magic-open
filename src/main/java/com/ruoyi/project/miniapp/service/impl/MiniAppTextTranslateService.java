package com.ruoyi.project.miniapp.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.domain.AiModelRoute;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiModelRouteService;
import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;
import com.ruoyi.project.miniapp.domain.dto.TranslateTextRequest;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MiniAppTextTranslateService {

    private static final String PRODUCT_TYPE = "miniapp";
    private static final String SCENE_CODE = "text_translate";
    private static final String FALLBACK_SCENE_CODE = "doc_translate";

    private final LangChain4jAgentService langChain4jAgentService;
    private final IAiModelRouteService modelRouteService;
    private final IAiModelConfigService modelConfigService;

    public MiniAppTextTranslateService(LangChain4jAgentService langChain4jAgentService,
            IAiModelRouteService modelRouteService,
            IAiModelConfigService modelConfigService) {
        this.langChain4jAgentService = langChain4jAgentService;
        this.modelRouteService = modelRouteService;
        this.modelConfigService = modelConfigService;
    }

    public Map<String, String> translateText(TranslateTextRequest request) {
        AiModelConfig modelConfig = resolveModelConfig();
        if (modelConfig == null) {
            throw new ServiceException("未配置可用的翻译模型");
        }

        String sourceLanguage = StrUtil.blankToDefault(request.getSourceLanguage(), "Auto");
        String systemPrompt = buildSystemPrompt(sourceLanguage, request.getTargetLanguage());
        String translatedText = langChain4jAgentService.chatWithSystem(
                modelConfig.getId(), systemPrompt, request.getText());
        translatedText = normalizeText(translatedText);

        Map<String, String> result = new HashMap<>();
        result.put("translatedText", translatedText);
        if ("Auto".equalsIgnoreCase(sourceLanguage)) {
            result.put("detectedSourceLanguage", "");
        }
        return result;
    }

    private String buildSystemPrompt(String sourceLanguage, String targetLanguage) {
        String srcDesc = "Auto".equalsIgnoreCase(sourceLanguage) ? "自动识别源语言" : sourceLanguage;
        return "你是专业文本翻译引擎。请将用户提供的文本翻译为目标语言。" +
                "要求：1. 仅输出翻译后的纯文本内容，禁止使用代码块包裹（不要用 ``` 包裹输出）；" +
                "2. 保留原有换行和格式；" +
                "3. 不要新增原文没有的内容，不要添加任何说明或注释；" +
                "4. 专有名词前后一致；" +
                "5. 本次源语言为" + srcDesc + "，目标语言为" + targetLanguage + "。";
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String result = text.trim();
        if (result.startsWith("```")) {
            int firstNewline = result.indexOf('\n');
            if (firstNewline > 0) {
                result = result.substring(firstNewline + 1);
            } else {
                result = result.substring(3);
            }
            if (result.endsWith("```")) {
                result = result.substring(0, result.length() - 3);
            }
            result = result.trim();
        }
        return result;
    }

    private AiModelConfig resolveModelConfig() {
        AiModelRoute route = findRoute(SCENE_CODE);
        if (route == null) {
            route = findRoute(FALLBACK_SCENE_CODE);
        }
        if (route != null && route.getPrimaryModelConfigId() != null) {
            AiModelConfig config = modelConfigService.getById(route.getPrimaryModelConfigId());
            if (config != null && "Y".equals(config.getEnabled()) && "0".equals(config.getStatus())) {
                return config;
            }
        }

        AiModelConfig fallback = modelConfigService.getEnabledByModel("deepseek-v4-flash");
        if (fallback != null) {
            return fallback;
        }

        List<AiModelConfig> configs = modelConfigService.listEnabledByProviderAndCapability("deepseek", "chat");
        return configs.isEmpty() ? null : configs.get(0);
    }

    private AiModelRoute findRoute(String sceneCode) {
        QueryWrapper qw = QueryWrapper.create()
                .from("ai_model_route")
                .where("product_type = ?", PRODUCT_TYPE)
                .and("scene_code = ?", sceneCode)
                .and("enabled = 'Y'")
                .and("del_flag = '0'")
                .limit(1);
        return modelRouteService.getOne(qw);
    }
}
