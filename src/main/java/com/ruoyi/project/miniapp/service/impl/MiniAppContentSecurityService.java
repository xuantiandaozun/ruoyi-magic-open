package com.ruoyi.project.miniapp.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.common.utils.file.ByteArrayMultipartFile;
import com.ruoyi.project.miniapp.config.MiniAppContentSecurityProperties;
import com.ruoyi.project.miniapp.domain.MiniApp;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.domain.vo.MiniAppMediaCheckResult;
import com.ruoyi.project.miniapp.service.IMiniAppService;
import com.ruoyi.project.miniapp.util.MiniAppContentSecurityScene;
import com.ruoyi.project.miniapp.util.MiniAppWxServiceFactory;

import cn.binarywang.wx.miniapp.api.WxMaSecCheckService;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaMediaAsyncCheckResult;
import cn.binarywang.wx.miniapp.bean.security.WxMaMediaSecCheckCheckRequest;
import cn.binarywang.wx.miniapp.bean.security.WxMaMsgSecCheckCheckRequest;
import cn.binarywang.wx.miniapp.bean.security.WxMaMsgSecCheckCheckResponse;
import cn.binarywang.wx.miniapp.constant.WxMaConstants;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;

/**
 * 微信小程序内容安全：文本 msgSecCheck、图片/音频 mediaCheckAsync
 */
@Slf4j
@Service
public class MiniAppContentSecurityService {

    private static final int MSG_MAX_LENGTH = 2500;
    private static final String SUGGEST_PASS = "pass";
    private static final String VERSION_TEXT = "2";
    private static final int VERSION_MEDIA = 2;
    private static final String CHECK_UNAVAILABLE_MESSAGE = "当前内容无法检测，请稍后重试";

    private final MiniAppContentSecurityProperties properties;
    private final IMiniAppService miniAppService;
    private final MiniAppWxServiceFactory wxServiceFactory;
    private final FileStorageService fileStorageService;
    private final MiniAppMediaCheckResultStore mediaCheckResultStore;

    public MiniAppContentSecurityService(MiniAppContentSecurityProperties properties,
            IMiniAppService miniAppService,
            MiniAppWxServiceFactory wxServiceFactory,
            FileStorageService fileStorageService,
            MiniAppMediaCheckResultStore mediaCheckResultStore) {
        this.properties = properties;
        this.miniAppService = miniAppService;
        this.wxServiceFactory = wxServiceFactory;
        this.fileStorageService = fileStorageService;
        this.mediaCheckResultStore = mediaCheckResultStore;
    }

    public void checkSocialText(MiniAppLoginUser loginUser, String content) {
        checkTextContent(loginUser, content, MiniAppContentSecurityScene.SOCIAL, null);
    }

    public void checkProfileText(MiniAppLoginUser loginUser, String content, String nickname) {
        checkTextContent(loginUser, content, MiniAppContentSecurityScene.PROFILE, nickname);
    }

    public void checkCommentText(MiniAppLoginUser loginUser, String content) {
        checkTextContent(loginUser, content, MiniAppContentSecurityScene.COMMENT, null);
    }

    public void checkImageContent(MiniAppLoginUser loginUser, byte[] imageBytes, String mimeType, String filename) {
        checkImageContent(loginUser, imageBytes, mimeType, filename, MiniAppContentSecurityScene.SOCIAL);
    }

    public void checkProfileImage(MiniAppLoginUser loginUser, byte[] imageBytes, String mimeType, String filename) {
        checkImageContent(loginUser, imageBytes, mimeType, filename, MiniAppContentSecurityScene.PROFILE);
    }

    public void checkSocialAudio(MiniAppLoginUser loginUser, byte[] audioBytes, String mimeType, String filename) {
        checkMediaContent(loginUser, audioBytes, mimeType, filename, WxMaConstants.SecCheckMediaType.VOICE,
                MiniAppContentSecurityScene.SOCIAL, "security-audio");
    }

