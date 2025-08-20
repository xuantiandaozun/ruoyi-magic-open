package com.ruoyi.common.utils.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * 字节数组MultipartFile实现类
 * 用于从字节数组创建MultipartFile对象
 * 
 * @author ruoyi
 */
public class ByteArrayMultipartFile implements MultipartFile {
    
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;
    
    public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.content = content != null ? content : new byte[0];
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public String getOriginalFilename() {
        return this.originalFilename;
    }
    
    @Override
    public String getContentType() {
        return this.contentType;
    }
    
    @Override
    public boolean isEmpty() {
        return this.content.length == 0;
    }
    
    @Override
    public long getSize() {
        return this.content.length;
    }
    
    @Override
    public byte[] getBytes() throws IOException {
        return this.content;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.content);
    }
    
    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(this.content);
        }
    }
}