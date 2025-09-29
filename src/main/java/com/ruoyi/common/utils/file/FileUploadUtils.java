package com.ruoyi.common.utils.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.file.FileNameLengthLimitExceededException;
import com.ruoyi.common.exception.file.FileSizeLimitExceededException;
import com.ruoyi.common.exception.file.InvalidExtensionException;
import com.ruoyi.common.storage.FileStorageService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.utils.uuid.Seq;
import com.ruoyi.framework.config.RuoYiConfig;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.common.enums.FileType;

/**
 * 文件上传工具类
 * 
 * <p>主要功能：</p>
 * <ul>
 *   <li>统一文件上传接口：根据yml配置自动选择本地存储或云存储</li>
 *   <li>支持多种云存储：阿里云OSS、腾讯云COS、亚马逊S3、微软Azure</li>
 *   <li>向后兼容：保留原有的强制本地存储和云存储方法</li>
 *   <li>云存储目录规范：按文件类型和时间自动分类存储</li>
 * </ul>
 * 
 * <p>配置说明：</p>
 * <pre>
 * # application.yml
 * ruoyi:
 *   cloud-storage:
 *     type: local    # 本地存储
 *     # type: aliyun  # 阿里云OSS
 *     # type: tencent # 腾讯云COS
 *     # type: amazon  # 亚马逊S3
 *     # type: azure   # 微软Azure
 * </pre>
 * 
 * <p>云存储目录结构：</p>
 * <pre>
 * 文件类型/年份/月份/日期/文件名
 * ├── images/2024/01/15/photo_123456.jpg      # 图片文件
 * ├── documents/2024/01/15/report_123456.pdf  # 文档文件
 * ├── videos/2024/01/15/video_123456.mp4      # 视频文件
 * ├── media/2024/01/15/audio_123456.mp3       # 音频文件
 * ├── archives/2024/01/15/data_123456.zip     # 压缩文件
 * └── others/2024/01/15/file_123456.xxx       # 其他文件
 * </pre>
 * 
 * <p>推荐使用方法：</p>
 * <ul>
 *   <li>{@link #upload(MultipartFile)} - 使用默认配置上传</li>
 *   <li>{@link #upload(String, MultipartFile)} - 指定基础目录上传</li>
 *   <li>{@link #upload(String, MultipartFile, String[])} - 完整参数上传</li>
 * </ul>
 *
 * @author ruoyi
 */
public class FileUploadUtils
{
    /**
     * 默认大小 50M
     */
    public static final long DEFAULT_MAX_SIZE = 200 * 1024 * 1024L;

    /**
     * 默认的文件名最大长度 100
     */
    public static final int DEFAULT_FILE_NAME_LENGTH = 100;

    /**
     * 默认上传的地址
     */
    private static String defaultBaseDir = RuoYiConfig.getProfile();

    public static void setDefaultBaseDir(String defaultBaseDir)
    {
        FileUploadUtils.defaultBaseDir = defaultBaseDir;
    }

    public static String getDefaultBaseDir()
    {
        return defaultBaseDir;
    }

