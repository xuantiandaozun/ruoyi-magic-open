package com.ruoyi.project.miniapp.util;

/**
 * 微信内容安全 scene 场景值
 */
public final class MiniAppContentSecurityScene {

    /** 资料（昵称、头像等） */
    public static final int PROFILE = 1;
    /** 评论（反馈等） */
    public static final int COMMENT = 2;
    /** 论坛 */
    public static final int FORUM = 3;
    /** 社交日志（翻译输入等） */
    public static final int SOCIAL = 4;

    public static final String VIOLATION_MESSAGE = "您发布的内容含违规信息";

    private MiniAppContentSecurityScene() {
    }
}
