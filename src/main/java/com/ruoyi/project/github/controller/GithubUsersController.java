package com.ruoyi.project.github.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.github.domain.GithubUsers;
import com.ruoyi.project.github.service.IGithubUsersService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * GitHub用户/组织监控Controller
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Tag(name = "GitHub用户监控")
@RestController
@RequestMapping("/github/users")
public class GithubUsersController extends BaseController
{
    @Autowired
    private IGithubUsersService githubUsersService;

    /**
     * 查询GitHub用户监控列表
     */
    @Operation(summary = "查询GitHub用户监控列表")
    @SaCheckPermission("github:users:list")
    @GetMapping("/list")
    public TableDataInfo list(GithubUsers githubUsers)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = QueryWrapper.create();
        
        // 添加查询条件
        if (githubUsers.getGithubId() != null) {
            queryWrapper.eq("github_id", githubUsers.getGithubId());
        }
        if (StrUtil.isNotBlank(githubUsers.getLogin())) {
            queryWrapper.like("login", "%" + githubUsers.getLogin() + "%");
        }
        if (StrUtil.isNotBlank(githubUsers.getName())) {
            queryWrapper.like("name", "%" + githubUsers.getName() + "%");
        }
        if (StrUtil.isNotBlank(githubUsers.getType())) {
            queryWrapper.eq("type", githubUsers.getType());
        }
        if (githubUsers.getIsTop100() != null) {
            queryWrapper.eq("is_top_100", githubUsers.getIsTop100());
        }
        if (githubUsers.getIsWatched() != null) {
            queryWrapper.eq("is_watched", githubUsers.getIsWatched());
        }
        
        queryWrapper.eq("del_flag", "0");
        queryWrapper.orderBy("followers", false);
        
        // 使用 MyBatisFlex 的分页方法
        Page<GithubUsers> page = githubUsersService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出GitHub用户监控列表
     */
    @Operation(summary = "导出GitHub用户监控列表")
    @SaCheckPermission("github:users:export")
    @Log(title = "GitHub用户监控", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GithubUsers githubUsers)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("del_flag", "0")
            .orderBy("followers", false);
        
        List<GithubUsers> list = githubUsersService.list(queryWrapper);
        MagicExcelUtil<GithubUsers> util = new MagicExcelUtil<>(GithubUsers.class);
        util.exportExcel(response, list, "GitHub用户监控数据");
    }

    /**
     * 获取GitHub用户监控详细信息
     */
    @Operation(summary = "获取GitHub用户监控详细信息")
    @SaCheckPermission("github:users:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(githubUsersService.getById(id));
    }

    /**
     * 新增GitHub用户监控
     */
    @Operation(summary = "新增GitHub用户监控")
    @SaCheckPermission("github:users:add")
    @Log(title = "GitHub用户监控", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GithubUsers githubUsers)
    {
        return toAjax(githubUsersService.save(githubUsers) ? 1 : 0);
    }

    /**
     * 修改GitHub用户监控
     */
    @Operation(summary = "修改GitHub用户监控")
    @SaCheckPermission("github:users:edit")
    @Log(title = "GitHub用户监控", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GithubUsers githubUsers)
    {
        return toAjax(githubUsersService.updateById(githubUsers) ? 1 : 0);
    }

    /**
     * 删除GitHub用户监控
     */
    @Operation(summary = "删除GitHub用户监控")
    @SaCheckPermission("github:users:remove")
    @Log(title = "GitHub用户监控", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(githubUsersService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
    
    /**
     * 标记为关注用户
     */
    @Operation(summary = "标记为关注用户")
    @SaCheckPermission("github:users:edit")
    @Log(title = "标记关注用户", businessType = BusinessType.UPDATE)
    @PutMapping("/watch/{ids}")
    public AjaxResult watchUsers(@PathVariable Long[] ids)
    {
        for (Long id : ids) {
            GithubUsers user = githubUsersService.getById(id);
            if (user != null) {
                user.setIsWatched(1);
                githubUsersService.updateById(user);
            }
        }
        return success("标记成功");
    }
    
    /**
     * 取消关注用户
     */
    @Operation(summary = "取消关注用户")
    @SaCheckPermission("github:users:edit")
    @Log(title = "取消关注用户", businessType = BusinessType.UPDATE)
    @PutMapping("/unwatch/{ids}")
    public AjaxResult unwatchUsers(@PathVariable Long[] ids)
    {
        for (Long id : ids) {
            GithubUsers user = githubUsersService.getById(id);
            if (user != null) {
                user.setIsWatched(0);
                githubUsersService.updateById(user);
            }
        }
        return success("取消关注成功");
    }
    
    /**
     * 手动同步用户信息
     */
    @Operation(summary = "手动同步用户信息")
    @SaCheckPermission("github:users:edit")
    @Log(title = "同步用户信息", businessType = BusinessType.UPDATE)
    @PostMapping("/sync/{ids}")
    public AjaxResult syncUsers(@PathVariable Long[] ids)
    {
        // TODO: 实现同步逻辑
        return success("同步任务已提交");
    }
}
