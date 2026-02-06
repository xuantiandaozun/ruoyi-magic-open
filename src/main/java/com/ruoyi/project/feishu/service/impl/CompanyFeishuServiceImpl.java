package com.ruoyi.project.feishu.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.bitable.v1.model.AppTableRecord;
import com.lark.oapi.service.bitable.v1.model.Condition;
import com.lark.oapi.service.bitable.v1.model.CreateAppTableRecordReq;
import com.lark.oapi.service.bitable.v1.model.CreateAppTableRecordResp;
import com.lark.oapi.service.bitable.v1.model.FilterInfo;
import com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordReq;
import com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordReqBody;
import com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordResp;
import com.lark.oapi.service.bitable.v1.model.UpdateAppTableRecordReq;
import com.lark.oapi.service.bitable.v1.model.UpdateAppTableRecordResp;
import com.lark.oapi.service.contact.v3.model.FindByDepartmentUserReq;
import com.lark.oapi.service.contact.v3.model.FindByDepartmentUserResp;
import com.ruoyi.common.utils.FeishuConfigUtils;
import com.ruoyi.project.feishu.domain.FeishuUsers;
import com.ruoyi.project.feishu.domain.dto.DomainCertRecordDto;
import com.ruoyi.project.feishu.domain.dto.FeishuBitablePageResponseDto;
import com.ruoyi.project.feishu.domain.dto.FeishuBitableRecordDto;
import com.ruoyi.project.feishu.service.ICompanyFeishuService;
import com.ruoyi.project.feishu.service.IFeishuUsersService;
import com.ruoyi.project.system.config.FeishuConfig;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 公司飞书服务实现类
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Slf4j
@Service
public class CompanyFeishuServiceImpl implements ICompanyFeishuService {
    
    @Autowired
    private IFeishuUsersService feishuUsersService;
    
    // 使用应用权限，无需用户令牌
    
    /**
     * 获取飞书客户端（使用固定密钥"公司飞书机器人"）
     * 
     * @return 飞书客户端实例
     */
    private Client getFeishuClient() {
        String keyName = "公司飞书机器人";
        
        // 获取飞书配置
        FeishuConfig feishuConfig = FeishuConfigUtils.getFeishuConfig(keyName);
        log.info("使用固定密钥名称: {} 获取飞书配置", keyName);
        
        if (feishuConfig == null || !feishuConfig.isValid()) {
            throw new RuntimeException("飞书配置无效，请检查密钥配置");
        }
        
        // 构建并返回飞书客户端
        return Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
    }
    
    @Override
    public Object getDepartmentUsers(String departmentId, String userIdType, String departmentIdType, Integer pageSize) {
        try {
            // 获取飞书客户端
            Client client = getFeishuClient();
            
            // 创建请求对象（使用固定参数）
            FindByDepartmentUserReq req = FindByDepartmentUserReq.newBuilder()
                    .userIdType("user_id")
                    .departmentIdType("open_department_id")
                    .departmentId(departmentId)
                    .pageSize(pageSize != null ? pageSize : 10)
                    .build();
            
            // 直接使用应用权限发起请求
            FindByDepartmentUserResp resp = client.contact().v3().user().findByDepartment(req);
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("获取部门用户列表失败，错误码: {}, 错误信息: {}, 请求ID: {}",
                        resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                            JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("获取部门用户列表失败: " + resp.getMsg());
            }
            
            // 返回业务数据
            return resp.getData();
            
        } catch (Exception e) {
            log.error("获取部门用户列表异常", e);
            throw new RuntimeException("获取部门用户列表异常: " + e.getMessage());
        }
    }
    
