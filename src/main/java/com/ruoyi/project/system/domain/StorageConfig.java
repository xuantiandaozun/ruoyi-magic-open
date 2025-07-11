package com.ruoyi.project.system.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 存储配置对象 sys_storage_config
 * 
 * @author ruoyi
 * @date 2025-07-11 11:32:00
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_storage_config")
public class StorageConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    @Id(keyType = KeyType.Auto)
    private String configId;

    /** 存储类型（local-本地存储, aliyun-阿里云OSS, tencent-腾讯云COS, amazon-亚马逊S3, azure-微软Azure） */
    @Excel(name = "存储类型", readConverterExp = "l=ocal-本地存储,,a=liyun-阿里云OSS,,t=encent-腾讯云COS,,a=mazon-亚马逊S3,,a=zure-微软Azure")
    private String storageType;

    /** 配置名称 */
    @Excel(name = "配置名称")
    private String configName;

    /** 是否默认配置（N否 Y是） */
    @Excel(name = "是否默认配置", readConverterExp = "N=否,Y=是")
    private String isDefault;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 配置数据（JSON格式） */
    private String configData;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

}
