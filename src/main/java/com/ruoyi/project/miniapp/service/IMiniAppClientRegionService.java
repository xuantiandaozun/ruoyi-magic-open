package com.ruoyi.project.miniapp.service;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

public interface IMiniAppClientRegionService {
    Map<String, Object> resolveClientRegion(HttpServletRequest request);
}
