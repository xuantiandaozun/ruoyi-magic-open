package com.ruoyi.project.miniapp.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向微信搜索爬虫的公开页面摘要，无需登录。
 */
@Tag(name = "小程序搜索收录")
@RestController
@RequestMapping("/miniapp/seo")
public class MiniAppSeoController {

    @Operation(summary = "可收录页面摘要")
    @GetMapping("/pages")
    public AjaxResult pages() {
        List<Map<String, Object>> pages = List.of(
                page("pages/index/index", "译舟文档翻译 - 文档上传翻译",
                        "支持 TXT、DOCX 文档上传，创建异步翻译任务，保留 DOCX 排版。"),
                page("pages/translate/index", "译舟文档翻译 - 文本即时翻译",
                        "支持多语言文本即时翻译，可选择源语言与目标语言。"),
                page("pages/tasks/index", "译舟文档翻译 - 翻译任务列表",
                        "查看文档翻译任务队列、处理进度与完成状态。"),
                page("pages/tasks/index?taskId={taskId}", "译舟文档翻译 - 任务详情",
                        "通过 taskId 参数直接打开指定翻译任务。"),
                page("pages/profile/guide/index", "译舟文档翻译 - 使用说明",
                        "介绍文档上传、文本翻译、任务查看与结果下载流程。"));
        return AjaxResult.success(pages);
    }

    private Map<String, Object> page(String path, String title, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("path", path);
        item.put("title", title);
        item.put("description", description);
        return item;
    }
}
