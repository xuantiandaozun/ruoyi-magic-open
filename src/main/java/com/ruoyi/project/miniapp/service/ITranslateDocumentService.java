package com.ruoyi.project.miniapp.service;

import org.springframework.web.multipart.MultipartFile;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.miniapp.domain.TranslateDocument;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;

public interface ITranslateDocumentService extends IService<TranslateDocument> {
    TranslateDocument upload(MultipartFile file, MiniAppLoginUser loginUser) throws Exception;

    TranslateDocument getOwnedDocument(Long documentId, MiniAppLoginUser loginUser);
}
