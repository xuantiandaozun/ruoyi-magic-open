package com.ruoyi.project.miniapp.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.miniapp.domain.dto.TranslateTextRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.service.impl.MiniAppTextTranslateService;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序文本翻译")
@RestController
@RequestMapping("/miniapp/translate")
public class MiniAppTranslateController {

    private final MiniAppTextTranslateService textTranslateService;

    public MiniAppTranslateController(MiniAppTextTranslateService textTranslateService) {
        this.textTranslateService = textTranslateService;
    }

    @Operation(summary = "文本翻译")
    @PostMapping("/text")
    public AjaxResult translateText(@Validated @RequestBody TranslateTextRequest request) {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(textTranslateService.translateText(request));
    }
}
