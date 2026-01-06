package com.ruoyi.project.secretkey.domain;

import java.util.Date;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密钥管理对象 secret_key_info
 * 
 * @author ruoyi
 * @date 2025-07-11 17:46:46
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("secret_key_info")
public class SecretKeyInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 厂商类型(1云厂商 2应用厂商) */
    @Excel(name = "厂商类型", readConverterExp = "1=云厂商,2=应用厂商")
    private String providerType;

    /** 厂商名称 */
    @Excel(name = "厂商名称")
    private String providerName;

    /** 厂商品牌 */
    @Excel(name = "厂商品牌")
    private String providerBrand;

    /** 密钥类型 */
    @Excel(name = "密钥类型")
    private String keyType;

    /** 密钥名称/别名 */
    @Excel(name = "密钥名称/别名")
    private String keyName;

    /** 访问密钥 */
    private String accessKey;

    /** 密钥 */
    private String secretKey;

    /** 使用范围 */
    @Excel(name = "使用范围")
    private String scopeType;

    /** 范围名称 */
    private String scopeName;

    /** 地域 */
    private String region;

    /** 过期时间 */
    private Date expireTime;

    /** 状态(0正常 1停用) */
    private String status;

    /** 删除标志(0存在 2删除) */
    @Column(isLogicDelete = true)
    private String delFlag;

}
