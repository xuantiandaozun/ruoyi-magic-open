package com.ruoyi.common.core.text;

import java.nio.charset.Charset;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 字符集工具类
 * 
 * @author ruoyi
 */
public class CharsetKit
{
    /** ISO-8859-1 */
    public static final String ISO_8859_1 = CharsetUtil.ISO_8859_1;
    /** UTF-8 */
    public static final String UTF_8 = CharsetUtil.UTF_8;
    /** GBK */
    public static final String GBK = CharsetUtil.GBK;

    /** ISO-8859-1 */
    public static final Charset CHARSET_ISO_8859_1 = CharsetUtil.CHARSET_ISO_8859_1;
    /** UTF-8 */
    public static final Charset CHARSET_UTF_8 = CharsetUtil.CHARSET_UTF_8;
    /** GBK */
    public static final Charset CHARSET_GBK = CharsetUtil.CHARSET_GBK;

    /**
     * 转换为Charset对象
     * 
     * @param charset 字符集，为空则返回默认字符集
     * @return Charset
     */
    public static Charset charset(String charset)
    {
        return StrUtil.isEmpty(charset) ? Charset.defaultCharset() : CharsetUtil.charset(charset);
    }

    /**
     * 转换字符串的字符集编码
     * 
     * @param source 字符串
     * @param srcCharset 源字符集，默认ISO-8859-1
     * @param destCharset 目标字符集，默认UTF-8
     * @return 转换后的字符集
     */
    public static String convert(String source, String srcCharset, String destCharset)
    {
        return CharsetUtil.convert(source, srcCharset, destCharset);
    }

    /**
     * 转换字符串的字符集编码
     * 
     * @param source 字符串
     * @param srcCharset 源字符集，默认ISO-8859-1
     * @param destCharset 目标字符集，默认UTF-8
     * @return 转换后的字符集
     */
    public static String convert(String source, Charset srcCharset, Charset destCharset)
    {
        return CharsetUtil.convert(source, srcCharset, destCharset);
    }

    /**
     * @return 系统字符集编码
     */
    public static String systemCharset()
    {
        return CharsetUtil.defaultCharset().name();
    }
}