    public void checkTextContent(MiniAppLoginUser loginUser, String content, int scene, String nickname) {
        if (!properties.isEnabled() || StrUtil.isBlank(content)) {
            return;
        }

        WxMaService wxMaService = resolveWxMaService(loginUser);
        WxMaSecCheckService secCheckService = wxMaService.getSecCheckService();

        for (String chunk : splitContent(content, MSG_MAX_LENGTH)) {
            WxMaMsgSecCheckCheckRequest.WxMaMsgSecCheckCheckRequestBuilder builder = WxMaMsgSecCheckCheckRequest.builder()
                    .version(VERSION_TEXT)
                    .openid(loginUser.getOpenid())
                    .scene(scene)
                    .content(chunk);
            if (scene == MiniAppContentSecurityScene.PROFILE && StrUtil.isNotBlank(nickname)) {
                builder.nickname(nickname);
            }
            try {
                WxMaMsgSecCheckCheckResponse response = secCheckService.checkMessage(builder.build());
                validateTextResponse(response);
            } catch (ServiceException e) {
                throw e;
            } catch (WxErrorException e) {
                log.warn("文本内容安全检测失败: openid={}, scene={}, err={}", loginUser.getOpenid(), scene, e.getMessage());
                throw new ServiceException(CHECK_UNAVAILABLE_MESSAGE);
            }
        }
    }

    public void checkImageContent(MiniAppLoginUser loginUser, byte[] imageBytes, String mimeType, String filename,
            int scene) {
        checkMediaContent(loginUser, imageBytes, mimeType, filename, WxMaConstants.SecCheckMediaType.IMAGE, scene,
                "security-check");
    }

    private void checkMediaContent(MiniAppLoginUser loginUser, byte[] mediaBytes, String mimeType, String filename,
            int mediaType, int scene, String objectPrefix) {
        if (!properties.isEnabled() || mediaBytes == null || mediaBytes.length == 0) {
            return;
        }

        WxMaService wxMaService = resolveWxMaService(loginUser);
        String objectKey = null;
        try {
            String extension = resolveMediaExtension(mimeType, filename, mediaType);
            objectKey = StrUtil.format(
                    "miniapp/{}/{}/{}/{}.{}",
                    loginUser.getAppCode(),
                    objectPrefix,
                    loginUser.getMiniUserId(),
                    UUID.randomUUID().toString().replace("-", ""),
                    extension);

            MultipartFile uploadFile = new ByteArrayMultipartFile(
                    "file",
                    StrUtil.blankToDefault(filename, "media." + extension),
                    StrUtil.blankToDefault(mimeType, guessMimeType(mediaType, extension)),
                    mediaBytes);
            String mediaUrl = fileStorageService.upload(uploadFile, objectKey);

            WxMaMediaSecCheckCheckRequest request = WxMaMediaSecCheckCheckRequest.builder()
                    .mediaUrl(mediaUrl)
                    .mediaType(mediaType)
                    .version(VERSION_MEDIA)
                    .scene(scene)
                    .openid(loginUser.getOpenid())
                    .build();

            WxMaMediaAsyncCheckResult asyncResult = wxMaService.getSecCheckService().mediaCheckAsync(request);
            if (asyncResult == null || StrUtil.isBlank(asyncResult.getTraceId())) {
                throw new ServiceException(CHECK_UNAVAILABLE_MESSAGE);
            }

            MiniAppMediaCheckResult checkResult = waitForMediaCheckResult(asyncResult.getTraceId());
            validateMediaResult(checkResult);
        } catch (ServiceException e) {
            throw e;
        } catch (WxErrorException e) {
            log.warn("多媒体内容安全检测失败: openid={}, scene={}, mediaType={}, err={}",
                    loginUser.getOpenid(), scene, mediaType, e.getMessage());
            throw new ServiceException(CHECK_UNAVAILABLE_MESSAGE);
        } catch (Exception e) {
            log.warn("多媒体内容安全检测异常: openid={}, scene={}, mediaType={}, err={}",
                    loginUser.getOpenid(), scene, mediaType, e.getMessage());
            throw new ServiceException(CHECK_UNAVAILABLE_MESSAGE);
        } finally {
            if (StrUtil.isNotBlank(objectKey)) {
                try {
                    fileStorageService.delete(objectKey);
                } catch (Exception e) {
                    log.debug("清理临时检测文件失败: {}", objectKey);
                }
            }
        }
    }

    private WxMaService resolveWxMaService(MiniAppLoginUser loginUser) {
        MiniApp miniApp = miniAppService.getEnabledByAppCode(loginUser.getAppCode());
        if (miniApp == null) {
            throw new ServiceException("小程序应用不存在或已停用");
        }
        return wxServiceFactory.getService(miniApp);
    }

