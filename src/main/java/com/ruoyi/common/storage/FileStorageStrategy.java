package com.ruoyi.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储策略接口
 * 支持多种云存储服务的统一接口
 *
 * @author ruoyi
 */
public interface FileStorageStrategy {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param fileName 文件名（包含路径）
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    String upload(MultipartFile file, String fileName) throws Exception;

    /**
     * 删除文件
     *
     * @param fileName 文件名（包含路径）
     * @return 是否删除成功
     */
    boolean delete(String fileName);

    /**
     * 获取文件访问URL
     *
     * @param fileName 文件名（包含路径）
     * @return 文件访问URL
     */
    String getFileUrl(String fileName);

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名（包含路径）
     * @return 是否存在
     */
    boolean exists(String fileName);

    /**
     * 获取存储类型
     *
     * @return 存储类型
     */
    String getStorageType();
}