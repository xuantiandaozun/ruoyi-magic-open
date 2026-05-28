package com.ruoyi.project.miniapp.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.common.utils.file.ByteArrayMultipartFile;
import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;
import com.ruoyi.project.miniapp.domain.TranslateDocument;
import com.ruoyi.project.miniapp.domain.TranslateTask;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;

@Service
public class MiniAppTranslateAsyncService {

    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final TranslateTaskServiceImpl translateTaskService;
    private final TranslateDocumentServiceImpl documentService;
    private final LangChain4jAgentService langChain4jAgentService;
    private final FileStorageService fileStorageService;
    private final DocxTranslateHelper docxTranslateHelper;
    private final MiniAppSubscribeMessageService subscribeMessageService;

    public MiniAppTranslateAsyncService(TranslateTaskServiceImpl translateTaskService,
            TranslateDocumentServiceImpl documentService,
            LangChain4jAgentService langChain4jAgentService,
            FileStorageService fileStorageService,
            DocxTranslateHelper docxTranslateHelper,
            MiniAppSubscribeMessageService subscribeMessageService) {
        this.translateTaskService = translateTaskService;
        this.documentService = documentService;
        this.langChain4jAgentService = langChain4jAgentService;
        this.fileStorageService = fileStorageService;
        this.docxTranslateHelper = docxTranslateHelper;
        this.subscribeMessageService = subscribeMessageService;
    }

    @Async("taskExecutor")
    public void executeTask(Long taskId) {
        TranslateTask task = translateTaskService.getById(taskId);
        if (task == null || !"0".equals(task.getDelFlag())) {
            return;
        }

        TranslateDocument document = documentService.getById(task.getDocumentId());
        if (document == null || !"0".equals(document.getDelFlag())) {
            failTask(task, null, "文档不存在，无法执行翻译");
            return;
        }

        try {
            startTask(task, document);
            if (task.getModelConfigId() == null) {
                throw new ServiceException("未配置可用的翻译模型路由");
            }

            if ("docx".equalsIgnoreCase(document.getFileExt())) {
                translateWord(task, document);
            } else {
                translateText(task, document);
            }
        } catch (Exception e) {
            failTask(task, document, e.getMessage());
        }
    }

    private void translateText(TranslateTask task, TranslateDocument document) throws Exception {
        byte[] sourceBytes = HttpUtil.downloadBytes(document.getSourceOssUrl());
        String sourceText = normalizeText(new String(sourceBytes, StandardCharsets.UTF_8));
        if (StrUtil.isBlank(sourceText)) {
            throw new ServiceException("源文档内容为空，无法翻译");
        }

        task.setStatus("translating");
        task.setProgress(40);
        translateTaskService.updateById(task);
        document.setStatus("translating");
        document.setParseStatus("parsed");
        documentService.updateById(document);

        String translatedText = langChain4jAgentService.chatWithSystem(task.getModelConfigId(),
                buildSystemPrompt(task), sourceText);
        translatedText = normalizeText(translatedText);

        task.setStatus("rebuilding");
        task.setProgress(80);
        translateTaskService.updateById(task);

        String resultFileName = buildResultFileName(document.getOriginalName(), task.getTargetLanguage(),
                document.getFileExt());
        String objectKey = buildResultObjectKey(document, resultFileName);
        ByteArrayMultipartFile resultFile = new ByteArrayMultipartFile("file", resultFileName, "text/plain",
                translatedText.getBytes(StandardCharsets.UTF_8));
        String resultUrl = fileStorageService.upload(resultFile, objectKey);

        completeTask(task, document, objectKey, resultUrl);
    }

