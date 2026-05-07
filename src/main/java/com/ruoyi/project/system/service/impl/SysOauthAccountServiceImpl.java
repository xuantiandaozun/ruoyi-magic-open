package com.ruoyi.project.system.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.system.domain.SysOauthAccount;
import com.ruoyi.project.system.mapper.SysOauthAccountMapper;
import com.ruoyi.project.system.service.ISysOauthAccountService;

@Service
@UseDataSource("MASTER")
public class SysOauthAccountServiceImpl extends ServiceImpl<SysOauthAccountMapper, SysOauthAccount>
        implements ISysOauthAccountService {
}
