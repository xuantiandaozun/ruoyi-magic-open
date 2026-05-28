package com.ruoyi.project.miniapp.service.impl;

import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.framework.redis.RedisLock;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.domain.AiModelRoute;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiModelRouteService;
import com.ruoyi.project.miniapp.domain.TranslateDocument;
import com.ruoyi.project.miniapp.domain.TranslateTask;
import com.ruoyi.project.miniapp.domain.dto.CreateTranslateTaskRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.mapper.TranslateTaskMapper;
import com.ruoyi.project.miniapp.service.ITranslateDocumentService;
import com.ruoyi.project.miniapp.service.ITranslateTaskService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import jakarta.servlet.http.HttpServletResponse;

@Service
@UseDataSource("MASTER")
public class TranslateTaskServiceImpl extends ServiceImpl<TranslateTaskMapper, TranslateTask>
        implements ITranslateTaskService {

    private static final String PRODUCT_TYPE = "miniapp";
    private static final String SCENE_CODE = "doc_translate";
    private final ITranslateDocumentService documentService;
    private final IAiModelRouteService modelRouteService;
    private final IAiModelConfigService modelConfigService;
    private final RedisLock redisLock;
    private final FileStorageService fileStorageService;
    private final MiniAppTranslateAsyncService translateAsyncService;

    public TranslateTaskServiceImpl(ITranslateDocumentService documentService,
            IAiModelRouteService modelRouteService,
            IAiModelConfigService modelConfigService,
            RedisLock redisLock,
            FileStorageService fileStorageService,
            @Lazy
            MiniAppTranslateAsyncService translateAsyncService) {
        this.documentService = documentService;
        this.modelRouteService = modelRouteService;
        this.modelConfigService = modelConfigService;
        this.redisLock = redisLock;
        this.fileStorageService = fileStorageService;
        this.translateAsyncService = translateAsyncService;
    }

    @Override
    @Transactional
    public TranslateTask createTask(CreateTranslateTaskRequest request, MiniAppLoginUser loginUser) {
        TranslateDocument document = documentService.getOwnedDocument(request.getDocumentId(), loginUser);
        documentService.ensureOriginalName(document, request.getFileName());
        validateCreateRequest(request, document);

        String lockKey = StrUtil.format("miniapp:translate:{}:{}:{}", loginUser.getMiniAppId(), loginUser.getMiniUserId(),
                document.getId());
        String lockValue = UUID.randomUUID().toString();
        if (!redisLock.tryLock(lockKey, lockValue, 30)) {
            throw new ServiceException("该文档已有任务正在创建或处理中，请稍后再试");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                redisLock.unlock(lockKey, lockValue);
            }
        });

        TranslateTask latestTask = getLatestPendingTask(document.getId(), loginUser);
        if (latestTask != null) {
            throw new ServiceException("该文档已有进行中的翻译任务，请稍后再试");
        }

        TranslateTask task = new TranslateTask();
        task.setMiniUserId(loginUser.getMiniUserId());
        task.setMiniAppId(loginUser.getMiniAppId());
        task.setDocumentId(document.getId());
        task.setProductType(PRODUCT_TYPE);
        task.setSceneCode(SCENE_CODE);
        task.setSourceLanguage(request.getSourceLanguage());
        task.setTargetLanguage(request.getTargetLanguage());
        task.setOutputFormat(resolveOutputFormat(request.getOutputFormat(), document.getFileExt()));
        task.setStatus("pending");
        task.setProgress(0);
        task.setRetryCount(0);
        task.setDelFlag("0");

        AiModelConfig modelConfig = resolveModelConfig();
        if (modelConfig != null) {
            task.setModelConfigId(modelConfig.getId());
            task.setModelName(modelConfig.getModel());
        }

        save(task);

        document.setStatus("queued");
        documentService.updateById(document);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                translateAsyncService.executeTask(task.getId());
            }
        });
        enrichTaskWithDocumentName(task, document);
        return task;
    }

    @Override
    public TranslateTask getOwnedTask(Long taskId, MiniAppLoginUser loginUser) {
        QueryWrapper qw = QueryWrapper.create()
                .from("translate_task")
                .where("id = ?", taskId)
                .and("mini_user_id = ?", loginUser.getMiniUserId())
                .and("mini_app_id = ?", loginUser.getMiniAppId())
                .and("del_flag = '0'")
                .limit(1);
        TranslateTask task = getOne(qw);
        if (task == null) {
            throw new ServiceException("任务不存在或无权访问");
        }
        enrichTaskWithDocumentName(task);
        return task;
    }

    @Override
    public List<TranslateTask> listByOwner(MiniAppLoginUser loginUser) {
        QueryWrapper qw = QueryWrapper.create()
                .from("translate_task")
                .where("mini_user_id = ?", loginUser.getMiniUserId())
                .and("mini_app_id = ?", loginUser.getMiniAppId())
                .and("del_flag = '0'")
                .orderBy("id desc");
        List<TranslateTask> tasks = list(qw);
        enrichTasksWithDocumentNames(tasks);
        return tasks;
    }

    @Override
    public void downloadResult(Long taskId, MiniAppLoginUser loginUser, HttpServletResponse response) throws Exception {
        TranslateTask task = getOwnedTask(taskId, loginUser);
        if (!"success".equals(task.getStatus()) || StrUtil.isBlank(task.getResultOssUrl())) {
            throw new ServiceException("翻译结果尚未生成，请稍后再试");
        }
        TranslateDocument document = documentService.getOwnedDocument(task.getDocumentId(), loginUser);
        String downloadUrl = fileStorageService.getFileUrl(task.getResultOssKey());
        response.setContentType("application/octet-stream");
        FileUtils.setAttachmentResponseHeader(response, buildDownloadFileName(document, task));
        try (HttpResponse httpResponse = HttpRequest.get(downloadUrl).execute();
                InputStream inputStream = httpResponse.bodyStream()) {
            if (httpResponse.getStatus() < 200 || httpResponse.getStatus() >= 300) {
                throw new ServiceException("翻译结果文件暂时不可下载，请稍后重试");
            }
            inputStream.transferTo(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    public int retryRecoverableTasksOnStartup() {
        QueryWrapper qw = QueryWrapper.create()
                .from("translate_task")
                .where("status in ('pending','parsing','translating','rebuilding','verifying')")
                .and("del_flag = '0'")
                .orderBy("id asc");
        List<TranslateTask> tasks = list(qw);
        if (tasks.isEmpty()) {
            return 0;
        }

        for (TranslateTask task : tasks) {
            TranslateDocument document = documentService.getById(task.getDocumentId());
            if (document == null || !"0".equals(document.getDelFlag())) {
                task.setStatus("failed");
                task.setErrorCode("DOCUMENT_MISSING");
                task.setErrorMessage("重启恢复失败：源文档不存在");
                task.setFinishedAt(new Date());
                updateById(task);
                continue;
            }

            task.setStatus("pending");
            task.setProgress(0);
            task.setFinishedAt(null);
            task.setErrorCode(null);
            task.setErrorMessage(null);
            task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
            updateById(task);

            document.setStatus("queued");
            document.setParseStatus("pending");
            documentService.updateById(document);

            translateAsyncService.executeTask(task.getId());
        }
        return tasks.size();
    }

    private TranslateTask getLatestPendingTask(Long documentId, MiniAppLoginUser loginUser) {
        QueryWrapper qw = QueryWrapper.create()
                .from("translate_task")
                .where("document_id = ?", documentId)
                .and("mini_user_id = ?", loginUser.getMiniUserId())
                .and("mini_app_id = ?", loginUser.getMiniAppId())
                .and("status in ('pending','parsing','translating','rebuilding','verifying')")
                .and("del_flag = '0'")
                .orderBy("id desc")
                .limit(1);
        return getOne(qw);
    }

    private String resolveOutputFormat(String outputFormat, String documentFileExt) {
        if (StrUtil.isNotBlank(outputFormat)) {
            return outputFormat;
        }
        return documentFileExt;
    }

    private void validateCreateRequest(CreateTranslateTaskRequest request, TranslateDocument document) {
        String fileExt = document.getFileExt();
        if (!"txt".equalsIgnoreCase(fileExt) && !"docx".equalsIgnoreCase(fileExt)) {
            throw new ServiceException("当前仅开放 txt 和 docx 文档翻译，md 即将支持");
        }
        String requestedFormat = resolveOutputFormat(request.getOutputFormat(), fileExt);
        if (!fileExt.equalsIgnoreCase(requestedFormat)) {
            throw new ServiceException(fileExt + " 文档当前仅支持输出为 " + fileExt + " 格式");
        }
    }

    private String buildDownloadFileName(TranslateDocument document, TranslateTask task) throws UnsupportedEncodingException {
        String extension = StrUtil.blankToDefault(task.getOutputFormat(), document.getFileExt());
        String baseName = FilenameUtils.getBaseName(document.getOriginalName());
        return baseName + "_" + task.getTargetLanguage() + "." + extension;
    }

    private void enrichTasksWithDocumentNames(List<TranslateTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Set<Long> documentIds = tasks.stream()
                .map(TranslateTask::getDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (documentIds.isEmpty()) {
            return;
        }
        Map<Long, TranslateDocument> documentMap = documentService.listByIds(documentIds).stream()
                .filter(document -> document != null && "0".equals(document.getDelFlag()))
                .collect(Collectors.toMap(TranslateDocument::getId, Function.identity(), (left, right) -> left));
        for (TranslateTask task : tasks) {
            enrichTaskWithDocumentName(task, documentMap.get(task.getDocumentId()));
        }
    }

    private void enrichTaskWithDocumentName(TranslateTask task) {
        if (task == null || task.getDocumentId() == null) {
            return;
        }
        TranslateDocument document = documentService.getById(task.getDocumentId());
        enrichTaskWithDocumentName(task, document);
    }

    private void enrichTaskWithDocumentName(TranslateTask task, TranslateDocument document) {
        if (task == null || document == null) {
            return;
        }
        String displayName = StrUtil.blankToDefault(document.getOriginalName(), "未命名文件");
        task.setFileName(displayName);
        task.setDocumentName(displayName);
    }

    private AiModelConfig resolveModelConfig() {
        QueryWrapper qw = QueryWrapper.create()
                .from("ai_model_route")
                .where("product_type = ?", PRODUCT_TYPE)
                .and("scene_code = ?", SCENE_CODE)
                .and("enabled = 'Y'")
                .and("del_flag = '0'")
                .limit(1);
        AiModelRoute route = modelRouteService.getOne(qw);
        if (route != null && route.getPrimaryModelConfigId() != null) {
            AiModelConfig modelConfig = modelConfigService.getById(route.getPrimaryModelConfigId());
            if (modelConfig != null && "Y".equals(modelConfig.getEnabled()) && "0".equals(modelConfig.getStatus())) {
                return modelConfig;
            }
        }

        AiModelConfig fallback = modelConfigService.getEnabledByModel("deepseek-v4-flash");
        if (fallback != null) {
            return fallback;
        }

        List<AiModelConfig> configs = modelConfigService.listEnabledByProviderAndCapability("deepseek", "chat");
        return configs.isEmpty() ? null : configs.get(0);
    }
}
