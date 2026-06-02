package com.ruoyi.project.miniapp.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.miniapp.domain.TranslateDocument;
import com.ruoyi.project.miniapp.service.ITranslateDocumentService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 翻译文档管理 Controller
 */
@Tag(name = "翻译文档管理")
@RestController
@RequestMapping("/manage/translateDocument")
public class TranslateDocumentManageController extends BaseController {

    @Autowired
    private ITranslateDocumentService translateDocumentService;

    @Operation(summary = "查询翻译文档列表")
    @SaCheckPermission("manage:translateDocument:list")
    @GetMapping("/list")
    public TableDataInfo list(TranslateDocument document) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .from("translate_document")
                .and("del_flag = '0'")
                .orderBy("create_time desc");
        if (document.getMiniAppId() != null) {
            queryWrapper.and("mini_app_id = ?", document.getMiniAppId());
        }
        if (document.getMiniUserId() != null) {
            queryWrapper.and("mini_user_id = ?", document.getMiniUserId());
        }
        if (StrUtil.isNotBlank(document.getStatus())) {
            queryWrapper.and("status = ?", document.getStatus());
        }
        if (StrUtil.isNotBlank(document.getParseStatus())) {
            queryWrapper.and("parse_status = ?", document.getParseStatus());
        }

        Page<TranslateDocument> page = translateDocumentService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "获取翻译文档详情")
    @SaCheckPermission("manage:translateDocument:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(translateDocumentService.getById(id));
    }
}
