package com.ruoyi.project.system.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.FeishuConfigUtils;
import com.ruoyi.project.system.config.FeishuConfig;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.domain.dto.FeishuOAuthRequest;
import com.ruoyi.project.system.domain.dto.FeishuOAuthResponse;
import com.ruoyi.project.system.service.IFeishuOAuthService;
import com.ruoyi.project.system.service.ISysUserService;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书OAuth服务实现
 * 
 * @author ruoyi
 */
@Slf4j
@Service
public class FeishuOAuthServiceImpl implements IFeishuOAuthService {
    
    @Autowired
    private ISysUserService userService;
    
    private static final String FEISHU_AUTH_URL = "https://accounts.feishu.cn/open-apis/authen/v1/authorize";
    private static final String FEISHU_TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";
    
    @Override
    public String generateAuthUrl(String redirectUri, String state) {
        FeishuConfig feishuConfig = FeishuConfigUtils.getFeishuConfig();
        if (feishuConfig == null || StrUtil.isEmpty(feishuConfig.getAppId())) {
            throw new ServiceException("飞书应用配置未设置");
        }
        
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8.toString());
            
            return String.format("%s?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                    FEISHU_AUTH_URL,
                    feishuConfig.getAppId(),
                    encodedRedirectUri,
                    "contact:contact drive:drive drive:drive:readonly docs:doc:readonly space:document:delete space:document:move space:document:retrieve space:document:shortcut docx:document docx:document:create", 
                    encodedState);
        } catch (Exception e) {
            log.error("生成飞书授权URL失败", e);
            throw new ServiceException("生成授权URL失败");
        }
    }
    
    @Override
    public boolean handleOAuthCallback(FeishuOAuthRequest request) {
        FeishuConfig feishuConfig = FeishuConfigUtils.getFeishuConfig();
        if (feishuConfig == null || StrUtil.isEmpty(feishuConfig.getAppId()) || StrUtil.isEmpty(feishuConfig.getAppSecret())) {
            throw new ServiceException("飞书应用配置未设置");
        }
        
        try {
            // 构建请求参数
            JSONObject requestBody = new JSONObject();
            requestBody.set("grant_type", "authorization_code");
            requestBody.set("client_id", feishuConfig.getAppId());
            requestBody.set("client_secret", feishuConfig.getAppSecret());
            requestBody.set("code", request.getCode());
            if (StrUtil.isNotEmpty(request.getRedirectUri())) {
                requestBody.set("redirect_uri", request.getRedirectUri());
            }
            
            // 发送请求获取token
            HttpResponse response = HttpRequest.post(FEISHU_TOKEN_URL)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(requestBody.toString())
                    .execute();
            
            if (!response.isOk()) {
                log.error("飞书token请求失败: {}", response.body());
                throw new ServiceException("获取飞书授权失败");
            }
            
            // 解析响应
            FeishuOAuthResponse oauthResponse = JSONUtil.toBean(response.body(), FeishuOAuthResponse.class);
            
            if (oauthResponse.getCode() != null && oauthResponse.getCode() != 0) {
                log.error("飞书授权失败: {} - {}", oauthResponse.getError(), oauthResponse.getError_description());
                throw new ServiceException("飞书授权失败: " + oauthResponse.getError_description());
            }
            
            if (StrUtil.isEmpty(oauthResponse.getAccess_token())) {
                log.error("飞书返回的access_token为空");
                throw new ServiceException("获取访问令牌失败");
            }
            
            // 保存token到当前用户
            Long userId = StpUtil.getLoginIdAsLong();
            SysUser user = userService.getById(userId);
            if (user == null) {
                throw new ServiceException("用户不存在");
            }
            
            // 计算过期时间
            Date expireTime = new Date(System.currentTimeMillis() + (oauthResponse.getExpires_in() * 1000L));
            
            user.setFeishuAccessToken(oauthResponse.getAccess_token());
            user.setFeishuTokenExpireTime(expireTime);
            
            boolean result = userService.updateById(user);
            if (result) {
                log.info("用户 {} 飞书授权成功", user.getUserName());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("处理飞书OAuth回调失败", e);
            throw new ServiceException("授权处理失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getCurrentUserFeishuToken() {
        boolean login = StpUtil.isLogin();
        if(!login){
            return null;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        
        if (user == null || StrUtil.isEmpty(user.getFeishuAccessToken())) {
            return null;
        }
        
        // 检查token是否过期
        if (user.getFeishuTokenExpireTime() != null && 
            user.getFeishuTokenExpireTime().before(new Date())) {
            log.warn("用户 {} 的飞书token已过期", user.getUserName());
            return null;
        }
        
        return user.getFeishuAccessToken();
    }
    
    @Override
    public boolean isCurrentUserAuthorized() {
        return StrUtil.isNotEmpty(getCurrentUserFeishuToken());
    }
    
    @Override
    public boolean revokeCurrentUserAuthorization() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            SysUser user = userService.getById(userId);
            
            if (user == null) {
                throw new ServiceException("用户不存在");
            }
            
            // 清除飞书相关信息
            user.setFeishuAccessToken(null);
            user.setFeishuTokenExpireTime(null);
            
            boolean result = userService.updateById(user);
            if (result) {
                log.info("用户 {} 飞书授权已注销", user.getUserName());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("注销飞书授权失败", e);
            throw new ServiceException("注销授权失败: " + e.getMessage());
        }
    }
}