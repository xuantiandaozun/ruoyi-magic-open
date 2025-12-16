package com.ruoyi.project.bill.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.bill.domain.BillReminder;

/**
 * 提醒设置Service接口
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
public interface IBillReminderService extends IService<BillReminder> {
    /**
     * 查询用户的提醒列表
     * 
     * @param userId       用户ID
     * @param reminderType 提醒类型（可选）
     * @return 提醒列表
     */
    List<BillReminder> selectReminderList(Long userId, String reminderType);

    /**
     * 查询用户的提醒列表（简化版）
     * 
     * @param userId 用户ID
     * @return 提醒列表
     */
    List<BillReminder> selectByUserId(Long userId);

    /**
     * 查询用户的启用提醒列表
     * 
     * @param userId 用户ID
     * @return 启用的提醒列表
     */
    List<BillReminder> selectEnabledByUserId(Long userId);

    /**
     * 启用/禁用提醒
     * 
     * @param reminderId 提醒ID
     * @param enabled    是否启用（0禁用 1启用）
     * @return 是否成功
     */
    boolean updateReminderStatus(Long reminderId, String enabled);

    /**
     * 启用/禁用提醒（boolean版本）
     * 
     * @param reminderId 提醒ID
     * @param enabled    是否启用（true启用 false禁用）
     * @return 是否成功
     */
    boolean enableReminder(Long reminderId, boolean enabled);
}
