package com.ruoyi.project.bill.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.mapper.BillUserProfileMapper;
import com.ruoyi.project.bill.service.IBillUserProfileService;

/**
 * 记账用户扩展Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Service
public class BillUserProfileServiceImpl extends ServiceImpl<BillUserProfileMapper, BillUserProfile>
        implements IBillUserProfileService {
    @Override
    public BillUserProfile selectByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean saveOrUpdateProfile(BillUserProfile profile) {
        BillUserProfile existProfile = selectByUserId(profile.getUserId());
        if (existProfile != null) {
            profile.setProfileId(existProfile.getProfileId());
            return this.updateById(profile);
        } else {
            return this.save(profile);
        }
    }

    @Override
    public boolean saveOrUpdateByUserId(BillUserProfile profile) {
        return saveOrUpdateProfile(profile);
    }
}
