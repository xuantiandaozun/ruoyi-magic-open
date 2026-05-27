package com.ruoyi.project.miniapp.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.miniapp.domain.MiniUserAuth;
import com.ruoyi.project.miniapp.mapper.MiniUserAuthMapper;
import com.ruoyi.project.miniapp.service.IMiniUserAuthService;

@Service
@UseDataSource("MASTER")
public class MiniUserAuthServiceImpl extends ServiceImpl<MiniUserAuthMapper, MiniUserAuth> implements IMiniUserAuthService {
    @Override
    public MiniUserAuth getByMiniAppAndOpenid(Long miniAppId, String openid) {
        QueryWrapper qw = QueryWrapper.create()
                .from("mini_user_auth")
                .where("mini_app_id = ?", miniAppId)
                .and("openid = ?", openid)
                .limit(1);
        return getOne(qw);
    }
}
