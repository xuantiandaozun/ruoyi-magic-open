package com.ruoyi.project.bill.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.bill.domain.BillFamily;

/**
 * 家庭组Service接口
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
public interface IBillFamilyService extends IService<BillFamily> {
    /**
     * 生成唯一的家庭组邀请码
     * 
     * @return 邀请码
     */
    String generateFamilyCode();

    /**
     * 生成唯一的家庭组邀请码（别名方法）
     * 
     * @return 邀请码
     */
    String generateInviteCode();

    /**
     * 根据邀请码查询家庭组
     * 
     * @param familyCode 邀请码
     * @return 家庭组信息
     */
    BillFamily selectByFamilyCode(String familyCode);

    /**
     * 根据邀请码查询家庭组（别名方法）
     * 
     * @param inviteCode 邀请码
     * @return 家庭组信息
     */
    BillFamily selectByInviteCode(String inviteCode);

    /**
     * 更新家庭组成员数量
     * 
     * @param familyId 家庭组ID
     * @return 是否成功
     */
    boolean updateMemberCount(Long familyId);

    /**
     * 更新家庭组成员数量（增加或减少）
     * 
     * @param familyId  家庭组ID
     * @param increment 增量（正数为增加，负数为减少）
     * @return 是否成功
     */
    boolean updateMemberCount(Long familyId, int increment);
}
