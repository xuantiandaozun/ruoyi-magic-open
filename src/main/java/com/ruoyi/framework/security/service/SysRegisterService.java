package com.ruoyi.framework.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.framework.security.RegisterBody;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.service.ISysUserService;
import cn.hutool.core.util.StrUtil;

/**
 * 注册校验方法
 */
@Service
public class SysRegisterService {
    @Autowired
    private ISysUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 注册
     */
    public String register(RegisterBody registerBody) {
        String msg = "", username = registerBody.getUsername(), password = registerBody.getPassword();

        if (StrUtil.isEmpty(username)) {
            msg = "用户名不能为空";
        }
        else if (StrUtil.isEmpty(password)) {
            msg = "用户密码不能为空";
        }
        else if (username.length() < UserConstants.USERNAME_MIN_LENGTH 
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            msg = "账户长度必须在2到20个字符之间";
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH 
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            msg = "密码长度必须在5到20个字符之间";
        }
        else {
            SysUser sysUser = new SysUser();
            sysUser.setUserName(username);
            if (!userService.checkUserNameUnique(sysUser)) {
                msg = "保存用户'" + username + "'失败，注册账号已存在";
            }
            else {
                sysUser.setNickName(username);
                sysUser.setPassword(passwordEncoder.encode(password));
                boolean regFlag = userService.registerUser(sysUser);
                if (!regFlag) {
                    msg = "注册失败,请联系系统管理人员";
                }
            }
        }
        return msg;
    }
} 