package com.ruoyi.project.miniapp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import cn.hutool.core.util.StrUtil;

/**
 * 微信搜索爬虫签名校验，算法与小程序消息推送一致。
 *
 * @see <a href="https://developers.weixin.qq.com/miniprogram/dev/framework/search/seo.html">小程序搜索优化指南</a>
 */
public final class MiniAppWxCrawlerSignatureUtil {

    private MiniAppWxCrawlerSignatureUtil() {
    }

    public static boolean verify(String token, String timestamp, String nonce, String signature) {
        if (StrUtil.hasBlank(token, timestamp, nonce, signature)) {
            return false;
        }
        String[] params = new String[] { token, timestamp, nonce };
        Arrays.sort(params);
        String content = String.join("", params);
        String expected = sha1Hex(content);
        return StrUtil.equalsIgnoreCase(expected, signature);
    }

    private static String sha1Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
