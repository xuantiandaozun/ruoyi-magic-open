package com.ruoyi.project.blogapi.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aspectj.lang.annotation.Anonymous;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.github.domain.GithubRepositories;
import com.ruoyi.project.github.domain.GithubUsers;
import com.ruoyi.project.github.service.IGithubRepositoriesService;
import com.ruoyi.project.github.service.IGithubUsersService;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * GitHub公开API Controller
 * 提供给博客网站前端的GitHub榜单数据接口（免登录访问）
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Tag(name = "GitHub公开API")
@Anonymous
@RestController
@RequestMapping("/api/github")
public class GithubApiController extends BaseController
{
    @Autowired
    private IGithubUsersService githubUsersService;
    
    @Autowired
    private IGithubRepositoriesService githubRepositoriesService;

    /**
     * 获取GitHub中国区开发者Top100
     * 
     * @param page 页码，默认1
     * @param size 每页数量，默认10
     * @return 开发者列表分页数据
     */
    @Operation(summary = "获取GitHub中国区开发者Top100")
    @GetMapping("/users/top100")
    public AjaxResult getUsersTop100(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size
    )
    {
        // 构建查询条件：类型为User、前100名、未删除
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("is_top_100", 1)
            .eq("type", "User")
            .eq("del_flag", "0")
            .orderBy("rank_position", true); // 按排名位置升序
        
        // 分页查询
        Page<GithubUsers> pageResult = githubUsersService.page(
            new Page<>(page, size), 
            queryWrapper
        );
        
        return success(pageResult);
    }

    /**
     * 获取GitHub中国区组织Top100
     * 
     * @param page 页码，默认1
     * @param size 每页数量，默认10
     * @return 组织列表分页数据
     */
    @Operation(summary = "获取GitHub中国区组织Top100")
    @GetMapping("/orgs/top100")
    public AjaxResult getOrgsTop100(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size
    )
    {
        // 构建查询条件：类型为Organization、前100名、未删除
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("is_top_100", 1)
            .eq("type", "Organization")
            .eq("del_flag", "0")
            .orderBy("rank_position", true); // 按排名位置升序
        
        // 分页查询
        Page<GithubUsers> pageResult = githubUsersService.page(
            new Page<>(page, size), 
            queryWrapper
        );
        
        return success(pageResult);
    }

    /**
     * 获取用户的仓库列表
     * 
     * @param login 用户登录名
     * @param page 页码，默认1
     * @param size 每页数量，默认10
     * @param sortBy 排序字段(updated/created/pushed/full_name)，默认updated
     * @param language 编程语言过滤，可选
     * @return 用户信息和仓库列表
     */
    @Operation(summary = "获取用户的仓库列表")
    @GetMapping("/users/repos")
    public AjaxResult getUserRepos(
        @RequestParam String login,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(defaultValue = "updated") String sortBy,
        @RequestParam(required = false) String language
    )
    {
        // 获取用户信息
        QueryWrapper userQuery = QueryWrapper.create()
            .eq("login", login)
            .eq("del_flag", "0");
        
        GithubUsers user = githubUsersService.getOne(userQuery);
        if (user == null) {
            return error("用户不存在");
        }
        
        // 构建仓库查询条件
        QueryWrapper repoQuery = QueryWrapper.create()
            .eq("owner_login", login)
            .eq("del_flag", "0");
        
        // 添加编程语言过滤
        if (StrUtil.isNotBlank(language)) {
            repoQuery.eq("language", language);
        }
        
        // 排序
        switch (sortBy) {
            case "created":
                repoQuery.orderBy("github_created_at", false);
                break;
            case "pushed":
                repoQuery.orderBy("github_pushed_at", false);
                break;
            case "full_name":
                repoQuery.orderBy("full_name", true);
                break;
            case "updated":
            default:
                repoQuery.orderBy("github_updated_at", false);
                break;
        }
        
        // 分页查询
        Page<GithubRepositories> pageResult = githubRepositoriesService.page(
            new Page<>(page, size), 
            repoQuery
        );
        
        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("userInfo", user);
        result.put("repos", pageResult);
        
        return success(result);
    }
}
