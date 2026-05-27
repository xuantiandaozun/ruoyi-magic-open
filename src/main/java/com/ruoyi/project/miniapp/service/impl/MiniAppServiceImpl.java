package com.ruoyi.project.miniapp.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.miniapp.domain.MiniApp;
import com.ruoyi.project.miniapp.mapper.MiniAppMapper;
import com.ruoyi.project.miniapp.service.IMiniAppService;

@Service
@UseDataSource("MASTER")
public class MiniAppServiceImpl extends ServiceImpl<MiniAppMapper, MiniApp> implements IMiniAppService {
    @Override
    public MiniApp getEnabledByAppCode(String appCode) {
        QueryWrapper qw = QueryWrapper.create()
                .from("mini_app")
                .where("app_code = ?", appCode)
                .and("enabled = 'Y'")
                .and("status = '0'")
                .and("del_flag = '0'")
                .limit(1);
        return getOne(qw);
    }
}