    @Override
    public String syncDepartmentUsers(String departmentId, String userIdType, String departmentIdType, Integer pageSize) {
        try {
            // 获取飞书客户端
            Client client = getFeishuClient();
            
            // 创建请求对象（使用固定参数）
            FindByDepartmentUserReq req = FindByDepartmentUserReq.newBuilder()
                    .userIdType("user_id")
                    .departmentIdType("open_department_id")
                    .departmentId(departmentId)
                    .pageSize(pageSize != null ? pageSize : 10)
                    .build();
            
            // 直接使用应用权限发起请求
            FindByDepartmentUserResp resp = client.contact().v3().user().findByDepartment(req);
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("获取部门用户列表失败，错误码: {}, 错误信息: {}, 请求ID: {}",
                        resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                            JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("获取部门用户列表失败: " + resp.getMsg());
            }
            
            // 解析用户数据并同步到本地数据库
            JsonObject data = (JsonObject) JsonParser.parseString(Jsons.DEFAULT.toJson(resp.getData()));
            JsonArray items = data.getAsJsonArray("items");
            
            if (items == null || items.size() == 0) {
                return "部门下暂无用户";
            }
            
            int addCount = 0;
            int updateCount = 0;
            List<String> syncResults = new ArrayList<>();
            
            // 遍历用户列表进行同步
            for (JsonElement item : items) {
                JsonObject userObj = item.getAsJsonObject();
                
                try {
                    // 转换为本地用户实体
                    FeishuUsers feishuUser = convertToFeishuUser(userObj);
                    
                    // 保存或更新用户信息
                    boolean isNew = feishuUser.getId() == null;
                    boolean saved = feishuUsersService.saveOrUpdateUser(feishuUser);
                    
                    if (saved) {
                        if (isNew) {
                            addCount++;
                            syncResults.add(String.format("新增用户: %s (%s)", feishuUser.getName(), feishuUser.getUserId()));
                        } else {
                            updateCount++;
                            syncResults.add(String.format("更新用户: %s (%s)", feishuUser.getName(), feishuUser.getUserId()));
                        }
                    }
                } catch (Exception e) {
                    log.error("同步用户失败: {}", userObj.get("name").getAsString(), e);
                    syncResults.add(String.format("同步失败: %s - %s", 
                            userObj.has("name") ? userObj.get("name").getAsString() : "未知用户", 
                            e.getMessage()));
                }
            }
            
            // 构建同步结果报告
            StringBuilder result = new StringBuilder();
            result.append(String.format("同步完成！新增: %d人, 更新: %d人\n", addCount, updateCount));
            result.append("详细信息:\n");
            
            for (String syncResult : syncResults) {
                result.append(syncResult).append("\n");
            }
            
            log.info("部门用户同步完成，新增{}人，更新{}人", addCount, updateCount);
            return result.toString();
            
        } catch (Exception e) {
            log.error("同步部门用户异常", e);
            throw new RuntimeException("同步部门用户异常: " + e.getMessage());
        }
    }
    
    /**
     * 将飞书API返回的用户数据转换为本地用户实体
     * 
     * @param userObj 飞书用户JSON对象
     * @return 本地用户实体
     */
    private FeishuUsers convertToFeishuUser(JsonObject userObj) {
        FeishuUsers feishuUser = new FeishuUsers();
        
        // 设置基本字段
        if (userObj.has("user_id")) {
            feishuUser.setUserId(userObj.get("user_id").getAsString());
        }
        
        if (userObj.has("open_id")) {
            feishuUser.setOpenId(userObj.get("open_id").getAsString());
        }
        
        if (userObj.has("union_id")) {
            feishuUser.setUnionId(userObj.get("union_id").getAsString());
        }
        
        if (userObj.has("name")) {
            feishuUser.setName(userObj.get("name").getAsString());
        }
        
        if (userObj.has("email")) {
            feishuUser.setEmail(userObj.get("email").getAsString());
        }
        
        if (userObj.has("mobile")) {
            feishuUser.setMobile(userObj.get("mobile").getAsString());
        }
        
        // 设置时间戳
        feishuUser.setCreatedAt(new Date());
        feishuUser.setUpdatedAt(new Date());
        
        // 设置密钥信息
        String keyName = "公司飞书机器人";
        feishuUser.setKeyName(keyName);
        
        // 获取并设置keyId
        FeishuConfig feishuConfig = FeishuConfigUtils.getFeishuConfig(keyName);
        if (feishuConfig != null && feishuConfig.getKeyId() != null) {
            feishuUser.setKeyId(feishuConfig.getKeyId());
        }
        
        // 检查用户是否已存在（通过open_id）
        if (feishuUser.getOpenId() != null) {
            FeishuUsers existingUser = feishuUsersService.selectByOpenId(feishuUser.getOpenId());
            if (existingUser != null) {
                feishuUser.setId(existingUser.getId());
                // 保持原有的keyId（如果存在）
                if (existingUser.getKeyId() != null) {
                    feishuUser.setKeyId(existingUser.getKeyId());
                }
                if (existingUser.getKeyName() != null) {
                    feishuUser.setKeyName(existingUser.getKeyName());
                }
            }
        }
        
        return feishuUser;
    }
    
