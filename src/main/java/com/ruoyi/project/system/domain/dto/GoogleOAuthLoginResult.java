package com.ruoyi.project.system.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Google OAuth 登录结果
 */
@Data
@Builder
public class GoogleOAuthLoginResult {

    /** 系统 token（Sa-Token） */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 昵称 */
    private String nickName;

    /** 邮箱 */
    private String email;

    /** 头像 URL */
    private String avatar;

    /** 用户等级：free / pro 等 */
    private String userTier;

    /** 今日已用请求次数 */
    private Integer todayUsedRequests;

    /** 今日请求上限（-1 表示无限制） */
    private Integer todayRequestLimit;
}
