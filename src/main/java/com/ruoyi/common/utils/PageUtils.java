package com.ruoyi.common.utils;

import com.ruoyi.framework.web.page.TableSupport;

/**
 * 分页工具类
 * 
 * @author ruoyi
 */
public class PageUtils
{


    /**
     * 清理分页的线程变量
     */
    public static void clearPage()
    {
        TableSupport.clearPage();
    }
}
