package com.ruoyi.project.bill.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.bill.domain.BillUserProfile;

/**
 * 记账用户扩展Service接口
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
public interface IBillUserProfileService extends IService<BillUserProfile> {
    /**
     * 根据用户ID查询扩展信息
     * 
     * @param userId 用户ID
     * @return 用户扩展信息
     */
    BillUserProfile selectByUserId(Long userId);

    /**
     * 创建或更新用户扩展信息
     * 
     * @param profile 用户扩展信息
     * @return 是否成功
     */
    boolean saveOrUpdateProfile(BillUserProfile profile);

    /**
     * 根据用户ID创建或更新用户扩展信息
     * 
     * @param profile 用户扩展信息
     * @return 是否成功
     */
    boolean saveOrUpdateByUserId(BillUserProfile profile);

    /**
     * 根据家庭组ID查询成员列表
     * 
     * @param familyId 家庭组ID
     * @return 成员列表
     */
    java.util.List<BillUserProfile> selectByFamilyId(Long familyId);
}
