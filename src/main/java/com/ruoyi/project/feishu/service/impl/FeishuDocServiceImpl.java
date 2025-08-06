package com.ruoyi.project.feishu.service.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.docx.v1.model.BatchUpdateDocumentBlockReq;
import com.lark.oapi.service.docx.v1.model.BatchUpdateDocumentBlockReqBody;
import com.lark.oapi.service.docx.v1.model.BatchUpdateDocumentBlockResp;
import com.lark.oapi.service.docx.v1.model.Block;
import com.lark.oapi.service.docx.v1.model.ConvertDocumentReq;
import com.lark.oapi.service.docx.v1.model.ConvertDocumentReqBody;
import com.lark.oapi.service.docx.v1.model.ConvertDocumentResp;
import com.lark.oapi.service.docx.v1.model.CreateDocumentBlockChildrenReq;
import com.lark.oapi.service.docx.v1.model.CreateDocumentBlockChildrenReqBody;
import com.lark.oapi.service.docx.v1.model.CreateDocumentBlockChildrenResp;
import com.lark.oapi.service.docx.v1.model.CreateDocumentBlockDescendantReq;
import com.lark.oapi.service.docx.v1.model.CreateDocumentBlockDescendantReqBody;
import com.lark.oapi.service.docx.v1.model.CreateDocumentBlockDescendantResp;
import com.lark.oapi.service.docx.v1.model.CreateDocumentReq;
import com.lark.oapi.service.docx.v1.model.CreateDocumentReqBody;
import com.lark.oapi.service.docx.v1.model.CreateDocumentResp;
import com.lark.oapi.service.docx.v1.model.GetDocumentReq;
import com.lark.oapi.service.docx.v1.model.GetDocumentResp;
import com.lark.oapi.service.docx.v1.model.ListDocumentBlockReq;
import com.lark.oapi.service.docx.v1.model.ListDocumentBlockResp;
import com.lark.oapi.service.docx.v1.model.RawContentDocumentReq;
import com.lark.oapi.service.docx.v1.model.RawContentDocumentResp;
import com.lark.oapi.service.docx.v1.model.UpdateBlockRequest;
import com.lark.oapi.service.drive.v1.model.CreateFolderFileReq;
import com.lark.oapi.service.drive.v1.model.CreateFolderFileReqBody;
import com.lark.oapi.service.drive.v1.model.CreateFolderFileResp;
import com.lark.oapi.service.drive.v1.model.CreateImportTaskReq;
import com.lark.oapi.service.drive.v1.model.CreateImportTaskResp;
import com.lark.oapi.service.drive.v1.model.DeleteFileReq;
import com.lark.oapi.service.drive.v1.model.DeleteFileResp;
import com.lark.oapi.service.drive.v1.model.ImportTask;
import com.lark.oapi.service.drive.v1.model.ImportTaskMountPoint;
import com.lark.oapi.service.drive.v1.model.ListFileReq;
import com.lark.oapi.service.drive.v1.model.ListFileResp;
import com.lark.oapi.service.drive.v1.model.UploadAllFileReq;
import com.lark.oapi.service.drive.v1.model.UploadAllFileReqBody;
import com.lark.oapi.service.drive.v1.model.UploadAllFileResp;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.utils.FeishuConfigUtils;
import com.ruoyi.project.feishu.domain.FeishuDoc;
import com.ruoyi.project.feishu.mapper.FeishuDocMapper;
import com.ruoyi.project.feishu.service.IFeishuDocService;
import com.ruoyi.project.system.config.FeishuConfig;
import com.ruoyi.project.system.service.IFeishuOAuthService;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书文档信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-07-31 16:47:44
 */
@Slf4j
@Service
public class FeishuDocServiceImpl extends ServiceImpl<FeishuDocMapper, FeishuDoc> implements IFeishuDocService
{
    @Autowired
    private IFeishuOAuthService feishuOAuthService;
    
