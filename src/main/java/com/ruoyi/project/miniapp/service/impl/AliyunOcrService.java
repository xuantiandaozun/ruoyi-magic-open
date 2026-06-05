package com.ruoyi.project.miniapp.service.impl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeGeneralRequest;
import com.aliyun.ocr_api20210707.models.RecognizeGeneralResponse;
import com.aliyun.ocr_api20210707.models.RecognizeMultiLanguageRequest;
import com.aliyun.ocr_api20210707.models.RecognizeMultiLanguageResponse;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.provider.AliyunCredentialProvider;
import com.ruoyi.framework.aliyun.service.AliyunService;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云 OCR 识别服务（凭证来自 secret_key_info，key_name=ALIYUN_RAM）
 */
@Slf4j
@Service
public class AliyunOcrService {

    private final AliyunService aliyunService;
    private final AliyunCredentialProvider credentialProvider;

    public AliyunOcrService(AliyunService aliyunService, AliyunCredentialProvider credentialProvider) {
        this.aliyunService = aliyunService;
        this.credentialProvider = credentialProvider;
    }

    /**
     * 识别图片中的文字
     *
     * @param imageBytes      图片二进制
     * @param sourceLanguage  源语言（Auto / 中文 / English 等）
     * @return 识别出的纯文本
     */
    public String recognizeText(byte[] imageBytes, String sourceLanguage) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new ServiceException("图片内容为空");
        }
        String normalizedLanguage = StrUtil.blankToDefault(sourceLanguage, "Auto");
        boolean useGeneral = "中文".equals(normalizedLanguage);

        AliyunCredential credential = credentialProvider.getOcrCredential();
        if (credential == null) {
            throw new ServiceException("未配置阿里云 RAM 密钥，请在密钥管理中维护 provider=aliyun 且别名为 ALIYUN_RAM 的 AccessKey");
        }

        return aliyunService.<Client, String>executeWithCredential("OCR", credential, client -> {
            try {
                if (useGeneral) {
                    return recognizeGeneral(client, imageBytes);
                }
                return recognizeMultiLanguage(client, imageBytes, resolveOcrLanguageCodes(normalizedLanguage));
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("阿里云 OCR 识别失败: {}", e.getMessage(), e);
                throw new ServiceException("图片文字识别失败: " + e.getMessage());
            }
        });
    }

    private String recognizeGeneral(Client client, byte[] imageBytes) throws Exception {
        RecognizeGeneralRequest request = new RecognizeGeneralRequest()
                .setBody(new ByteArrayInputStream(imageBytes));
        RecognizeGeneralResponse response = client.recognizeGeneral(request);
        return parseOcrData(response == null || response.getBody() == null ? null : response.getBody().getData());
    }

    private String recognizeMultiLanguage(Client client, byte[] imageBytes, List<String> languages) throws Exception {
        RecognizeMultiLanguageRequest request = new RecognizeMultiLanguageRequest()
                .setBody(new ByteArrayInputStream(imageBytes))
                .setLanguages(languages)
                .setNeedRotate(true);
        RecognizeMultiLanguageResponse response = client.recognizeMultiLanguage(request);
        return parseOcrData(response == null || response.getBody() == null ? null : response.getBody().getData());
    }

    private List<String> resolveOcrLanguageCodes(String sourceLanguage) {
        return switch (sourceLanguage) {
            case "English" -> List.of("eng");
            case "Japanese" -> List.of("ja");
            case "Korean" -> List.of("kor");
            case "French" -> List.of("lading");
            case "Auto" -> List.of("chn", "eng", "ja", "kor", "lading");
            default -> List.of("chn", "eng", "ja", "kor", "lading");
        };
    }

    private String parseOcrData(String data) {
        if (StrUtil.isBlank(data)) {
            return "";
        }
        try {
            JSONObject root = JSON.parseObject(data);
            String content = root.getString("content");
            if (StrUtil.isNotBlank(content)) {
                return normalizeText(content);
            }
            JSONArray words = root.getJSONArray("prism_wordsInfo");
            if (words != null && !words.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < words.size(); i++) {
                    JSONObject word = words.getJSONObject(i);
                    if (word == null) {
                        continue;
                    }
                    String wordText = word.getString("word");
                    if (StrUtil.isNotBlank(wordText)) {
                        if (builder.length() > 0) {
                            builder.append('\n');
                        }
                        builder.append(wordText);
                    }
                }
                return normalizeText(builder.toString());
            }
        } catch (Exception e) {
            log.warn("解析 OCR 返回数据失败，尝试按原文返回: {}", e.getMessage());
            return normalizeText(data);
        }
        return "";
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").trim();
        normalized = normalized.replaceAll("\n{3,}", "\n\n");
        return normalized;
    }
}
