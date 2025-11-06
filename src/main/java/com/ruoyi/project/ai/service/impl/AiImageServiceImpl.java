package com.ruoyi.project.ai.service.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.file.ByteArrayMultipartFile;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.project.ai.domain.AiCoverGenerationRecord;
import com.ruoyi.project.ai.dto.ImageGenerationRequest;
import com.ruoyi.project.ai.service.IAiCoverGenerationRecordService;
import com.ruoyi.project.ai.service.IAiImageService;
import com.ruoyi.project.ai.tool.impl.AliImageGenerationLangChain4jTool;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * AI图片处理服务实现类
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Slf4j
@Service
public class AiImageServiceImpl implements IAiImageService {
    
    @Autowired
    private AliImageGenerationLangChain4jTool aliImageGenerationTool;
    
    @Autowired
    private IAiCoverGenerationRecordService aiCoverGenerationRecordService;
    
    @Override
    public String[] generateImage(String prompt, String model, String size, Integer n) {
        try {
            // 构建工具参数
            Map<String, Object> toolParams = buildToolParams(prompt, model, size, n, null, null);
            
            // 调用AI生图工具
            String result = aliImageGenerationTool.execute(toolParams);
            log.info("AI生图工具返回结果: {}", result);
            
            // 处理返回结果
            if (StrUtil.startWith(result, "ERROR:")) {
                throw new RuntimeException("AI生图失败: " + result);
            }
            
            if (StrUtil.startWith(result, "TASK_ID:")) {
                throw new RuntimeException("AI生图任务异步处理，暂不支持: " + result);
            }
            
            // 解析图片URL
            String[] tempUrls = result.split(",");
            
            // 下载并上传到OSS
            String baseFileName = "ai_image_" + DateUtil.format(DateUtil.date(), "yyyyMMdd_HHmmss");
            return batchDownloadAndUpload(tempUrls, baseFileName);
            
        } catch (Exception e) {
            log.error("AI生图处理失败", e);
            throw new RuntimeException("AI生图处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建工具参数
     */
    private Map<String, Object> buildToolParams(String prompt, String model, String size, Integer n, Boolean promptExtend, Boolean watermark) {
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", prompt);
        
        if (model != null && !model.isEmpty()) {
            params.put("model", model);
        }
        if (size != null && !size.isEmpty()) {
            params.put("size", size);
        }
        if (n != null && n > 0) {
            params.put("n", n);
        }
        if (promptExtend != null) {
            params.put("promptExtend", promptExtend);
        }
        if (watermark != null) {
            params.put("watermark", watermark);
        }
        
        return params;
    }
    
    @Override
    public String downloadAndUploadToOss(String tempUrl, String fileName) {
        try {
            log.info("开始下载图片: {}", tempUrl);
            
            // 下载图片
            byte[] imageBytes = HttpUtil.downloadBytes(tempUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("下载图片失败，图片内容为空");
            }
            
            log.info("图片下载成功，大小: {} bytes", imageBytes.length);
            
            // 生成文件名
            String fileExtension = getFileExtension(tempUrl);
            String fullFileName = fileName + fileExtension;
            
            // 上传到OSS
            try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
                // 创建ByteArrayMultipartFile包装InputStream
                ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                    "file", 
                    fullFileName, 
                    "image/jpeg", 
                    imageBytes
                );
                String ossUrl = FileUploadUtils.upload(multipartFile);
                log.info("图片上传OSS成功: {}", ossUrl);
                return ossUrl;
            }
            
        } catch (Exception e) {
            log.error("下载并上传图片失败: {}", tempUrl, e);
            throw new RuntimeException("下载并上传图片失败: " + e.getMessage());
        }
    }
    
    @Override
    public String[] batchDownloadAndUpload(String[] tempUrls, String baseFileName) {
        List<String> ossUrls = new ArrayList<>();
        
        for (int i = 0; i < tempUrls.length; i++) {
            String tempUrl = tempUrls[i].trim();
            if (StrUtil.isBlank(tempUrl)) {
                continue;
            }
            
            String fileName = baseFileName;
            if (tempUrls.length > 1) {
                fileName = baseFileName + "_" + (i + 1);
            }
            
            try {
                String ossUrl = downloadAndUploadToOss(tempUrl, fileName);
                ossUrls.add(ossUrl);
            } catch (Exception e) {
                log.error("处理第{}张图片失败: {}", i + 1, tempUrl, e);
                // 继续处理其他图片，不中断整个流程
            }
        }
        
        if (ossUrls.isEmpty()) {
            throw new RuntimeException("所有图片处理都失败了");
        }
        
        return ossUrls.toArray(new String[0]);
    }
    
    @Override
    public void saveImageGenerationRecords(String[] imageUrls, ImageGenerationRequest request) {
        try {
            for (int i = 0; i < imageUrls.length; i++) {
                AiCoverGenerationRecord record = new AiCoverGenerationRecord();
                record.setPrompt(request.getPrompt());
                record.setImageUrl(imageUrls[i]);
                record.setAiModel(request.getModel());
                record.setCoverType("0");  // 0-通用封面
                record.setGenerationStatus("1");  // 1-成功
                record.setIsUsed("0");  // 0-未使用
                record.setDelFlag("0");
                record.setGenerationTime(new Date());
                // 保存记录，通用字段由MyBatis-Flex自动填充
                aiCoverGenerationRecordService.save(record);
                log.info("AI生图记录保存成功，图片URL: {}", imageUrls[i]);
            }
        } catch (Exception e) {
            log.error("保存AI生图记录失败", e);
            // 不中断业务流程，仅记录日志
        }
    }
    
    /**
     * 从URL中获取文件扩展名
     */
    private String getFileExtension(String url) {
        try {
            // 移除查询参数
            String cleanUrl = url.split("\\?")[0];
            
            // 获取文件扩展名
            int lastDotIndex = cleanUrl.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < cleanUrl.length() - 1) {
                return cleanUrl.substring(lastDotIndex);
            }
            
            // 默认为jpg
            return ".jpg";
        } catch (Exception e) {
            log.warn("无法从URL获取文件扩展名: {}", url);
            return ".jpg";
        }
    }
}