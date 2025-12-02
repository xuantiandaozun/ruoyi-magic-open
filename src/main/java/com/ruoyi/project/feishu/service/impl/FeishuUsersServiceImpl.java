package com.ruoyi.project.feishu.service.impl;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.feishu.domain.FeishuUsers;
import com.ruoyi.project.feishu.mapper.FeishuUsersMapper;
import com.ruoyi.project.feishu.service.IFeishuUsersService;

/**
 * 飞书用户信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-11-27
 */
@Service
public class FeishuUsersServiceImpl extends ServiceImpl<FeishuUsersMapper, FeishuUsers> implements IFeishuUsersService {

    @Override
    public FeishuUsers selectByOpenId(String openId) {
        return getOne(QueryWrapper.create()
                .from("feishu_users")
                .where("open_id = ?", openId));
    }

    @Override
    public FeishuUsers selectByMobile(String mobile) {
        return getOne(QueryWrapper.create()
                .from("feishu_users")
                .where("mobile = ?", mobile));
    }

    @Override
    public boolean saveOrUpdateUser(FeishuUsers feishuUsers) {
        FeishuUsers existUser = selectByOpenId(feishuUsers.getOpenId());
        if (existUser != null) {
            feishuUsers.setId(existUser.getId());
            feishuUsers.setUpdatedAt(new Date());
            return updateById(feishuUsers);
        } else {
            feishuUsers.setCreatedAt(new Date());
            feishuUsers.setUpdatedAt(new Date());
            return save(feishuUsers);
        }
    }
}
