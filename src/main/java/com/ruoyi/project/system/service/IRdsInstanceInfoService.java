package com.ruoyi.project.system.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.system.domain.RdsInstanceInfo;
import com.ruoyi.framework.web.domain.AjaxResult;

/**
 * RDS实例管理Service接口
 * 
 * @author ruoyi
 * @date 2025-07-11 17:49:40
 */
public interface IRdsInstanceInfoService extends IService<RdsInstanceInfo>
{
    /**
     * 同步阿里云RDS实例数据
     * 
     * @return 同步结果
     */
    AjaxResult syncAliyunRdsInstances();
}
