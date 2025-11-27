package com.ruoyi.project.feishu.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.feishu.domain.FeishuUsers;

/**
 * 飞书用户信息Service接口
 * 
 * @author ruoyi
 * @date 2025-11-27
 */
public interface IFeishuUsersService extends IService<FeishuUsers> {

    /**
     * 根据open_id查询飞书用户
     * 
     * @param openId 飞书用户open_id
     * @return 飞书用户信息
     */
    FeishuUsers selectByOpenId(String openId);

    /**
     * 根据mobile查询飞书用户
     * 
     * @param mobile 手机号
     * @return 飞书用户信息
     */
    FeishuUsers selectByMobile(String mobile);

    /**
     * 保存或更新飞书用户信息
     * 
     * @param feishuUsers 飞书用户信息
     * @return 结果
     */
    boolean saveOrUpdateUser(FeishuUsers feishuUsers);
}
