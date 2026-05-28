package com.ruoyi.project.miniapp.service;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.project.miniapp.domain.dto.MiniAppLoginRequest;
import com.ruoyi.project.miniapp.domain.dto.UpdateMiniUserProfileRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;

public interface IMiniAppAuthService {
    Map<String, Object> login(MiniAppLoginRequest request);

    Map<String, Object> currentUser(MiniAppLoginUser loginUser);

    Map<String, Object> updateProfile(UpdateMiniUserProfileRequest request, MiniAppLoginUser loginUser);

    Map<String, Object> uploadAvatar(MultipartFile file, MiniAppLoginUser loginUser) throws Exception;

    void logout();
}
