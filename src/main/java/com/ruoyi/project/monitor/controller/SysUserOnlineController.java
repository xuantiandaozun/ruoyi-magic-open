package com.ruoyi.project.monitor.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSON;
import com.mybatisflex.core.paginate.Page;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.monitor.domain.SysUserOnline;
import com.ruoyi.project.system.service.ISysUserOnlineService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 在线用户监控
 * 
 * @author ruoyi
 */
@Tag(name = "在线用户监控")
@RestController
@RequestMapping("/monitor/online")
public class SysUserOnlineController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(SysUserOnlineController.class);

    @Autowired
    private ISysUserOnlineService userOnlineService;

    @SaCheckPermission("monitor:online:list")
    @GetMapping("/list")
    public TableDataInfo list(String ipaddr, String userName)
    {
        log.info("开始获取在线用户列表, ipaddr={}, userName={}", ipaddr, userName);
        
        // 获取所有在线会话
        List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);
        log.info("获取到的sessionId列表: {}", sessionIds);
        
        List<SysUserOnline> userOnlineList = new ArrayList<SysUserOnline>();
        
        for (String sessionId : sessionIds)
        {
            log.info("处理sessionId: {}", sessionId);
            // 获取此 sessionId 对应的在线用户
            SaSession session = StpUtil.getSessionBySessionId(sessionId);
            if (session == null) {
                log.warn("sessionId {} 对应的session为空", sessionId);
                continue;
            }
            
            LoginUser loginUser = (LoginUser) JSON.to(LoginUser.class, session.get(Constants.LOGIN_USER_KEY));
            if (loginUser == null) {
                log.warn("sessionId {} 对应的LoginUser为空", sessionId);
                continue;
            }
            
            log.info("获取到登录用户信息: userId={}, userName={}, ipaddr={}", 
                loginUser.getUserId(), 
                loginUser.getUser() != null ? loginUser.getUser().getUserName() : "null",
                loginUser.getIpaddr());
            
            if (StrUtil.isNotEmpty(ipaddr) && StrUtil.isNotEmpty(userName))
            {
                if (StrUtil.equals(ipaddr, loginUser.getIpaddr()) && 
                    loginUser.getUser() != null && 
                    StrUtil.equals(userName, loginUser.getUser().getUserName()))
                {
                    userOnlineList.add(userOnlineService.loginUserToUserOnline(loginUser));
                    log.info("根据IP和用户名匹配到用户");
                }
            }
            else if (StrUtil.isNotEmpty(ipaddr))
            {
                if (StrUtil.equals(ipaddr, loginUser.getIpaddr()))
                {
                    userOnlineList.add(userOnlineService.loginUserToUserOnline(loginUser));
                    log.info("根据IP匹配到用户");
                }
            }
            else if (StrUtil.isNotEmpty(userName))
            {
                if (loginUser.getUser() != null && 
                    StrUtil.equals(userName, loginUser.getUser().getUserName()))
                {
                    userOnlineList.add(userOnlineService.loginUserToUserOnline(loginUser));
                    log.info("根据用户名匹配到用户");
                }
            }
            else
            {
                userOnlineList.add(userOnlineService.loginUserToUserOnline(loginUser));
                log.info("添加在线用户到列表");
            }
        }
        
        Collections.reverse(userOnlineList);
        userOnlineList.removeAll(Collections.singleton(null));
        log.info("最终获取到的在线用户数量: {}", userOnlineList.size());
        
        // 获取分页参数
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 对列表进行手动分页
        int total = userOnlineList.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        // 对索引进行有效性检查
        if (fromIndex >= total) {
            fromIndex = 0;
            toIndex = Math.min(pageSize, total);
        }
        
        // 获取当前页数据
        List<SysUserOnline> pageList = userOnlineList;
        if (fromIndex < toIndex) {
            pageList = userOnlineList.subList(fromIndex, toIndex);
        } else {
            pageList = new ArrayList<>();
        }
        
        // 手动构造Page对象
        Page<SysUserOnline> page = new Page<>(pageNum, pageSize);
        page.setRecords(pageList);
        page.setTotalRow(total);
        
        return getDataTable(page);
    }

    /**
     * 强退用户
     */
    @SaCheckPermission("monitor:online:forceLogout")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenValue}")
    public AjaxResult forceLogout(@PathVariable String tokenValue)
    {
        log.info("强制退出用户, tokenValue={}", tokenValue);
        try {
            // 获取 token 对应的登录 id
            Object loginId = StpUtil.getLoginIdByToken(tokenValue);
            if (loginId != null) {
                log.info("获取到登录ID: {}", loginId);
                // 删除用户的 session
                String sessionId = String.format("Authorization:login:session:%s", loginId);
                StpUtil.getSessionBySessionId(sessionId).delete(Constants.LOGIN_USER_KEY);
                log.info("删除用户session: {}", sessionId);
                
                // 踢下线
                StpUtil.kickoutByTokenValue(tokenValue);
                log.info("用户已被踢下线");
                
                // 删除token session
                StpUtil.getTokenSessionByToken(tokenValue).delete(Constants.LOGIN_USER_KEY);
                log.info("删除token session完成");
            } else {
                log.warn("未找到token对应的登录ID: {}", tokenValue);
            }
            return success();
        } catch (Exception e) {
            log.error("强制退出用户时发生错误", e);
            return error("强制退出用户失败");
        }
    }
}
