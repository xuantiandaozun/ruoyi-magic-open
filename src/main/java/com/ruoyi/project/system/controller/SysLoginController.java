package com.ruoyi.project.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.security.service.SaTokenLoginService;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.domain.SysRole;
import com.ruoyi.project.system.domain.SysDept;
import com.ruoyi.project.system.service.ISysMenuService;
import com.ruoyi.project.system.service.ISysRoleService;
import com.ruoyi.project.system.service.ISysUserService;
import com.ruoyi.project.system.service.ISysPostService;
import com.ruoyi.project.system.service.ISysDeptService;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;

/**
 * 登录验证
 */
@Tag(name = "登录管理")
@RestController
public class SysLoginController {
    
    @Autowired
    private SaTokenLoginService loginService;
    
    @Autowired
    private ISysUserService userService;
    
    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysPostService postService;

    @Autowired
    private ISysDeptService deptService;
    

    /**
     * 登录方法
     */
    @Operation(summary = "系统用户登录")
    @Parameters({
        @Parameter(name = "username", description = "用户名", required = true),
        @Parameter(name = "password", description = "密码", required = true)
    })
    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> loginBody) {
        // 登录验证
        LoginUser loginUser = loginService.login(loginBody.get("username"), loginBody.get("password"));
        
        Map<String, Object> ajax = new HashMap<>();
        ajax.put(Constants.TOKEN, StpUtil.getTokenValue());
        ajax.put("user", loginUser.getUser());
        
        return AjaxResult.success("登录成功", ajax);
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public AjaxResult logout() {
        loginService.logout();
        return AjaxResult.success("退出成功");
    }

    /**
     * 获取用户信息
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("/getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser.getUserId();
        
        // 查询用户信息
        SysUser user = userService.getById(userId);
        
        // 查询用户的部门信息
        if (user.getDeptId() != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            user.setDept(dept);
        }
        
        // 查询用户的角色信息
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        user.setRoles(roles);
        
        // 设置角色ID数组
        List<Long> roleIdList = roles.stream().map(SysRole::getRoleId).collect(Collectors.toList());
        user.setRoleIds(roleIdList.toArray(new Long[0]));
        
        // 查询用户的岗位信息
        List<Long> postIdList = postService.selectPostListByUserId(userId);
        user.setPostIds(postIdList.toArray(new Long[0]));
        
        // 如果用户有角色，设置第一个角色ID
        if (!roles.isEmpty()) {
            user.setRoleId(roles.get(0).getRoleId());
        }
        
        // 获取用户权限
        Set<String> permissions = menuService.selectMenuPermsByUserId(userId);
        
        Map<String, Object> ajax = new HashMap<>();
        ajax.put("user", user);
        ajax.put("permissions", permissions);
        return AjaxResult.success(ajax);
    }

    /**
     * 验证token是否有效
     */
    @Operation(summary = "验证token是否有效")
    @GetMapping("/checkToken")
    public AjaxResult checkToken() {
        // 使用StpUtil验证当前会话是否有效
        boolean isValid = StpUtil.isLogin();
        return AjaxResult.success("token验证结果", isValid);
    }
}