    /**
     * 同步飞书文档列表
     * 
     * @param keyName 密钥名称
     * @param orderBy 排序字段
     * @param direction 排序方向
     * @param pageSize 页面大小
     * @param pageToken 分页标记
     * @return 同步结果信息
     */
    public String syncFeishuDocuments(String keyName, String orderBy, String direction, Integer pageSize, String pageToken) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            ListFileReq.Builder reqBuilder = ListFileReq.newBuilder();
            if (StrUtil.isNotBlank(orderBy)) {
                reqBuilder.orderBy(orderBy);
            } else {
                reqBuilder.orderBy("EditedTime");
            }
            if (StrUtil.isNotBlank(direction)) {
                reqBuilder.direction(direction);
            } else {
                reqBuilder.direction("DESC");
            }
            if (pageSize != null && pageSize > 0) {
                reqBuilder.pageSize(pageSize);
            }
            if (StrUtil.isNotBlank(pageToken)) {
                reqBuilder.pageToken(pageToken);
            }
            
            ListFileReq req = reqBuilder.build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 构建请求选项
            RequestOptions.Builder optionsBuilder = RequestOptions.newBuilder();
            optionsBuilder.userAccessToken(userAccessToken);
            
            // 发起请求
            ListFileResp resp = client.drive().v1().file().list(req, optionsBuilder.build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("同步飞书文档失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("同步失败: " + resp.getMsg());
            }
            
            // 处理业务数据
            List<FeishuDoc> docsToSave = new ArrayList<>();
            if (resp.getData() != null && resp.getData().getFiles() != null) {
                for (com.lark.oapi.service.drive.v1.model.File file : resp.getData().getFiles()) {
                    FeishuDoc feishuDoc = new FeishuDoc();
                    feishuDoc.setToken(file.getToken());
                    feishuDoc.setName(file.getName());
                    feishuDoc.setType(file.getType());
                    feishuDoc.setUrl(file.getUrl());
                    feishuDoc.setOwnerId(file.getOwnerId());
                    feishuDoc.setParentToken(file.getParentToken());
                    feishuDoc.setIsFolder("folder".equals(file.getType()) ? 1 : 0);
                    feishuDoc.setFeishuCreatedTime(file.getCreatedTime());
                    feishuDoc.setFeishuModifiedTime(file.getModifiedTime());
                    feishuDoc.setKeyName(StrUtil.isNotBlank(keyName) ? keyName : "feishu");
                    
                    docsToSave.add(feishuDoc);
                }
            }
            
            // 批量保存或更新文档信息
            if (!docsToSave.isEmpty()) {
                int newCount = 0;
                int updateCount = 0;
                
                for (FeishuDoc feishuDoc : docsToSave) {
                    // 根据token和keyName查询是否已存在
                    QueryWrapper queryWrapper = QueryWrapper.create()
                        .eq("token", feishuDoc.getToken())
                        .eq("key_name", feishuDoc.getKeyName());
                    
                    FeishuDoc existingDoc = this.getOne(queryWrapper);
                    
                    if (existingDoc != null) {
                        // 存在则更新
                        feishuDoc.setId(existingDoc.getId());
                        this.updateById(feishuDoc);
                        updateCount++;
                    } else {
                        // 不存在则新增
                        this.save(feishuDoc);
                        newCount++;
                    }
                }
                
                log.info("成功同步飞书文档，新增: {} 个，更新: {} 个", newCount, updateCount);
                return String.format("同步成功，新增 %d 个文档，更新 %d 个文档", newCount, updateCount);
            } else {
                return "同步完成，未发现新文档";
            }
            
        } catch (Exception e) {
            log.error("同步飞书文档异常", e);
            throw new RuntimeException("同步异常: " + e.getMessage());
        }
    }
    
    /**
     * 上传文件到飞书云盘
     * 
     * @param file 要上传的文件
     * @param fileName 文件名称
     * @param parentType 父节点类型
     * @param parentNode 父节点token
     * @param keyName 密钥名称
     * @return 上传结果
     */
    public FeishuDoc uploadFileToFeishu(File file, String fileName, String parentType, String parentNode, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            UploadAllFileReq req = UploadAllFileReq.newBuilder()
                .uploadAllFileReqBody(UploadAllFileReqBody.newBuilder()
                    .fileName(StrUtil.isNotBlank(fileName) ? fileName : file.getName())
                    .parentType(StrUtil.isNotBlank(parentType) ? parentType : "explorer")
                    .parentNode(StrUtil.isNotBlank(parentNode) ? parentNode : "")
                    .size((int) file.length())
                    .file(file)
                    .build())
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            UploadAllFileResp resp = client.drive().v1().file().uploadAll(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("上传文件到飞书失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("上传失败: " + resp.getMsg());
            }
            
            // 处理业务数据
            if (resp.getData() != null) {
                FeishuDoc feishuDoc = new FeishuDoc();
                feishuDoc.setToken(resp.getData().getFileToken());
                feishuDoc.setName(fileName);
                feishuDoc.setType("file");
                // 注意：上传文件的响应中不包含URL，需要通过其他API获取
                feishuDoc.setUrl(null);
                feishuDoc.setParentToken(parentNode);
                feishuDoc.setIsFolder(0);
                feishuDoc.setKeyName(StrUtil.isNotBlank(keyName) ? keyName : "feishu");
                
                // 保存到数据库
                this.save(feishuDoc);
                
                log.info("文件上传成功，文件token: {}, 文件名: {}", feishuDoc.getToken(), feishuDoc.getName());
                return feishuDoc;
            }
            
            throw new RuntimeException("上传响应数据为空");
            
        } catch (Exception e) {
            log.error("上传文件到飞书异常", e);
            throw new RuntimeException("上传异常: " + e.getMessage());
        }
    }
    
    /**
     * 删除飞书文档
     * 
     * @param fileToken 文件token
     * @param type 文件类型
     * @param keyName 密钥名称
     * @return 删除结果
     */
    public boolean deleteFeishuFile(String fileToken, String type, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            DeleteFileReq req = DeleteFileReq.newBuilder()
                .fileToken(fileToken)
                .type(StrUtil.isNotBlank(type) ? type : "file")
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            DeleteFileResp resp = client.drive().v1().file().delete(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("删除飞书文件失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("删除失败: " + resp.getMsg());
            }
            
            // 从数据库中删除记录
            QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("token", fileToken)
                .eq("key_name", StrUtil.isNotBlank(keyName) ? keyName : "feishu");
            
            this.remove(queryWrapper);
            
            log.info("文件删除成功，文件token: {}", fileToken);
            return true;
            
        } catch (Exception e) {
            log.error("删除飞书文件异常", e);
            throw new RuntimeException("删除异常: " + e.getMessage());
        }
    }
    
    /**
     * 创建飞书文件夹
     * 
     * @param name 文件夹名称
     * @param folderToken 父文件夹token
     * @param keyName 密钥名称
     * @return 创建的文件夹信息
     */
    public FeishuDoc createFeishuFolder(String name, String folderToken, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            CreateFolderFileReq req = CreateFolderFileReq.newBuilder()
                .createFolderFileReqBody(CreateFolderFileReqBody.newBuilder()
                    .name(name)
                    .folderToken(folderToken)
                    .build())
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            CreateFolderFileResp resp = client.drive().v1().file().createFolder(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("创建飞书文件夹失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("创建文件夹失败: " + resp.getMsg());
            }
            
            // 处理业务数据
            if (resp.getData() != null) {
                FeishuDoc feishuDoc = new FeishuDoc();
                feishuDoc.setToken(resp.getData().getToken());
                feishuDoc.setName(name);
                feishuDoc.setType("folder");
                feishuDoc.setUrl(resp.getData().getUrl());
                feishuDoc.setParentToken(folderToken);
                feishuDoc.setIsFolder(1);
                feishuDoc.setKeyName(StrUtil.isNotBlank(keyName) ? keyName : "feishu");
                
                // 保存到数据库
                this.save(feishuDoc);
                
                log.info("文件夹创建成功，文件夹token: {}, 文件夹名: {}", feishuDoc.getToken(), feishuDoc.getName());
                return feishuDoc;
            }
            
            throw new RuntimeException("创建文件夹响应数据为空");
            
        } catch (Exception e) {
            log.error("创建飞书文件夹异常", e);
            throw new RuntimeException("创建文件夹异常: " + e.getMessage());
        }
    }
    
    /**
     * 创建导入任务
     * 
     * @param fileExtension 文件扩展名
     * @param fileToken 文件token
     * @param type 导入类型
     * @param fileName 文件名
     * @param mountType 挂载类型
     * @param mountKey 挂载键
     * @param keyName 密钥名称
     * @return 导入任务结果
     */
    public String createImportTask(String fileExtension, String fileToken, String type, String fileName, 
                                 Integer mountType, String mountKey, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            CreateImportTaskReq req = CreateImportTaskReq.newBuilder()
                .importTask(ImportTask.newBuilder()
                    .fileExtension(fileExtension)
                    .fileToken(fileToken)
                    .type(type)
                    .fileName(fileName)
                    .point(ImportTaskMountPoint.newBuilder()
                        .mountType(mountType != null ? mountType : 1)
                        .mountKey(mountKey)
                        .build())
                    .build())
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            CreateImportTaskResp resp = client.drive().v1().importTask().create(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("创建导入任务失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("创建导入任务失败: " + resp.getMsg());
            }
            
            // 处理业务数据
            if (resp.getData() != null) {
                log.info("导入任务创建成功，任务ID: {}", resp.getData().getTicket());
                return Jsons.DEFAULT.toJson(resp.getData());
            }
            
            throw new RuntimeException("创建导入任务响应数据为空");
            
        } catch (Exception e) {
            log.error("创建导入任务异常", e);
            throw new RuntimeException("创建导入任务异常: " + e.getMessage());
        }
    }
    
    /**
     * 创建飞书文档
     * 
     * @param title 文档标题
     * @param folderToken 文件夹token
     * @param keyName 密钥名称
     * @return 创建的文档信息
     */
    public String createFeishuDocument(String title, String folderToken, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            CreateDocumentReq req = CreateDocumentReq.newBuilder()
                .createDocumentReqBody(CreateDocumentReqBody.newBuilder()
                    .folderToken(folderToken)
                    .title(title)
                    .build())
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            CreateDocumentResp resp = client.docx().v1().document().create(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("创建飞书文档失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("创建文档失败: " + resp.getMsg());
            }
            
            // 处理业务数据
            if (resp.getData() != null && resp.getData().getDocument() != null) {
                FeishuDoc feishuDoc = new FeishuDoc();
                feishuDoc.setToken(resp.getData().getDocument().getDocumentId());
                feishuDoc.setName(title);
                feishuDoc.setType("docx");
                feishuDoc.setUrl(null);
                feishuDoc.setParentToken(folderToken);
                feishuDoc.setIsFolder(0);
                feishuDoc.setKeyName(StrUtil.isNotBlank(keyName) ? keyName : "feishu");
                
                // 保存到数据库
                this.save(feishuDoc);
                
                log.info("文档创建成功，文档ID: {}, 文档标题: {}", feishuDoc.getToken(), feishuDoc.getName());
                return Jsons.DEFAULT.toJson(resp.getData());
            }
            
            throw new RuntimeException("创建文档响应数据为空");
            
        } catch (Exception e) {
            log.error("创建飞书文档异常", e);
            throw new RuntimeException("创建文档异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取飞书文档信息
     * 
     * @param documentId 文档ID
     * @param keyName 密钥名称
     * @return 文档信息
     */
    public String getFeishuDocument(String documentId, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            GetDocumentReq req = GetDocumentReq.newBuilder()
                .documentId(documentId)
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            GetDocumentResp resp = client.docx().v1().document().get(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("获取飞书文档失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("获取文档失败: " + resp.getMsg());
            }
            
            // 返回文档数据
            return Jsons.DEFAULT.toJson(resp.getData());
            
        } catch (Exception e) {
            log.error("获取飞书文档异常", e);
            throw new RuntimeException("获取文档异常: " + e.getMessage());
        }
    }
    
    /**
     * 转换文档内容（Markdown/HTML转换为文档块）
     * 
     * @param contentType 内容类型（markdown/html）
     * @param content 内容
     * @param keyName 密钥名称
     * @return 转换结果
     */
    public String convertDocumentContent(String contentType, String content, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            ConvertDocumentReq req = ConvertDocumentReq.newBuilder()
                .userIdType("user_id")
                .convertDocumentReqBody(ConvertDocumentReqBody.newBuilder()
                    .contentType(StrUtil.isNotBlank(contentType) ? contentType : "markdown")
                    .content(content)
                    .build())
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            ConvertDocumentResp resp = client.docx().v1().document().convert(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("转换文档内容失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("转换内容失败: " + resp.getMsg());
            }
            
            // 返回转换结果
            return Jsons.DEFAULT.toJson(resp.getData());
            
        } catch (Exception e) {
            log.error("转换文档内容异常", e);
            throw new RuntimeException("转换内容异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取文档原始内容
     * 
     * @param documentId 文档ID
     * @param lang 语言（0-中文，1-英文）
     * @param keyName 密钥名称
     * @return 原始内容
     */
    public String getDocumentRawContent(String documentId, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            RawContentDocumentReq req = RawContentDocumentReq.newBuilder()
                .documentId(documentId)
                .lang(0)
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            RawContentDocumentResp resp = client.docx().v1().document().rawContent(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("获取文档原始内容失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("获取原始内容失败: " + resp.getMsg());
            }
            
            // 返回原始内容
            return Jsons.DEFAULT.toJson(resp.getData());
            
        } catch (Exception e) {
            log.error("获取文档原始内容异常", e);
            throw new RuntimeException("获取原始内容异常: " + e.getMessage());
        }
    }
    
    /**
     * 列出文档块
     * 
     * @param documentId 文档ID
     * @param pageSize 页面大小
     * @param documentRevisionId 文档版本ID
     * @param keyName 密钥名称
     * @return 文档块列表
     */
    public String listDocumentBlocks(String documentId, Integer pageSize, Integer documentRevisionId, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            ListDocumentBlockReq req = ListDocumentBlockReq.newBuilder()
                .documentId(documentId)
                .pageSize(pageSize != null ? pageSize : 500)
                .documentRevisionId(documentRevisionId != null ? documentRevisionId : -1)
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            ListDocumentBlockResp resp = client.docx().v1().documentBlock().list(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("列出文档块失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("列出文档块失败: " + resp.getMsg());
            }
            
            // 返回文档块数据
            return Jsons.DEFAULT.toJson(resp.getData());
            
        } catch (Exception e) {
            log.error("列出文档块异常", e);
            throw new RuntimeException("列出文档块异常: " + e.getMessage());
        }
    }
    
    /**
     * 创建文档块子元素
     * 
     * @param documentId 文档ID
     * @param blockId 块ID
     * @param documentRevisionId 文档版本ID
     * @param blocks 要创建的块数组
     * @param keyName 密钥名称
     * @return 创建结果
     */
    public String createDocumentBlockChildren(String documentId, String blockId, Integer documentRevisionId, 
                                            String children, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 解析children字符串为Block数组
            Block[] blocks = null;
            if (StrUtil.isNotBlank(children)) {
                blocks = Jsons.DEFAULT.fromJson(children, Block[].class);
            }
            
            // 创建请求对象
            CreateDocumentBlockChildrenReq req = CreateDocumentBlockChildrenReq.newBuilder()
                .documentId(documentId)
                .blockId(blockId)
                .documentRevisionId(documentRevisionId != null ? documentRevisionId : -1)
                .createDocumentBlockChildrenReqBody(CreateDocumentBlockChildrenReqBody.newBuilder()
                    .children(blocks)
                    .build())
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            CreateDocumentBlockChildrenResp resp = client.docx().v1().documentBlockChildren().create(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("创建文档块子元素失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("创建文档块子元素失败: " + resp.getMsg());
            }
            
            // 返回创建结果
            return Jsons.DEFAULT.toJson(resp.getData());
            
        } catch (Exception e) {
            log.error("创建文档块子元素异常", e);
            throw new RuntimeException("创建文档块子元素异常: " + e.getMessage());
        }
    }
    
    /**
     * 创建文档块后代元素
     * 
     * @param documentId 文档ID
     * @param blockId 块ID
     * @param documentRevisionId 文档版本ID
     * @param children 子元素内容
     * @param keyName 密钥名称
     * @return 创建结果
     */
    public String createDocumentBlockDescendant(String documentId, String blockId, Integer documentRevisionId,
                                               String children, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 解析children字符串为Block数组
            Block[] descendants = Jsons.DEFAULT.fromJson(children, Block[].class);
            
            // 创建请求对象
            CreateDocumentBlockDescendantReq req = CreateDocumentBlockDescendantReq.newBuilder()
                .documentId(documentId)
                .blockId(blockId)
                .documentRevisionId(documentRevisionId != null ? documentRevisionId : -1)
                .createDocumentBlockDescendantReqBody(CreateDocumentBlockDescendantReqBody.newBuilder()
                    .descendants(descendants)
                    .build())
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            CreateDocumentBlockDescendantResp resp = client.docx().v1().documentBlockDescendant().create(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("创建文档块后代元素失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("创建文档块后代元素失败: " + resp.getMsg());
            }
            
            // 返回创建结果
            return Jsons.DEFAULT.toJson(resp.getData());
            
        } catch (Exception e) {
            log.error("创建文档块后代元素异常", e);
            throw new RuntimeException("创建文档块后代元素异常: " + e.getMessage());
        }
    }
    
    /**
     * 批量更新文档块
     * 
     * @param documentId 文档ID
     * @param documentRevisionId 文档版本ID
     * @param requests 更新请求内容
     * @param keyName 密钥名称
     * @return 更新结果
     */
    public String batchUpdateDocumentBlock(String documentId, Integer documentRevisionId, String requests, String keyName) {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                throw new RuntimeException("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 解析requests字符串为UpdateBlockRequest数组
            UpdateBlockRequest[] requestArray = Jsons.DEFAULT.fromJson(requests, UpdateBlockRequest[].class);
            
            // 创建请求对象
            BatchUpdateDocumentBlockReq req = BatchUpdateDocumentBlockReq.newBuilder()
                .documentId(documentId)
                .documentRevisionId(documentRevisionId != null ? documentRevisionId : -1)
                .userIdType("user_id")
                .batchUpdateDocumentBlockReqBody(BatchUpdateDocumentBlockReqBody.newBuilder()
                    .requests(requestArray)
                    .build())
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                throw new RuntimeException("当前用户未绑定飞书访问令牌，请先进行飞书授权");
            }
            
            // 发起请求
            BatchUpdateDocumentBlockResp resp = client.docx().v1().documentBlock().batchUpdate(req, RequestOptions.newBuilder()
                .userAccessToken(userAccessToken)
                .build());
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("批量更新文档块失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("批量更新文档块失败: " + resp.getMsg());
            }
            
            // 返回更新结果
            return Jsons.DEFAULT.toJson(resp.getData());
            
        } catch (Exception e) {
            log.error("批量更新文档块异常", e);
            throw new RuntimeException("批量更新文档块异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取飞书配置
     * 
     * @param keyName 密钥名称
     * @return 飞书配置
     */
    private FeishuConfig getFeishuConfig(String keyName) {
        return FeishuConfigUtils.getFeishuConfig(keyName);
    }
}
