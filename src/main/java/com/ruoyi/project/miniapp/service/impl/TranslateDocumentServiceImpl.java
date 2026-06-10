package com.ruoyi.project.miniapp.service.impl;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.common.utils.file.ByteArrayMultipartFile;
import com.ruoyi.project.miniapp.domain.TranslateDocument;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.mapper.TranslateDocumentMapper;
import com.ruoyi.project.miniapp.service.ITranslateDocumentService;
import com.ruoyi.project.miniapp.util.MiniAppDocumentTextExtractor;

import cn.hutool.core.util.StrUtil;

@Service
@UseDataSource("MASTER")
public class TranslateDocumentServiceImpl extends ServiceImpl<TranslateDocumentMapper, TranslateDocument>
        implements ITranslateDocumentService {

    private static final String[] ALLOWED_EXTENSIONS = { "txt", "docx" };

    private final FileStorageService fileStorageService;
    private final MiniAppContentSecurityService contentSecurityService;

    public TranslateDocumentServiceImpl(FileStorageService fileStorageService,
            MiniAppContentSecurityService contentSecurityService) {
        this.fileStorageService = fileStorageService;
        this.contentSecurityService = contentSecurityService;
    }

    @Override
    public TranslateDocument upload(MultipartFile file, String clientFileName, MiniAppLoginUser loginUser) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String originalName = resolveOriginalName(clientFileName, file.getOriginalFilename());
        String extension = StrUtil.nullToDefault(FilenameUtils.getExtension(originalName), "").toLowerCase();
        if (!isAllowedExtension(extension)) {
            throw new ServiceException("当前支持 txt 和 docx 文件上传，md 即将支持");
        }

        byte[] fileBytes = file.getBytes();
        String documentText = MiniAppDocumentTextExtractor.extract(fileBytes, extension);
        contentSecurityService.checkSocialText(loginUser, documentText);

        LocalDate now = LocalDate.now();
        String objectKey = StrUtil.format(
                "miniapp/{}/{}/{}/{}/{}.{}",
                loginUser.getAppCode(),
                "source",
                now.getYear(),
                String.format("%02d/%02d", now.getMonthValue(), now.getDayOfMonth()),
                UUID.randomUUID().toString().replace("-", ""),
                extension);

        String fileUrl = fileStorageService.upload(
                new ByteArrayMultipartFile("file", originalName, file.getContentType(), fileBytes),
                objectKey);

        TranslateDocument document = new TranslateDocument();
        document.setMiniUserId(loginUser.getMiniUserId());
        document.setMiniAppId(loginUser.getMiniAppId());
        document.setSourceType(extension);
        document.setOriginalName(originalName);
        document.setFileExt(extension);
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setContentHash(calculateHash(fileBytes));
        document.setSourceOssUrl(fileUrl);
        document.setSourceOssKey(objectKey);
        document.setParseStatus("pending");
        document.setStatus("uploaded");
        document.setDelFlag("0");
        save(document);
        return document;
    }

    @Override
    public TranslateDocument getOwnedDocument(Long documentId, MiniAppLoginUser loginUser) {
        QueryWrapper qw = QueryWrapper.create()
                .from("translate_document")
                .where("id = ?", documentId)
                .and("mini_user_id = ?", loginUser.getMiniUserId())
                .and("mini_app_id = ?", loginUser.getMiniAppId())
                .and("del_flag = '0'")
                .limit(1);
        TranslateDocument document = getOne(qw);
        if (document == null) {
            throw new ServiceException("文档不存在或无权访问");
        }
        return document;
    }

    private boolean isAllowedExtension(String extension) {
        for (String allowedExtension : ALLOWED_EXTENSIONS) {
            if (StrUtil.equalsIgnoreCase(allowedExtension, extension)) {
                return true;
            }
        }
        return false;
    }

    private String calculateHash(byte[] fileBytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(fileBytes);
        return HexFormat.of().formatHex(hash);
    }

    public String resolveOriginalName(String clientFileName, String multipartFileName) {
        if (isMeaningfulFileName(clientFileName)) {
            return StrUtil.trim(clientFileName);
        }
        if (isMeaningfulFileName(multipartFileName)) {
            return StrUtil.trim(multipartFileName);
        }
        String extension = StrUtil.nullToDefault(FilenameUtils.getExtension(multipartFileName), "txt").toLowerCase();
        if (!isAllowedExtension(extension)) {
            extension = "txt";
        }
        return "document." + extension;
    }

    public boolean isMeaningfulFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return false;
        }
        String trimmed = StrUtil.trim(fileName);
        String lower = trimmed.toLowerCase();
        if (!trimmed.contains(".")) {
            return false;
        }
        if (lower.startsWith("tmp") || lower.contains("wxfile") || lower.contains("temp")) {
            return false;
        }
        return isAllowedExtension(FilenameUtils.getExtension(trimmed));
    }

    @Override
    public void ensureOriginalName(TranslateDocument document, String preferredFileName) {
        if (document == null) {
            return;
        }
        if (isMeaningfulFileName(document.getOriginalName())) {
            return;
        }
        String resolved = resolveOriginalName(preferredFileName, document.getOriginalName());
        document.setOriginalName(resolved);
        String extension = FilenameUtils.getExtension(resolved);
        if (StrUtil.isNotBlank(extension)) {
            document.setFileExt(extension.toLowerCase());
            document.setSourceType(extension.toLowerCase());
        }
        updateById(document);
    }
}
