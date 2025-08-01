package com.ruoyi.project.feishu.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.annotation.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 飞书文档信息对象 feishu_doc
 * 
 * @author ruoyi
 * @date 2025-07-31 16:47:44
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("feishu_doc")
public class FeishuDoc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Id(keyType = KeyType.Auto)
    private String id;

    /** 文档token */
    @Excel(name = "文档token")
    private String token;

    /** 文档名称 */
    @Excel(name = "文档名称")
    private String name;

    /** 文档类型(doc/sheet/bitable/mindnote/file/folder) */
    @Excel(name = "文档类型(doc/sheet/bitable/mindnote/file/folder)")
    private String type;

    /** 文档访问URL */
    @Excel(name = "文档访问URL")
    private String url;

    /** 拥有者ID */
    @Excel(name = "拥有者ID")
    private String ownerId;

    /** 父文件夹token */
    @Excel(name = "父文件夹token")
    private String parentToken;

    /** 是否为文件夹(0-否,1-是) */
    @Excel(name = "是否为文件夹(0-否,1-是)")
    private Integer isFolder;

    /** 文档内容(缓存) */
    @Excel(name = "文档内容(缓存)")
    private String content;

    /** 飞书创建时间(时间戳) */
    @Excel(name = "飞书创建时间(时间戳)")
    private String feishuCreatedTime;

    /** 飞书修改时间(时间戳) */
    @Excel(name = "飞书修改时间(时间戳)")
    private String feishuModifiedTime;

    /** 关联的密钥名称 */
    @Excel(name = "关联的密钥名称")
    private String keyName;

}
