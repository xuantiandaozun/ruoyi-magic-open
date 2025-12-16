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

    @Override
    public java.util.List<BillUserProfile> selectByFamilyId(Long familyId) {
        // 查询家庭组的所有成员扩展信息
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("family_id", familyId);
        java.util.List<BillUserProfile> profiles = this.list(queryWrapper);

        // 关联查询用户基本信息
        if (profiles != null && !profiles.isEmpty()) {
            // 获取所有用户ID
            java.util.List<Long> userIds = profiles.stream()
                    .map(BillUserProfile::getUserId)
                    .collect(java.util.stream.Collectors.toList());

            // 查询用户信息
            com.ruoyi.project.system.service.ISysUserService userService = com.ruoyi.common.utils.spring.SpringUtils
                    .getBean(com.ruoyi.project.system.service.ISysUserService.class);
            java.util.List<com.ruoyi.project.system.domain.SysUser> users = userService.listByIds(userIds);

            // 创建用户ID到用户信息的映射
            java.util.Map<Long, com.ruoyi.project.system.domain.SysUser> userMap = users.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            com.ruoyi.project.system.domain.SysUser::getUserId,
                            user -> user));

            // 将用户信息合并到扩展信息中
            for (BillUserProfile profile : profiles) {
                com.ruoyi.project.system.domain.SysUser user = userMap.get(profile.getUserId());
                if (user != null) {
                    // 直接设置到临时字段中
                    profile.setNickName(user.getNickName());
                    profile.setAvatar(user.getAvatar());
                    profile.setPhonenumber(user.getPhonenumber());
                }
            }
        }

        return profiles;
    }
}
