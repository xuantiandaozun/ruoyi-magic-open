package com.ruoyi.project.secretkey.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.secretkey.domain.SecretKeyInfo;
import com.ruoyi.project.secretkey.mapper.SecretKeyInfoMapper;
import com.ruoyi.project.secretkey.service.ISecretKeyInfoService;

/**
 * 密钥管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-07-11 17:46:46
 */
@Service
public class SecretKeyInfoServiceImpl extends ServiceImpl<SecretKeyInfoMapper, SecretKeyInfo>
        implements ISecretKeyInfoService {
    // 可以添加自定义的业务方法
}