    /**
     * 以默认配置进行文件上传
     * 根据yml配置自动选择本地存储或云存储
     *
     * @param file 上传的文件
     * @return 文件访问URL或文件路径
     * @throws IOException 上传异常
     */
    public static final String upload(MultipartFile file) throws IOException
    {
        try
        {
            return upload(getDefaultBaseDir(), file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 强制使用本地存储上传文件（向后兼容方法）
     * 注意：此方法会忽略yml配置，强制使用本地存储
     *
     * @param file 上传的文件
     * @return 文件名称
     * @throws IOException 上传异常
     */
    public static final String uploadLocal(MultipartFile file) throws IOException
    {
        try
        {
            return uploadToLocal(getDefaultBaseDir(), file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 根据文件路径上传
     * 根据yml配置自动选择本地存储或云存储
     *
     * @param baseDir 相对应用的基目录（仅本地存储时使用）
     * @param file 上传的文件
     * @return 文件名称或URL
     * @throws IOException 上传异常
     */
    public static final String upload(String baseDir, MultipartFile file) throws IOException
    {
        try
        {
            return upload(baseDir, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 统一文件上传方法
     * 优先使用数据库配置，如果数据库没有配置则使用yml配置
     * 自动记录文件上传信息到数据库
     *
     * @param baseDir 相对应用的基目录（仅本地存储时使用）
     * @param file 上传的文件
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件名或URL
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws FileNameLengthLimitExceededException 文件名太长
     * @throws IOException 比如读写文件出错时
     * @throws InvalidExtensionException 文件校验异常
     */
    public static final String upload(String baseDir, MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException
    {
        try {
            // 获取存储配置（优先使用数据库配置）
            FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
            String storageType = fileStorageService.getCurrentStorageType();
            
            // 根据配置选择存储方式
            if ("local".equalsIgnoreCase(storageType)) {
                return uploadToLocal(baseDir, file, allowedExtension);
            } else {
                // 使用云存储（包含数据库配置优先和上传记录功能）
                return uploadToCloud(file, allowedExtension);
            }
        } catch (Exception e) {
            // 如果获取配置失败，默认使用本地存储
            return uploadToLocal(baseDir, file, allowedExtension);
        }
    }

    /**
     * 本地文件上传
     * 使用增强的FileStorageService，支持数据库配置优先和文件上传记录
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件名
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws FileNameLengthLimitExceededException 文件名太长
     * @throws IOException 比如读写文件出错时
     * @throws InvalidExtensionException 文件校验异常
     */
    private static final String uploadToLocal(String baseDir, MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException
    {
        int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNameLength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        assertAllowed(file, allowedExtension);

        try {
            // 使用增强的文件存储服务上传（自动记录上传信息和使用数据库配置）
            String fileName = extractFilename(file);
            FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
            return fileStorageService.uploadLocal(file, fileName, baseDir);
        } catch (Exception e) {
            // 如果通过FileStorageService失败，回退到原始方式
            String fileName = extractFilename(file);
            String absPath = getAbsoluteFile(baseDir, fileName).getAbsolutePath();
            file.transferTo(Paths.get(absPath));
            return getPathFileName(baseDir, fileName);
        }
    }

    /**
     * 云存储文件上传
     * 使用增强的FileStorageService，支持数据库配置优先和文件上传记录
     *
     * @param file 上传的文件
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件URL
     * @throws Exception 上传异常
     */
    private static final String uploadToCloud(MultipartFile file, String[] allowedExtension) throws Exception
    {
        // 文件校验
        int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNameLength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        assertAllowed(file, allowedExtension);

        // 为云存储生成规范的文件名（包含分类目录）
        String fileName = extractCloudFilename(file);

        // 使用增强的文件存储服务上传（自动记录上传信息和使用数据库配置）
        FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
        return fileStorageService.upload(file, fileName);
    }

    /**
     * 编码文件名（本地存储使用）
     */
    public static final String extractFilename(MultipartFile file)
    {
        return StrUtil.format("{}/{}_{}.{}", DateUtil.format(DateUtil.date(), "yyyy/MM/dd"),
                FilenameUtils.getBaseName(file.getOriginalFilename()), Seq.getId(Seq.uploadSeqType), getExtension(file));
    }

    /**
     * 为云存储生成规范的文件名（包含分类目录）
     * 目录结构：文件类型/年份/月份/日期/文件名
     * 例如：images/2024/01/15/filename_123456.jpg
     *      documents/2024/01/15/filename_123456.pdf
     *      videos/2024/01/15/filename_123456.mp4
     */
    public static final String extractCloudFilename(MultipartFile file)
    {
        String extension = getExtension(file);
        String fileType = getFileTypeCategory(extension);
        String datePath = DateUtil.format(DateUtil.date(), "yyyy/MM/dd");
        String baseName = FilenameUtils.getBaseName(file.getOriginalFilename());
        String uniqueId = Seq.getId(Seq.uploadSeqType);
        
        return StrUtil.format("{}/{}/{}_{}.{}", fileType, datePath, baseName, uniqueId, extension);
    }

    /**
     * 根据文件扩展名获取文件类型分类
     * 
     * @param extension 文件扩展名
     * @return 文件类型分类目录名
     */
    private static final String getFileTypeCategory(String extension)
    {
        FileType fileType = FileType.getByExtension(extension);
        return fileType.getCode();
    }

    public static final File getAbsoluteFile(String uploadDir, String fileName) throws IOException
    {
        File desc = new File(uploadDir + File.separator + fileName);

        if (!desc.exists())
        {
            if (!desc.getParentFile().exists())
            {
                desc.getParentFile().mkdirs();
            }
        }
        return desc;
    }

    public static final String getPathFileName(String uploadDir, String fileName) throws IOException
    {
        int dirLastIndex = RuoYiConfig.getProfile().length() + 1;
        String currentDir = StrUtil.sub(uploadDir, dirLastIndex, uploadDir.length());
        return Constants.RESOURCE_PREFIX + "/" + currentDir + "/" + fileName;
    }

    /**
     * 文件大小校验
     *
     * @param file 上传的文件
     * @return
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws InvalidExtensionException
     */
    public static final void assertAllowed(MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, InvalidExtensionException
    {
        long size = file.getSize();
        if (size > DEFAULT_MAX_SIZE)
        {
            throw new FileSizeLimitExceededException(DEFAULT_MAX_SIZE / 1024 / 1024);
        }

        String fileName = file.getOriginalFilename();
        String extension = getExtension(file);
        if (allowedExtension != null && !isAllowedExtension(extension, allowedExtension))
        {
            if (allowedExtension == MimeTypeUtils.IMAGE_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidImageExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.FLASH_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidFlashExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.MEDIA_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidMediaExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.VIDEO_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidVideoExtensionException(allowedExtension, extension,
                        fileName);
            }
            else
            {
                throw new InvalidExtensionException(allowedExtension, extension, fileName);
            }
        }
    }

    /**
     * 判断MIME类型是否是允许的MIME类型
     *
     * @param extension
     * @param allowedExtension
     * @return
     */
    public static final boolean isAllowedExtension(String extension, String[] allowedExtension)
    {
        for (String str : allowedExtension)
        {
            if (str.equalsIgnoreCase(extension))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件名的后缀
     *
     * @param file 表单文件
     * @return 后缀名
     */
    public static final String getExtension(MultipartFile file)
    {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StrUtil.isEmpty(extension))
        {
            extension = MimeTypeUtils.getExtension(Objects.requireNonNull(file.getContentType()));
        }
        return extension;
    }

    /**
     * 强制使用云存储上传文件（向后兼容方法）
     * 注意：此方法会忽略yml配置，强制使用云存储
     * 但仍会使用数据库配置优先和记录上传信息
     *
     * @param file 上传的文件
     * @param allowedExtension 允许的文件扩展名
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    public static final String uploadWithCloudStorage(MultipartFile file, String[] allowedExtension) throws Exception
    {
        // 文件校验
        int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNameLength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        assertAllowed(file, allowedExtension);

        // 为云存储生成规范的文件名（包含分类目录）
        String fileName = extractCloudFilename(file);

        // 使用增强的文件存储服务上传（自动记录上传信息和使用数据库配置）
        FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
        return fileStorageService.upload(file, fileName);
    }

    /**
     * 强制使用云存储上传文件（指定文件名，向后兼容方法）
     * 注意：此方法会忽略yml配置，强制使用云存储
     * 但仍会使用数据库配置优先和记录上传信息
     *
     * @param file 上传的文件
     * @param fileName 指定的文件名
     * @param allowedExtension 允许的文件扩展名
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    public static final String uploadWithCloudStorage(MultipartFile file, String fileName, String[] allowedExtension) throws Exception
    {
        // 文件校验
        int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNameLength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        assertAllowed(file, allowedExtension);

        // 使用增强的文件存储服务上传（自动记录上传信息和使用数据库配置）
        FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
        return fileStorageService.upload(file, fileName);
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @return 是否删除成功
     */
    public static final boolean deleteFile(String fileName)
    {
        try
        {
            FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
            return fileStorageService.delete(fileName);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 获取文件访问URL
     *
     * @param fileName 文件名
     * @return 文件访问URL
     */
    public static final String getFileUrl(String fileName)
    {
        try
        {
            FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
            return fileStorageService.getFileUrl(fileName);
        }
        catch (Exception e)
        {
            return "";
        }
    }

    /**
     * 获取当前存储配置信息
     * 包含配置来源（数据库或YML）、存储类型等信息
     *
     * @return 存储配置信息
     */
    public static final java.util.Map<String, Object> getCurrentStorageConfig()
    {
        try
        {
            FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
            return fileStorageService.getCurrentStorageConfig();
        }
        catch (Exception e)
        {
            java.util.Map<String, Object> errorConfig = new java.util.HashMap<>();
            errorConfig.put("source", "error");
            errorConfig.put("error", e.getMessage());
            return errorConfig;
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名
     * @return 是否存在
     */
    public static final boolean fileExists(String fileName)
    {
        try
        {
            FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
            return fileStorageService.exists(fileName);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 获取当前存储类型
     *
     * @return 存储类型（local、aliyun、tencent、amazon、azure等）
     */
    public static final String getCurrentStorageType()
    {
        try
        {
            FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
            return fileStorageService.getCurrentStorageType();
        }
        catch (Exception e)
        {
            return "local"; // 默认返回本地存储
        }
    }
}