    @Override
    public Object searchAppTableRecord(String appToken, String tableId, String viewId, Integer pageSize, String domain) {
        try {
            // 获取飞书客户端
            Client client = getFeishuClient();
            
            // 创建请求对象
            SearchAppTableRecordReq.Builder reqBuilder = SearchAppTableRecordReq.newBuilder()
                    .appToken(appToken)
                    .tableId(tableId)
                    .pageSize(pageSize != null ? pageSize : 20);
            
            // 构建请求体
            SearchAppTableRecordReqBody.Builder bodyBuilder = SearchAppTableRecordReqBody.newBuilder()
                    .automaticFields(true);
            
            // 设置视图ID（如果提供）
            if (StrUtil.isNotBlank(viewId)) {
                bodyBuilder.viewId(viewId);
            }
            
            // 设置过滤条件（如果提供域名参数）
            if (StrUtil.isNotBlank(domain)) {
                // 构建过滤器条件
                FilterInfo filter = buildFilterCondition(domain);
                bodyBuilder.filter(filter);
            }
            
            reqBuilder.searchAppTableRecordReqBody(bodyBuilder.build());
            
            SearchAppTableRecordReq req = reqBuilder.build();
            
            // 发起请求
            SearchAppTableRecordResp resp = client.bitable().v1().appTableRecord().search(req);
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("查询多维表格数据失败，错误码: {}, 错误信息: {}, 请求ID: {}",
                        resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                            JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("查询多维表格数据失败: " + resp.getMsg());
            }
            
            // 打印响应数据类型用于调试
            Object data = resp.getData();
            log.info("飞书API响应数据类型: {}", data != null ? data.getClass().getName() : "null");
            
            // 处理响应数据，转换为标准格式
            return processBitableResponse(data);
            
        } catch (Exception e) {
            log.error("查询多维表格数据异常", e);
            throw new RuntimeException("查询多维表格数据异常: " + e.getMessage());
        }
    }
    
    /**
     * 构建过滤条件
     * 
     * @param domain 域名过滤条件
     * @return FilterInfo对象
     */
    private FilterInfo buildFilterCondition(String domain) {
        // 创建条件对象
        Condition condition = new Condition();
        condition.setFieldName("域名");  // 根据内存中的信息，'域名'字段为文本类型
        condition.setOperator("is");
        
        // 设置条件值
        String[] values = {domain};
        condition.setValue(values);
        
        // 创建过滤器信息
        FilterInfo filter = new FilterInfo();
        filter.setConjunction("and");
        filter.setConditions(new Condition[]{condition});
        
        return filter;
    }
    
    /**
     * 处理多维表格响应数据
     * 
     * @param responseData 原始响应数据
     * @return 处理后的标准格式数据
     */
    private Object processBitableResponse(Object responseData) {
        try {
            if (responseData == null) {
                return new FeishuBitablePageResponseDto()
                    .setHasMore(false)
                    .setItems(new ArrayList<>())
                    .setTotal(0);
            }
            
            // 检查是否是飞书SDK的响应对象
            if (responseData instanceof com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordRespBody) {
                com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordRespBody respBody = 
                    (com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordRespBody) responseData;
                
                FeishuBitablePageResponseDto pageResponse = new FeishuBitablePageResponseDto();
                pageResponse.setHasMore(respBody.getHasMore());
                pageResponse.setPageToken(respBody.getPageToken());
                pageResponse.setTotal(respBody.getTotal());
                
                // 转换items
                List<FeishuBitableRecordDto> items = new ArrayList<>();
                if (respBody.getItems() != null) {
                    for (com.lark.oapi.service.bitable.v1.model.AppTableRecord sdkRecord : respBody.getItems()) {
                        FeishuBitableRecordDto record = new FeishuBitableRecordDto();
                        record.setRecordId(sdkRecord.getRecordId());
                        record.setFields(sdkRecord.getFields());
                        record.setCreatedTime(sdkRecord.getCreatedTime());
                        record.setLastModifiedTime(sdkRecord.getLastModifiedTime());
                        items.add(record);
                    }
                }
                pageResponse.setItems(items);
                
                if (pageResponse.getTotal() == null) {
                    pageResponse.setTotal(items.size());
                }
                
                log.info("成功处理多维表格数据，共{}条记录", pageResponse.getTotal());
                return pageResponse;
            }
            
            // 如果不是SDK对象，尝试JSON转换（兼容其他情况）
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            String jsonResponse;
            if (responseData instanceof String) {
                jsonResponse = (String) responseData;
            } else {
                jsonResponse = objectMapper.writeValueAsString(responseData);
            }
            
            log.debug("飞书多维表格原始响应长度: {} 字符", jsonResponse.length());
            
            FeishuBitablePageResponseDto pageResponse = objectMapper.readValue(jsonResponse, FeishuBitablePageResponseDto.class);
            
            if (pageResponse.getItems() == null) {
                pageResponse.setItems(new ArrayList<>());
            }
            
            if (pageResponse.getTotal() == null) {
                pageResponse.setTotal(pageResponse.getItems().size());
            }
            
            log.info("成功处理多维表格数据，共{}条记录", pageResponse.getTotal());
            return pageResponse;
            
        } catch (Exception e) {
            log.error("处理多维表格响应数据失败", e);
            return new FeishuBitablePageResponseDto()
                .setHasMore(false)
                .setItems(new ArrayList<>())
                .setTotal(0);
        }
    }
    
