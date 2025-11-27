package com.ruoyi.project.feishu.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 飞书消息发送记录对象 feishu_message_record
 * 
 * @author ruoyi
 * @date 2025-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("feishu_message_record")
public class FeishuMessageRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 接收者ID */
    @Excel(name = "接收者ID")
    private String receiveId;

    /** 接收者ID类型 */
    @Excel(name = "接收者ID类型")
    private String receiveIdType;

    /** 消息类型 */
    @Excel(name = "消息类型")
    private String msgType;

    /** 消息内容 */
    @Excel(name = "消息内容")
    private String content;

    /** 消息UUID */
    @Excel(name = "消息UUID")
    private String uuid;

    /** 发送状态：0-失败，1-成功 */
    @Excel(name = "发送状态", readConverterExp = "0=失败,1=成功")
    private Integer status;

    /** 发送结果 */
    @Excel(name = "发送结果")
    private String result;

    /** 使用的密钥ID */
    @Excel(name = "使用的密钥ID")
    private Long keyId;

    /** 使用的密钥名称 */
    @Excel(name = "使用的密钥名称")
    private String keyName;

    /** 发送时间 */
    @Excel(name = "发送时间")
    private String sendTime;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}