package com.ruoyi.project.miniapp.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import com.ruoyi.common.exception.ServiceException;

import cn.hutool.core.util.StrUtil;

/**
 * 从待翻译文档中提取纯文本，用于内容安全检测
 */
public final class MiniAppDocumentTextExtractor {

    private MiniAppDocumentTextExtractor() {
    }

    public static String extract(byte[] fileBytes, String extension) {
        if (fileBytes == null || fileBytes.length == 0) {
            return "";
        }
        if ("txt".equalsIgnoreCase(extension)) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        }
        if ("docx".equalsIgnoreCase(extension)) {
            return extractDocx(fileBytes);
        }
        return "";
    }

    private static String extractDocx(byte[] fileBytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
                XWPFDocument document = new XWPFDocument(inputStream)) {
            List<String> lines = new ArrayList<>();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    appendParagraphText(lines, paragraph.getText());
                } else if (element instanceof XWPFTable table) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                appendParagraphText(lines, paragraph.getText());
                            }
                        }
                    }
                }
            }
            return String.join("\n", lines);
        } catch (Exception e) {
            throw new ServiceException("文档内容解析失败，请更换文件后重试");
        }
    }

    private static void appendParagraphText(List<String> lines, String text) {
        if (StrUtil.isNotBlank(text)) {
            lines.add(text.trim());
        }
    }
}
