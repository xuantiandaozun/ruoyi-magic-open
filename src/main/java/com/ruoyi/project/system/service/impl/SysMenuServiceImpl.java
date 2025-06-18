package com.ruoyi.project.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.domain.TreeSelect;
import com.ruoyi.project.system.domain.SysMenu;
import com.ruoyi.project.system.domain.SysRole;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.domain.vo.MetaVo;
import com.ruoyi.project.system.domain.vo.RouterVo;
import com.ruoyi.project.system.mapper.SysMenuMapper;
import com.ruoyi.project.system.mapper.SysRoleMapper;
import com.ruoyi.project.system.mapper.SysRoleMenuMapper;
import com.ruoyi.project.system.service.ISysMenuService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 菜单 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService
{
    public static final String PREMISSION_STRING = "perms[\"{0}\"]";

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    /**
     * 根据用户查询系统菜单列表
     * 
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(Long userId)
    {
        return selectMenuList(new SysMenu(), userId);
    }

    /**
     * 查询系统菜单列表
     * 
     * @param menu 菜单信息
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(SysMenu menu, Long userId)
    {
        List<SysMenu> menuList;
        // 管理员显示所有菜单信息
        if (SysUser.isAdmin(userId))
        {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .from("sys_menu")
                .where(new QueryColumn("menu_name").like(menu.getMenuName(), ObjectUtil.isNotEmpty(menu.getMenuName())))
                .and(new QueryColumn("visible").eq(menu.getVisible(), ObjectUtil.isNotNull(menu.getVisible())))
                .and(new QueryColumn("status").eq(menu.getStatus(), ObjectUtil.isNotNull(menu.getStatus())))
                .orderBy(new QueryColumn("parent_id").asc())
                .orderBy(new QueryColumn("order_num").asc());
            menuList = list(queryWrapper);
        }        else
        {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .select("DISTINCT m.menu_id, m.parent_id, m.menu_name, m.path, m.component, m.query, m.route_name, m.visible, m.status, IFNULL(m.perms,'') as perms, m.is_frame, m.is_cache, m.menu_type, m.icon, m.order_num, m.create_time")
                .from("sys_menu m")
                .leftJoin("sys_role_menu rm").on("m.menu_id = rm.menu_id")
                .leftJoin("sys_user_role ur").on("rm.role_id = ur.role_id")
                .leftJoin("sys_role ro").on("ur.role_id = ro.role_id")
                .where("ur.user_id = " + userId);
                
            // 附加条件
            if (ObjectUtil.isNotEmpty(menu.getMenuName())) {
                queryWrapper.and("m.menu_name like '%" + menu.getMenuName() + "%'");
            }
            if (ObjectUtil.isNotNull(menu.getVisible())) {
                queryWrapper.and("m.visible = '" + menu.getVisible() + "'");
            }
            if (ObjectUtil.isNotNull(menu.getStatus())) {
                queryWrapper.and("m.status = '" + menu.getStatus() + "'");
            }
            
            // 排序
            queryWrapper.orderBy("m.parent_id asc, m.order_num asc");
            
            menuList = list(queryWrapper);
        }
        return menuList;
    }

    /**
     * 根据用户ID查询权限
     * 
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId)
    {
        List<String> perms;
        // 判断是否是超级管理员
        if (userId != null && userId == 1L)
        {
            // 超级管理员返回所有权限
            QueryWrapper queryWrapper = QueryWrapper.create()
                .select("perms")
                .from("sys_menu")
                .where(new QueryColumn("perms").isNotNull())
                .and(new QueryColumn("perms").ne(""));
            perms = list(queryWrapper).stream()
                    .map(SysMenu::getPerms)
                    .collect(Collectors.toList());
        }        else
        {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .select("DISTINCT m.perms")
                .from("sys_menu m")
                .leftJoin("sys_role_menu rm").on("m.menu_id = rm.menu_id")
                .leftJoin("sys_user_role ur").on("rm.role_id = ur.role_id")
                .leftJoin("sys_role r").on("r.role_id = ur.role_id")
                .where("m.status = '0' and r.status = '0' and ur.user_id = #{userId}".replace("#{userId}", userId.toString()));
            
            perms = list(queryWrapper).stream()
                    .map(SysMenu::getPerms)
                    .collect(Collectors.toList());
        }
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms)
        {
            if (StrUtil.isNotEmpty(perm))
            {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }    /**
     * 根据角色ID查询权限
     * 
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByRoleId(Long roleId)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("DISTINCT m.perms")
            .from("sys_menu m")
            .leftJoin("sys_role_menu rm").on("m.menu_id = rm.menu_id")
            .where("m.status = '0'")
            .and("rm.role_id = " + roleId);
        
        List<SysMenu> menuList = list(queryWrapper);
        List<String> perms = menuList.stream()
                .map(SysMenu::getPerms)
                .collect(Collectors.toList());
        
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms)
        {
            if (StrUtil.isNotEmpty(perm))
            {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根据用户ID查询菜单
     * 
     * @param userId 用户名称
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId)
    {
        List<SysMenu> menus;
        if (SecurityUtils.isAdmin(userId))
        {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .from("sys_menu")
                .where(new QueryColumn("status").eq("0"))
                .orderBy(new QueryColumn("parent_id").asc())
                .orderBy(new QueryColumn("order_num").asc());
            menus = list(queryWrapper);
        }
        else
        {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .select("DISTINCT m.*")
                .from("sys_menu m")
                .leftJoin("sys_role_menu rm").on("m.menu_id = rm.menu_id")
                .leftJoin("sys_user_role ur").on("rm.role_id = ur.role_id")
                .where("m.status = '0'")
                .and("ur.user_id = " + userId)
                .orderBy("m.parent_id asc, m.order_num asc");
            menus = list(queryWrapper);
        }
        return getChildPerms(menus, 0);
    }

    /**
     * 根据角色ID查询菜单树信息
     * 
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> selectMenuListByRoleId(Long roleId)
    {
        SysRole role = roleMapper.selectOneById(roleId);
        if (role.getMenuCheckStrictly())
        {
            return list(QueryWrapper.create()
                .from("sys_menu")
                .where("menu_id IN (SELECT menu_id FROM sys_role_menu WHERE role_id = " + roleId + ")"))
                .stream()
                .map(SysMenu::getMenuId)
                .collect(Collectors.toList());
        }
        return list().stream()
               .map(SysMenu::getMenuId)
               .collect(Collectors.toList());
    }

    /**
     * 构建前端路由所需要的菜单
     * 
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus)
    {
        List<RouterVo> routers = new LinkedList<RouterVo>();
        for (SysMenu menu : menus)
        {
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setQuery(menu.getQuery());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StrUtil.equals("1", menu.getIsCache()), menu.getPath()));
            List<SysMenu> cMenus = menu.getChildren();
            if (CollUtil.isNotEmpty(cMenus) && UserConstants.TYPE_DIR.equals(menu.getMenuType()))
            {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            }
            else if (isMenuFrame(menu))
            {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(StrUtil.upperFirst(menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StrUtil.equals("1", menu.getIsCache()), menu.getPath()));
                children.setQuery(menu.getQuery());
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            else if (menu.getParentId().intValue() == 0 && isInnerLink(menu))
            {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                String routerPath = innerLinkReplaceEach(menu.getPath());
                children.setPath(routerPath);
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(StrUtil.upperFirst(routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 构建前端所需要树结构
     * 
     * @param menus 菜单列表
     * @return 树结构列表
     */
    @Override
    public List<SysMenu> buildMenuTree(List<SysMenu> menus)
    {
        List<SysMenu> returnList = new ArrayList<SysMenu>();
        List<Long> tempList = menus.stream().map(SysMenu::getMenuId).collect(Collectors.toList());
        for (SysMenu menu : menus)
        {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(menu.getParentId()))
            {
                recursionFn(menus, menu);
                returnList.add(menu);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = menus;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     * 
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildMenuTreeSelect(List<SysMenu> menus)
    {
        List<SysMenu> menuTrees = buildMenuTree(menus);
        return menuTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 是否存在菜单子节点
     * 
     * @param menuId 菜单ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean hasChildByMenuId(Long menuId)
    {
        return count(QueryWrapper.create()
            .from("sys_menu")
            .where(new QueryColumn("parent_id").eq(menuId))) > 0;
    }

    /**
     * 查询菜单是否存在角色
     * 
     * @param menuId 菜单ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkMenuExistRole(Long menuId)
    {
        return roleMenuMapper.selectCountByQuery(QueryWrapper.create()
            .from("sys_role_menu")
            .where(new QueryColumn("menu_id").eq(menuId))) > 0;
    }

    /**
     * 校验菜单名称是否唯一
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkMenuNameUnique(SysMenu menu)
    {
        Long menuId = ObjectUtil.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        
        SysMenu info = getOne(QueryWrapper.create()
            .from("sys_menu")
            .where(new QueryColumn("menu_name").eq(menu.getMenuName()))
            .and(new QueryColumn("parent_id").eq(menu.getParentId()))
            .limit(1));
            
        if (ObjectUtil.isNotNull(info) && info.getMenuId().longValue() != menuId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 获取路由名称
     * 
     * @param menu 菜单信息
     * @return 路由名称
     */
    public String getRouteName(SysMenu menu)
    {
        String routerName = StrUtil.upperFirst(menu.getPath());
        // 非外链并且是一级目录（类型为目录）
        if (isMenuFrame(menu))
        {
            routerName = StrUtil.EMPTY;
        }
        return routerName;
    }

    /**
     * 获取路由地址
     * 
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu)
    {
        String routerPath = menu.getPath();
        // 内链打开外网方式
        if (menu.getParentId().intValue() != 0 && isInnerLink(menu))
        {
            routerPath = innerLinkReplaceEach(routerPath);
        }
        // 非外链并且是一级目录（类型为目录）
        if (0 == menu.getParentId().intValue() && UserConstants.TYPE_DIR.equals(menu.getMenuType())
                && UserConstants.NO_FRAME.equals(menu.getIsFrame()))
        {
            routerPath = "/" + menu.getPath();
        }
        // 非外链并且是一级目录（类型为菜单）
        else if (isMenuFrame(menu))
        {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * 获取组件信息
     * 
     * @param menu 菜单信息
     * @return 组件信息
     */
    public String getComponent(SysMenu menu)
    {
        String component = UserConstants.LAYOUT;
        if (StrUtil.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu))
        {
            component = menu.getComponent();
        }
        else if (StrUtil.isEmpty(menu.getComponent()) && menu.getParentId().intValue() != 0 && isInnerLink(menu))
        {
            component = UserConstants.INNER_LINK;
        }
        else if (StrUtil.isEmpty(menu.getComponent()) && isParentView(menu))
        {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    /**
     * 是否为菜单内部跳转
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isMenuFrame(SysMenu menu)
    {
        return menu.getParentId().intValue() == 0 && UserConstants.TYPE_MENU.equals(menu.getMenuType())
                && menu.getIsFrame().equals(UserConstants.NO_FRAME);
    }

    /**
     * 是否为内链组件
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isInnerLink(SysMenu menu)
    {
        return menu.getIsFrame().equals(UserConstants.NO_FRAME) && StrUtil.startWith(menu.getPath(), "http");
    }

    /**
     * 是否为parent_view组件
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isParentView(SysMenu menu)
    {
        return menu.getParentId().intValue() != 0 && UserConstants.TYPE_DIR.equals(menu.getMenuType());
    }

    /**
     * 根据父节点的ID获取所有子节点
     * 
     * @param list 分类表
     * @param parentId 传入的父节点ID
     * @return String
     */
    public List<SysMenu> getChildPerms(List<SysMenu> list, int parentId)
    {
        List<SysMenu> returnList = new ArrayList<SysMenu>();
        for (SysMenu t : list)
        {
            // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
            if (t.getParentId() == parentId)
            {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    /**
     * 递归列表
     * 
     * @param list 分类表
     * @param t 子节点
     */
    private void recursionFn(List<SysMenu> list, SysMenu t)
    {
        // 得到子节点列表
        List<SysMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysMenu tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t)
    {
        List<SysMenu> tlist = new ArrayList<SysMenu>();
        for (SysMenu n : list)
        {
            if (n.getParentId().longValue() == t.getMenuId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysMenu> list, SysMenu t)
    {
        return getChildList(list, t).size() > 0;
    }

    /**
     * 内链域名特殊字符替换
     * 
     * @return 替换后的内链域名
     */
    public String innerLinkReplaceEach(String path)
    {
        return StrUtil.replace(path, "http://", "").replace("https://", "");
    }
}
