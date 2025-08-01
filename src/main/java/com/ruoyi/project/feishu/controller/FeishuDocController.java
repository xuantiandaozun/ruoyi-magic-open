package com.ruoyi.project.feishu.controller;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.drive.v1.model.ListFileReq;
import com.lark.oapi.service.drive.v1.model.ListFileResp;
import com.lark.oapi.service.drive.v1.model.File;
import com.ruoyi.common.utils.FeishuConfigUtils;
import com.ruoyi.project.system.config.FeishuConfig;
import com.ruoyi.project.system.service.IFeishuOAuthService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.feishu.domain.FeishuDoc;
import com.ruoyi.project.feishu.service.IFeishuDocService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 飞书文档信息Controller
 * 
 * @author ruoyi
 * @date 2025-07-31 16:47:44
 */
@Slf4j
@Tag(name = "飞书文档信息")
@RestController
@RequestMapping("/feishu/feishudoc")
public class FeishuDocController extends BaseController
{
    @Autowired
    private IFeishuDocService feishuDocService;
    
    @Autowired
    private IFeishuOAuthService feishuOAuthService;


    

    /**
     * 查询飞书文档信息列表
     */
    @Operation(summary = "查询飞书文档信息列表")
    @SaCheckPermission("feishu:feishudoc:list")
    @GetMapping("/list")
    public TableDataInfo list(FeishuDoc feishuDoc)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(feishuDoc);
        
        // 使用 MyBatisFlex 的分页方法
        Page<FeishuDoc> page = feishuDocService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出飞书文档信息列表
     */
    @Operation(summary = "导出飞书文档信息列表")
    @SaCheckPermission("feishu:feishudoc:export")
    @Log(title = "飞书文档信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FeishuDoc feishuDoc)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<FeishuDoc> list = feishuDocService.list(queryWrapper);
        MagicExcelUtil<FeishuDoc> util = new MagicExcelUtil<>(FeishuDoc.class);
        util.exportExcel(response, list, "飞书文档信息数据");
    }

    /**
     * 获取飞书文档信息详细信息
     */
    @Operation(summary = "获取飞书文档信息详细信息")
    @SaCheckPermission("feishu:feishudoc:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(feishuDocService.getById(id));
    }

    /**
     * 新增飞书文档信息
     */
    @Operation(summary = "新增飞书文档信息")
    @SaCheckPermission("feishu:feishudoc:add")
    @Log(title = "飞书文档信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FeishuDoc feishuDoc)
    {
        return toAjax(feishuDocService.save(feishuDoc) ? 1 : 0);
    }

    /**
     * 修改飞书文档信息
     */
    @Operation(summary = "修改飞书文档信息")
    @SaCheckPermission("feishu:feishudoc:edit")
    @Log(title = "飞书文档信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FeishuDoc feishuDoc)
    {
        return toAjax(feishuDocService.updateById(feishuDoc) ? 1 : 0);
    }

    /**
     * 删除飞书文档信息
     */
    @Operation(summary = "删除飞书文档信息")
    @SaCheckPermission("feishu:feishudoc:remove")
    @Log(title = "飞书文档信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(feishuDocService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
    
    /**
     * 同步飞书文档
     */
    @Operation(summary = "同步飞书文档")
    @SaCheckPermission("feishu:feishudoc:sync")
    @Log(title = "同步飞书文档", businessType = BusinessType.OTHER)
    @PostMapping("/sync")
    public AjaxResult syncFeishuDocs(@RequestParam(value = "keyName", required = false) String keyName)
    {
        try {
            // 获取飞书配置
            FeishuConfig feishuConfig = getFeishuConfig(keyName);
            if (feishuConfig == null || !feishuConfig.isValid()) {
                return error("飞书配置无效，请检查密钥配置");
            }
            
            // 构建飞书客户端
            Client client = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            
            // 创建请求对象
            ListFileReq req = ListFileReq.newBuilder()
                .orderBy("EditedTime")
                .direction("DESC")
                .build();
            
            // 获取当前用户的飞书访问令牌
            String userAccessToken = feishuOAuthService.getCurrentUserFeishuToken();
            if (StrUtil.isBlank(userAccessToken)) {
                return error("当前用户未绑定飞书访问令牌，请先进行飞书授权");
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
                return error("同步失败: " + resp.getMsg());
            }
            
            // 处理业务数据
            List<FeishuDoc> docsToSave = new ArrayList<>();
            if (resp.getData() != null && resp.getData().getFiles() != null) {
                for (File file : resp.getData().getFiles()) {
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
                feishuDocService.saveOrUpdateBatch(docsToSave);
                log.info("成功同步 {} 个飞书文档", docsToSave.size());
                return success("同步成功，共同步 " + docsToSave.size() + " 个文档");
            } else {
                return success("同步完成，未发现新文档");
            }
            
        } catch (Exception e) {
            log.error("同步飞书文档异常", e);
            return error("同步异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取飞书配置
     */
    private FeishuConfig getFeishuConfig(String keyName) {
        return FeishuConfigUtils.getFeishuConfig(keyName);
    }
}
