package com.ruoyi.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 文件类型枚举
 * 
 * @author ruoyi
 */
public enum FileType {
    
    /** 图片类型 */
    IMAGE("image", "图片", "jpg,jpeg,png,gif,bmp,webp,svg,ico"),
    
    /** 视频类型 */
    VIDEO("video", "视频", "mp4,avi,mov,wmv,flv,mkv,webm,m4v"),
    
    /** 音频类型 */
    AUDIO("audio", "音频", "mp3,wav,flac,aac,ogg,wma,m4a"),
    
    /** 文档类型 */
    DOCUMENT("document", "文档", "pdf,doc,docx,xls,xlsx,ppt,pptx,txt,html,htm,md"),
    
    /** 压缩文件 */
    ARCHIVE("archive", "压缩包", "zip,rar,gz,bz2,7z,tar"),
    
    /** 其他类型 */
    OTHER("other", "其他", "");
    
    /** 类型代码 */
    private final String code;
    
    /** 类型名称 */
    private final String name;
    
    /** 支持的扩展名 */
    private final String extensions;
    
    FileType(String code, String name, String extensions) {
        this.code = code;
        this.name = name;
        this.extensions = extensions;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getExtensions() {
        return extensions;
    }
    
    /**
     * 根据文件扩展名获取文件类型
     * 
     * @param extension 文件扩展名
     * @return 文件类型枚举
     */
    public static FileType getByExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return OTHER;
        }
        
        String ext = extension.toLowerCase().trim();
        
        for (FileType fileType : values()) {
            if (fileType.extensions.contains(ext)) {
                return fileType;
            }
        }
        
        return OTHER;
    }
    
    /**
     * 根据代码获取文件类型
     * 
     * @param code 类型代码
     * @return 文件类型枚举
     */
    @JsonCreator
    public static FileType getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return OTHER;
        }
        
        for (FileType fileType : values()) {
            if (fileType.code.equals(code)) {
                return fileType;
            }
        }
        
        return OTHER;
    }
    
    /**
     * 获取所有文件类型的代码数组
     * 
     * @return 代码数组
     */
    public static String[] getCodes() {
        FileType[] types = values();
        String[] codes = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            codes[i] = types[i].code;
        }
        return codes;
    }
    
    /**
     * 获取所有文件类型的名称数组
     * 
     * @return 名称数组
     */
    public static String[] getNames() {
        FileType[] types = values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].name;
        }
        return names;
    }
}