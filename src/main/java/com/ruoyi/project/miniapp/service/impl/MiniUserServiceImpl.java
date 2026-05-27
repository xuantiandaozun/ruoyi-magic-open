package com.ruoyi.project.miniapp.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.miniapp.domain.MiniUser;
import com.ruoyi.project.miniapp.mapper.MiniUserMapper;
import com.ruoyi.project.miniapp.service.IMiniUserService;

@Service
@UseDataSource("MASTER")
public class MiniUserServiceImpl extends ServiceImpl<MiniUserMapper, MiniUser> implements IMiniUserService {
}
