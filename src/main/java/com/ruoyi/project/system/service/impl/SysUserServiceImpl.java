package com.ruoyi.project.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.DbChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.bean.BeanValidators;
import com.ruoyi.framework.aspectj.lang.annotation.DataScope;
import com.ruoyi.framework.security.service.PasswordEncoder;
import com.ruoyi.project.system.domain.SysPost;
import com.ruoyi.project.system.domain.SysRole;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.domain.SysUserRole;
import com.ruoyi.project.system.mapper.SysPostMapper;
import com.ruoyi.project.system.mapper.SysRoleMapper;
import com.ruoyi.project.system.mapper.SysUserMapper;
import com.ruoyi.project.system.mapper.SysUserRoleMapper;
import com.ruoyi.project.system.service.ISysConfigService;
import com.ruoyi.project.system.service.ISysUserService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.Validator;

/**
 * 用户 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService
{
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysPostMapper postMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    protected Validator validator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 根据条件分页查询用户列表
     * 
     * @param user 用户信息
     * @param page 分页对象
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUserList(SysUser user, Page<SysUser> page)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_user") 
            .where(new QueryColumn("user_name").like(user.getUserName(), StrUtil.isNotEmpty(user.getUserName())))
            .and(new QueryColumn("status").eq(user.getStatus(), StrUtil.isNotEmpty(user.getStatus())))
            .and(new QueryColumn("phonenumber").like(user.getPhonenumber(), StrUtil.isNotEmpty(user.getPhonenumber())))
            .and(new QueryColumn("create_time").between(user.getParams().get("beginTime"), user.getParams().get("endTime"), 
                        user.getParams().get("beginTime") != null && user.getParams().get("endTime") != null))
            .and(new QueryColumn("dept_id").eq(user.getDeptId(), ObjectUtil.isNotNull(user.getDeptId())))
            .orderBy(new QueryColumn("create_time").desc());
        Page<SysUser> result = page(new Page<>(page.getPageNumber(), page.getPageSize()), queryWrapper);
        return result.getRecords();
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     * 
     * @param page 分页对象
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public com.mybatisflex.core.paginate.Page<SysUser> selectAllocatedList(com.mybatisflex.core.paginate.Page<SysUser> page, SysUser user)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("user_name").like(user.getUserName(), StrUtil.isNotEmpty(user.getUserName())))
            .and(new QueryColumn("phonenumber").like(user.getPhonenumber(), StrUtil.isNotEmpty(user.getPhonenumber())))
            .and(new QueryColumn("del_flag").eq("0"))
            .and("user_id IN (SELECT user_id FROM sys_user_role WHERE role_id = " + user.getRoleId() + ")");
        return page(page, queryWrapper);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param page 分页对象
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public com.mybatisflex.core.paginate.Page<SysUser> selectUnallocatedList(com.mybatisflex.core.paginate.Page<SysUser> page, SysUser user)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("user_name").like(user.getUserName(), StrUtil.isNotEmpty(user.getUserName())))
            .and(new QueryColumn("phonenumber").like(user.getPhonenumber(), StrUtil.isNotEmpty(user.getPhonenumber())))
            .and(new QueryColumn("del_flag").eq("0"))
            .and("user_id NOT IN (SELECT user_id FROM sys_user_role WHERE role_id = " + user.getRoleId() + ")");
        return page(page, queryWrapper);
    }

    // 保留原来的非分页方法，用于兼容现有代码
    /**
     * 根据条件查询已分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectAllocatedList(SysUser user)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("user_name").like(user.getUserName(), StrUtil.isNotEmpty(user.getUserName())))
            .and(new QueryColumn("phonenumber").like(user.getPhonenumber(), StrUtil.isNotEmpty(user.getPhonenumber())))
            .and(new QueryColumn("del_flag").eq("0"))
            .and("user_id IN (SELECT user_id FROM sys_user_role WHERE role_id = " + user.getRoleId() + ")");
        return list(queryWrapper);
    }

    /**
     * 根据条件查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUnallocatedList(SysUser user)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("user_name").like(user.getUserName(), StrUtil.isNotEmpty(user.getUserName())))
            .and(new QueryColumn("phonenumber").like(user.getPhonenumber(), StrUtil.isNotEmpty(user.getPhonenumber())))
            .and(new QueryColumn("del_flag").eq("0"))
            .and("user_id NOT IN (SELECT user_id FROM sys_user_role WHERE role_id = " + user.getRoleId() + ")");
        return list(queryWrapper);
    }

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserByUserName(String userName)
    {
        return getOne(QueryWrapper.create().from("sys_user").where(new QueryColumn("user_name").eq(userName)).and(new QueryColumn("del_flag").eq("0")));
    }

    /**
     * 查询用户所属角色组
     * 
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName)
    {
        SysUser user = getOne(QueryWrapper.create().from("sys_user").where(new QueryColumn("user_name").eq(userName)).and(new QueryColumn("del_flag").eq("0")));
        if (ObjectUtil.isNull(user)) {
            return "";
        }
        List<SysRole> list = roleMapper.selectListByQuery(QueryWrapper.create()
            .from("sys_role") 
            .where("role_id IN (SELECT role_id FROM sys_user_role WHERE user_id = " + user.getUserId() + ")"));
        if (CollectionUtils.isEmpty(list))
        {
            return "";
        }
        return list.stream().map(SysRole::getRoleName).collect(Collectors.joining(","));
    }

    /**
     * 查询用户所属岗位组
     * 
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(String userName)
    {
        SysUser user = getOne(QueryWrapper.create().from("sys_user").where(new QueryColumn("user_name").eq(userName)).and(new QueryColumn("del_flag").eq("0")));
        if (ObjectUtil.isNull(user)) {
            return "";
        }
        List<Long> postIds = postMapper.selectListByQuery(QueryWrapper.create()
            .from("sys_post") 
            .where("post_id IN (SELECT post_id FROM sys_user_post WHERE user_id = " + user.getUserId() + ")"))
            .stream()
            .map(SysPost::getPostId)
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(postIds))
        {
            return "";
        }
        List<SysPost> list = postMapper.selectListByQuery(QueryWrapper.create().from("sys_post").where(new QueryColumn("post_id").in(postIds)));
        return list.stream().map(SysPost::getPostName).collect(Collectors.joining(","));
    }

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果 true 表示唯一，false 表示不唯一
     */
    @Override
    public boolean checkUserNameUnique(SysUser user)
    {
        Long userId = ObjectUtil.isNull(user.getUserId()) ? -1L : user.getUserId();
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.from("sys_user").where(new QueryColumn("user_name")
        .eq(user.getUserName()))
        .and(new QueryColumn("del_flag")
        .eq("0"));
        SysUser info = getOne(queryWrapper);
        if (ObjectUtil.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return 结果 true 表示唯一，false 表示不唯一
     */
    @Override
    public boolean checkPhoneUnique(SysUser user)
    {
        Long userId = ObjectUtil.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = getOne(QueryWrapper.create().from("sys_user").where(new QueryColumn("phonenumber").eq(user.getPhonenumber())).and(new QueryColumn("del_flag").eq("0")).limit(1));
        if (ObjectUtil.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return 结果 true 表示唯一，false 表示不唯一
     */
    @Override
    public boolean checkEmailUnique(SysUser user)
    {
        Long userId = ObjectUtil.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = getOne(QueryWrapper.create().from("sys_user").where(new QueryColumn("email").eq(user.getEmail())).and(new QueryColumn("del_flag").eq("0")).limit(1));
        if (ObjectUtil.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 校验用户是否允许操作
     * 
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(SysUser user)
    {
        if (ObjectUtil.isNotNull(user.getUserId()) && user.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     * 
     * @param userId 用户id
     */
    @Override
    public void checkUserDataScope(Long userId)
    {
        if (!SysUser.isAdmin(SecurityUtils.getUserId()))
        {
             List<SysUser> users = list(QueryWrapper.create().from("sys_user").where(new QueryColumn("user_id").eq(userId)));
            if (CollUtil.isEmpty(users))
            {
                throw new ServiceException("没有权限访问用户数据！");
            }
        }
    }

    /**
     * 注册用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUser user)
    {
        return save(user);
    }

    /**
     * 用户授权角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional
    public void insertUserAuth(Long userId, Long[] roleIds)
    {
        userRoleMapper.deleteByQuery(QueryWrapper.create().from("sys_user_role").where(new QueryColumn("user_id").eq(userId)));
        insertUserRole(userId, roleIds);
    }

    /**
     * 修改用户状态
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean updateUserStatus(SysUser user)
    {
        return DbChain.table("sys_user")
            .where("user_id = ?", user.getUserId())
            .set("status", user.getStatus())
            .update();
    }

    /**
     * 修改用户基本信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean updateUserProfile(SysUser user)
    {
        return DbChain.table("sys_user")
            .where("user_id = ?", user.getUserId())
            .set("nick_name", user.getNickName())
            .set("email", user.getEmail())
            .set("phonenumber", user.getPhonenumber())
            .set("sex", user.getSex())
            .update();
    }

    /**
     * 修改用户头像
     * 
     * @param userName 用户名
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(String userName, String avatar)
    {
        return DbChain.table("sys_user")
            .where("user_name = ?", userName)
            .set("avatar", avatar)
            .update();
    }

    /**
     * 重置用户密码
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean resetPwd(SysUser user)
    {
        return DbChain.table("sys_user")
            .where("user_id = ?", user.getUserId())
            .set("password", user.getPassword())
            .update();
    }

    /**
     * 重置用户密码
     * 
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public boolean resetUserPwd(String userName, String password)
    {
        return DbChain.table("sys_user")
            .where("user_name = ?", userName)
            .set("password", password)
            .update();
    }

    /**
     * 新增用户角色信息
     * 
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(Long userId, Long[] roleIds)
    {
        if (ObjectUtil.isNotNull(roleIds))
        {
            // 新增用户与角色管理
            List<SysUserRole> list = new ArrayList<SysUserRole>();
            for (Long roleId : roleIds)
            {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            if (list.size() > 0)
            {
                 for (SysUserRole ur : list) {
                    userRoleMapper.insert(ur); 
                }
            }
        }
    }

    /**
     * 导入用户数据
     * 
     * @param userList 用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName)
    {
        if (ObjectUtil.isNull(userList) || userList.size() == 0)
        {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        String password = configService.selectConfigByKey("sys.user.initPassword");
        for (SysUser user : userList)
        {
            try
            {
                // 验证是否存在这个用户
                SysUser u = getOne(QueryWrapper.create().from("sys_user").where(new QueryColumn("user_name").eq(user.getUserName())).and(new QueryColumn("del_flag").eq("0")));
                if (ObjectUtil.isNull(u))
                {
                    BeanValidators.validateWithException(validator, user);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setCreateBy(operName);
                    save(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    BeanValidators.validateWithException(validator, user);
                    checkUserAllowed(user);
                    checkUserDataScope(user.getUserId()); 
                    user.setUpdateBy(operName);
                    updateById(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
