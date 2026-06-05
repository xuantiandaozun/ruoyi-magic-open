package com.ruoyi.project.miniapp.service.impl;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.config.AliyunNlsConfig;
import com.ruoyi.framework.aliyun.provider.AliyunCredentialProvider;
import com.ruoyi.framework.aliyun.util.AliyunNlsTokenClient;
import com.ruoyi.framework.aliyun.util.AliyunNlsTokenClient.TokenResult;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云智能语音：一句话识别
 */
@Slf4j
@Service
public class AliyunNlsService {

    private static final int SUCCESS_STATUS = 20000000;
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 120;

    private final AliyunCredentialProvider credentialProvider;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    @Value("${miniapp.translate.nls-app-key:}")
    private String nlsAppKey;

    public AliyunNlsService(AliyunCredentialProvider credentialProvider) {
        this.credentialProvider = credentialProvider;
    }

    public String recognizeSpeech(byte[] audioBytes, String format, Integer sampleRate) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new ServiceException("音频内容为空");
        }

        AliyunNlsConfig config = credentialProvider.getNlsConfig();
        if (config == null || config.getCredential() == null) {
            throw new ServiceException(
                    "未配置语音识别：请在密钥管理中维护别名为 ALIYUN_RAM 的 aliyun 密钥");
        }
        if (StrUtil.isBlank(nlsAppKey)) {
            throw new ServiceException(
                    "未配置语音识别 AppKey：请在 application.yml 设置 miniapp.translate.nls-app-key");
        }

        String normalizedFormat = normalizeFormat(format);
        int normalizedSampleRate = normalizeSampleRate(sampleRate);
        String token = getOrCreateToken(config.getCredential());

        String requestUrl = config.getGatewayUrl()
                + "?appkey=" + nlsAppKey.trim()
                + "&format=" + normalizedFormat
                + "&sample_rate=" + normalizedSampleRate
                + "&enable_punctuation_prediction=true"
                + "&enable_inverse_text_normalization=true";

        try (HttpResponse response = HttpRequest.post(requestUrl)
                .header("X-NLS-Token", token)
                .header("Content-Type", "application/octet-stream")
                .body(audioBytes)
                .timeout(60000)
                .execute()) {
            int httpStatus = response.getStatus();
            String body = StrUtil.blankToDefault(response.body(), "");
            log.info("阿里云语音识别响应: httpStatus={}, body={}", httpStatus, body);

            if (StrUtil.isBlank(body)) {
                throw new ServiceException("语音识别请求失败，HTTP=" + httpStatus + "，响应体为空");
            }

            JSONObject json = JSON.parseObject(body);
            int status = json.getIntValue("status");
            String message = json.getString("message");
            String taskId = json.getString("task_id");
            if (!response.isOk() || status != SUCCESS_STATUS) {
                log.error("阿里云语音识别失败: httpStatus={}, serviceStatus={}, message={}, taskId={}, body={}",
                        httpStatus, status, message, taskId, body);
                throw new ServiceException(formatAsrError(httpStatus, status, message, taskId));
            }

            String result = StrUtil.trim(json.getString("result"));
            if (StrUtil.isBlank(result)) {
                throw new ServiceException("未识别到语音内容，请靠近麦克风重试");
            }
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云语音识别异常: {}", e.getMessage(), e);
            throw new ServiceException("语音识别失败: " + e.getMessage());
        }
    }

    private String getOrCreateToken(AliyunCredential credential) {
        CachedToken current = cachedToken.get();
        long now = System.currentTimeMillis() / 1000;
        if (current != null
                && credential.getAccessKeyId().equals(current.accessKeyId())
                && now < current.expireTime() - TOKEN_REFRESH_BUFFER_SECONDS) {
            return current.token();
        }

        synchronized (this) {
            current = cachedToken.get();
            if (current != null
                    && credential.getAccessKeyId().equals(current.accessKeyId())
                    && now < current.expireTime() - TOKEN_REFRESH_BUFFER_SECONDS) {
                return current.token();
            }

            try {
                TokenResult tokenResult = AliyunNlsTokenClient.createToken(
                        credential.getAccessKeyId(),
                        credential.getAccessKeySecret());
                String token = tokenResult.token();
                long expireTime = tokenResult.expireTime();
                if (StrUtil.isBlank(token) || expireTime <= now) {
                    throw new ServiceException("获取语音识别 Token 失败: Token 已过期或为空");
                }
                cachedToken.set(new CachedToken(credential.getAccessKeyId(), token, expireTime));
                return token;
            } catch (ServiceException e) {
                log.error("获取阿里云 NLS Token 失败: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("获取阿里云 NLS Token 失败: {}", e.getMessage(), e);
                throw new ServiceException("获取语音识别 Token 失败: " + e.getMessage());
            }
        }
    }

    private String normalizeFormat(String format) {
        String normalized = StrUtil.blankToDefault(format, "mp3").trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "mp3", "aac", "wav", "pcm", "amr", "opus", "speex" -> normalized;
            default -> "mp3";
        };
    }

    private int normalizeSampleRate(Integer sampleRate) {
        if (sampleRate != null && sampleRate == 8000) {
            return 8000;
        }
        return 16000;
    }

    private String formatAsrError(int httpStatus, int serviceStatus, String message, String taskId) {
        String friendly = switch (serviceStatus) {
            case 40000001 -> "语音识别鉴权失败，请检查阿里云密钥配置";
            case 40000005 -> "语音识别请求过于频繁，请稍后再试";
            case 41010101 -> "音频采样率与模型不匹配，请使用 16k 录音";
            default -> StrUtil.blankToDefault(message, "语音识别失败");
        };
        return StrUtil.format("语音识别失败，HTTP={}，Status={}，Message={}，TaskId={}，Detail={}",
                httpStatus,
                serviceStatus,
                StrUtil.blankToDefault(message, "-"),
                StrUtil.blankToDefault(taskId, "-"),
                friendly);
    }

    private record CachedToken(String accessKeyId, String token, long expireTime) {
    }
}
