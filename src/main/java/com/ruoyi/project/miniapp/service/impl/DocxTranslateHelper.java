package com.ruoyi.project.miniapp.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DocxTranslateHelper {

    private static final int SINGLE_PARA_CHAR_LIMIT = 6000;
    private static final int BATCH_GROUP_CHAR_LIMIT = 3000;
    private static final String BATCH_SEPARATOR = "\n\n---\n\n";
    private static final int CONCURRENCY = 3;
    private static final long DOCX_TRANSLATE_TIMEOUT_SECONDS = 300;

    private final LangChain4jAgentService langChain4jAgentService;

    public DocxTranslateHelper(LangChain4jAgentService langChain4jAgentService) {
        this.langChain4jAgentService = langChain4jAgentService;
    }

    public byte[] translateDocx(byte[] sourceBytes, Long modelConfigId, String sourceLanguage, String targetLanguage)
            throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(sourceBytes);
                XWPFDocument doc = new XWPFDocument(bis)) {

            List<XWPFParagraph> paragraphs = collectParagraphs(doc);
            if (paragraphs.isEmpty()) {
                log.info("docx 文档无可翻译段落，原样返回");
                return toBytes(doc);
            }

            String systemPrompt = buildSystemPrompt(sourceLanguage, targetLanguage);
            List<TranslateUnit> units = buildTranslateUnits(paragraphs);
            log.info("docx 共 {} 个段落，拆分为 {} 个翻译单元", paragraphs.size(), units.size());

            ExecutorService executor = Executors.newFixedThreadPool(Math.min(CONCURRENCY, units.size()));
            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (TranslateUnit unit : units) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        try {
                            executeUnit(unit, modelConfigId, systemPrompt);
                        } catch (Exception e) {
                            log.error("翻译单元失败: {}", e.getMessage(), e);
                        }
                    }, executor));
                }
                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(DOCX_TRANSLATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Word 文档翻译被中断", e);
                } catch (ExecutionException e) {
                    throw new IOException("Word 文档翻译失败: " + e.getCause().getMessage(), e);
                } catch (TimeoutException e) {
                    futures.forEach(future -> future.cancel(true));
                    throw new IOException("Word 文档翻译超时，请缩短文档内容或稍后重试", e);
                }
            } finally {
                executor.shutdown();
            }

            return toBytes(doc);
        }
    }

    private void executeUnit(TranslateUnit unit, Long modelConfigId, String systemPrompt) {
        if (unit.paragraphIndices.size() == 1) {
            translateSingle(unit, modelConfigId, systemPrompt);
        } else {
            translateGrouped(unit, modelConfigId, systemPrompt);
        }
    }

    private void translateSingle(TranslateUnit unit, Long modelConfigId, String systemPrompt) {
        int idx = unit.paragraphIndices.get(0);
        String text = unit.texts.get(0);
        if (StrUtil.isBlank(text)) {
            return;
        }

        String translated = callTranslate(modelConfigId, systemPrompt, text);
        translated = stripCodeFences(translated);
        if (StrUtil.isNotBlank(translated)) {
            replaceParagraphText(unit.paragraphs.get(idx), translated);
        }
    }

    private void translateGrouped(TranslateUnit unit, Long modelConfigId, String systemPrompt) {
        String combined = String.join(BATCH_SEPARATOR, unit.texts);
        String translated = callTranslate(modelConfigId, systemPrompt, combined);
        translated = stripCodeFences(translated);

        String[] parts = translated.split("\\n\\s*---\\s*\\n");
        if (parts.length != unit.texts.size()) {
            log.warn("分组翻译结果数量不匹配: 期望 {}, 实际 {}, 回退逐段翻译", unit.texts.size(), parts.length);
            for (int i = 0; i < unit.paragraphIndices.size(); i++) {
                translateSingle(new TranslateUnit(
                        List.of(unit.paragraphIndices.get(i)),
                        List.of(unit.texts.get(i)),
                        unit.paragraphs), modelConfigId, systemPrompt);
            }
            return;
        }

        for (int i = 0; i < unit.paragraphIndices.size(); i++) {
            String part = parts[i].trim();
            if (StrUtil.isNotBlank(part)) {
                replaceParagraphText(unit.paragraphs.get(unit.paragraphIndices.get(i)), part);
            }
        }
    }

    private String callTranslate(Long modelConfigId, String systemPrompt, String text) {
        String result = langChain4jAgentService.chatWithSystem(modelConfigId, systemPrompt, text);
        return result != null ? result.trim() : "";
    }

    private List<TranslateUnit> buildTranslateUnits(List<XWPFParagraph> paragraphs) {
        List<TranslateUnit> units = new ArrayList<>();
        List<Integer> groupIndices = new ArrayList<>();
        List<String> groupTexts = new ArrayList<>();
        int groupChars = 0;

        for (int i = 0; i < paragraphs.size(); i++) {
            String text = getParagraphText(paragraphs.get(i));

            if (StrUtil.isBlank(text)) {
                if (!groupIndices.isEmpty()) {
                    units.add(new TranslateUnit(new ArrayList<>(groupIndices), new ArrayList<>(groupTexts), paragraphs));
                    groupIndices.clear();
                    groupTexts.clear();
                    groupChars = 0;
                }
                continue;
            }

            if (text.length() > BATCH_GROUP_CHAR_LIMIT) {
                if (!groupIndices.isEmpty()) {
                    units.add(new TranslateUnit(new ArrayList<>(groupIndices), new ArrayList<>(groupTexts), paragraphs));
                    groupIndices.clear();
                    groupTexts.clear();
                    groupChars = 0;
                }
                String processedText = text.length() > SINGLE_PARA_CHAR_LIMIT ? splitLongParagraph(text) : text;
                units.add(new TranslateUnit(List.of(i), List.of(processedText), paragraphs));
                continue;
            }

            if (groupChars + text.length() > BATCH_GROUP_CHAR_LIMIT && !groupIndices.isEmpty()) {
                units.add(new TranslateUnit(new ArrayList<>(groupIndices), new ArrayList<>(groupTexts), paragraphs));
                groupIndices.clear();
                groupTexts.clear();
                groupChars = 0;
            }

            groupIndices.add(i);
            groupTexts.add(text);
            groupChars += text.length();
        }

        if (!groupIndices.isEmpty()) {
            units.add(new TranslateUnit(new ArrayList<>(groupIndices), new ArrayList<>(groupTexts), paragraphs));
        }

        return units;
    }

    private String buildSystemPrompt(String sourceLanguage, String targetLanguage) {
        String src = StrUtil.blankToDefault(sourceLanguage, "自动识别源语言");
        return "你是专业文档翻译引擎。将用户输入的文本翻译为目标语言。"
                + "只输出翻译结果，不要包含原文，不要代码块包裹，不要任何解释。"
                + "如果输入包含 --- 分隔的多段文本，请逐段翻译并用同样的 --- 分隔输出。"
                + "源语言：" + src + "，目标语言：" + targetLanguage + "。";
    }

    private List<XWPFParagraph> collectParagraphs(XWPFDocument doc) {
        List<XWPFParagraph> result = new ArrayList<>();
        for (IBodyElement element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                result.add((XWPFParagraph) element);
            } else if (element instanceof XWPFTable) {
                for (XWPFTableRow row : ((XWPFTable) element).getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        result.addAll(cell.getParagraphs());
                    }
                }
            }
        }
        doc.getHeaderList().forEach(h -> result.addAll(h.getParagraphs()));
        doc.getFooterList().forEach(f -> result.addAll(f.getParagraphs()));
        return result;
    }

    private void replaceParagraphText(XWPFParagraph paragraph, String translatedText) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) {
            paragraph.createRun().setText(translatedText);
            return;
        }

        CTRPr savedRPr = null;
        try {
            if (runs.get(0).getCTR().getRPr() != null) {
                savedRPr = (CTRPr) runs.get(0).getCTR().getRPr().copy();
            }
        } catch (Exception e) {
            log.debug("复制段落格式失败: {}", e.getMessage());
        }

        while (paragraph.getRuns().size() > 1) {
            paragraph.removeRun(paragraph.getRuns().size() - 1);
        }

        XWPFRun firstRun = paragraph.getRuns().get(0);
        while (firstRun.getCTR().sizeOfTArray() > 0) {
            firstRun.getCTR().removeT(firstRun.getCTR().sizeOfTArray() - 1);
        }
        firstRun.setText(translatedText, 0);

        if (savedRPr != null) {
            try {
                firstRun.getCTR().setRPr(savedRPr);
            } catch (Exception e) {
                log.debug("设置段落格式失败: {}", e.getMessage());
            }
        }
    }

    private String splitLongParagraph(String text) {
        if (text.length() <= SINGLE_PARA_CHAR_LIMIT) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + SINGLE_PARA_CHAR_LIMIT, text.length());
            if (end < text.length()) {
                int lastSentence = -1;
                for (int i = end; i > start; i--) {
                    char c = text.charAt(i);
                    if (c == '.' || c == '!' || c == '?' || c == '\u3002' || c == '\uff01' || c == '\uff1f') {
                        lastSentence = i;
                        break;
                    }
                }
                if (lastSentence > start) {
                    end = lastSentence + 1;
                }
            }
            if (sb.length() > 0) {
                sb.append(BATCH_SEPARATOR);
            }
            sb.append(text, start, end);
            start = end;
        }
        return sb.toString();
    }

    private String getParagraphText(XWPFParagraph paragraph) {
        return StrUtil.nullToDefault(paragraph.getText(), "");
    }

    private String stripCodeFences(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            } else {
                trimmed = trimmed.substring(3);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            return trimmed.trim();
        }
        return trimmed;
    }

    private byte[] toBytes(XWPFDocument doc) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.write(bos);
        return bos.toByteArray();
    }

    private static class TranslateUnit {
        final List<Integer> paragraphIndices;
        final List<String> texts;
        final List<XWPFParagraph> paragraphs;

        TranslateUnit(List<Integer> paragraphIndices, List<String> texts, List<XWPFParagraph> paragraphs) {
            this.paragraphIndices = paragraphIndices;
            this.texts = texts;
            this.paragraphs = paragraphs;
        }
    }
}
