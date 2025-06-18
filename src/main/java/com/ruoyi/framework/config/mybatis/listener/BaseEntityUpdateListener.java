package com.ruoyi.framework.config.mybatis.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mybatisflex.annotation.UpdateListener;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 实体更新监听器，用于自动设置更新者信息
 */
public class BaseEntityUpdateListener implements UpdateListener {
    private static final Logger log = LoggerFactory.getLogger(BaseEntityUpdateListener.class);

    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof BaseEntity) {
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
}
