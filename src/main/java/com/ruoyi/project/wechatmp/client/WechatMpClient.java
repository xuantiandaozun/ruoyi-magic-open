package com.ruoyi.project.wechatmp.client;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.wechatmp.config.WechatMpProperties;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.RequiredArgsConstructor;

/**
 * 微信公众号服务端 API 客户端
 */
@Component
@RequiredArgsConstructor
public class WechatMpClient {

    private static final String TOKEN_CACHE_KEY = "wechat:mp:stable_access_token";
    private static final int TOKEN_CACHE_BUFFER_SECONDS = 300;
    private static final int HTTP_TIMEOUT_MS = 120_000;

    private static final String STABLE_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";
    private static final String UPLOAD_IMAGE_URL = "https://api.weixin.qq.com/cgi-bin/media/uploadimg";
    private static final String ADD_MATERIAL_URL = "https://api.weixin.qq.com/cgi-bin/material/add_material";
    private static final String DRAFT_ADD_URL = "https://api.weixin.qq.com/cgi-bin/draft/add";

    private final WechatMpProperties properties;
    private final RedisCache redisCache;

    public String getAccessToken() {
        String cached = redisCache.getCacheObject(TOKEN_CACHE_KEY);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        JSONObject body = new JSONObject();
        body.put("grant_type", "client_credential");
        body.put("appid", properties.getAppId());
        body.put("secret", properties.getAppSecret());

        JSONObject data = postJson(STABLE_TOKEN_URL, null, body);
        String token = data.getString("access_token");
        Integer expiresIn = data.getInteger("expires_in");
        if (StrUtil.isBlank(token)) {
            throw new ServiceException("获取微信公众号 access_token 失败");
        }
        int ttl = expiresIn != null ? Math.max(expiresIn - TOKEN_CACHE_BUFFER_SECONDS, 60) : 7000;
        redisCache.setCacheObject(TOKEN_CACHE_KEY, token, ttl, TimeUnit.SECONDS);
        return token;
    }

    public String uploadInlineImage(byte[] imageBytes, String fileName) {
        String accessToken = getAccessToken();
        HttpResponse response = HttpRequest.post(UPLOAD_IMAGE_URL + "?access_token=" + accessToken)
            .timeout(HTTP_TIMEOUT_MS)
            .form("media", imageBytes, fileName)
            .execute();
        JSONObject data = parseResponse(response);
        String url = data.getString("url");
        if (StrUtil.isBlank(url)) {
            throw new ServiceException("上传正文图片失败");
        }
        return url;
    }

    public String uploadCoverMaterial(byte[] imageBytes, String fileName) {
        String accessToken = getAccessToken();
        HttpResponse response = HttpRequest.post(
                ADD_MATERIAL_URL + "?access_token=" + accessToken + "&type=image")
            .timeout(HTTP_TIMEOUT_MS)
            .form("media", imageBytes, fileName)
            .execute();
        JSONObject data = parseResponse(response);
        String mediaId = data.getString("media_id");
        if (StrUtil.isBlank(mediaId)) {
            throw new ServiceException("上传封面素材失败");
        }
        return mediaId;
    }

    public String addDraft(Map<String, Object> payload) {
        String accessToken = getAccessToken();
        JSONObject data = postJson(DRAFT_ADD_URL, accessToken, payload);
        String mediaId = data.getString("media_id");
        if (StrUtil.isBlank(mediaId)) {
            throw new ServiceException("创建微信公众号草稿失败");
        }
        return mediaId;
    }

    private JSONObject postJson(String url, String accessToken, Object body) {
        String requestUrl = StrUtil.isNotBlank(accessToken)
            ? url + "?access_token=" + accessToken
            : url;
        HttpResponse response = HttpRequest.post(requestUrl)
            .timeout(HTTP_TIMEOUT_MS)
            .body(JSON.toJSONString(body))
            .contentType("application/json; charset=utf-8")
            .execute();
        return parseResponse(response);
    }

    private JSONObject parseResponse(HttpResponse response) {
        if (response == null) {
            throw new ServiceException("微信公众号接口无响应");
        }
        String raw = response.body();
        if (!response.isOk()) {
            throw new ServiceException("微信公众号接口 HTTP " + response.getStatus() + ": " + raw);
        }
        JSONObject data = JSON.parseObject(raw);
        if (data == null) {
            throw new ServiceException("微信公众号接口返回为空");
        }
        Integer errcode = data.getInteger("errcode");
        if (errcode != null && errcode != 0) {
            throw new ServiceException("微信公众号接口错误: " + data.getString("errmsg") + " (" + errcode + ")");
        }
        return data;
    }
}
