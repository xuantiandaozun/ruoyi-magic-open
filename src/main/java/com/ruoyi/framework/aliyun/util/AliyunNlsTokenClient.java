package com.ruoyi.framework.aliyun.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.nls.client.AccessToken;
import com.ruoyi.common.exception.ServiceException;

import cn.hutool.core.util.StrUtil;

/**
 * 通过阿里云官方 nls-sdk-common 获取智能语音 Token。
 */
public final class AliyunNlsTokenClient {

    private static final Logger log = LoggerFactory.getLogger(AliyunNlsTokenClient.class);

    private AliyunNlsTokenClient() {
    }

    public static TokenResult createToken(String accessKeyId, String accessKeySecret) {
        if (StrUtil.hasBlank(accessKeyId, accessKeySecret)) {
            throw new ServiceException("阿里云 AccessKey 未配置");
        }

        try {
            AccessToken accessToken = new AccessToken(accessKeyId, accessKeySecret);
            accessToken.apply();
            String token = accessToken.getToken();
            long expireTime = accessToken.getExpireTime();
            if (StrUtil.isBlank(token) || expireTime <= 0) {
                log.error("阿里云 CreateToken SDK 返回无效 Token: expireTime={}", expireTime);
                throw new ServiceException("获取语音识别 Token 失败: SDK 返回 Token 无效");
            }
            log.info("阿里云 CreateToken SDK 成功: expireTime={}", expireTime);
            return new TokenResult(token, expireTime);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云 CreateToken SDK 失败: accessKeyId={}, error={}",
                    maskAccessKey(accessKeyId), e.getMessage(), e);
            throw new ServiceException("获取语音识别 Token 失败: " + e.getMessage());
        }
    }

    private static String maskAccessKey(String accessKeyId) {
        if (StrUtil.isBlank(accessKeyId) || accessKeyId.length() <= 8) {
            return "****";
        }
        return accessKeyId.substring(0, 4) + "****" + accessKeyId.substring(accessKeyId.length() - 4);
    }

    public record TokenResult(String token, long expireTime) {
    }
}
