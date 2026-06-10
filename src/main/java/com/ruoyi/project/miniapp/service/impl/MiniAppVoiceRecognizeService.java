package com.ruoyi.project.miniapp.service.impl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MiniAppVoiceRecognizeService {

    private static final long MAX_AUDIO_SIZE = 2 * 1024 * 1024;

    private final AliyunNlsService aliyunNlsService;
    private final MiniAppContentSecurityService contentSecurityService;

    public MiniAppVoiceRecognizeService(AliyunNlsService aliyunNlsService,
            MiniAppContentSecurityService contentSecurityService) {
        this.aliyunNlsService = aliyunNlsService;
        this.contentSecurityService = contentSecurityService;
    }

    public Map<String, String> recognizeVoice(MultipartFile file, String format, Integer sampleRate) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传语音文件");
        }
        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw new ServiceException("语音文件不能超过2MB");
        }

        String normalizedFormat = resolveFormat(file, format);
        byte[] audioBytes = readAudioBytes(file);
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        contentSecurityService.checkSocialAudio(
                loginUser,
                audioBytes,
                resolveAudioMimeType(normalizedFormat),
                file.getOriginalFilename());

        long startMs = System.currentTimeMillis();
        String recognizedText = aliyunNlsService.recognizeSpeech(audioBytes, normalizedFormat, sampleRate);
        contentSecurityService.checkSocialText(loginUser, recognizedText);

        Map<String, String> result = new HashMap<>();
        result.put("recognizedText", recognizedText);
        log.info("语音录入识别完成: format={}, size={}, textLength={}, costMs={}",
                normalizedFormat, audioBytes.length, recognizedText.length(), System.currentTimeMillis() - startMs);
        return result;
    }

    private byte[] readAudioBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new ServiceException("读取语音文件失败");
        }
    }

    private String resolveFormat(MultipartFile file, String format) {
        if (StrUtil.isNotBlank(format)) {
            return format.trim().toLowerCase(Locale.ROOT);
        }
        String filename = file.getOriginalFilename();
        if (StrUtil.isNotBlank(filename) && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        String contentType = StrUtil.blankToDefault(file.getContentType(), "").toLowerCase(Locale.ROOT);
        if (contentType.contains("mpeg") || contentType.contains("mp3")) {
            return "mp3";
        }
        if (contentType.contains("aac")) {
            return "aac";
        }
        if (contentType.contains("wav")) {
            return "wav";
        }
        return "mp3";
    }

    private String resolveAudioMimeType(String format) {
        return switch (StrUtil.blankToDefault(format, "mp3").toLowerCase(Locale.ROOT)) {
            case "aac" -> "audio/aac";
            case "wav" -> "audio/wav";
            default -> "audio/mpeg";
        };
    }
}
