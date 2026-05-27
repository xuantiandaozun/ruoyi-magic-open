package com.ruoyi.project.miniapp.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.miniapp.domain.MiniApp;

public interface IMiniAppService extends IService<MiniApp> {
    MiniApp getEnabledByAppCode(String appCode);
}
