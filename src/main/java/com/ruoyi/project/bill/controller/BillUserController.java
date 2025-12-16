package com.ruoyi.project.bill.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.security.service.PasswordEncoder;
import com.ruoyi.framework.security.service.SaTokenLoginService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.bill.domain.dto.RegisterDTO;
import com.ruoyi.project.bill.domain.dto.SendSmsDTO;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.service.ISysUserService;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 账单用户控制器
 * 
 * @author ruoyi
 */
@Tag(name = "账单用户管理")
@RestController
@RequestMapping("/api/bill/user")
public class BillUserController extends BaseController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SaTokenLoginService loginService;

    /**
     * 用户注册（支持手机号+验证码 或 邮箱+验证码）
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterDTO registerDTO) {
        // 验证必填字段
        if (StrUtil.isEmpty(registerDTO.getNickName())) {
            return error("昵称不能为空");
        }
        if (StrUtil.isEmpty(registerDTO.getPassword())) {
            return error("密码不能为空");
        }
        if (registerDTO.getPassword().length() < 6 || registerDTO.getPassword().length() > 20) {
            return error("密码长度必须在6到20个字符之间");
        }

        // 验证码校验
        if (StrUtil.isEmpty(registerDTO.getVerifyCode())) {
            return error("验证码不能为空");
        }
        // TODO: 实际项目中需要验证验证码是否正确
        // 目前先跳过验证码校验，后续完善

        String username = null;
        SysUser sysUser = new SysUser();

        // 判断是手机号注册还是邮箱注册
        if (StrUtil.isNotEmpty(registerDTO.getPhone())) {
            // 手机号注册
            if (!isValidPhone(registerDTO.getPhone())) {
                return error("手机号格式不正确");
            }

            // 检查手机号是否已注册
            SysUser checkUser = new SysUser();
            checkUser.setPhonenumber(registerDTO.getPhone());
            if (!userService.checkPhoneUnique(checkUser)) {
                return error("该手机号已被注册");
            }

            username = registerDTO.getPhone();
            sysUser.setPhonenumber(registerDTO.getPhone());
        } else if (StrUtil.isNotEmpty(registerDTO.getEmail())) {
            // 邮箱注册
            if (!isValidEmail(registerDTO.getEmail())) {
                return error("邮箱格式不正确");
            }

            // 检查邮箱是否已注册
            SysUser checkUser = new SysUser();
            checkUser.setEmail(registerDTO.getEmail());
            if (!userService.checkEmailUnique(checkUser)) {
                return error("该邮箱已被注册");
            }

            username = registerDTO.getEmail();
            sysUser.setEmail(registerDTO.getEmail());
        } else {
            return error("请提供手机号或邮箱");
        }

        // 设置用户信息
        sysUser.setUserName(username);
        sysUser.setNickName(registerDTO.getNickName());
        sysUser.setPassword(passwordEncoder.encode(registerDTO.getPassword()));

        // 设置用户类型为注册用户
        sysUser.setUserType("01"); // 01-注册用户

        // 设置默认角色：记账用户(role_id=100)
        sysUser.setRoleIds(new Long[] { 100L });

        // 注册用户
        boolean registerFlag = userService.registerUser(sysUser);
        if (!registerFlag) {
            return error("注册失败，请联系系统管理员");
        }

        return success("注册成功");
    }

    /**
     * 发送短信验证码
     */
    @Operation(summary = "发送短信验证码")
    @PostMapping("/sendSms")
    public AjaxResult sendSms(@RequestBody SendSmsDTO sendSmsDTO) {
        // 验证手机号
        if (StrUtil.isEmpty(sendSmsDTO.getPhone())) {
            return error("手机号不能为空");
        }
        if (!isValidPhone(sendSmsDTO.getPhone())) {
            return error("手机号格式不正确");
        }

        // TODO: 这里是假的发送验证码接口，实际项目中需要对接短信服务商
        // 1. 生成6位随机验证码
        // 2. 将验证码存储到Redis，设置5分钟过期
        // 3. 调用短信服务商API发送验证码

        // 模拟发送成功
        logger.info("发送验证码到手机号: {}, 验证码: 123456 (模拟)", sendSmsDTO.getPhone());

        return success("验证码发送成功");
    }

    /**
     * 发送邮箱验证码
     */
    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/sendEmail")
    public AjaxResult sendEmail(@RequestBody SendSmsDTO sendSmsDTO) {
        // 验证邮箱
        if (StrUtil.isEmpty(sendSmsDTO.getEmail())) {
            return error("邮箱不能为空");
        }
        if (!isValidEmail(sendSmsDTO.getEmail())) {
            return error("邮箱格式不正确");
        }

        // TODO: 这里是假的发送验证码接口，实际项目中需要对接邮件服务
        // 1. 生成6位随机验证码
        // 2. 将验证码存储到Redis，设置5分钟过期
        // 3. 发送邮件

        // 模拟发送成功
        logger.info("发送验证码到邮箱: {}, 验证码: 123456 (模拟)", sendSmsDTO.getEmail());

        return success("验证码发送成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public AjaxResult getUserInfo() {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            return success(loginUser.getUser());
        } catch (Exception e) {
            return error("未登录");
        }
    }

    /**
     * 用户登录（账单系统专用）
     */
    @Operation(summary = "账单用户登录")
    @PostMapping("/login")
    public AjaxResult login(@RequestBody RegisterDTO loginDTO) {
        try {
            // 验证必填字段
            String username = StrUtil.isNotEmpty(loginDTO.getPhone()) ? loginDTO.getPhone() : loginDTO.getEmail();
            if (StrUtil.isEmpty(username) || StrUtil.isEmpty(loginDTO.getPassword())) {
                return error("用户名或密码不能为空");
            }

            // 调用系统登录服务进行认证
            LoginUser loginUser = loginService.login(username, loginDTO.getPassword());

            // 验证用户类型必须是注册用户
            SysUser user = loginUser.getUser();
            if (!"01".equals(user.getUserType())) {
                // 退出登录
                cn.dev33.satoken.stp.StpUtil.logout();
                return error("此账号不能使用账单系统登录，请使用系统管理端登录");
            }

            // 验证用户是否拥有记账用户角色
            boolean hasBillUserRole = false;
            if (user.getRoles() != null) {
                for (com.ruoyi.project.system.domain.SysRole role : user.getRoles()) {
                    if ("bill_user".equals(role.getRoleKey())) {
                        hasBillUserRole = true;
                        break;
                    }
                }
            }

            if (!hasBillUserRole) {
                // 退出登录
                cn.dev33.satoken.stp.StpUtil.logout();
                return error("此账号没有权限使用账单系统");
            }

            // 返回成功结果
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("token", cn.dev33.satoken.stp.StpUtil.getTokenValue());
            result.put("user", user);

            return success("登录成功", result);

        } catch (Exception e) {
            logger.error("登录失败", e);
            return error("用户名或密码错误");
        }
    }

    /**
     * 用户退出
     */
    @Operation(summary = "用户退出")
    @PostMapping("/logout")
    public AjaxResult logout() {
        try {
            SecurityUtils.getLoginUser();
            cn.dev33.satoken.stp.StpUtil.logout();
            return success("退出成功");
        } catch (Exception e) {
            return success("退出成功");
        }
    }

    /**
     * 验证手机号格式
     */
    private boolean isValidPhone(String phone) {
        if (StrUtil.isEmpty(phone)) {
            return false;
        }
        // 简单的手机号验证：11位数字
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 验证邮箱格式
     */
    private boolean isValidEmail(String email) {
        if (StrUtil.isEmpty(email)) {
            return false;
        }
        // 简单的邮箱验证
        return email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");
    }
}
