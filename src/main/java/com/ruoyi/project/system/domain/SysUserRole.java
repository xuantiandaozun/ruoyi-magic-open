package com.ruoyi.project.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import lombok.Data;

/**
 * 用户和角色关联 sys_user_role
 * 
 * @author ruoyi
 */
@Table("sys_user_role")
@Data
public class SysUserRole
{
    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 用户ID */
    private Long userId;
    
    /** 角色ID */
    private Long roleId;



    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("roleId", getRoleId())
            .toString();
    }
}
