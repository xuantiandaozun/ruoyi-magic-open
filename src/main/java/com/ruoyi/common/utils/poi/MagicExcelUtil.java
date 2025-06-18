package com.ruoyi.common.utils.poi;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.common.exception.UtilException;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.aspectj.lang.annotation.Excel.Type;
import com.ruoyi.framework.aspectj.lang.annotation.Excels;
import com.ruoyi.framework.config.RuoYiConfig;
import com.ruoyi.framework.web.domain.AjaxResult;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.StyleSet;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Excel相关处理
 */
public class MagicExcelUtil<T>
{
    private static final Logger log = LoggerFactory.getLogger(MagicExcelUtil.class);

    /**
     * 实体对象
     */
    public Class<T> clazz;

    /**
     * 最大高度
     */
    public static final short MAX_HEIGHT = Short.MAX_VALUE;

    /**
     * 单元格最大宽度
     */
    public static final int MAX_WIDTH = 255;

    /**
     * 默认日期格式
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 默认时间格式
     */
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 字段注解缓存
     */
    private Map<String, Object> fieldAnnotationCache = new HashMap<>();

    public MagicExcelUtil(Class<T> clazz)
    {
        this.clazz = clazz;
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName)
    {
        exportExcel(response, list, sheetName, StrUtil.EMPTY);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, String title)
    {
        exportExcel(response, list, sheetName, title, null);
    }
    
    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @param response 响应对象
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @param title 标题
     * @param fileName 指定的文件名（不包含扩展名）
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, String title, String fileName)
    {
        try
        {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 设置文件名
            if (fileName == null || fileName.isEmpty()) {
                fileName = sheetName; // 使用sheet名称作为文件名
            }
            // 对文件名进行编码
            String encodedFileName = customUrlEncode(fileName + ".xlsx");
            response.setHeader("Content-disposition", "attachment;filename=" + encodedFileName);

            // 创建ExcelWriter
            ExcelWriter writer = createExcelWriter();
            
            // 设置sheet名称
            writer.renameSheet(sheetName);
            
            // 如果有标题，添加标题行
            if (StrUtil.isNotEmpty(title)) {
                writer.merge(getExcelFields().size() - 1, title);
            }
            
            // 写入数据
            writeData(writer, list);
            
            // 自动调整列宽
            autoSizeColumns(writer);
            
            // 写入响应
            writer.flush(response.getOutputStream());
            writer.close();
        }
        catch (Exception e)
        {
            log.error("导出Excel异常{}", e.getMessage());
            throw new UtilException("导出Excel失败，请联系网站管理员！");
        }
    }

  
    
    /**
     * 导出Excel
     *
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @param title 标题
     * @return 结果
     */
    public AjaxResult exportExcel(List<T> list, String sheetName, String title)
    {
        try
        {
            // 获取导出数据
            List<Map<String, Object>> listMap = writeData(list);
            // 创建工作簿对象
            ExcelWriter writer = createExcelWriter();
            // 设置sheet名称
            writer.renameSheet(0, sheetName);
            // 写入数据
            writer.write(listMap, true);
            // 自动调整列宽
            autoSizeColumns(writer);
            // 编码文件名
            String filename = encodingFilename(sheetName, title);
            // 获取文件存储路径
            String downloadPath = getAbsoluteFile(filename);
            // 写入文件
            writer.flush(new File(downloadPath));
            // 关闭writer
            writer.close();
            // 返回结果
            return AjaxResult.success(filename);
        }
        catch (Exception e)
        {
            log.error("导出Excel异常{}", e.getMessage());
            throw new UtilException("导出Excel失败，请联系网站管理员！");
        }
    }

    /**
     * 创建Excel写入器
     */
    private ExcelWriter createExcelWriter() {
        // 使用false参数，避免创建多个sheet
        ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter(false);
        
        // 设置样式
        StyleSet styleSet = writer.getStyleSet();
        
        // 设置表头样式
        CellStyle headCellStyle = styleSet.getHeadCellStyle();
        headCellStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headFont = writer.createFont();
        headFont.setBold(true);
        headFont.setFontHeightInPoints((short) 11);
        headFont.setColor(IndexedColors.WHITE.getIndex());
        headCellStyle.setFont(headFont);
        headCellStyle.setAlignment(HorizontalAlignment.CENTER);
        headCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headCellStyle.setWrapText(true); // 允许内容换行
        
        // 设置内容样式
        CellStyle contentCellStyle = styleSet.getCellStyle();
        Font contentFont = writer.createFont();
        contentFont.setFontHeightInPoints((short) 11);
        contentCellStyle.setFont(contentFont);
        contentCellStyle.setAlignment(HorizontalAlignment.CENTER); // 默认居中对齐
        contentCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentCellStyle.setWrapText(true); // 允许内容换行
        
        return writer;
    }
    
    /**
     * 写入数据
     * 
     * @param <T> 泛型
     * @param list 数据列表
     * @return 结果
     */
    private List<Map<String, Object>> writeData(List<T> list)
    {
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (CollUtil.isEmpty(list))
        {
            return mapList;
        }
        // 获取实体对象的所有字段
        List<Field> fields = getExcelFields();
        // 遍历数据
        for (T vo : list)
        {
            Map<String, Object> resultMap = new HashMap<>();
            // 遍历字段
            for (Field field : fields)
            {
                // 字段名
                String fieldName = field.getName();
                // 获取注解
                Excel attr = field.getAnnotation(Excel.class);
                // 如果没有Excel注解或者不导出，则跳过
                if (attr == null || attr.type() == Type.IMPORT)
                {
                    continue;
                }
                // 字段值
                Object value = null;
                try
                {
                    // 设置可访问
                    field.setAccessible(true);
                    // 获取字段值
                    value = field.get(vo);
                    // 处理日期格式
                    if (StrUtil.isNotEmpty(attr.dateFormat()) && value != null)
                    {
                        value = DateUtil.format((Date) value, attr.dateFormat());
                    }
                    // 处理表达式
                    else if (StrUtil.isNotEmpty(attr.readConverterExp()) && value != null)
                    {
                        value = convertByExp(Convert.toStr(value), attr.readConverterExp(), attr.separator());
                    }
                    // 处理BigDecimal精度和舍入规则
                    else if (attr.scale() >= 0 && value != null && value instanceof BigDecimal)
                    {
                        value = ((BigDecimal) value).setScale(attr.scale(), attr.roundingMode());
                    }
                    // 处理后缀
                    if (StrUtil.isNotEmpty(attr.suffix()) && value != null)
                    {
                        value = value + attr.suffix();
                    }
                    // 处理默认值
                    if (value == null && StrUtil.isNotEmpty(attr.defaultValue()))
                    {
                        value = attr.defaultValue();
                    }
                    // 将字段值添加到结果映射中
                    resultMap.put(fieldName, value);
                }
                catch (Exception e)
                {
                    log.error("导出Excel失败{}", e.getMessage());
                }
                // 处理嵌套对象的Excel注解在writeData方法中统一处理
            }
            mapList.add(resultMap);
        }
        return mapList;
    }
    
  
    
    /**
     * 写入数据到Excel
     */
    private void writeData(ExcelWriter writer, List<T> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        
        // 获取Excel字段信息
        List<Field> fields = getExcelFields();
        if (CollUtil.isEmpty(fields)) {
            throw new UtilException("没有找到Excel注解字段");
        }
        
        // 设置表头别名和列样式
        for (Field field : fields) {
            Excel excel = field.getAnnotation(Excel.class);
            if (excel != null) {
                writer.addHeaderAlias(field.getName(), excel.name());
                
                // 设置列宽度
                if (excel.width() > 0) {
                    int columnIndex = fields.indexOf(field);
                    writer.setColumnWidth(columnIndex, (int) excel.width());
                }
            }
        }
        
        // 处理数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (T item : list) {
            Map<String, Object> dataMap = BeanUtil.beanToMap(item);
            Map<String, Object> resultMap = new HashMap<>();
            
            // 处理每个字段的值
            for (Field field : fields) {
                String fieldName = field.getName();
                Object value = dataMap.get(fieldName);
                
                // 处理Excel注解
                Excel excel = field.getAnnotation(Excel.class);
                if (excel != null) {
                    // 处理日期格式
                    if (value instanceof Date && StrUtil.isNotEmpty(excel.dateFormat())) {
                        value = DateUtil.format((Date) value, excel.dateFormat());
                    }
                    
                    // 处理读取转换表达式
                    if (StrUtil.isNotEmpty(excel.readConverterExp())) {
                        value = convertByExp(Convert.toStr(value), excel.readConverterExp(), excel.separator());
                    }
                    
                    // 处理数字精度
                    if (value instanceof BigDecimal && excel.scale() >= 0) {
                        value = ((BigDecimal) value).setScale(excel.scale(), excel.roundingMode());
                    }
                    
                    // 处理后缀
                    if (StrUtil.isNotEmpty(excel.suffix()) && value != null) {
                        value = value + excel.suffix();
                    }
                    
                    // 处理默认值
                    if (ObjectUtil.isEmpty(value) && StrUtil.isNotEmpty(excel.defaultValue())) {
                        value = excel.defaultValue();
                    }
                    
                    // 只添加有Excel注解的字段
                    resultMap.put(fieldName, value);
                }
            }
            
            // 处理嵌套对象的Excel注解
            handleNestedExcel(dataMap, resultMap, fields, writer);
            
            dataList.add(resultMap);
        }
        
        // 写入数据
        writer.write(dataList, true);
    }
    
    /**
     * 自动调整列宽
     */
    private void autoSizeColumns(ExcelWriter writer) {
        try {
            // 获取当前sheet
            org.apache.poi.ss.usermodel.Sheet sheet = writer.getSheet();
            
            // 获取Excel字段信息
            List<Field> fields = getExcelFields();
            if (CollUtil.isEmpty(fields)) {
                return;
            }
            
            // 遍历所有列，设置自动列宽
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                Excel excel = field.getAnnotation(Excel.class);
                
                if (excel != null) {
                    // 如果注解中指定了宽度，则使用注解中的宽度
                    if (excel.width() > 0) {
                        writer.setColumnWidth(i, (int) excel.width());
                    } else {
                        // 否则自动计算列宽
                        writer.autoSizeColumn(i);
                    }
                }
            }
        } catch (Exception e) {
            log.error("自动调整列宽异常: {}", e.getMessage());
        }
    }
    
    /**
     * 处理嵌套对象的Excel注解
     */
    private void handleNestedExcel(Map<String, Object> dataMap, Map<String, Object> resultMap, List<Field> fields, ExcelWriter writer) {
        for (Field field : fields) {
            Excels excels = field.getAnnotation(Excels.class);
            if (excels != null) {
                Excel[] excelArray = excels.value();
                if (excelArray != null && excelArray.length > 0) {
                    String fieldName = field.getName();
                    Object nestedObj = dataMap.get(fieldName);
                    if (nestedObj != null) {
                        Map<String, Object> nestedMap = BeanUtil.beanToMap(nestedObj);
                        for (Excel excel : excelArray) {
                            String targetAttr = excel.targetAttr();
                            if (StrUtil.isNotEmpty(targetAttr)) {
                                Object value = nestedMap.get(targetAttr);
                                
                                // 处理日期格式
                                if (value instanceof Date && StrUtil.isNotEmpty(excel.dateFormat())) {
                                    value = DateUtil.format((Date) value, excel.dateFormat());
                                }
                                
                                // 处理读取转换表达式
                                if (StrUtil.isNotEmpty(excel.readConverterExp())) {
                                    value = convertByExp(Convert.toStr(value), excel.readConverterExp(), excel.separator());
                                }
                                
                                // 处理后缀
                                if (StrUtil.isNotEmpty(excel.suffix()) && value != null) {
                                    value = value + excel.suffix();
                                }
                                
                                // 处理默认值
                                if (ObjectUtil.isEmpty(value) && StrUtil.isNotEmpty(excel.defaultValue())) {
                                    value = excel.defaultValue();
                                }
                                
                                resultMap.put(fieldName + "." + targetAttr, value);
                                writer.addHeaderAlias(fieldName + "." + targetAttr, excel.name());
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 获取Excel字段
     */
    private List<Field> getExcelFields() {
        String cacheKey = "excel_fields_" + clazz.getName();
        @SuppressWarnings("unchecked")
        List<Field> fields = (List<Field>) fieldAnnotationCache.get(cacheKey);
        
        if (fields == null) {
            fields = new ArrayList<>();
            Field[] allFields = ReflectUtil.getFields(clazz);
            
            // 获取所有带有Excel注解的字段
            for (Field field : allFields) {
                Excel excel = field.getAnnotation(Excel.class);
                if (excel != null && excel.type() != Type.IMPORT) {
                    fields.add(field);
                }
                
                Excels excels = field.getAnnotation(Excels.class);
                if (excels != null) {
                    fields.add(field);
                }
            }
            
            // 按照sort排序
            fields.sort(Comparator.comparing(field -> {
                Excel excel = field.getAnnotation(Excel.class);
                return excel != null ? excel.sort() : Integer.MAX_VALUE;
            }));
            
            fieldAnnotationCache.put(cacheKey, fields);
        }
        
        return fields;
    }
    
    /**
     * 根据表达式转换值
     */
    private String convertByExp(String value, String expression, String separator) {
        if (StrUtil.isEmpty(value) || StrUtil.isEmpty(expression)) {
            return value;
        }
        
        String[] exps = expression.split(",");
        for (String exp : exps) {
            String[] items = exp.split("=");
            if (items.length == 2 && value.equals(items[0])) {
                return items[1];
            }
        }
        
        return value;
    }

    /**
     * 编码文件名
     */
    public String encodingFilename(String filename)
    {
        return encodingFilename(filename, null);
    }
    
    /**
     * 编码文件名
     * 
     * @param filename 文件名
     * @param customFileName 自定义文件名
     * @return 编码后的文件名
     */
    public String encodingFilename(String filename, String customFileName)
    {
        if (customFileName != null && !customFileName.isEmpty()) {
            filename = customFileName + ".xlsx";
        } else {
            filename = filename + ".xlsx";
        }
        // 自定义URL编码，避免中文字符问题
        return customUrlEncode(filename);
    }
    
    /**
     * 自定义URL编码
     * 1.字符"a"-"z"，"A"-"Z"，"0"-"9"，"."，"-"，"*"，和"_" 都不会被编码
     * 2.将空格转换为%20
     * 3.将非文本内容转换成"%xy"的形式,xy是两位16进制的数值
     * 
     * @param str 需要编码的字符串
     * @return 编码后的字符串
     */
    private String customUrlEncode(String str) {
        if (str == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') 
                    || c == '.' || c == '-' || c == '*' || c == '_') {
                // 这些字符不编码
                sb.append(c);
            } else if (c == ' ') {
                // 空格转换为%20
                sb.append("%20");
            } else {
                // 其他字符转换为%xy形式
                byte[] bytes;
                try {
                    bytes = String.valueOf(c).getBytes("UTF-8");
                    for (byte b : bytes) {
                        sb.append(String.format("%%%02X", b & 0xff));
                    }
                } catch (UnsupportedEncodingException e) {
                    log.error("字符编码异常", e);
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 获取下载路径
     */
    public String getAbsoluteFile(String filename)
    {
        String downloadPath = RuoYiConfig.getDownloadPath() + filename;
        File desc = new File(downloadPath);
        if (!desc.getParentFile().exists())
        {
            desc.getParentFile().mkdirs();
        }
        return downloadPath;
    }

    /**
     * 导入Excel数据
     * 
     * @param is 文件输入流
     * @return 数据列表
     * @throws Exception
     */
    public List<T> importExcelData(InputStream is) throws Exception
    {
        List<T> list = new ArrayList<T>();
        try {
            // 使用Hutool的ExcelReader读取Excel
            ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(is);
            
            // 获取所有带有Excel注解的字段
            Field[] allFields = ReflectUtil.getFields(clazz);
            Map<String, Field> fieldMap = new HashMap<>();
            Map<String, Excel> excelMap = new HashMap<>();
            
            for (Field field : allFields) {
                Excel excel = field.getAnnotation(Excel.class);
                if (excel != null && (excel.type() == Type.ALL || excel.type() == Type.IMPORT)) {
                    fieldMap.put(excel.name(), field);
                    excelMap.put(excel.name(), excel);
                }
            }
            
            // 读取Excel数据
            List<Map<String, Object>> readAll = reader.readAll();
            for (Map<String, Object> dataMap : readAll) {
                T entity = clazz.newInstance();
                
                for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    Field field = fieldMap.get(key);
                    if (field != null) {
                        Excel excel = excelMap.get(key);
                        
                        // 处理值的转换
                        if (value != null) {
                            // 处理日期
                            if (StrUtil.isNotEmpty(excel.dateFormat()) && field.getType() == Date.class) {
                                value = DateUtil.parse(value.toString(), excel.dateFormat());
                            }
                            
                            // 处理表达式转换
                            if (StrUtil.isNotEmpty(excel.readConverterExp())) {
                                value = reverseByExp(value.toString(), excel.readConverterExp(), excel.separator());
                            }
                            
                            // 设置字段值
                            field.setAccessible(true);
                            field.set(entity, Convert.convert(field.getType(), value));
                        }
                    }
                }
                
                list.add(entity);
            }
            
            return list;
        } catch (Exception e) {
            log.error("导入Excel异常{}", e.getMessage());
            throw new UtilException("导入Excel失败，请检查Excel文件格式是否正确！");
        } finally {
            IoUtil.close(is);
        }
    }
    
    /**
     * 根据表达式反向转换值
     */
    private String reverseByExp(String value, String expression, String separator) {
        if (StrUtil.isEmpty(value) || StrUtil.isEmpty(expression)) {
            return value;
        }
        
        String[] exps = expression.split(",");
        for (String exp : exps) {
            String[] items = exp.split("=");
            if (items.length == 2 && value.equals(items[1])) {
                return items[0];
            }
        }
        
        return value;
    }
}
