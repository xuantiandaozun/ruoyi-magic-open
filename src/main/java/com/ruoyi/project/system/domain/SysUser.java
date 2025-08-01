package com.ruoyi.project.system.domain;

import java.util.Date;
import java.util.List;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.Table;
import com.ruoyi.common.xss.Xss;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.aspectj.lang.annotation.Excel.ColumnType;
import com.ruoyi.framework.aspectj.lang.annotation.Excel.Type;
import com.ruoyi.framework.aspectj.lang.annotation.Excels;
import com.ruoyi.framework.web.domain.BaseEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户对象 sys_user
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
@AllArgsConstructor
@NoArgsConstructor
public class SysUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Id(keyType = KeyType.Auto)
    @Excel(name = "用户序号", type = Type.EXPORT, cellType = ColumnType.NUMERIC)
    private Long userId;

    /** 部门ID */
    @Excel(name = "部门编号", type = Type.IMPORT)
    private Long deptId;

    /** 用户账号 */
    @Excel(name = "登录名称")
    @Xss(message = "用户账号不能包含脚本字符")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过30个字符")
    private String userName;

    /** 用户昵称 */
    @Excel(name = "用户名称")
    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过30个字符")
    private String nickName;

    /** 用户邮箱 */
    @Excel(name = "用户邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    /** 手机号码 */
    @Excel(name = "手机号码", cellType = ColumnType.TEXT)
    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    private String phonenumber;

    /** 用户性别 */
    @Excel(name = "用户性别", readConverterExp = "0=男,1=女,2=未知")
    private String sex;

    /** 用户头像 */
    private String avatar;

    /** 密码 */
    private String password;

    /** 帐号状态（0正常 1停用） */
    @Excel(name = "帐号状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    /** 最后登录IP */
    @Excel(name = "最后登录IP", type = Type.EXPORT)
    private String loginIp;

    /** 最后登录时间 */
    @Excel(name = "最后登录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Type.EXPORT)
    private Date loginDate;

    /** 飞书用户访问令牌 */
    private String feishuAccessToken;

    /** 飞书令牌过期时间 */
    private Date feishuTokenExpireTime;

    /** 部门对象 */
    @Excels({
        @Excel(name = "部门名称", targetAttr = "deptName", type = Type.EXPORT),
        @Excel(name = "部门负责人", targetAttr = "leader", type = Type.EXPORT)
    })
    @RelationManyToOne(selfField = "deptId", targetField = "deptId")
    private SysDept dept;

    /** 角色对象 */
    @RelationOneToMany(joinTable = "sys_user_role", joinSelfColumn = "user_id", joinTargetColumn = "role_id", selfField = "userId", targetField = "roleId")
    private List<SysRole> roles;

    /** 角色组 */
    @Column(ignore = true)
    private Long[] roleIds;

    /** 岗位组 */
    @Column(ignore = true)
    private Long[] postIds;

    /** 角色ID */
    @Column(ignore = true)
    private Long roleId;
    
    public boolean isAdmin()
    {
        return isAdmin(this.userId);
    }

    public static boolean isAdmin(Long userId)
    {
        return userId != null && 1L == userId;
    }
}
