package com.ruoyi.project.aliyun.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aliyun.sdk.service.rds20140815.AsyncClient;
import com.aliyun.sdk.service.rds20140815.models.DescribeRegionsRequest;
import com.aliyun.sdk.service.rds20140815.models.DescribeRegionsResponse;
import com.aliyun.sdk.service.rds20140815.models.DescribeRegionsResponseBody.RDSRegion;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.service.AliyunService;
import com.ruoyi.project.aliyun.domain.vo.AliyunRegionVO;
import com.ruoyi.project.aliyun.service.IAliyunRegionSyncService;
import com.ruoyi.project.system.domain.SysDictData;
import com.ruoyi.project.system.domain.SysDictType;
import com.ruoyi.project.system.service.ISysDictDataService;
import com.ruoyi.project.system.service.ISysDictTypeService;

import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云地域同步服务实现类
 * 
 * @author ruoyi
 */
@Slf4j
@Service
public class AliyunRegionSyncServiceImpl implements IAliyunRegionSyncService {

    @Autowired
    private AliyunService aliyunService;

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private ISysDictDataService dictDataService;

    /** 阿里云地域字典类型 */
    private static final String ALIYUN_REGION_DICT_TYPE = "aliyun_region";

    /** 阿里云地域字典名称 */
    private static final String ALIYUN_REGION_DICT_NAME = "阿里云地域";

    @Override
    public List<AliyunRegionVO> fetchAliyunRegions() {
        try {
            // 获取所有可用的阿里云凭证
            List<AliyunCredential> credentials = aliyunService.getAllCredentials();
            if (credentials.isEmpty()) {
                throw new RuntimeException("未找到可用的阿里云凭证，请先配置阿里云密钥");
            }

            // 使用第一个凭证获取地域信息
            AliyunCredential credential = credentials.get(0);
            return aliyunService.executeWithCredential("RDS", credential, (AsyncClient client) -> {
                try {
                    // 构建请求
                    DescribeRegionsRequest request = DescribeRegionsRequest.builder()
                            .acceptLanguage("zh-CN")
                            .build();

                    // 发送请求
                    CompletableFuture<DescribeRegionsResponse> response = client.describeRegions(request);
                    DescribeRegionsResponse resp = response.get();

                    // 转换数据
                    List<AliyunRegionVO> regions = new ArrayList<>();
                    List<RDSRegion> rdsRegions = resp.getBody().getRegions().getRDSRegion();

                    for (RDSRegion rdsRegion : rdsRegions) {
                        AliyunRegionVO regionVO = new AliyunRegionVO();
                        regionVO.setLocalName(rdsRegion.getLocalName());
                        regionVO.setRegionEndpoint(rdsRegion.getRegionEndpoint());
                        regionVO.setRegionId(rdsRegion.getRegionId());
                        regionVO.setZoneId(rdsRegion.getZoneId());
                        regionVO.setZoneName(rdsRegion.getZoneName());
                        regions.add(regionVO);
                    }

                    return regions;
                } catch (Exception e) {
                    log.error("获取阿里云地域信息失败", e);
                    throw new RuntimeException("获取阿里云地域信息失败: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            log.error("获取阿里云地域信息失败", e);
            throw new RuntimeException("获取阿里云地域信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean syncRegionsToDict() {
        try {
            // 1. 初始化字典类型
            if (!initRegionDictType()) {
                log.error("初始化阿里云地域字典类型失败");
                return false;
            }

            // 2. 获取阿里云地域信息
            List<AliyunRegionVO> regions = fetchAliyunRegions();
            if (regions == null || regions.isEmpty()) {
                log.warn("未获取到阿里云地域信息");
                return false;
            }

            // 3. 清除旧的字典数据
            SysDictData queryDict = new SysDictData();
            queryDict.setDictType(ALIYUN_REGION_DICT_TYPE);
            List<SysDictData> existingData = dictDataService.selectDictDataList(queryDict);
            for (SysDictData data : existingData) {
                dictDataService.removeById(data.getDictCode());
            }

            // 4. 插入新的字典数据（去重和过滤）
            long sortOrder = 1;
            Set<String> insertedRegionIds = new HashSet<>();

            for (AliyunRegionVO region : regions) {
                // 跳过包含"已关停"的地域
                if (region.getLocalName() != null && region.getLocalName().contains("已关停")) {
                    log.debug("跳过已关停地域: {}", region.getLocalName());
                    continue;
                }

                // 跳过重复的regionId
                if (insertedRegionIds.contains(region.getRegionId())) {
                    log.debug("跳过重复的regionId: {}", region.getRegionId());
                    continue;
                }

                SysDictData dictData = new SysDictData();
                dictData.setDictType(ALIYUN_REGION_DICT_TYPE);
                dictData.setDictLabel(region.getLocalName());
                dictData.setDictValue(region.getRegionId());
                dictData.setDictSort(sortOrder++);
                dictData.setStatus("0"); // 正常状态
                dictData.setIsDefault("N");
                dictData.setCreateTime(new Date());
                dictData.setUpdateTime(new Date());

                dictDataService.save(dictData);
                insertedRegionIds.add(region.getRegionId());
            }

            log.info("成功同步{}个阿里云地域到数据字典", regions.size());
            return true;
        } catch (Exception e) {
            log.error("同步阿里云地域到数据字典失败", e);
            return false;
        }
    }

    @Override
    public boolean initRegionDictType() {
        try {
            // 检查字典类型是否已存在
            SysDictType existingType = dictTypeService.selectDictTypeByType(ALIYUN_REGION_DICT_TYPE);

            if (existingType != null) {
                log.info("阿里云地域字典类型已存在，跳过初始化");
                return true;
            }

            // 创建新的字典类型
            SysDictType dictType = new SysDictType();
            dictType.setDictName(ALIYUN_REGION_DICT_NAME);
            dictType.setDictType(ALIYUN_REGION_DICT_TYPE);
            dictType.setStatus("0"); // 正常状态
            dictType.setCreateTime(new Date());
            dictType.setUpdateTime(new Date());

            boolean result = dictTypeService.save(dictType);
            if (result) {
                log.info("成功创建阿里云地域字典类型");
            } else {
                log.error("创建阿里云地域字典类型失败");
            }

            return result;
        } catch (Exception e) {
            log.error("初始化阿里云地域字典类型失败", e);
            return false;
        }
    }
}
