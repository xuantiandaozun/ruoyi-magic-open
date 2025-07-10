package com.ruoyi.project.system.service;

import java.util.List;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.ruoyi.project.system.domain.SysUser;

/**
 * 用户 业务层
 * 
 * @author ruoyi
 */
public interface ISysUserService extends IService<SysUser>
{
    /**
     * 根据条件分页查询用户列表
     * 
     * @param user 用户信息
     * @param page 分页对象
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser user, Page<SysUser> page);

    /**
     * 根据条件分页查询已分配用户角色列表
     * 
     * @param page 分页信息
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public com.mybatisflex.core.paginate.Page<SysUser> selectAllocatedList(com.mybatisflex.core.paginate.Page<SysUser> page, SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param page 分页信息
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public com.mybatisflex.core.paginate.Page<SysUser> selectUnallocatedList(com.mybatisflex.core.paginate.Page<SysUser> page, SysUser user);

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    SysUser selectUserByUserName(String userName);

    /**
     * 根据用户ID查询用户所属角色组
     * 
     * @param userName 用户名
     * @return 结果
     */
    String selectUserRoleGroup(String userName);

    /**
     * 根据用户ID查询用户所属岗位组
     * 
     * @param userName 用户名
     * @return 结果
     */
    String selectUserPostGroup(String userName);

    /**
     * 校验用户名称是否唯一
     * 
     * @param user 用户信息
     * @return 结果
     */
    boolean checkUserNameUnique(SysUser user);

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    boolean checkPhoneUnique(SysUser user);

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    boolean checkEmailUnique(SysUser user);

    /**
     * 校验用户是否允许操作
     * 
     * @param user 用户信息
     */
    void checkUserAllowed(SysUser user);

    /**
     * 校验用户是否有数据权限
     * 
     * @param userId 用户id
     */
    void checkUserDataScope(Long userId);

    /**
     * 注册用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    boolean registerUser(SysUser user);
    
    /**
     * 用户授权角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    void insertUserAuth(Long userId, Long[] roleIds);

    /**
     * 修改用户状态
     * 
     * @param user 用户信息
     * @return 结果
     */
    boolean updateUserStatus(SysUser user);

    /**
     * 修改用户基本信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    boolean updateUserProfile(SysUser user);

    /**
     * 修改用户头像
     * 
     * @param userName 用户id
     * @param avatar 头像地址
     * @return 结果
     */
    boolean updateUserAvatar(String userId, String avatar);

    /**
     * 重置用户密码
     * 
     * @param user 用户信息
     * @return 结果
     */
    boolean resetPwd(SysUser user);

    /**
     * 重置用户密码
     * 
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    boolean resetUserPwd(String userName, String password);

    /**
     * 导入用户数据
     * 
     * @param userList 用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName);

    /**
     * 根据条件分页查询已分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectAllocatedList(SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUnallocatedList(SysUser user);
}
