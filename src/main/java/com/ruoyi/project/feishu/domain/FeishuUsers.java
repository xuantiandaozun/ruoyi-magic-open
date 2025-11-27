package com.ruoyi.project.feishu.domain;

import java.io.Serializable;
import java.util.Date;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;

import lombok.Data;

/**
 * 飞书用户信息对象 feishu_users
 * 
 * @author ruoyi
 * @date 2025-11-27
 */
@Data
@Table("feishu_users")
public class FeishuUsers implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 飞书用户open_id */
    @Excel(name = "飞书用户open_id")
    private String openId;

    /** 飞书用户union_id */
    @Excel(name = "飞书用户union_id")
    private String unionId;

    /** 飞书用户user_id */
    @Excel(name = "飞书用户user_id")
    private String userId;

    /** 飞书用户名称 */
    @Excel(name = "飞书用户名称")
    private String name;

    /** 飞书用户邮箱 */
    @Excel(name = "飞书用户邮箱")
    private String email;

    /** 飞书用户手机号 */
    @Excel(name = "飞书用户手机号")
    private String mobile;

    /** 创建时间 */
    @Column("created_at")
    private Date createdAt;

    /** 修改时间 */
    @Column("updated_at")
    private Date updatedAt;

    /** 关联的密钥ID */
    @Excel(name = "关联的密钥ID")
    private Long keyId;

    /** 关联的密钥名称 */
    @Excel(name = "关联的密钥名称")
    private String keyName;
}
