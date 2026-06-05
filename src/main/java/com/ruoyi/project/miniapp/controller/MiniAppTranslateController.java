package com.ruoyi.project.miniapp.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.miniapp.domain.dto.TranslateTextRequest;
import com.ruoyi.project.miniapp.service.impl.MiniAppImageTranslateService;
import com.ruoyi.project.miniapp.service.impl.MiniAppTextTranslateService;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序文本翻译")
@RestController
@RequestMapping("/miniapp/translate")
public class MiniAppTranslateController {

    private final MiniAppTextTranslateService textTranslateService;
    private final MiniAppImageTranslateService imageTranslateService;

    public MiniAppTranslateController(MiniAppTextTranslateService textTranslateService,
            MiniAppImageTranslateService imageTranslateService) {
        this.textTranslateService = textTranslateService;
        this.imageTranslateService = imageTranslateService;
    }

    @Operation(summary = "文本翻译")
    @PostMapping("/text")
    public AjaxResult translateText(@Validated @RequestBody TranslateTextRequest request) {
        MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(textTranslateService.translateText(request));
    }

    @Operation(summary = "图片翻译（OCR + AI）")
    @PostMapping("/image")
    public AjaxResult translateImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceLanguage", required = false, defaultValue = "Auto") String sourceLanguage,
            @RequestParam("targetLanguage") String targetLanguage) {
        MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(imageTranslateService.translateImage(file, sourceLanguage, targetLanguage));
    }
}
