package com.ruoyi.project.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.ai.dto.ImageGenerationRequest;
import com.ruoyi.project.ai.service.IAiImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI图片生成控制器
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Slf4j
@Tag(name = "AI图片生成")
@RestController
@RequestMapping("/ai/image")
public class AiImageController extends BaseController {
    
    @Autowired
    private IAiImageService aiImageService;
    
    /**
     * 生成AI图片
     */
    @Operation(summary = "生成AI图片")
    @SaCheckPermission("ai:image:generate")
    @PostMapping("/generate")
    public AjaxResult generateImage(@RequestBody ImageGenerationRequest request) {
        try {
            // 参数验证
            if (StrUtil.isBlank(request.getPrompt())) {
                return error("提示词不能为空");
            }
            
            // 验证生成数量
            Integer n = request.getN();
            if (n != null && (n < 1 || n > 4)) {
                return error("生成数量必须在1-4之间");
            }
            
            // 验证图片尺寸格式
            String size = request.getSize();
            if (StrUtil.isNotBlank(size) && !isValidSize(size)) {
                return error("图片尺寸格式不正确，应为 宽*高 格式，如：1328*1328");
            }
            
            log.info("用户 {} 请求生成AI图片，提示词：{}", StpUtil.getLoginIdAsLong(), request.getPrompt());
            
            // 调用服务生成图片
            String[] imageUrls = aiImageService.generateImage(
                request.getPrompt(),
                request.getModel(),
                request.getSize(),
                request.getN()
            );
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("images", imageUrls);
            result.put("count", imageUrls.length);
            result.put("prompt", request.getPrompt());
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("AI图片生成成功，共生成 {} 张图片", imageUrls.length);
            
            return success("AI图片生成成功", result);
            
        } catch (Exception e) {
            log.error("AI图片生成失败", e);
            return error("AI图片生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取支持的模型列表
     */
    @Operation(summary = "获取支持的模型列表")
    @SaCheckPermission("ai:image:generate")
    @GetMapping("/models")
    public AjaxResult getSupportedModels() {
        try {
            Map<String, Object> models = new HashMap<>();
            models.put("qwen-image-plus", "通义万相增强版");
            models.put("qwen-image", "通义万相标准版");
            
            return success(models);
        } catch (Exception e) {
            log.error("获取模型列表失败", e);
            return error("获取模型列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取支持的图片尺寸列表
     */
    @Operation(summary = "获取支持的图片尺寸列表")
    @SaCheckPermission("ai:image:generate")
    @GetMapping("/sizes")
    public AjaxResult getSupportedSizes() {
        try {
            String[] sizes = {
                "1664*928",   // 16:9
                "1472*1140",  // 4:3
                "1328*1328",  // 1:1 (默认)
                "1140*1472",  // 3:4
                "928*1664"    // 9:16
            };
            
            Map<String, Object> result = new HashMap<>();
            result.put("sizes", sizes);
            result.put("default", "1328*1328");
            
            return success(result);
        } catch (Exception e) {
            log.error("获取尺寸列表失败", e);
            return error("获取尺寸列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证图片尺寸格式
     */
    private boolean isValidSize(String size) {
        if (StrUtil.isBlank(size)) {
            return false;
        }
        
        try {
            String[] parts = size.split("\\*");
            if (parts.length != 2) {
                return false;
            }
            
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            
            // 验证尺寸范围（根据阿里云通义万相的限制）
            return width > 0 && height > 0 && width <= 2048 && height <= 2048;
            
        } catch (NumberFormatException e) {
            return false;
        }
    }
}