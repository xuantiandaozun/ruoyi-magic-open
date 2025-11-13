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
import com.ruoyi.project.github.domain.GithubRepositories;
import com.ruoyi.project.github.service.IGithubRepositoriesService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * GitHub仓库监控Controller
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Tag(name = "GitHub仓库监控")
@RestController
@RequestMapping("/github/repositories")
public class GithubRepositoriesController extends BaseController
{
    @Autowired
    private IGithubRepositoriesService githubRepositoriesService;

    /**
     * 查询GitHub仓库监控列表
     */
    @Operation(summary = "查询GitHub仓库监控列表")
    @SaCheckPermission("github:repositories:list")
    @GetMapping("/list")
    public TableDataInfo list(GithubRepositories githubRepositories)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = QueryWrapper.create();
        
        // 添加查询条件
        if (githubRepositories.getGithubId() != null) {
            queryWrapper.eq("github_id", githubRepositories.getGithubId());
        }
        if (StrUtil.isNotBlank(githubRepositories.getName())) {
            queryWrapper.like("name", "%" + githubRepositories.getName() + "%");
        }
        if (StrUtil.isNotBlank(githubRepositories.getFullName())) {
            queryWrapper.like("full_name", "%" + githubRepositories.getFullName() + "%");
        }
        if (StrUtil.isNotBlank(githubRepositories.getOwnerLogin())) {
            queryWrapper.like("owner_login", "%" + githubRepositories.getOwnerLogin() + "%");
        }
        if (StrUtil.isNotBlank(githubRepositories.getOwnerType())) {
            queryWrapper.eq("owner_type", githubRepositories.getOwnerType());
        }
        if (StrUtil.isNotBlank(githubRepositories.getLanguage())) {
            queryWrapper.eq("language", githubRepositories.getLanguage());
        }
        if (StrUtil.isNotBlank(githubRepositories.getSyncStatus())) {
            queryWrapper.eq("sync_status", githubRepositories.getSyncStatus());
        }
        
        queryWrapper.eq("del_flag", "0");
        queryWrapper.orderBy("stargazers_count", false);
        
        // 使用 MyBatisFlex 的分页方法
        Page<GithubRepositories> page = githubRepositoriesService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出GitHub仓库监控列表
     */
    @Operation(summary = "导出GitHub仓库监控列表")
    @SaCheckPermission("github:repositories:export")
    @Log(title = "GitHub仓库监控", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GithubRepositories githubRepositories)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("del_flag", "0")
            .orderBy("stargazers_count", false);
        
        List<GithubRepositories> list = githubRepositoriesService.list(queryWrapper);
        MagicExcelUtil<GithubRepositories> util = new MagicExcelUtil<>(GithubRepositories.class);
        util.exportExcel(response, list, "GitHub仓库监控数据");
    }

    /**
     * 获取GitHub仓库监控详细信息
     */
    @Operation(summary = "获取GitHub仓库监控详细信息")
    @SaCheckPermission("github:repositories:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(githubRepositoriesService.getById(id));
    }

    /**
     * 新增GitHub仓库监控
     */
    @Operation(summary = "新增GitHub仓库监控")
    @SaCheckPermission("github:repositories:add")
    @Log(title = "GitHub仓库监控", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GithubRepositories githubRepositories)
    {
        return toAjax(githubRepositoriesService.save(githubRepositories) ? 1 : 0);
    }

    /**
     * 修改GitHub仓库监控
     */
    @Operation(summary = "修改GitHub仓库监控")
    @SaCheckPermission("github:repositories:edit")
    @Log(title = "GitHub仓库监控", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GithubRepositories githubRepositories)
    {
        return toAjax(githubRepositoriesService.updateById(githubRepositories) ? 1 : 0);
    }

    /**
     * 删除GitHub仓库监控
     */
    @Operation(summary = "删除GitHub仓库监控")
    @SaCheckPermission("github:repositories:remove")
    @Log(title = "GitHub仓库监控", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(githubRepositoriesService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
    
    /**
     * 手动同步仓库信息
     */
    @Operation(summary = "手动同步仓库信息")
    @SaCheckPermission("github:repositories:edit")
    @Log(title = "同步仓库信息", businessType = BusinessType.UPDATE)
    @PostMapping("/sync/{ids}")
    public AjaxResult syncRepositories(@PathVariable Long[] ids)
    {
        // TODO: 实现同步逻辑
        return success("同步任务已提交");
    }
}