    private void translateWord(TranslateTask task, TranslateDocument document) throws Exception {
        byte[] sourceBytes = HttpUtil.downloadBytes(document.getSourceOssUrl());

        task.setStatus("translating");
        task.setProgress(40);
        translateTaskService.updateById(task);
        document.setStatus("translating");
        document.setParseStatus("parsed");
        documentService.updateById(document);

        byte[] resultBytes = docxTranslateHelper.translateDocx(sourceBytes, task.getModelConfigId(),
                task.getSourceLanguage(), task.getTargetLanguage());

        task.setStatus("rebuilding");
        task.setProgress(80);
        translateTaskService.updateById(task);

        String resultFileName = buildResultFileName(document.getOriginalName(), task.getTargetLanguage(),
                document.getFileExt());
        String objectKey = buildResultObjectKey(document, resultFileName);
        ByteArrayMultipartFile resultFile = new ByteArrayMultipartFile("file", resultFileName, DOCX_CONTENT_TYPE,
                resultBytes);
        String resultUrl = fileStorageService.upload(resultFile, objectKey);

        completeTask(task, document, objectKey, resultUrl);
    }

    private void completeTask(TranslateTask task, TranslateDocument document, String objectKey, String resultUrl) {
        task.setResultOssKey(objectKey);
        task.setResultOssUrl(resultUrl);
        task.setPreviewOssUrl(resultUrl);
        task.setStatus("success");
        task.setProgress(100);
        task.setFinishedAt(new Date());
        task.setErrorCode(null);
        task.setErrorMessage(null);
        translateTaskService.updateById(task);

        document.setStatus("success");
        document.setParseStatus("done");
        documentService.updateById(document);

        subscribeMessageService.sendTranslateTaskCompleteNotice(task, document);
    }

    private void startTask(TranslateTask task, TranslateDocument document) {
        documentService.ensureOriginalName(document, null);
        task.setStatus("parsing");
        task.setProgress(10);
        task.setStartedAt(new Date());
        translateTaskService.updateById(task);
        document.setStatus("processing");
        document.setParseStatus("processing");
        documentService.updateById(document);
    }

    private void failTask(TranslateTask task, TranslateDocument document, String message) {
        task.setStatus("failed");
        task.setErrorCode("TRANSLATE_FAILED");
        task.setErrorMessage(StrUtil.maxLength(message, 1000));
        task.setFinishedAt(new Date());
        translateTaskService.updateById(task);
        if (document != null) {
            document.setStatus("failed");
            document.setParseStatus("failed");
            documentService.updateById(document);
        }
    }

    private String buildSystemPrompt(TranslateTask task) {
        String sourceLanguage = StrUtil.blankToDefault(task.getSourceLanguage(), "自动识别源语言");
        return "你是专业文档翻译引擎。请将用户提供的文档内容翻译为目标语言。" +
                "要求：1. 仅输出翻译后的纯文本内容，禁止使用代码块包裹（不要用 ``` 包裹输出）；" +
                "2. 尽量保留原有段落、换行、列表顺序；" +
                "3. 不要新增原文没有的内容，不要添加任何说明或注释；" +
                "4. 专有名词前后一致；" +
                "5. 本次源语言为" + sourceLanguage + "，目标语言为" + task.getTargetLanguage() + "。";
    }

    private String buildResultFileName(String originalName, String targetLanguage, String extension) {
        String baseName = FilenameUtils.getBaseName(originalName);
        return baseName + "_" + targetLanguage + "." + extension;
    }

    private String buildResultObjectKey(TranslateDocument document, String resultFileName) {
        LocalDate now = LocalDate.now();
        return StrUtil.format("miniapp/{}/{}/{}/{}/{}/{}",
                document.getMiniAppId(),
                "result",
                now.getYear(),
                String.format("%02d", now.getMonthValue()),
                String.format("%02d", now.getDayOfMonth()),
                resultFileName);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String result = text.replace("\r\n", "\n").replace("\r", "\n").replace("\uFEFF", "").trim();
        result = stripCodeFences(result);
        return result;
    }

    private String stripCodeFences(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            } else {
                trimmed = trimmed.substring(3);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            return trimmed.trim();
        }
        return trimmed;
    }
}
