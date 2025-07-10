package com.ruoyi.project.gen.tools.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.system.domain.SysConfig;
import com.ruoyi.project.system.service.ISysConfigService;

import cn.hutool.core.util.StrUtil;

/**
 * 配置管理工具
 * 提供系统参数的管理操作
 */
@Service
public class ConfigTool {
    private static final Logger logger = LoggerFactory.getLogger(ConfigTool.class);

    @Autowired
    private ISysConfigService sysConfigService;

    /**
     * 查询系统参数列表
     */
    @Tool(name = "getConfigList", description = "查询系统参数列表")
    public  List<SysConfig> getConfigList(String configName, String configKey, String configType) {
        try {
            logger.info("getConfigList查询系统参数列表");
            SysConfig sysConfig = new SysConfig();
            if (StrUtil.isNotBlank(configName)) {
                sysConfig.setConfigName(configName);
            }
            if (StrUtil.isNotBlank(configKey)) {
                sysConfig.setConfigKey(configKey);
            }
            if (StrUtil.isNotBlank(configType)) {
                sysConfig.setConfigType(configType);
            }

           List<SysConfig> configList = sysConfigService.selectConfigList(sysConfig);
            
    
    
            return configList;
        } catch (Exception e) {
            logger.error("查询系统参数列表失败", e);
            throw new ServiceException("查询系统参数列表失败：" + e.getMessage());
        }
    }

    /**
     * 新增系统参数
     */
    @Tool(name = "addConfig", description = "新增系统参数")
    public Map<String, Object> addConfig(SysConfig config) {
        try {
            logger.info("addConfig新增系统参数: {}", config);
            boolean success = sysConfigService.save(config);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("config", config);
            result.put("message", success ? "系统参数新增成功" : "系统参数新增失败");
            
            return result;
        } catch (Exception e) {
            logger.error("新增系统参数失败", e);
            throw new ServiceException("新增系统参数失败：" + e.getMessage());
        }
    }

    /**
     * 修改系统参数
     */
    @Tool(name = "updateConfig", description = "修改系统参数")
    public Map<String, Object> updateConfig(SysConfig config, String configId) {
        try {
            logger.info("updateConfig修改系统参数: {}, configId: {}", config, configId);

            // 如果提供了字符串ID，设置到实体类中
            if (StrUtil.isNotBlank(configId)) {
                try {
                    config.setConfigId(Long.parseLong(configId));
                } catch (NumberFormatException e) {
                    logger.error("系统参数ID格式错误: {}", configId);
                    throw new ServiceException("系统参数ID格式错误");
                }
            }

            boolean success = sysConfigService.updateById(config);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("config", config);
            result.put("configId", configId);
            result.put("message", success ? "系统参数修改成功" : "系统参数修改失败");
            
            return result;
        } catch (Exception e) {
            logger.error("修改系统参数失败", e);
            throw new ServiceException("修改系统参数失败：" + e.getMessage());
        }
    }

    /**
     * 根据参数键名查询参数值
     */
    @Tool(name = "getConfigByKey", description = "根据参数键名查询参数值")
    public Map<String, Object> getConfigByKey(String configKey) {
        try {
            logger.info("getConfigByKey根据参数键名查询参数值: {}", configKey);
            String configValue = sysConfigService.selectConfigByKey(configKey);
            
            Map<String, Object> result = new HashMap<>();
            result.put("configKey", configKey);
            result.put("configValue", configValue);
            
            if (StrUtil.isBlank(configValue)) {
                result.put("message", "参数不存在或值为空");
            } else {
                result.put("message", "查询参数值成功");
            }
            
            return result;
        } catch (Exception e) {
            logger.error("根据参数键名查询参数值失败", e);
            throw new ServiceException("根据参数键名查询参数值失败：" + e.getMessage());
        }
    }
}