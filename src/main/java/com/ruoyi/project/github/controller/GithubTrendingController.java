package com.ruoyi.project.github.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import com.ruoyi.project.feishu.domain.FeishuUsers;
import com.ruoyi.project.feishu.domain.dto.FeishuMessageDto;
import com.ruoyi.project.feishu.service.IFeishuService;
import com.ruoyi.project.feishu.service.IFeishuUsersService;
import com.ruoyi.project.github.domain.GithubTrending;
import com.ruoyi.project.github.domain.dto.FeishuPushRequest;
import com.ruoyi.project.github.domain.dto.GithubTrendingIngestRequest;
import com.ruoyi.project.github.domain.dto.GithubTrendingIngestResponse;
import com.ruoyi.project.github.service.IGithubTrendingService;
import com.ruoyi.project.github.task.GithubReadmeUploadTask;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * github流行榜单Controller
 * 
 * @author ruoyi
 * @date 2025-07-03 11:47:11
 */
@Slf4j
@Tag(name = "GitHub Trending 管理")
@RestController
@RequestMapping("/github/trending")
public class GithubTrendingController extends BaseController {
    @Autowired
    private IGithubTrendingService githubTrendingService;

    @Autowired
    private IFeishuService feishuService;

    @Autowired
    private IFeishuUsersService feishuUsersService;

    @Autowired
    private GithubReadmeUploadTask githubReadmeUploadTask;

    /**
     * 接收云函数推送的 GitHub Trending 数据
     * <p>
     * 端点: POST /ingest/github/trending
     * </p>
     * <p>
     * 用于接收云函数抓取的 GitHub Trending 仓库数据，进行去重、落库和更新
     * </p>
     * 
     * @param request        请求数据
     * @param idempotencyKey 幂等键（可选）
     * @param source         数据来源（默认 weibosider）
     * @param period         时间周期（daily/weekly/monthly，默认 daily）
     * @return 处理结果
     */
    @Operation(summary = "接收 GitHub Trending 数据", description = "云函数推送 Trending 仓库数据接口")
    @Log(title = "GitHub Trending", businessType = BusinessType.INSERT)
    @PostMapping("/ingest")
    public AjaxResult ingestTrendingData(
            @Validated @RequestBody GithubTrendingIngestRequest request,
            @Parameter(description = "幂等键") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(description = "数据来源") @RequestParam(value = "source", required = false, defaultValue = "weibosider") String source,
            @Parameter(description = "时间周期") @RequestParam(value = "period", required = false, defaultValue = "daily") String period) {

        // 如果没有提供幂等键，生成一个
        if (StrUtil.isBlank(idempotencyKey)) {
            idempotencyKey = IdUtil.fastSimpleUUID();
        }

        log.info("收到 GitHub Trending 数据推送请求: source={}, period={}, repos={}, idempotencyKey={}",
                source, period, request.getRepos().size(), idempotencyKey);

        GithubTrendingIngestResponse response = githubTrendingService.ingestTrendingData(request, idempotencyKey);

        // 如果有新增仓库，异步触发 README 上传任务
        if (response.getNewInserted() != null && response.getNewInserted() > 0) {
            log.info("检测到 {} 个新增仓库，触发 README 上传任务", response.getNewInserted());
            // 异步执行，避免阻塞响应
            new Thread(() -> {
                try {
                    githubReadmeUploadTask.execute();
                } catch (Exception e) {
                    log.error("异步执行 README 上传任务失败", e);
                }
            }).start();
        }

        return success(response);
    }

    /**
     * 查询github流行榜单列表
     */
    @SaCheckPermission("github:trending:list")
    @GetMapping("/list")
    public TableDataInfo list(GithubTrending githubTrending) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(githubTrending);

        queryWrapper.orderBy("first_trending_date desc,id desc");
        // 使用 MyBatisFlex 的分页方法
        Page<GithubTrending> page = githubTrendingService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出github流行榜单列表
     */
    @SaCheckPermission("github:trending:export")
    @Log(title = "github流行榜单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GithubTrending githubTrending) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件

        List<GithubTrending> list = githubTrendingService.list(queryWrapper);
        MagicExcelUtil<GithubTrending> util = new MagicExcelUtil<>(GithubTrending.class);
        util.exportExcel(response, list, "github流行榜单数据");
    }

    /**
     * 获取github流行榜单详细信息
     */
    @SaCheckPermission("github:trending:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(githubTrendingService.getById(id));
    }

    /**
     * 新增github流行榜单
     */
    @SaCheckPermission("github:trending:add")
    @Log(title = "github流行榜单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GithubTrending githubTrending) {
        return toAjax(githubTrendingService.save(githubTrending) ? 1 : 0);
    }

    /**
     * 修改github流行榜单
     */
    @SaCheckPermission("github:trending:edit")
    @Log(title = "github流行榜单", businessType = BusinessType.UPDATE)
    @PutMapping(value = "")
    public AjaxResult edit(@RequestBody GithubTrending githubTrending) {
        return toAjax(githubTrendingService.updateById(githubTrending) ? 1 : 0);
    }

    /**
     * 删除github流行榜单
     */
    @SaCheckPermission("github:trending:remove")
    @Log(title = "github流行榜单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(githubTrendingService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }

    /**
     * 发送飞书消息（云函数调用）
     */
    @Operation(summary = "发送飞书消息", description = "云函数调用发送飞书消息")
    @Log(title = "发送飞书消息", businessType = BusinessType.OTHER)
    @PostMapping("/push/feishu")
    public AjaxResult pushFeishuMessage(@RequestBody FeishuPushRequest request) {
        // 1. 获取默认飞书用户
        FeishuUsers user = feishuUsersService.getOne(QueryWrapper.create().limit(1));
        if (user == null) {
            return error("未找到飞书用户");
        }

        // 2. 构建消息
        FeishuMessageDto messageDto;
        String receiveId = StrUtil.isNotBlank(user.getUserId()) ? user.getUserId() : user.getOpenId();
        String receiveIdType = StrUtil.isNotBlank(user.getUserId()) ? "user_id" : "open_id";

        if (StrUtil.isNotBlank(request.getTitle())) {
            messageDto = FeishuMessageDto.createPostMessage(receiveId, receiveIdType, request.getTitle(),
                    request.getContent());
        } else {
            messageDto = FeishuMessageDto.createTextMessage(receiveId, receiveIdType, request.getContent());
        }

        // 3. 发送
        boolean success = feishuService.sendMessage(messageDto);
        return success ? success("发送成功") : error("发送失败");
    }
}
