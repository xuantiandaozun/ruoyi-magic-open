package com.ruoyi.project.feishu.domain.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 飞书用户信息DTO
 * 对应飞书API返回的用户信息结构
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Data
public class FeishuUserInfoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户的 union_id，是应用开发商发布的不同应用中同一用户的标识 */
    private String unionId;

    /** 用户的 user_id，租户内用户的唯一标识 */
    private String userId;

    /** 用户的 open_id，应用内用户的唯一标识 */
    private String openId;

    /** 用户名 */
    private String name;

    /** 英文名 */
    private String enName;

    /** 别名 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String mobile;

    /** 手机号码是否可见 */
    private Boolean mobileVisible;

    /** 性别（0：保密，1：男，2：女，3：其他） */
    private Integer gender;

    /** 头像的文件 Key */
    private String avatarKey;

    /** 用户头像信息 */
    private AvatarInfo avatar;

    /** 用户状态 */
    private UserStatus status;

    /** 用户所属部门的 ID 列表 */
    private List<String> departmentIds;

    /** 用户的直接主管的用户ID */
    private String leaderUserId;

    /** 工作城市 */
    private String city;

    /** 国家或地区 Code 缩写 */
    private String country;

    /** 工位 */
    private String workStation;

    /** 入职时间（秒级时间戳） */
    private Long joinTime;

    /** 用户是否为租户超级管理员 */
    private Boolean isTenantManager;

    /** 工号 */
    private String employeeNo;

    /** 员工类型 */
    private Integer employeeType;

    /** 用户排序信息 */
    private List<UserOrder> orders;

    /** 自定义字段 */
    private List<UserCustomAttr> customAttrs;

    /** 企业邮箱 */
    private String enterpriseEmail;

    /** 职务 */
    private String jobTitle;

    /** 是否为暂停状态的用户 */
    private Boolean isFrozen;

    /** 数据驻留地 */
    private String geo;

    /** 职级 ID */
    private String jobLevelId;

    /** 序列 ID */
    private String jobFamilyId;

    /** 部门路径 */
    private List<DepartmentDetail> departmentPath;

    /** 虚线上级的用户 ID */
    private List<String> dottedLineLeaderUserIds;

    /**
     * 头像信息
     */
    @Data
    public static class AvatarInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 头像图片链接 */
        private String avatarOrigin;
        
        /** 72x72尺寸头像 */
        private String avatar72;
        
        /** 240x240尺寸头像 */
        private String avatar240;
        
        /** 640x640尺寸头像 */
        private String avatar640;
    }

    /**
     * 用户状态
     */
    @Data
    public static class UserStatus implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 是否被冻结 */
        private Boolean isFrozen;
        
        /** 是否已离职 */
        private Boolean isResigned;
        
        /** 是否已激活 */
        private Boolean isActivated;
        
        /** 是否已退出 */
        private Boolean isExited;
    }

    /**
     * 用户排序信息
     */
    @Data
    public static class UserOrder implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 部门ID */
        private String departmentId;
        
        /** 用户在部门中的排序 */
        private Integer userOrder;
        
        /** 部门内的排序类型 */
        private String departmentOrderType;
    }

    /**
     * 自定义字段
     */
    @Data
    public static class UserCustomAttr implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 字段类型 */
        private String fieldType;
        
        /** 字段名称 */
        private String fieldName;
        
        /** 字段值 */
        private Object value;
    }

    /**
     * 部门详情
     */
    @Data
    public static class DepartmentDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 部门ID */
        private String departmentId;
        
        /** 部门名称 */
        private String departmentName;
    }
}