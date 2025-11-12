package com.ruoyi.project.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * IP地理位置服务
 * 
 * @author ruoyi
 */
@Service
public class IpLocationService
{
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取IP地理位置信息
     * 使用免费的IP地理位置API
     */
    public Map<String, String> getIpLocation(String ip) {
        Map<String, String> location = new HashMap<>();
        
        try {
            // 使用ip-api.com的免费API
            String url = "http://ip-api.com/json/" + ip + "?fields=status,message,country,regionName,city,isp";
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                
                if ("success".equals(jsonNode.get("status").asText())) {
                    location.put("country", jsonNode.get("country").asText());
                    location.put("region", jsonNode.get("regionName").asText());
                    location.put("city", jsonNode.get("city").asText());
                    location.put("isp", jsonNode.get("isp").asText());
                } else {
                    // API调用失败，返回默认值
                    location.put("country", "未知");
                    location.put("region", "未知");
                    location.put("city", "未知");
                    location.put("isp", "未知");
                }
            }
        } catch (Exception e) {
            // 异常情况，返回默认值
            location.put("country", "未知");
            location.put("region", "未知");
            location.put("city", "未知");
            location.put("isp", "未知");
        }
        
        return location;
    }
    
    /**
     * 检查是否为内网IP
     */
    public boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // 检查常见的内网IP段
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               ip.startsWith("172.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("localhost") ||
               ip.startsWith("169.254.");
    }
}
