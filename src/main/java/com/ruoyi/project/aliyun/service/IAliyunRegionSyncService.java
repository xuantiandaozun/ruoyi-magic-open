package com.ruoyi.project.aliyun.service;

import java.util.List;

import com.ruoyi.project.aliyun.domain.vo.AliyunRegionVO;

/**
 * 阿里云地域同步服务接口
 * 
 * @author ruoyi
 */
public interface IAliyunRegionSyncService {

    /**
     * 从阿里云获取地域信息
     * 
     * @return 地域信息列表
     */
    List<AliyunRegionVO> fetchAliyunRegions();

    /**
     * 同步阿里云地域信息到数据字典
     * 
     * @return 同步结果
     */
    boolean syncRegionsToDict();

    /**
     * 初始化阿里云地域字典类型
     * 
     * @return 初始化结果
     */
    boolean initRegionDictType();
}
