package com.ruoyi.framework.config.mybatis.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mybatisflex.annotation.UpdateListener;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.domain.BaseEntity;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;

/**
 * 实体更新监听器，用于自动设置更新者信息
 */
public class BaseEntityUpdateListener implements UpdateListener {
    private static final Logger log = LoggerFactory.getLogger(BaseEntityUpdateListener.class);

    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof BaseEntity) {
            // 检查 SaToken 上下文是否已初始化（应用启动时上下文可能未初始化）
            if (!isSaTokenContextAvailable()) {
                log.debug("SaToken上下文未初始化，跳过设置更新者信息");
                return;
            }
            // 检查是否有用户登录
            if (!StpUtil.isLogin()) {
                log.debug("用户未登录，跳过设置更新者信息");
                return;
            }
            
            try {
                String username = SecurityUtils.getUsername();
                BaseEntity baseEntity = (BaseEntity) entity;
                baseEntity.setUpdateBy(username);
                log.debug("自动设置更新者信息：{}", username);
            } catch (Exception e) {
                log.warn("设置更新者信息失败：{}", e.getMessage());
            }
        }
    }

    @Override
    public int order() {
        return 10; // 优先级较高
    }

    /**
     * 检查 SaToken 上下文是否可用
     * 在应用启动阶段或非Web线程中，上下文可能未初始化
     */
    private boolean isSaTokenContextAvailable() {
        try {
            SaHolder.getStorage();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
