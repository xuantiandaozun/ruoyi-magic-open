package com.ruoyi.project.system.controller;

import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传测试控制器
 * 用于测试云存储文件上传功能
 *
 * @author ruoyi
 */
@Tag(name = "文件上传测试", description = "文件上传功能测试接口")
@RestController
@RequestMapping("/test/upload")
public class FileUploadTestController extends BaseController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 使用云存储上传文件
     */
    @Operation(summary = "云存储上传", description = "使用配置的云存储服务上传文件")
    @PostMapping("/cloud")
    public AjaxResult uploadWithCloudStorage(@RequestParam("file") MultipartFile file) {
        try {
            String url = FileUploadUtils.upload(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("fileName", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("storageType", fileStorageService.getCurrentStorageType());
            
            return AjaxResult.success("上传成功", result);
        } catch (Exception e) {
            return AjaxResult.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 使用本地存储上传文件
     */
    @Operation(summary = "本地存储上传", description = "强制使用本地存储上传文件")
    @PostMapping("/local")
    public AjaxResult uploadWithLocalStorage(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = FileUploadUtils.uploadLocal(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("originalName", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("storageType", "local");
            
            return AjaxResult.success("上传成功", result);
        } catch (Exception e) {
            return AjaxResult.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @Operation(summary = "删除文件", description = "删除指定的文件")
    @DeleteMapping("/delete")
    public AjaxResult deleteFile(@RequestParam("fileName") String fileName) {
        try {
            boolean success = FileUploadUtils.deleteFile(fileName);
            if (success) {
                return AjaxResult.success("删除成功");
            } else {
                return AjaxResult.error("删除失败，文件可能不存在");
            }
        } catch (Exception e) {
            return AjaxResult.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件URL
     */
    @Operation(summary = "获取文件URL", description = "获取指定文件的访问URL")
    @GetMapping("/url")
    public AjaxResult getFileUrl(@RequestParam("fileName") String fileName) {
        try {
            String url = FileUploadUtils.getFileUrl(fileName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("url", url);
            result.put("exists", fileStorageService.exists(fileName));
            result.put("storageType", fileStorageService.getCurrentStorageType());
            
            return AjaxResult.success("获取成功", result);
        } catch (Exception e) {
            return AjaxResult.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取存储配置信息
     */
    @Operation(summary = "获取存储配置", description = "获取当前的存储配置信息")
    @GetMapping("/config")
    public AjaxResult getStorageConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("currentStorageType", fileStorageService.getCurrentStorageType());
            config.put("availableStrategies", fileStorageService.getAllStrategies().keySet());
            
            return AjaxResult.success("获取成功", config);
        } catch (Exception e) {
            return AjaxResult.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 批量上传文件
     */
    @Operation(summary = "批量上传", description = "批量上传多个文件")
    @PostMapping("/batch")
    public AjaxResult batchUpload(@RequestParam("files") MultipartFile[] files) {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, String> successFiles = new HashMap<>();
            Map<String, String> failedFiles = new HashMap<>();
            
            for (MultipartFile file : files) {
                try {
                    String url = FileUploadUtils.upload(file);
                    successFiles.put(file.getOriginalFilename(), url);
                } catch (Exception e) {
                    failedFiles.put(file.getOriginalFilename(), e.getMessage());
                }
            }
            
            result.put("successCount", successFiles.size());
            result.put("failedCount", failedFiles.size());
            result.put("successFiles", successFiles);
            result.put("failedFiles", failedFiles);
            result.put("storageType", fileStorageService.getCurrentStorageType());
            
            return AjaxResult.success("批量上传完成", result);
        } catch (Exception e) {
            return AjaxResult.error("批量上传失败：" + e.getMessage());
        }
    }
}