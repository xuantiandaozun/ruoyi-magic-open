package com.ruoyi.project.ai.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI聊天消息对象 ai_chat_message
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_chat_message")
public class AiChatMessage extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 会话ID */
    @Excel(name = "会话ID")
    private Long sessionId;

    /** 父消息ID（用于消息树结构） */
    @Excel(name = "父消息ID")
    private Long parentMessageId;

    /** 消息角色（user=用户, assistant=AI助手, system=系统） */
    @Excel(name = "消息角色", readConverterExp = "user=用户,assistant=AI助手,system=系统")
    private String messageRole;

    /** 消息内容 */
    @Excel(name = "消息内容")
    private String messageContent;

    /** 消息类型（text=文本, image=图片, file=文件, tool_call=工具调用） */
    @Excel(name = "消息类型", readConverterExp = "text=文本,image=图片,file=文件,tool_call=工具调用")
    private String messageType;

    /** 使用的模型配置ID */
    @Excel(name = "模型配置ID")
    private Long modelConfigId;

    /** Token数量 */
    @Excel(name = "Token数量")
    private Integer tokenCount;

    /** 工具调用信息（JSON格式） */
    @Excel(name = "工具调用信息")
    private String toolCalls;

    /** 元数据（JSON格式，存储额外信息） */
    @Excel(name = "元数据")
    private String metadata;

    /** 消息顺序 */
    @Excel(name = "消息顺序")
    private Integer messageOrder;

    /** 是否删除（0=否 1=是） */
    @Excel(name = "是否删除", readConverterExp = "0=否,1=是")
    private String isDeleted;
}