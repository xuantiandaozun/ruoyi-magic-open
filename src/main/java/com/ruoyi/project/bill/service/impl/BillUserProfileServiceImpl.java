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
    public BillUserProfile selectByMiniUserId(Long miniUserId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("mini_user_id", miniUserId);
        return this.getOne(queryWrapper);
    }

    @Override
    public BillUserProfile ensureForMiniUser(Long miniUserId, String openid) {
        BillUserProfile profile = selectByMiniUserId(miniUserId);
        if (profile != null) {
            if (openid != null && !openid.equals(profile.getWechatOpenid())) {
                profile.setWechatOpenid(openid);
                this.updateById(profile);
            }
            return profile;
        }

        // uk_user_id 按 user_id 唯一；清理 mini_user 后可能残留仅 user_id 匹配的旧扩展行
        profile = selectByUserId(miniUserId);
        if (profile != null) {
            profile.setMiniUserId(miniUserId);
            if (openid != null) {
                profile.setWechatOpenid(openid);
            }
            if (profile.getBudgetAlertEnabled() == null) {
                profile.setBudgetAlertEnabled("1");
            }
            if (profile.getDailyRemindEnabled() == null) {
                profile.setDailyRemindEnabled("0");
            }
            this.updateById(profile);
            return profile;
        }

        profile = new BillUserProfile();
        profile.setMiniUserId(miniUserId);
        profile.setUserId(miniUserId);
        profile.setWechatOpenid(openid);
        profile.setBudgetAlertEnabled("1");
        profile.setDailyRemindEnabled("0");
        this.save(profile);
        return profile;
    }

    @Override
    public boolean saveOrUpdateProfile(BillUserProfile profile) {
        BillUserProfile existProfile = profile.getMiniUserId() != null
                ? selectByMiniUserId(profile.getMiniUserId())
                : selectByUserId(profile.getUserId());
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

    @Override
    public java.util.List<BillUserProfile> selectByFamilyId(Long familyId) {
        // 查询家庭组的所有成员扩展信息
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("family_id", familyId);
        java.util.List<BillUserProfile> profiles = this.list(queryWrapper);

        // 关联查询用户基本信息
        if (profiles != null && !profiles.isEmpty()) {
            java.util.List<Long> userIds = profiles.stream()
                    .filter(profile -> profile.getMiniUserId() == null)
                    .map(BillUserProfile::getUserId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());

            java.util.List<Long> miniUserIds = profiles.stream()
                    .map(BillUserProfile::getMiniUserId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());

            java.util.Map<Long, com.ruoyi.project.system.domain.SysUser> userMap = new java.util.HashMap<>();
            if (!userIds.isEmpty()) {
                com.ruoyi.project.system.service.ISysUserService userService = com.ruoyi.common.utils.spring.SpringUtils
                        .getBean(com.ruoyi.project.system.service.ISysUserService.class);
                java.util.List<com.ruoyi.project.system.domain.SysUser> users = userService.listByIds(userIds);
                userMap = users.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.ruoyi.project.system.domain.SysUser::getUserId,
                                user -> user,
                                (a, b) -> a));
            }

            java.util.Map<Long, com.ruoyi.project.miniapp.domain.MiniUser> miniUserMap = new java.util.HashMap<>();
            if (!miniUserIds.isEmpty()) {
                com.ruoyi.project.miniapp.service.IMiniUserService miniUserService = com.ruoyi.common.utils.spring.SpringUtils
                        .getBean(com.ruoyi.project.miniapp.service.IMiniUserService.class);
                java.util.List<com.ruoyi.project.miniapp.domain.MiniUser> miniUsers = miniUserService.listByIds(miniUserIds);
                miniUserMap = miniUsers.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.ruoyi.project.miniapp.domain.MiniUser::getId,
                                user -> user,
                                (a, b) -> a));
            }

            for (BillUserProfile profile : profiles) {
                com.ruoyi.project.system.domain.SysUser user = userMap.get(profile.getUserId());
                if (user != null) {
                    profile.setNickName(user.getNickName());
                    profile.setAvatar(user.getAvatar());
                    profile.setPhonenumber(user.getPhonenumber());
                    continue;
                }
                com.ruoyi.project.miniapp.domain.MiniUser miniUser = miniUserMap.get(profile.getMiniUserId());
                if (miniUser != null) {
                    profile.setNickName(miniUser.getNickname());
                    profile.setAvatar(miniUser.getAvatar());
                    profile.setPhonenumber(miniUser.getMobile());
                }
            }
        }

        return profiles;
    }
}
