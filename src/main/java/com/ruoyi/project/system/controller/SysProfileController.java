package com.ruoyi.project.system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.security.service.PasswordEncoder;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.domain.dto.FeishuOAuthRequest;
import com.ruoyi.project.system.service.IFeishuOAuthService;
import com.ruoyi.project.system.service.ISysUserService;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 个人信息 业务处理
 */
@Tag(name = "个人信息")
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController {
    @Autowired
    private ISysUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private IFeishuOAuthService feishuOAuthService;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile() {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        AjaxResult ajax = success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(user.getUserName()));
        ajax.put("postGroup", userService.selectUserPostGroup(user.getUserName()));
        ajax.put("feishuAuthorized", feishuOAuthService.isCurrentUserAuthorized());
        return ajax;
    }

    /**
     * 修改用户
     */
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user) {
        user.setUserId(StpUtil.getLoginIdAsLong());
        if (!userService.checkUserNameUnique(user)) {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        if (StrUtil.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        if (StrUtil.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        return toAjax(userService.updateById(user));
    }

    /**
     * 重置密码
     */
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(String oldPassword, String newPassword) {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        String password = user.getPassword();
        if (!passwordEncoder.matches(oldPassword, password)) {
            return error("修改密码失败，旧密码错误");
        }
        if (passwordEncoder.matches(newPassword, password)) {
            return error("新密码不能与旧密码相同");
        }
        if (userService.resetUserPwd(user.getUserName(), passwordEncoder.encode(newPassword))) {
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                // 使用云存储上传头像，指定avatar目录和图片类型限制
                String avatar = FileUploadUtils.upload("avatar", file, MimeTypeUtils.IMAGE_EXTENSION);
                boolean updateUserAvatar = userService.updateUserAvatar(StpUtil.getLoginIdAsString(), avatar);
                if (updateUserAvatar) {
                    return success("操作成功", avatar);
                }
            } catch (Exception e) {
                return error(e.getMessage());
            }
        }
        return error("上传图片异常，请联系管理员");
    }
    
    /**
     * 生成飞书授权URL
     */
    @Operation(summary = "生成飞书授权URL")
    @GetMapping("/feishu/authUrl")
    public AjaxResult getFeishuAuthUrl(@RequestParam String redirectUri, @RequestParam(required = false) String state) {
        try {
            if (StrUtil.isEmpty(state)) {
                state = "feishu_auth_" + System.currentTimeMillis();
            }
            String authUrl = feishuOAuthService.generateAuthUrl(redirectUri, state);
            return success("操作成功", authUrl);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
    
    /**
     * 处理飞书OAuth回调
     */
    @Operation(summary = "处理飞书OAuth回调")
    @PostMapping("/feishu/callback")
    public AjaxResult handleFeishuCallback(@Valid @RequestBody FeishuOAuthRequest request) {
        try {
            boolean success = feishuOAuthService.handleOAuthCallback(request);
            if (success) {
                return success("飞书授权成功");
            } else {
                return error("飞书授权失败");
            }
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
    
    /**
     * 获取当前用户飞书授权状态
     */
    @Operation(summary = "获取飞书授权状态")
    @GetMapping("/feishu/status")
    public AjaxResult getFeishuAuthStatus() {
        boolean authorized = feishuOAuthService.isCurrentUserAuthorized();
        AjaxResult result = success();
        result.put("authorized", authorized);
        if (authorized) {
            result.put("message", "已授权飞书访问");
        } else {
            result.put("message", "未授权飞书访问");
        }
        return result;
    }
    
    /**
     * 注销飞书授权
     */
    @Operation(summary = "注销飞书授权")
    @PostMapping("/feishu/revoke")
    public AjaxResult revokeFeishuAuth() {
        try {
            boolean success = feishuOAuthService.revokeCurrentUserAuthorization();
            if (success) {
                return success("飞书授权注销成功");
            } else {
                return error("飞书授权注销失败");
            }
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
