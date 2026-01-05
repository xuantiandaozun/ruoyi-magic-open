package com.ruoyi.project.aliyun.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyi.project.aliyun.domain.RdsInstanceInfo;
import com.ruoyi.project.aliyun.domain.dto.ModifySecurityIpsRequest;
import com.ruoyi.project.aliyun.service.IRdsInstanceInfoService;
import com.ruoyi.project.system.service.IpLocationService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * RDS实例管理Controller
 * 
 * @author ruoyi
 * @date 2025-07-11 17:49:40
 */
@RestController
@RequestMapping("/aliyun/rdsInstance")
public class RdsInstanceInfoController extends BaseController
{
    @Autowired
    private IRdsInstanceInfoService rdsInstanceInfoService;
    
    @Autowired
    private IpLocationService ipLocationService;

    /**
     * 查询RDS实例管理列表
     */
    @SaCheckPermission("aliyun:rdsInstance:list")
    @GetMapping("/list")
    public TableDataInfo list(RdsInstanceInfo rdsInstanceInfo)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(rdsInstanceInfo);
        
        // 使用 MyBatisFlex 的分页方法
        Page<RdsInstanceInfo> page = rdsInstanceInfoService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出RDS实例管理列表
     */
    @SaCheckPermission("aliyun:rdsInstance:export")
    @Log(title = "RDS实例管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RdsInstanceInfo rdsInstanceInfo)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<RdsInstanceInfo> list = rdsInstanceInfoService.list(queryWrapper);
        MagicExcelUtil<RdsInstanceInfo> util = new MagicExcelUtil<>(RdsInstanceInfo.class);
        util.exportExcel(response, list, "RDS实例管理数据");
    }

    /**
     * 获取RDS实例管理详细信息
     */
    @SaCheckPermission("aliyun:rdsInstance:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(rdsInstanceInfoService.getById(id));
    }

    /**
     * 新增RDS实例管理
     */
    @SaCheckPermission("aliyun:rdsInstance:add")
    @Log(title = "RDS实例管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RdsInstanceInfo rdsInstanceInfo)
    {
        return toAjax(rdsInstanceInfoService.save(rdsInstanceInfo) ? 1 : 0);
    }

    /**
     * 修改RDS实例管理
     */
    @SaCheckPermission("aliyun:rdsInstance:edit")
    @Log(title = "RDS实例管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RdsInstanceInfo rdsInstanceInfo)
    {
        return toAjax(rdsInstanceInfoService.updateById(rdsInstanceInfo) ? 1 : 0);
    }

    /**
     * 删除RDS实例管理
     */
    @SaCheckPermission("aliyun:rdsInstance:remove")
    @Log(title = "RDS实例管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(rdsInstanceInfoService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }

    /**
     * 同步阿里云RDS实例数据
     */
    @SaCheckPermission("aliyun:rdsInstance:sync")
    @Log(title = "同步阿里云RDS实例", businessType = BusinessType.OTHER)
    @PostMapping("/syncAliyun")
    public AjaxResult syncAliyunRdsInstances()
    {
        return rdsInstanceInfoService.syncAliyunRdsInstances();
    }
    
    /**
     * 获取RDS实例连接信息
     */
    @SaCheckPermission("aliyun:rdsInstance:query")
    @Log(title = "获取RDS实例连接信息", businessType = BusinessType.OTHER)
    @GetMapping("/netInfo/{dbInstanceId}")
    public AjaxResult getRdsInstanceNetInfo(@PathVariable("dbInstanceId") String dbInstanceId)
    {
        return rdsInstanceInfoService.getRdsInstanceNetInfo(dbInstanceId);
    }
    
    /**
     * 获取RDS实例白名单信息
     */
    @SaCheckPermission("aliyun:rdsInstance:query")
    @Log(title = "获取RDS实例白名单信息", businessType = BusinessType.OTHER)
    @GetMapping("/ipArrayList/{dbInstanceId}")
    public AjaxResult getRdsInstanceIPArrayList(@PathVariable("dbInstanceId") String dbInstanceId)
    {
        return rdsInstanceInfoService.getRdsInstanceIPArrayList(dbInstanceId);
    }
    
    /**
     * 修改RDS实例白名单
     */
    @SaCheckPermission("aliyun:rdsInstance:edit")
    @Log(title = "修改RDS实例白名单", businessType = BusinessType.UPDATE)
    @PutMapping("/modifySecurityIps/{dbInstanceId}")
    public AjaxResult modifyRdsInstanceSecurityIps(
            @PathVariable("dbInstanceId") String dbInstanceId,
            @RequestBody ModifySecurityIpsRequest request)
    {
        return rdsInstanceInfoService.modifyRdsInstanceSecurityIps(
            dbInstanceId, 
            request.getSecurityIps(), 
            request.getDbInstanceIPArrayName()
        );
    }
    
    /**
     * 批量更新所有RDS实例的客户端白名单
     * 自动获取客户端IP并设置到所有RDS实例的client分组中
     */
    @SaCheckPermission("aliyun:rdsInstance:edit")
    @Log(title = "批量更新RDS客户端白名单", businessType = BusinessType.UPDATE)
    @PostMapping("/updateClientWhitelist")
    public AjaxResult updateAllRdsClientWhitelist(HttpServletRequest request)
    {
        String clientIp = getClientIpAddress(request);
        
        // 检查是否为IPv6地址
        if (isIPv6Address(clientIp)) {
            return AjaxResult.error("不支持IPv6地址，当前客户端IP: " + clientIp);
        }
        
        return rdsInstanceInfoService.updateAllRdsClientWhitelist(clientIp);
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp;
        }
        
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp;
        }
        
        String httpClientIp = request.getHeader("HTTP_CLIENT_IP");
        if (httpClientIp != null && !httpClientIp.isEmpty() && !"unknown".equalsIgnoreCase(httpClientIp)) {
            return httpClientIp;
        }
        
        String httpXForwardedFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (httpXForwardedFor != null && !httpXForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(httpXForwardedFor)) {
            return httpXForwardedFor;
        }
        
        // 如果以上都没有获取到，则使用request.getRemoteAddr()
        return request.getRemoteAddr();
    }
    
    /**
     * 获取当前客户端IP地址和地理位置信息
     */
    @SaCheckPermission("aliyun:rdsInstance:query")
    @Log(title = "获取客户端IP地址", businessType = BusinessType.OTHER)
    @GetMapping("/getClientIp")
    public AjaxResult getClientIp(HttpServletRequest request)
    {
        String clientIp = getClientIpAddress(request);
        
        // 检查是否为IPv6地址
        if (isIPv6Address(clientIp)) {
            Map<String, Object> result = new HashMap<>();
            result.put("ip", clientIp);
            result.put("isIPv6", true);
            result.put("location", null);
            return AjaxResult.error("当前客户端为IPv6地址: " + clientIp, result);
        }
        
        // 获取地理位置信息
        Map<String, String> location = null;
        if (!ipLocationService.isInternalIp(clientIp)) {
            location = ipLocationService.getIpLocation(clientIp);
        } else {
            // 内网IP设置默认地理位置
            location = new HashMap<>();
            location.put("country", "本地网络");
            location.put("region", "内网");
            location.put("city", "内部");
            location.put("isp", "局域网");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("ip", clientIp);
        result.put("isIPv6", false);
        result.put("location", location);
        
        return AjaxResult.success("获取客户端IP成功", result);
    }
    
    /**
     * 检查是否为IPv6地址
     */
    private boolean isIPv6Address(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        // IPv6地址包含冒号
        return ip.contains(":");
    }
}