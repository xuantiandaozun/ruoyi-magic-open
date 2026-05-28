package com.ruoyi.project.miniapp.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.miniapp.domain.dto.MiniAppLoginRequest;
import com.ruoyi.project.miniapp.domain.dto.UpdateMiniUserProfileRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.service.IMiniAppAuthService;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序认证")
@RestController
@RequestMapping("/miniapp/auth")
public class MiniAppAuthController {

    private final IMiniAppAuthService miniAppAuthService;

    public MiniAppAuthController(IMiniAppAuthService miniAppAuthService) {
        this.miniAppAuthService = miniAppAuthService;
    }

    @Operation(summary = "小程序登录")
    @PostMapping("/login")
    public AjaxResult login(@Validated @RequestBody MiniAppLoginRequest request) {
        return AjaxResult.success(miniAppAuthService.login(request));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public AjaxResult me() {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(miniAppAuthService.currentUser(loginUser));
    }

    @Operation(summary = "更新当前用户资料")
    @PutMapping("/profile")
    public AjaxResult updateProfile(@Validated @RequestBody UpdateMiniUserProfileRequest request) {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(miniAppAuthService.updateProfile(request, loginUser));
    }

    @Operation(summary = "上传用户头像")
    @PostMapping("/avatar/upload")
    public AjaxResult uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(miniAppAuthService.uploadAvatar(file, loginUser));
    }

    @Operation(summary = "小程序退出登录")
    @PostMapping("/logout")
    public AjaxResult logout() {
        miniAppAuthService.logout();
        return AjaxResult.success("退出成功");
    }
}
