package com.ruoyi.framework.security.sign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ruoyi.common.filter.RepeatedlyRequestWrapper;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.service.ISysMenuService;
import com.ruoyi.project.system.service.ISysRoleService;
import com.ruoyi.project.system.service.ISysUserService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import cn.dev33.satoken.stp.StpUtil;
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

    @Autowired
    private ApiSignNonceService apiSignNonceService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private ISysRoleService roleService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 全局关闭时，仍允许对指定受保护路径强制启用签名验证
        if (!apiSignConfig.requiresSignature(request.getRequestURI())) {
            return true;
        }

        // 获取请求头
        String clientId = request.getHeader("X-Client-Id");
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");
        String bodyHash = request.getHeader("X-Body-SHA256");
        String path = request.getRequestURI();

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

            if (apiSignNonceService.isReplay(clientId, nonce, apiSignConfig.getTimestampTolerance())) {
                log.warn("API签名验证失败: 检测到重复 nonce, clientId={}, nonce={}", clientId, nonce);
                writeErrorResponse(response, "重复请求已被拦截");
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("API签名验证失败: 无效的时间戳格式: {}", timestamp);
            writeErrorResponse(response, "无效的时间戳格式");
            return false;
        }

        if (apiSignConfig.requiresBodyHash(path)) {
            byte[] requestBody = extractRequestBody(request);
            if (requestBody == null) {
                log.warn("API签名验证失败: 请求体缓存不可用, path={}", path);
                writeErrorResponse(response, "请求体校验失败");
                return false;
            }
            if (StrUtil.isBlank(bodyHash)) {
                log.warn("API签名验证失败: 缺少请求体摘要, path={}", path);
                writeErrorResponse(response, "缺少请求体摘要");
                return false;
            }
            String actualBodyHash = DigestUtil.sha256Hex(requestBody);
            if (!actualBodyHash.equalsIgnoreCase(bodyHash)) {
                log.warn("API签名验证失败: 请求体摘要不匹配, path={}, headerHash={}, actualHash={}, bodyLength={}",
                        path, bodyHash, actualBodyHash, requestBody.length);
                writeErrorResponse(response, "请求体校验失败");
                return false;
            }
        }

        // 构建待签名字符串
        String method = request.getMethod();
        String signContent = method + path + timestamp + nonce;
        if (apiSignConfig.requiresBodyHash(path)) {
            signContent += bodyHash;
        }

        // 计算签名
        String expectedSignature = SecureUtil.hmacSha256(secret).digestHex(signContent, StandardCharsets.UTF_8);

        // 验证签名
        if (!expectedSignature.equalsIgnoreCase(signature)) {
            log.warn("API签名验证失败: 签名不匹配, clientId={}, expected={}, actual={}", 
                     clientId, expectedSignature, signature);
            writeErrorResponse(response, "签名验证失败");
            return false;
        }

        bindInternalLoginIfNeeded(clientId);

        log.info("API签名验证成功: clientId={}, path={}", clientId, path);
        return true;
    }

    private void bindInternalLoginIfNeeded(String clientId) {
        if (!"ai-blog-skill".equals(clientId) || StpUtil.isLogin()) {
            return;
        }

        SysUser user = userService.selectUserByUserName("admin");
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("未找到 admin 用户，无法为 ai-blog-skill 绑定登录态");
        }

        Set<String> permissions = menuService.selectMenuPermsByUserId(user.getUserId());
        var roles = roleService.selectRolesByUserId(user.getUserId());
        user.setRoles(roles);

        LoginUser loginUser = new LoginUser(user.getUserId(), user, permissions);
        StpUtil.login(user.getUserId());
        StpUtil.getSession().set(Constants.LOGIN_USER_KEY, loginUser);
    }

    private byte[] extractRequestBody(HttpServletRequest request) {
        if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
            return cachedRequest.getCachedBody();
        }
        if (request instanceof RepeatedlyRequestWrapper repeatedlyRequestWrapper) {
            return repeatedlyRequestWrapper.getBody();
        }
        return null;
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
