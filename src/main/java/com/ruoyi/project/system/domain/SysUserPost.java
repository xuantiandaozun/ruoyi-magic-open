package com.ruoyi.project.system.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import lombok.Data;

/**
 * 用户和岗位关联 sys_user_post
 * 
 * @author ruoyi
 */
@Data
@Table("sys_user_post")
public class SysUserPost
{
    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 用户ID */
    private Long userId;
    
    /** 岗位ID */
    private Long postId;
}
