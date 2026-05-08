package com.ruoyi.project.system.service;

import com.ruoyi.project.system.domain.dto.GoogleOAuthLoginResult;

/**
 * Google OAuth 服务接口（插件用户登录专用）
 */
public interface IGoogleOAuthService {

    /**
     * 使用 Google ID Token 登录或注册用户，返回系统 token 及用户信息
     *
     * @param idToken Google 前端返回的 id_token（credential）
     * @return 登录结果（含系统 token、用户基本信息、配额信息）
     */
    GoogleOAuthLoginResult loginWithIdToken(String idToken);
}
