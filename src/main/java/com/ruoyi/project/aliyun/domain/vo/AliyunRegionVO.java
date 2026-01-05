package com.ruoyi.project.aliyun.domain.vo;

import lombok.Data;

/**
 * 阿里云地域信息VO
 * 
 * @author ruoyi
 */
@Data
public class AliyunRegionVO {

    /** 地域本地名称 */
    private String localName;

    /** 地域端点 */
    private String regionEndpoint;

    /** 地域ID */
    private String regionId;

    /** 可用区ID */
    private String zoneId;

    /** 可用区名称 */
    private String zoneName;
}
