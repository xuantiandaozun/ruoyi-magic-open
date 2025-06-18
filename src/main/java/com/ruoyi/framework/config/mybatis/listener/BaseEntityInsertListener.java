package com.ruoyi.framework.config.mybatis.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mybatisflex.annotation.InsertListener;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 实体插入监听器，用于自动设置创建者和更新者信息
 */
public class BaseEntityInsertListener implements InsertListener {
    private static final Logger log = LoggerFactory.getLogger(BaseEntityInsertListener.class);

    @Override
    public void onInsert(Object entity) {
        if (entity instanceof BaseEntity) {
            try {
                String username = SecurityUtils.getUsername();
                BaseEntity baseEntity = (BaseEntity) entity;
                baseEntity.setCreateBy(username);
                baseEntity.setUpdateBy(username);
                log.debug("自动设置创建者和更新者信息：{}", username);
            } catch (Exception e) {
                log.warn("设置创建者和更新者信息失败：{}", e.getMessage());
            }
        }
    }

    @Override
    public int order() {
        return 10; // 优先级较高
    }
}
