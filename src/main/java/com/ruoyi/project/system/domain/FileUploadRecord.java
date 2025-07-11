package com.ruoyi.project.system.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.enums.FileType;

/**
 * 文件上传记录对象 sys_file_upload_record
 * 
 * @author ruoyi
 * @date 2025-07-11 12:01:15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_file_upload_record")
public class FileUploadRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Id(keyType = KeyType.Auto)
    private String recordId;

    /** 原始文件名 */
    @Excel(name = "原始文件名")
    private String originalFilename;

    /** 存储文件名 */
    private String storedFilename;

    /** 文件路径 */
    private String filePath;

    /** 文件访问URL */
    private String fileUrl;

    /** 文件大小（字节） */
    @Excel(name = "文件大小", readConverterExp = "字=节")
    private Long fileSize;

    /** 文件类型 */
    @Excel(name = "文件类型", readConverterExp = "image=图片,video=视频,audio=音频,document=文档,archive=压缩包,other=其他")
    private FileType fileType;

    /** 文件扩展名 */
    private String fileExtension;

    /** MIME类型 */
    private String mimeType;

    /** 存储类型 */
    private String storageType;

    /** 关联的配置ID */
    private String configId;

    /** 上传状态（0失败 1成功） */
    @Excel(name = "上传状态", readConverterExp = "0=失败,1=成功")
    private String uploadStatus;

    /** 错误信息 */
    private String errorMessage;

    /** 上传IP地址 */
    private String uploadIp;

    /** 用户代理 */
    private String userAgent;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

}
