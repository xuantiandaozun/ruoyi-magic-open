package com.ruoyi.project.feishu.service;

import java.io.File;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.feishu.domain.FeishuDoc;

/**
 * 飞书文档信息Service接口
 * 
 * @author ruoyi
 * @date 2025-07-31 16:47:44
 */
public interface IFeishuDocService extends IService<FeishuDoc>
{
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
    String syncFeishuDocuments(String keyName, String orderBy, String direction, Integer pageSize, String pageToken);
    
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
    FeishuDoc uploadFileToFeishu(File file, String fileName, String parentType, String parentNode, String keyName);
    
    /**
     * 删除飞书文档
     * 
     * @param fileToken 文件token
     * @param type 文件类型
     * @param keyName 密钥名称
     * @return 删除结果
     */
    boolean deleteFeishuFile(String fileToken, String type, String keyName);
    
    /**
     * 创建飞书文件夹
     * 
     * @param name 文件夹名称
     * @param folderToken 父文件夹token
     * @param keyName 密钥名称
     * @return 创建的文件夹信息
     */
    FeishuDoc createFeishuFolder(String name, String folderToken, String keyName);
    
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
    String createImportTask(String fileExtension, String fileToken, String type, String fileName, 
                          Integer mountType, String mountKey, String keyName);
    
    /**
     * 创建飞书文档
     * 
     * @param title 文档标题
     * @param folderToken 父文件夹token
     * @param keyName 密钥名称
     * @return 创建的文档信息
     */
    String createFeishuDocument(String title, String folderToken, String keyName);
    
    /**
     * 获取飞书文档信息
     * 
     * @param documentId 文档ID
     * @param keyName 密钥名称
     * @return 文档信息
     */
    String getFeishuDocument(String documentId, String keyName);
    
    /**
     * 转换文档内容（Markdown/HTML转换为文档块）
     * 
     * @param contentType 内容类型（markdown/html）
     * @param content 内容
     * @param keyName 密钥名称
     * @return 转换结果
     */
    String convertDocumentContent(String contentType, String content, String keyName);
    
    /**
     * 获取文档原始内容
     * 
     * @param documentId 文档ID
     * @param keyName 密钥名称
     * @return 文档原始内容
     */
    String getDocumentRawContent(String documentId, String keyName);
    
    /**
     * 列出文档块
     * 
     * @param documentId 文档ID
     * @param pageSize 页面大小
     * @param documentRevisionId 文档版本ID
     * @param keyName 密钥名称
     * @return 文档块列表
     */
    String listDocumentBlocks(String documentId, Integer pageSize, Integer documentRevisionId, String keyName);
    
    /**
     * 创建文档块子元素
     * 
     * @param documentId 文档ID
     * @param blockId 块ID
     * @param documentRevisionId 文档版本ID
     * @param children 子元素内容
     * @param keyName 密钥名称
     * @return 创建结果
     */
    String createDocumentBlockChildren(String documentId, String blockId, Integer documentRevisionId, String children ,String keyName);
    
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
    String createDocumentBlockDescendant(String documentId, String blockId, Integer documentRevisionId, String children, String keyName);
    

    /**
     * 批量查询文档元数据
     * 
     * @param requestDocs 请求文档列表（JSON字符串）
     * @param withUrl 是否包含URL
     * @param keyName 密钥名称
     * @return 查询结果
     */
    String batchQueryDocumentMeta(String requestDocs, Boolean withUrl, String keyName);
    
    /**
     * 批量更新文档块
     * 
     * @param documentId 文档ID
     * @param documentRevisionId 文档版本ID
     * @param requests 更新请求列表
     * @param keyName 密钥名称
     * @return 更新结果
     */
    String batchUpdateDocumentBlock(String documentId, Integer documentRevisionId, String requests, String keyName);
}
