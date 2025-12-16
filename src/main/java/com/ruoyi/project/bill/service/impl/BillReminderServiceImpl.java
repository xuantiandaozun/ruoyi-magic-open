package com.ruoyi.project.bill.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.bill.domain.BillReminder;
import com.ruoyi.project.bill.mapper.BillReminderMapper;
import com.ruoyi.project.bill.service.IBillReminderService;

/**
 * 提醒设置Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Service
public class BillReminderServiceImpl extends ServiceImpl<BillReminderMapper, BillReminder>
        implements IBillReminderService {
    @Override
    public List<BillReminder> selectReminderList(Long userId, String reminderType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId);

        if (reminderType != null && !reminderType.isEmpty()) {
            queryWrapper.eq("reminder_type", reminderType);
        }

        return this.list(queryWrapper);
    }

    @Override
    public boolean updateReminderStatus(Long reminderId, String enabled) {
        BillReminder reminder = this.getById(reminderId);
        if (reminder != null) {
            reminder.setEnabled(enabled);
            return this.updateById(reminder);
        }
        return false;
    }

    @Override
    public List<BillReminder> selectByUserId(Long userId) {
        return selectReminderList(userId, null);
    }

    @Override
    public List<BillReminder> selectEnabledByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId)
                .eq("enabled", "1");
        return this.list(queryWrapper);
    }

    @Override
    public boolean enableReminder(Long reminderId, boolean enabled) {
        return updateReminderStatus(reminderId, enabled ? "1" : "0");
    }
}
