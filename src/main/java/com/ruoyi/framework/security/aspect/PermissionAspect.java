package com.ruoyi.framework.security.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.security.annotation.RequiresPermissions;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限注解处理器
 */
@Aspect
@Component
public class PermissionAspect {
    
    /**
     * 定义切点
     */
    @Pointcut("@annotation(com.ruoyi.framework.security.annotation.RequiresPermissions)")
    public void permissionPointCut() {
    }

    /**
     * 权限校验
     */
    @Before("permissionPointCut()")
    public void doBefore(JoinPoint point) throws Throwable {
        // 获取注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RequiresPermissions requiresPermissions = method.getAnnotation(RequiresPermissions.class);
        
        if (requiresPermissions != null) {
            // 如果是超级管理员(userId=1)，则直接放行，拥有所有权限
            if (SecurityUtils.isAdmin(StpUtil.getLoginIdAsLong())) {
                return;
            }
            
            // 根据注解中的权限码和逻辑运算符进行权限校验
            if (requiresPermissions.logical() == com.ruoyi.framework.security.annotation.Logical.AND) {
                // AND 逻辑，必须具有所有权限
                StpUtil.checkPermissionAnd(requiresPermissions.value());
            } else {
                // OR 逻辑，只需具有其中一个权限
                StpUtil.checkPermissionOr(requiresPermissions.value());
            }
        }
    }
} 