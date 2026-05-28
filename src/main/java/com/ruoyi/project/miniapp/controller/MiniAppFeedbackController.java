package com.ruoyi.project.miniapp.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.miniapp.domain.MiniFeedback;
import com.ruoyi.project.miniapp.domain.dto.MiniFeedbackSubmitRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.service.IMiniFeedbackService;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序反馈")
@RestController
@RequestMapping("/miniapp/feedback")
public class MiniAppFeedbackController {

    private final IMiniFeedbackService miniFeedbackService;

    public MiniAppFeedbackController(IMiniFeedbackService miniFeedbackService) {
        this.miniFeedbackService = miniFeedbackService;
    }

    @Operation(summary = "提交问题反馈")
    @PostMapping
    public AjaxResult submit(@Validated @RequestBody MiniFeedbackSubmitRequest request) {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        MiniFeedback feedback = miniFeedbackService.submit(request, loginUser);
        return AjaxResult.success(feedback);
    }

    @Operation(summary = "我的反馈列表")
    @GetMapping("/mine")
    public AjaxResult mine() {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        List<MiniFeedback> feedbackList = miniFeedbackService.listByOwner(loginUser);
        return AjaxResult.success(feedbackList);
    }
}