    private void validateTextResponse(WxMaMsgSecCheckCheckResponse response) {
        if (response == null) {
            throw new ServiceException(CHECK_UNAVAILABLE_MESSAGE);
        }
        if (response.getErrcode() != null && response.getErrcode() != 0) {
            throw new ServiceException(mapTechnicalMessage(response.getErrcode()));
        }
        if (response.getResult() == null || !SUGGEST_PASS.equalsIgnoreCase(response.getResult().getSuggest())) {
            throw new ServiceException(MiniAppContentSecurityScene.VIOLATION_MESSAGE);
        }
    }

    private void validateMediaResult(MiniAppMediaCheckResult result) {
        if (result == null) {
            throw new ServiceException(CHECK_UNAVAILABLE_MESSAGE);
        }
        if (result.getErrcode() != null && result.getErrcode() != 0) {
            throw new ServiceException(mapTechnicalMessage(result.getErrcode()));
        }
        if (!SUGGEST_PASS.equalsIgnoreCase(result.getSuggest())) {
            throw new ServiceException(MiniAppContentSecurityScene.VIOLATION_MESSAGE);
        }
    }

    private MiniAppMediaCheckResult waitForMediaCheckResult(String traceId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + properties.getMediaCheckTimeoutMs();
        while (System.currentTimeMillis() < deadline) {
            MiniAppMediaCheckResult cached = mediaCheckResultStore.get(traceId);
            if (cached != null) {
                return cached;
            }
            Thread.sleep(Math.max(properties.getMediaCheckPollIntervalMs(), 100L));
        }
        return null;
    }

    private List<String> splitContent(String content, int maxLength) {
        List<String> chunks = new ArrayList<>();
        String normalized = content.trim();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxLength, normalized.length());
            chunks.add(normalized.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private String resolveMediaExtension(String mimeType, String filename, int mediaType) {
        if (mediaType == WxMaConstants.SecCheckMediaType.VOICE) {
            return resolveAudioExtension(mimeType, filename);
        }
        return resolveImageExtension(mimeType, filename);
    }

    private String resolveImageExtension(String mimeType, String filename) {
        String lowerName = StrUtil.blankToDefault(filename, "").toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".png")) {
            return "png";
        }
        if (lowerName.endsWith(".gif")) {
            return "gif";
        }
        if (lowerName.endsWith(".bmp")) {
            return "bmp";
        }
        if (lowerName.endsWith(".jpeg")) {
            return "jpeg";
        }
        if (lowerName.endsWith(".jpg")) {
            return "jpg";
        }
        if (StrUtil.containsIgnoreCase(mimeType, "png")) {
            return "png";
        }
        if (StrUtil.containsIgnoreCase(mimeType, "gif")) {
            return "gif";
        }
        if (StrUtil.containsIgnoreCase(mimeType, "bmp")) {
            return "bmp";
        }
        return "jpg";
    }

    private String resolveAudioExtension(String mimeType, String filename) {
        String lowerName = StrUtil.blankToDefault(filename, "").toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".aac")) {
            return "aac";
        }
        if (lowerName.endsWith(".wav")) {
            return "wav";
        }
        if (lowerName.endsWith(".mp3")) {
            return "mp3";
        }
        if (StrUtil.containsIgnoreCase(mimeType, "aac")) {
            return "aac";
        }
        if (StrUtil.containsIgnoreCase(mimeType, "wav")) {
            return "wav";
        }
        return "mp3";
    }

    private String guessMimeType(int mediaType, String extension) {
        if (mediaType == WxMaConstants.SecCheckMediaType.VOICE) {
            return switch (extension) {
                case "aac" -> "audio/aac";
                case "wav" -> "audio/wav";
                default -> "audio/mpeg";
            };
        }
        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            default -> "image/jpeg";
        };
    }

    private String mapTechnicalMessage(Integer errcode) {
        if (errcode == null) {
            return CHECK_UNAVAILABLE_MESSAGE;
        }
        return switch (errcode) {
            case 61010 -> "请重新打开小程序后再试";
            case 44991, 45009, 87020 -> CHECK_UNAVAILABLE_MESSAGE;
            case -1008 -> CHECK_UNAVAILABLE_MESSAGE;
            default -> CHECK_UNAVAILABLE_MESSAGE;
        };
    }
}
