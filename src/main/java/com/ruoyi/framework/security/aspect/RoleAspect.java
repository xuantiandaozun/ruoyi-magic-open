package com.ruoyi.framework.security.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.framework.security.annotation.RequiresRoles;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 角色注解处理器
 */
@Aspect
@Component
public class RoleAspect {
    
    /**
     * 定义切点
     */
    @Pointcut("@annotation(com.ruoyi.framework.security.annotation.RequiresRoles)")
    public void rolePointCut() {
    }

    /**
     * 角色校验
     */
    @Before("rolePointCut()")
    public void doBefore(JoinPoint point) throws Throwable {
        // 获取注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
        
        if (requiresRoles != null) {
            // 根据注解中的角色标识和逻辑运算符进行角色校验
            if (requiresRoles.logical() == com.ruoyi.framework.security.annotation.Logical.AND) {
                // AND 逻辑，必须具有所有角色
                StpUtil.checkRoleAnd(requiresRoles.value());
            } else {
                // OR 逻辑，只需具有其中一个角色
                StpUtil.checkRoleOr(requiresRoles.value());
            }
        }
    }
} 