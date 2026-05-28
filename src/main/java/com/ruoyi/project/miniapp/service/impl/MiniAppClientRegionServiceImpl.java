package com.ruoyi.project.miniapp.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.project.miniapp.service.IMiniAppClientRegionService;
import com.ruoyi.project.system.service.IpLocationService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class MiniAppClientRegionServiceImpl implements IMiniAppClientRegionService {

    private static final String TARGET_LANGUAGE_ZH = "中文";
    private static final String TARGET_LANGUAGE_EN = "English";

    private final IpLocationService ipLocationService;

    public MiniAppClientRegionServiceImpl(IpLocationService ipLocationService) {
        this.ipLocationService = ipLocationService;
    }

    @Override
    public Map<String, Object> resolveClientRegion(HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        boolean domestic = isDomesticIp(ip);
        String country = resolveCountry(ip, domestic);

        Map<String, Object> result = new HashMap<>();
        result.put("ip", ip);
        result.put("domestic", domestic);
        result.put("country", country);
        result.put("defaultTargetLanguage", domestic ? TARGET_LANGUAGE_ZH : TARGET_LANGUAGE_EN);
        return result;
    }

    private boolean isDomesticIp(String ip) {
        if (IpUtils.internalIp(ip) || ipLocationService.isInternalIp(ip)) {
            return true;
        }

        Map<String, String> location = ipLocationService.getIpLocation(ip);
        String country = location.get("country");
        if (isChinaCountry(country)) {
            return true;
        }
        if (!"未知".equals(country)) {
            return false;
        }

        return isDomesticByPconline(ip);
    }

    private String resolveCountry(String ip, boolean domestic) {
        if (IpUtils.internalIp(ip) || ipLocationService.isInternalIp(ip)) {
            return "China";
        }

        Map<String, String> location = ipLocationService.getIpLocation(ip);
        String country = location.get("country");
        if (!"未知".equals(country)) {
            return country;
        }

        if (domestic) {
            return "China";
        }
        return country;
    }

    private boolean isChinaCountry(String country) {
        return "China".equalsIgnoreCase(country) || "中国".equals(country);
    }

    private boolean isDomesticByPconline(String ip) {
        String address = AddressUtils.getRealAddressByIP(ip);
        return !AddressUtils.UNKNOWN.equals(address) && !"内网IP".equals(address);
    }
}
