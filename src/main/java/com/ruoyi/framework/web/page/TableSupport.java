package com.ruoyi.framework.web.page;

import cn.hutool.core.convert.Convert;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 表格数据处理
 * 
 * @author ruoyi
 */
public class TableSupport
{
    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 分页参数合理化
     */
    public static final String REASONABLE = "reasonable";

    /**
     * 封装分页对象
     */
    public static PageDomain getPageDomain()
    {
        PageDomain pageDomain = new PageDomain();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("当前不在Web请求上下文中");
        }
        HttpServletRequest request = attributes.getRequest();
        pageDomain.setPageNum(Convert.toInt(request.getParameter(PAGE_NUM), 1));
        pageDomain.setPageSize(Convert.toInt(request.getParameter(PAGE_SIZE), 10));
        pageDomain.setOrderByColumn(request.getParameter(ORDER_BY_COLUMN));
        pageDomain.setIsAsc(request.getParameter(IS_ASC));
        pageDomain.setReasonable(Convert.toBool(request.getParameter(REASONABLE), true));
        return pageDomain;
    }

    public static PageDomain buildPageRequest()
    {
        return getPageDomain();
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage()
    {
        RequestContextHolder.resetRequestAttributes();
    }
}
