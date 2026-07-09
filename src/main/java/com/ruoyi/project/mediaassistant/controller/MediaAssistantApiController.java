package com.ruoyi.project.mediaassistant.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aspectj.lang.annotation.Anonymous;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.mediaassistant.domain.MediaSource;
import com.ruoyi.project.mediaassistant.domain.dto.RedditSourceCaptureRequest;
import com.ruoyi.project.mediaassistant.service.IMediaSourceService;
import com.ruoyi.project.mediaassistant.service.MediaAssistantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 自媒体助手公开 API。
 */
@Tag(name = "自媒体助手 API")
@Anonymous
@RestController
@RequestMapping("/api/media-assistant")
public class MediaAssistantApiController extends BaseController {

    private final MediaAssistantService mediaAssistantService;
    private final IMediaSourceService mediaSourceService;

    public MediaAssistantApiController(MediaAssistantService mediaAssistantService,
            IMediaSourceService mediaSourceService) {
        this.mediaAssistantService = mediaAssistantService;
        this.mediaSourceService = mediaSourceService;
    }

    @Operation(summary = "采集 Reddit 帖子")
    @Log(title = "自媒体助手采集Reddit", businessType = BusinessType.INSERT)
    @PostMapping("/sources/reddit")
    public AjaxResult captureReddit(@Validated @RequestBody RedditSourceCaptureRequest request) {
        return success(mediaAssistantService.captureReddit(request));
    }

    @Operation(summary = "素材列表")
    @GetMapping("/sources/list")
    public TableDataInfo listSources(MediaSource source) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper query = QueryWrapper.create()
                .eq("source_platform", source.getSourcePlatform(), source.getSourcePlatform() != null)
                .eq("collect_status", source.getCollectStatus(), source.getCollectStatus() != null)
                .like("title", source.getTitle(), source.getTitle() != null)
                .orderBy("source_id", false);
        Page<MediaSource> page = mediaSourceService.page(new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()),
                query);
        return getDataTable(page);
    }

    @Operation(summary = "素材详情")
    @GetMapping("/sources/{sourceId}")
    public AjaxResult getSource(@PathVariable Long sourceId) {
        return success(mediaSourceService.getById(sourceId));
    }

    @Operation(summary = "重新分析素材")
    @Log(title = "自媒体助手分析素材", businessType = BusinessType.OTHER)
    @PostMapping("/sources/{sourceId}/analyze")
    public AjaxResult analyze(@PathVariable Long sourceId) {
        return success(mediaAssistantService.analyze(sourceId));
    }

    @Operation(summary = "草稿转博客草稿")
    @Log(title = "自媒体助手草稿转博客", businessType = BusinessType.INSERT)
    @PostMapping("/drafts/{draftId}/to-blog")
    public AjaxResult saveDraftToBlog(@PathVariable Long draftId) {
        return success(mediaAssistantService.saveDraftToBlog(draftId));
    }
}
