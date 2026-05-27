package com.ruoyi.project.miniapp.service;

import java.util.Map;

import com.ruoyi.project.miniapp.domain.dto.MiniAppLoginRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;

public interface IMiniAppAuthService {
    Map<String, Object> login(MiniAppLoginRequest request);

    Map<String, Object> currentUser(MiniAppLoginUser loginUser);

    void logout();
}
