package com.ruoyi.project.gen.tools.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.system.domain.SysMenu;
import com.ruoyi.project.system.service.ISysMenuService;

import cn.hutool.core.util.StrUtil;

/**
 * 菜单管理工具
 * 提供菜单相关的管理操作
 */
@Service
public class MenuTool {
    private static final Logger logger = LoggerFactory.getLogger(MenuTool.class);

    @Autowired
    private ISysMenuService sysMenuService;

    /**
     * 查询主数据源的菜单列表
     */
    @Tool(name = "getMenuList", description = "查询主数据源的菜单列表")
    public Map<String, Object> getMenuList(Long userId, String menuName, String visible, Integer pageNum, Integer pageSize) {
        try {
            logger.info("getMenuList查询菜单列表, userId: {}", userId);
            
            // 限制每页最大500条记录
            if (pageSize == null || pageSize > 500) {
                pageSize = 500;
            }
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            
            // 构建查询条件
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .select()
                    .from("sys_menu");
            
            // 添加菜单名称条件
            if (StrUtil.isNotBlank(menuName)) {
                queryWrapper.and(new QueryColumn("menu_name").like(menuName));
            }
            
            // 添加可见性条件
            if (StrUtil.isNotBlank(visible)) {
                queryWrapper.and(new QueryColumn("visible").eq(visible));
            }
            
            // 如果指定了用户ID，可以添加相关的权限过滤（这里简化处理）
            // 实际业务中可能需要根据用户角色权限进行过滤
            
            // 添加排序
            queryWrapper.orderBy(new QueryColumn("order_num").asc(), new QueryColumn("menu_id").asc());

            // 创建分页对象
            Page<SysMenu> pageObj = Page.of(pageNum, pageSize);
            
            // 执行分页查询
            Page<SysMenu> page = sysMenuService.page(pageObj, queryWrapper);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalCount", page != null ? page.getTotalRow() : 0);
            result.put("totalPage", page != null ? page.getTotalPage() : 0);
            
            if (page == null || page.getRecords().isEmpty()) {
                result.put("message", "没有找到匹配的菜单");
                result.put("menuList", new ArrayList<>());
                return result;
            }
            
            List<SysMenu> menuList = new ArrayList<>(page.getRecords());
            result.put("menuList", menuList);
            result.put("message", "查询菜单列表成功");
            
            return result;
        } catch (Exception e) {
            logger.error("查询菜单列表失败", e);
            throw new ServiceException("查询菜单列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询菜单树
     */
    @Tool(name = "getMenuTreeByUserId", description = "根据用户ID查询菜单树")
    public Map<String, Object> getMenuTreeByUserId(Long userId) {
        try {
            logger.info("getMenuTreeByUserId根据用户ID查询菜单树: {}", userId);
            List<SysMenu> menus = sysMenuService.selectMenuTreeByUserId(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("totalCount", menus != null ? menus.size() : 0);
            
            if (menus == null || menus.isEmpty()) {
                result.put("message", "用户没有菜单权限");
                result.put("menuTree", new ArrayList<>());
                return result;
            }
            
            List<SysMenu> menuTree = new ArrayList<>(menus);
            result.put("menuTree", menuTree);
            result.put("message", "查询菜单树成功");
            
            return result;
        } catch (Exception e) {
            logger.error("根据用户ID查询菜单树失败", e);
            throw new ServiceException("根据用户ID查询菜单树失败：" + e.getMessage());
        }
    }

    /**
     * 根据菜单ID查询菜单信息
     */
    @Tool(name = "getMenuById", description = "根据菜单ID查询菜单信息")
    public Map<String, Object> getMenuById(Long menuId) {
        try {
            logger.info("getMenuById根据菜单ID查询菜单信息: {}", menuId);
            SysMenu menu = sysMenuService.getById(menuId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("menuId", menuId);
            result.put("menu", menu);
            
            if (menu == null) {
                result.put("message", "菜单不存在");
            } else {
                result.put("message", "查询菜单信息成功");
            }
            
            return result;
        } catch (Exception e) {
            logger.error("根据菜单ID查询菜单信息失败", e);
            throw new ServiceException("根据菜单ID查询菜单信息失败：" + e.getMessage());
        }
    }
}