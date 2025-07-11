package com.ruoyi.common.storage.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.storage.FileStorageStrategy;
import com.ruoyi.framework.config.RuoYiConfig;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * 本地文件存储策略实现
 *
 * @author ruoyi
 */
@Component("localFileStorageStrategy")
public class LocalFileStorageStrategy implements FileStorageStrategy {

    @Override
    public String upload(MultipartFile file, String fileName) throws Exception {
        String uploadDir = RuoYiConfig.getProfile();
        File desc = new File(uploadDir + File.separator + fileName);

        if (!desc.exists()) {
            if (!desc.getParentFile().exists()) {
                desc.getParentFile().mkdirs();
            }
        }

        file.transferTo(Paths.get(desc.getAbsolutePath()));
        return getFileUrl(fileName);
    }

    @Override
    public boolean delete(String fileName) {
        try {
            String uploadDir = RuoYiConfig.getProfile();
            File file = new File(uploadDir + File.separator + fileName);
            return file.exists() && file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        int dirLastIndex = RuoYiConfig.getProfile().length() + 1;
        String uploadDir = RuoYiConfig.getProfile();
        String currentDir = StrUtil.sub(uploadDir, dirLastIndex, uploadDir.length());
        return Constants.RESOURCE_PREFIX + "/" + currentDir + "/" + fileName;
    }

    @Override
    public boolean exists(String fileName) {
        try {
            String uploadDir = RuoYiConfig.getProfile();
            File file = new File(uploadDir + File.separator + fileName);
            return file.exists();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "local";
    }
}