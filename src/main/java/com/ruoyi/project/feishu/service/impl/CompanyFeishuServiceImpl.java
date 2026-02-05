package com.ruoyi.project.feishu.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.contact.v3.model.FindByDepartmentUserReq;
import com.lark.oapi.service.contact.v3.model.FindByDepartmentUserResp;
import com.ruoyi.common.utils.FeishuConfigUtils;
import com.ruoyi.project.feishu.domain.FeishuUsers;
import com.ruoyi.project.feishu.service.ICompanyFeishuService;
import com.ruoyi.project.feishu.service.IFeishuUsersService;
import com.ruoyi.project.system.config.FeishuConfig;

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
        feishuUser.setKeyName("公司飞书机器人");
        
        // 检查用户是否已存在（通过open_id）
        if (feishuUser.getOpenId() != null) {
            FeishuUsers existingUser = feishuUsersService.selectByOpenId(feishuUser.getOpenId());
            if (existingUser != null) {
                feishuUser.setId(existingUser.getId());
            }
        }
        
        return feishuUser;
    }
}