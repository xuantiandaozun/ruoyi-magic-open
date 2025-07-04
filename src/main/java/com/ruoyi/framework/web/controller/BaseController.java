package com.ruoyi.framework.web.controller;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.core.paginate.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.domain.BaseEntity;
import com.ruoyi.framework.web.page.TableDataInfo;

import cn.hutool.core.date.DateUtil;

/**
 * web层通用数据处理
 * 
 * @author ruoyi
 */
public class BaseController
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 将前台传递过来的日期格式的字符串，自动转化为Date类型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        // Date 类型转换
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText(String text)
            {
                setValue(DateUtil.parseDate(text));
            }
        });
    }


    /**
     * 清理分页的线程变量
     */
    protected void clearPage()
    {
        PageUtils.clearPage();
    }

    /**
     * 响应请求分页数据
     */
    protected <T> TableDataInfo getDataTable(Page<T> page)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(page.getRecords());
        rspData.setTotal(page.getTotalRow());
        return rspData;
    }

  

    /**
     * 返回成功
     */
    public AjaxResult success()
    {
        return AjaxResult.success();
    }

    /**
     * 返回成功消息
     */
    public AjaxResult success(String message)
    {
        return AjaxResult.success(message);
    }

      /**
     * 返回成功消息
     */
    public AjaxResult success(String message,Object data)
    {
        return AjaxResult.success(message, data);
    }

    /**
     * 返回成功消息
     */
    public AjaxResult success(Object data)
    {
        return AjaxResult.success(data);
    }

    /**
     * 返回失败消息
     */
    public AjaxResult error()
    {
        return AjaxResult.error();
    }

    /**
     * 返回失败消息
     */
    public AjaxResult error(String message)
    {
        return AjaxResult.error(message);
    }

    /**
     * 返回警告消息
     */
    public AjaxResult warn(String message)
    {
        return AjaxResult.warn(message);
    }

    /**
     * 响应返回结果
     * 
     * @param rows 影响行数
     * @return 操作结果
     */
    protected AjaxResult toAjax(int rows)
    {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 响应返回结果
     * 
     * @param result 结果
     * @return 操作结果
     */
    protected AjaxResult toAjax(boolean result)
    {
        return result ? success() : error();
    }

    /**
     * 获取用户缓存信息
     */
    public LoginUser getLoginUser()
    {
        return SecurityUtils.getLoginUser();
    }

    /**
     * 获取登录用户id
     */
    public Long getUserId()
    {
        return getLoginUser().getUserId();
    }

    /**
     * 获取登录部门id
     */
    public Long getDeptId()
    {
        return getLoginUser().getUser().getDeptId();
    }

    /**
     * 获取登录用户名
     */
    public String getUsername()
    {
        return getLoginUser().getUser().getUserName();
    }

    /**
     * 获取分页对象
     */
    protected <T> Page<T> getPage()
    {
        return new Page<>();
    }



    /**
     * 构建查询条件 (MyBatisFlex版本)
     * 自动处理字符串类型使用LIKE查询，其他类型使用EQ查询
     * 支持 params 参数中的时间范围查询：
     * beginTime, endTime - 创建时间范围
     */
    protected <T> com.mybatisflex.core.query.QueryWrapper buildFlexQueryWrapper(T entity) {
        com.mybatisflex.core.query.QueryWrapper queryWrapper = com.mybatisflex.core.query.QueryWrapper.create();
        if (entity == null) {
            return queryWrapper;
        }
        
        // 如果是BaseEntity类型，处理时间范围查询
        if (entity instanceof BaseEntity) {
            BaseEntity baseEntity = (BaseEntity) entity;
            Map<String, Object> params = baseEntity.getParams();
            if (params != null) {
                // 创建时间范围
                Object beginCreateTime = params.get("beginTime");
                Object endCreateTime = params.get("endTime");
                if (beginCreateTime != null && endCreateTime != null) {
                    queryWrapper.between("create_time", beginCreateTime, endCreateTime);
                } else if (beginCreateTime != null) {
                    queryWrapper.ge("create_time", beginCreateTime);
                } else if (endCreateTime != null) {
                    queryWrapper.le("create_time", endCreateTime);
                }
            }
            queryWrapper.orderBy("create_time desc");
        }

        // 反射获取实体字段值并构建查询条件
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = field.get(entity);
                  // 跳过serialVersionUID字段、null值字段，以及标记为ignore的字段
                if ("serialVersionUID".equals(fieldName) || value == null) {
                    continue;
                }
                
                // 检查是否有@Column注解，以及是否标记为ignore
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null && columnAnnotation.ignore()) {
                    continue;
                }

                // 获取字段对应的数据库列名
                String columnName = camelToUnderline(fieldName);
                
                // 根据字段类型构建不同的查询条件
                if (field.getType() == String.class) {
                    if (!((String) value).isEmpty()) {
                        queryWrapper.like(columnName, value);
                    }
                } else {
                    queryWrapper.eq(columnName, value);
                }
            } catch (Exception e) {
                logger.error("构建查询条件异常", e);
            }
        }
        
        return queryWrapper;
    }
    
    /**
     * 驼峰命名转下划线命名
     */
    private String camelToUnderline(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "";
        }
        int len = str.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
