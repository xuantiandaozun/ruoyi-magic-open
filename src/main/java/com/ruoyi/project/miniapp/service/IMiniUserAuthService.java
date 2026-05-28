package com.ruoyi.project.miniapp.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.miniapp.domain.MiniUserAuth;

public interface IMiniUserAuthService extends IService<MiniUserAuth> {
    MiniUserAuth getByMiniAppAndOpenid(Long miniAppId, String openid);

    MiniUserAuth getByMiniUserAndApp(Long miniUserId, Long miniAppId);
}
