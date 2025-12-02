package com.ruoyi.framework.security.sign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ruoyi.framework.web.domain.AjaxResult;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * API 签名拦截器
 * 实现HMAC-SHA256签名验证
 * 
 * @author ruoyi
 * @date 2025-12-02
 */
@Slf4j
@Component
public class ApiSignInterceptor implements HandlerInterceptor {

    @Autowired
    private ApiSignConfig apiSignConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未启用签名验证，直接放行
        if (!Boolean.TRUE.equals(apiSignConfig.getEnabled())) {
            return true;
        }

        // 获取请求头
        String clientId = request.getHeader("X-Client-Id");
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");

        // 校验必填参数
        if (StrUtil.isBlank(clientId) || StrUtil.isBlank(timestamp) || 
            StrUtil.isBlank(nonce) || StrUtil.isBlank(signature)) {
            log.warn("API签名验证失败: 缺少必要的签名参数");
            writeErrorResponse(response, "缺少必要的签名参数");
            return false;
        }

        // 获取客户端密钥
        String secret = apiSignConfig.getSecretByClientId(clientId);
        if (StrUtil.isBlank(secret)) {
            log.warn("API签名验证失败: 无效的客户端ID: {}", clientId);
            writeErrorResponse(response, "无效的客户端ID");
            return false;
        }

        // 验证时间戳
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - requestTime);

            if (timeDiff > apiSignConfig.getTimestampTolerance()) {
                log.warn("API签名验证失败: 请求时间戳过期, clientId={}, timeDiff={}ms", clientId, timeDiff);
                writeErrorResponse(response, "请求时间戳过期");
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("API签名验证失败: 无效的时间戳格式: {}", timestamp);
            writeErrorResponse(response, "无效的时间戳格式");
            return false;
        }

        // 构建待签名字符串
        String method = request.getMethod();
        String path = request.getRequestURI();
        String signContent = method + path + timestamp + nonce;

        // 计算签名
        String expectedSignature = SecureUtil.hmacSha256(secret).digestHex(signContent, StandardCharsets.UTF_8);

        // 验证签名
        if (!expectedSignature.equalsIgnoreCase(signature)) {
            log.warn("API签名验证失败: 签名不匹配, clientId={}, expected={}, actual={}", 
                     clientId, expectedSignature, signature);
            writeErrorResponse(response, "签名验证失败");
            return false;
        }

        log.info("API签名验证成功: clientId={}, path={}", clientId, path);
        return true;
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        AjaxResult result = AjaxResult.error(401, message);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }
}
