package com.ruoyi.project.bill.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.bill.domain.BillFamily;
import com.ruoyi.project.bill.mapper.BillFamilyMapper;
import com.ruoyi.project.bill.service.IBillFamilyService;

/**
 * 家庭组Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Service
public class BillFamilyServiceImpl extends ServiceImpl<BillFamilyMapper, BillFamily> implements IBillFamilyService {
    @Override
    public String generateFamilyCode() {
        // 生成6位随机邀请码
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (selectByFamilyCode(code) != null);

        return code;
    }

    @Override
    public BillFamily selectByFamilyCode(String familyCode) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("family_code", familyCode);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean updateMemberCount(Long familyId) {
        // 统计家庭组成员数量
        QueryWrapper countWrapper = QueryWrapper.create()
                .from("bill_user_profile")
                .eq("family_id", familyId);

        long count = getMapper().selectCountByQuery(countWrapper);

        BillFamily family = this.getById(familyId);
        if (family != null) {
            family.setMemberCount((int) count);
            return this.updateById(family);
        }
        return false;
    }

    @Override
    public String generateInviteCode() {
        return generateFamilyCode();
    }

    @Override
    public BillFamily selectByInviteCode(String inviteCode) {
        return selectByFamilyCode(inviteCode);
    }

    @Override
    public boolean updateMemberCount(Long familyId, int increment) {
        BillFamily family = this.getById(familyId);
        if (family == null) {
            return false;
        }
        family.setMemberCount(family.getMemberCount() + increment);
        return this.updateById(family);
    }
}
