package com.ruoyi.project.miniapp.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.miniapp.domain.dto.TranslateTextRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.util.BillImageOptimizer;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 图片翻译：阿里云 OCR 识文 + 现有文本翻译
 */
@Slf4j
@Service
public class MiniAppImageTranslateService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_OCR_CHARS = 8000;
    private static final int TRANSLATE_CHUNK_SIZE = 1800;

    private final AliyunOcrService aliyunOcrService;
    private final MiniAppTextTranslateService textTranslateService;
    private final MiniAppContentSecurityService contentSecurityService;

    public MiniAppImageTranslateService(AliyunOcrService aliyunOcrService,
            MiniAppTextTranslateService textTranslateService,
            MiniAppContentSecurityService contentSecurityService) {
        this.aliyunOcrService = aliyunOcrService;
        this.textTranslateService = textTranslateService;
        this.contentSecurityService = contentSecurityService;
    }

    public Map<String, String> translateImage(MultipartFile file, String sourceLanguage, String targetLanguage) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ServiceException("图片大小不能超过5MB");
        }
        if (StrUtil.isBlank(targetLanguage)) {
            throw new ServiceException("目标语言不能为空");
        }

        String normalizedSource = StrUtil.blankToDefault(sourceLanguage, "Auto");
        long startMs = System.currentTimeMillis();
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();

        byte[] imageBytes = readImageBytes(file);
        contentSecurityService.checkImageContent(
                loginUser,
                imageBytes,
                resolveImageMimeType(file),
                file.getOriginalFilename());

        String ocrText = aliyunOcrService.recognizeText(imageBytes, normalizedSource);
        if (StrUtil.isBlank(ocrText)) {
            throw new ServiceException("未识别到文字，请换一张更清晰的图片");
        }

        boolean truncated = false;
        if (ocrText.length() > MAX_OCR_CHARS) {
            ocrText = ocrText.substring(0, MAX_OCR_CHARS);
            truncated = true;
        }

        String translatedText = translateInChunks(ocrText, normalizedSource, targetLanguage);

        Map<String, String> result = new HashMap<>();
        result.put("sourceText", ocrText);
        result.put("translatedText", translatedText);
        if (truncated) {
            result.put("warning", "识别文字过长，已截断部分内容后翻译");
        }
        log.info("图片翻译完成: ocrLength={}, translatedLength={}, costMs={}",
                ocrText.length(), translatedText.length(), System.currentTimeMillis() - startMs);
        return result;
    }

    private byte[] readImageBytes(MultipartFile file) {
        String mimeType = resolveImageMimeType(file);
        try {
            return BillImageOptimizer.optimize(file.getBytes(), mimeType).getBytes();
        } catch (Exception e) {
            log.warn("图片预处理失败，回退原图: {}", e.getMessage());
            try {
                return file.getBytes();
            } catch (Exception ex) {
                throw new ServiceException("读取图片失败");
            }
        }
    }

    private String translateInChunks(String text, String sourceLanguage, String targetLanguage) {
        if (text.length() <= TRANSLATE_CHUNK_SIZE) {
            return translateChunk(text, sourceLanguage, targetLanguage);
        }
        List<String> chunks = splitText(text, TRANSLATE_CHUNK_SIZE);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            String part = translateChunk(chunks.get(i), sourceLanguage, targetLanguage);
            if (StrUtil.isNotBlank(part)) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append(part);
            }
        }
        return builder.toString();
    }

    private String translateChunk(String text, String sourceLanguage, String targetLanguage) {
        TranslateTextRequest request = new TranslateTextRequest();
        request.setText(text);
        request.setSourceLanguage(sourceLanguage);
        request.setTargetLanguage(targetLanguage);
        Map<String, String> result = textTranslateService.translateText(request);
        return StrUtil.blankToDefault(result.get("translatedText"), "");
    }

    private List<String> splitText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            if (end < text.length()) {
                int breakAt = text.lastIndexOf('\n', end);
                if (breakAt > start + chunkSize / 2) {
                    end = breakAt + 1;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    private String resolveImageMimeType(MultipartFile file) {
        String contentType = StrUtil.blankToDefault(file.getContentType(), "");
        if (contentType.startsWith("image/")) {
            return contentType;
        }
        String filename = StrUtil.blankToDefault(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename.endsWith(".webp")) {
            return "image/webp";
        }
        if (filename.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }
}