    @Override
    public Object createAppTableRecord(String appToken, String tableId, DomainCertRecordDto record) {
        try {
            // 获取飞书客户端
            Client client = getFeishuClient();
            
            // 将DTO转换为Map类型
            Map<String, Object> fieldsMap = convertDomainCertRecordToMap(record);
            
            // 创建请求对象
            CreateAppTableRecordReq req = CreateAppTableRecordReq.newBuilder()
                .appToken(appToken)
                .tableId(tableId)
                .appTableRecord(AppTableRecord.newBuilder()
                    .fields(fieldsMap)
                    .build())
                .build();
            
            // 发起请求
            CreateAppTableRecordResp resp = client.bitable().v1().appTableRecord().create(req);
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("新增多维表格记录失败，错误码: {}, 错误信息: {}, 请求ID: {}",
                        resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                            JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("新增多维表格记录失败: " + resp.getMsg());
            }
            
            // 返回新增记录的数据
            return resp.getData();
            
        } catch (Exception e) {
            log.error("新增多维表格记录异常", e);
            throw new RuntimeException("新增多维表格记录异常: " + e.getMessage());
        }
    }
    
    /**
     * 将DomainCertRecordDto转换为Map<String, Object>
     * 
     * @param record 域名证书记录DTO对象
     * @return Map格式的字段数据
     */
    private Map<String, Object> convertDomainCertRecordToMap(DomainCertRecordDto record) {
        Map<String, Object> fieldsMap = new HashMap<>();
        
        // 转换各个字段
        // 剩余天数 - 数字类型
        if (record.getRemainingDays() != null) {
            fieldsMap.put("剩余天数", record.getRemainingDays());
        }
        
        // 域名字段 - 文本类型（直接字符串）
        if (record.getDomain() != null) {
            fieldsMap.put("域名", record.getDomain());
        }
        
        // 备注字段 - 文本类型（直接字符串）
        if (record.getRemark() != null) {
            String remarkValue = record.getRemark();
            log.debug("备注原始值: {} (类型: {})", remarkValue, remarkValue.getClass().getName());
            fieldsMap.put("备注", remarkValue);
        }
        
        // 过期时间 - 日期类型（毫秒级时间戳）
        if (record.getExpireTime() != null) {
            fieldsMap.put("过期时间", record.getExpireTime());
        }
        
        log.debug("转换后的字段数据: {}", fieldsMap);
        return fieldsMap;
    }
    
    @Override
    public Object updateAppTableRecord(String appToken, String tableId, String recordId, DomainCertRecordDto record) {
        try {
            // 获取飞书客户端
            Client client = getFeishuClient();
            
            // 将DTO转换为Map类型
            Map<String, Object> fieldsMap = convertDomainCertRecordToMap(record);
            
            // 创建请求对象
            UpdateAppTableRecordReq req = UpdateAppTableRecordReq.newBuilder()
                .appToken(appToken)
                .tableId(tableId)
                .recordId(recordId)
                .appTableRecord(AppTableRecord.newBuilder()
                    .fields(fieldsMap)
                    .build())
                .build();
            
            // 发起请求
            UpdateAppTableRecordResp resp = client.bitable().v1().appTableRecord().update(req);
            
            // 处理服务端错误
            if (!resp.success()) {
                log.error("更新多维表格记录失败，错误码: {}, 错误信息: {}, 请求ID: {}",
                        resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                            JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                throw new RuntimeException("更新多维表格记录失败: " + resp.getMsg());
            }
            
            // 返回更新记录的数据
            return resp.getData();
            
        } catch (Exception e) {
            log.error("更新多维表格记录异常", e);
            throw new RuntimeException("更新多维表格记录异常: " + e.getMessage());
        }
    }
}