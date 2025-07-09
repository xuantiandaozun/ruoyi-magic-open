package com.ruoyi.framework.security.service;

import cn.dev33.satoken.stp.StpInterface;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.system.service.ISysMenuService;
import com.ruoyi.project.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 权限认证接口实现
 */
@Component
@Lazy(false)  // 禁用懒加载，确保Sa-Token能正确找到权限接口实现
public class StpInterfaceImpl implements StpInterface {
    @Autowired
    private ISysMenuService menuService;
    
    @Autowired
    private ISysRoleService roleService;

    /**
     * 获取角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        // 如果是超级管理员，则拥有所有角色
        if (SecurityUtils.isAdmin(userId)) {
            List<String> roles = new ArrayList<>();
            roles.add("admin");
            return roles;
        }
        Set<String> roles = roleService.selectRolePermissionByUserId(userId);
        return new ArrayList<>(roles);
    }

    /**
     * 获取权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        // 如果是超级管理员，则拥有所有权限
        if (SecurityUtils.isAdmin(userId)) {
            List<String> permissions = new ArrayList<>();
            permissions.add("*:*:*");
            return permissions;
        }
        Set<String> permissions = menuService.selectMenuPermsByUserId(userId);
        return new ArrayList<>(permissions);
    }
}