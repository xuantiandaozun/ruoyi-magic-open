package com.ruoyi.project.miniapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.miniapp.domain.TranslateDocument;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.service.ITranslateDocumentService;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序文档")
@RestController
@RequestMapping("/miniapp/documents")
public class MiniAppDocumentController {

    private final ITranslateDocumentService documentService;

    public MiniAppDocumentController(ITranslateDocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "上传翻译文档")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file) throws Exception {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        TranslateDocument document = documentService.upload(file, loginUser);
        return AjaxResult.success(document);
    }

    @Operation(summary = "查询文档详情")
    @GetMapping("/{documentId}")
    public AjaxResult detail(@PathVariable Long documentId) {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(documentService.getOwnedDocument(documentId, loginUser));
    }
}
