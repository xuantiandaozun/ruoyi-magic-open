package com.ruoyi.project.ai.service;

/**
 * AI图片处理服务接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public interface IAiImageService {
    
    /**
     * 生成AI图片
     * 
     * @param prompt 提示词
     * @param model 模型名称（可选）
     * @param size 图片尺寸（可选）
     * @param n 生成数量（可选）
     * @return 生成的图片URL列表（已上传到OSS）
     */
    String[] generateImage(String prompt, String model, String size, Integer n);
    
    /**
     * 从临时URL下载图片并上传到OSS
     * 
     * @param tempUrl 临时图片URL
     * @param fileName 文件名（不含扩展名）
     * @return OSS中的图片URL
     */
    String downloadAndUploadToOss(String tempUrl, String fileName);
    
    /**
     * 批量处理图片URL
     * 
     * @param tempUrls 临时图片URL数组
     * @param baseFileName 基础文件名
     * @return OSS中的图片URL数组
     */
    String[] batchDownloadAndUpload(String[] tempUrls, String baseFileName);
}