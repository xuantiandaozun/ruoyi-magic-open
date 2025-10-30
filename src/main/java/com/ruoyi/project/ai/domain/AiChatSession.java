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
 * AI聊天会话对象 ai_chat_session
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_chat_session")
public class AiChatSession extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 会话名称 */
    @Excel(name = "会话名称")
    private String sessionName;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 使用的模型配置ID */
    @Excel(name = "模型配置ID")
    private Long modelConfigId;

    /** 系统提示词 */
    @Excel(name = "系统提示词")
    private String systemPrompt;

    /** 会话类型（chat=普通聊天, workflow=工作流） */
    @Excel(name = "会话类型", readConverterExp = "chat=普通聊天,workflow=工作流")
    private String sessionType;

    /** 状态（0=正常 1=已结束 2=已删除） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=已结束,2=已删除")
    private String status;

    /** 消息数量 */
    @Excel(name = "消息数量")
    private Integer messageCount;

    /** 最后消息时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后消息时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastMessageTime;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}