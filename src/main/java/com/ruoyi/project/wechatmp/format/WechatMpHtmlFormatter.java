package com.ruoyi.project.wechatmp.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将通用 Markdown HTML 转为微信公众号可识别的内联样式 HTML（无第三方 HTML 库依赖）。
 */
public final class WechatMpHtmlFormatter {

    private static final String ACCENT = "#576b95";
    private static final String MONO = "Menlo, Consolas, \"Courier New\", monospace";

    private static final String STYLE_P =
        "margin: 16px 0; font-size: 16px; line-height: 1.75; color: #3f3f3f; "
            + "letter-spacing: 0.5px; text-align: justify;";
    private static final String STYLE_H2 =
        "margin: 28px 0 14px; font-size: 19px; font-weight: bold; color: #333; "
            + "padding-bottom: 6px; border-bottom: 2px solid " + ACCENT + ";";
    private static final String STYLE_H3 =
        "margin: 22px 0 10px; font-size: 17px; font-weight: bold; color: #444;";
    private static final String STYLE_H4 =
        "margin: 18px 0 8px; font-size: 16px; font-weight: bold; color: #555;";
    private static final String STYLE_BLOCKQUOTE =
        "margin: 16px 0; padding: 12px 16px; border-left: 4px solid " + ACCENT
            + "; background: #f7f8fa; color: #555; font-size: 15px; line-height: 1.7;";
    private static final String STYLE_STRONG = "font-weight: bold; color: #333;";
    private static final String STYLE_EM = "font-style: italic; color: #666;";
    private static final String STYLE_CODE_INLINE =
        "padding: 2px 6px; background: #f5f5f5; color: #c7254e; border-radius: 3px; "
            + "font-family: " + MONO + "; font-size: 14px;";
    private static final String STYLE_PRE_WRAP =
        "margin: 16px 0; padding: 16px; background: #f6f8fa; border-radius: 6px; overflow-x: auto;";
    private static final String STYLE_PRE_CODE =
        "display: block; font-family: " + MONO + "; font-size: 13px; line-height: 1.6; "
            + "color: #333; white-space: pre-wrap; word-break: break-all;";
    private static final String STYLE_IMG =
        "display: block; max-width: 100%; height: auto; margin: 16px auto; border-radius: 4px;";
    private static final String STYLE_HR =
        "margin: 24px 0; border: none; border-top: 1px solid #e8e8e8;";
    private static final String STYLE_TABLE =
        "width: 100%; margin: 16px 0; border-collapse: collapse; font-size: 14px;";
    private static final String STYLE_TH =
        "padding: 8px 12px; border: 1px solid #dfe2e5; background: #f6f8fa; font-weight: bold; text-align: left;";
    private static final String STYLE_TD =
        "padding: 8px 12px; border: 1px solid #dfe2e5; color: #3f3f3f;";
    private static final String STYLE_LINK =
        "color: " + ACCENT + "; text-decoration: none; border-bottom: 1px solid " + ACCENT + ";";
    private static final String STYLE_LIST_ITEM =
        "margin: 8px 0; padding-left: 4px; font-size: 16px; line-height: 1.75; color: #3f3f3f;";

    private static final Pattern PANGU =
        Pattern.compile("([\\u4e00-\\u9fa5])([A-Za-z0-9#])|([A-Za-z0-9#])([\\u4e00-\\u9fa5])");
    private static final Pattern UNSAFE_TAGS =
        Pattern.compile("(?is)<(script|style|iframe|object|embed)\\b[^>]*>.*?</\\1>");
    private static final Pattern UNSAFE_SELF =
        Pattern.compile("(?is)<(script|style|iframe|object|embed)\\b[^>]*/>");
    private static final Pattern PRE_BLOCK =
        Pattern.compile("(?is)<pre>\\s*<code[^>]*>(.*?)</code>\\s*</pre>");
    private static final Pattern PRE_PLAIN =
        Pattern.compile("(?is)<pre[^>]*>(.*?)</pre>");
    private static final Pattern LIST_BLOCK =
        Pattern.compile("(?is)<(ul|ol)\\b[^>]*>(.*?)</\\1>");
    private static final Pattern LIST_ITEM =
        Pattern.compile("(?is)<li\\b[^>]*>(.*?)</li>");
    private static final Pattern LINK_TAG =
        Pattern.compile("(?is)<a\\b([^>]*?)href=[\"']([^\"']+)[\"']([^>]*?)>(.*?)</a>");
    private static final Pattern OPEN_TAG =
        Pattern.compile("(?i)<([a-z][a-z0-9]*)\\b([^>]*)>");
    private static final Pattern CODE_PLACEHOLDER =
        Pattern.compile("(?is)<!--WECHAT_CODE_(\\d+)-->");

    private WechatMpHtmlFormatter() {
    }

    public static String format(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        String html = rawHtml.trim();
        html = UNSAFE_TAGS.matcher(html).replaceAll("");
        html = UNSAFE_SELF.matcher(html).replaceAll("");

        List<String> codeBlocks = new ArrayList<>();
        html = stashCodeBlocks(html, codeBlocks);
        html = convertPreBlocks(html);
        html = convertLists(html);
        html = styleLinks(html);
        html = applyOpenTagStyles(html);
        html = restoreCodeBlocks(html, codeBlocks);
        html = applyPanguOutsideCode(html);
        return html.trim();
    }

    private static String stashCodeBlocks(String html, List<String> codeBlocks) {
        Matcher matcher = PRE_BLOCK.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            int index = codeBlocks.size();
            codeBlocks.add(matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("<!--WECHAT_CODE_" + index + "-->"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String restoreCodeBlocks(String html, List<String> codeBlocks) {
        Matcher matcher = CODE_PLACEHOLDER.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String code = index < codeBlocks.size() ? escapeHtml(codeBlocks.get(index)) : "";
            String block = "<section style=\"" + STYLE_PRE_WRAP + "\"><code style=\"" + STYLE_PRE_CODE + "\">"
                + code + "</code></section>";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(block));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String convertPreBlocks(String html) {
        Matcher matcher = PRE_PLAIN.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String inner = matcher.group(1).replaceAll("(?is)</?code[^>]*>", "");
            String block = "<section style=\"" + STYLE_PRE_WRAP + "\"><code style=\"" + STYLE_PRE_CODE + "\">"
                + inner + "</code></section>";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(block));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String convertLists(String html) {
        Matcher listMatcher = LIST_BLOCK.matcher(html);
        StringBuffer listBuffer = new StringBuffer();
        while (listMatcher.find()) {
            boolean ordered = "ol".equalsIgnoreCase(listMatcher.group(1));
            String body = listMatcher.group(2);
            Matcher itemMatcher = LIST_ITEM.matcher(body);
            StringBuilder section = new StringBuilder("<section style=\"margin: 12px 0;\">");
            int index = 1;
            while (itemMatcher.find()) {
                String prefix = ordered ? (index++ + ". ") : "• ";
                section.append("<p style=\"").append(STYLE_LIST_ITEM).append("\">")
                    .append(prefix).append(itemMatcher.group(1).trim()).append("</p>");
            }
            section.append("</section>");
            listMatcher.appendReplacement(listBuffer, Matcher.quoteReplacement(section.toString()));
        }
        listMatcher.appendTail(listBuffer);
        return listBuffer.toString();
    }

    private static String styleLinks(String html) {
        Matcher matcher = LINK_TAG.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String href = matcher.group(2);
            String text = matcher.group(4);
            String replacement;
            if (href.startsWith("#")) {
                replacement = text;
            } else {
                String suffix = (text.contains(href) || !href.startsWith("http"))
                    ? ""
                    : " (" + shortenUrl(href) + ")";
                replacement = "<a style=\"" + STYLE_LINK + "\" href=\"" + href + "\">" + text + suffix + "</a>";
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String applyOpenTagStyles(String html) {
        Matcher matcher = OPEN_TAG.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase();
            String attrs = matcher.group(2);
            if (attrs.contains("style=")) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String style = styleForTag(tag);
            if (style == null) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String replacement = "<" + tag + " style=\"" + style + "\"" + attrs + ">";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String styleForTag(String tag) {
        return switch (tag) {
            case "h1", "h2" -> STYLE_H2;
            case "h3" -> STYLE_H3;
            case "h4", "h5", "h6" -> STYLE_H4;
            case "p" -> STYLE_P;
            case "blockquote" -> STYLE_BLOCKQUOTE;
            case "strong", "b" -> STYLE_STRONG;
            case "em", "i" -> STYLE_EM;
            case "code" -> STYLE_CODE_INLINE;
            case "img" -> STYLE_IMG;
            case "hr" -> STYLE_HR;
            case "table" -> STYLE_TABLE;
            case "th" -> STYLE_TH;
            case "td" -> STYLE_TD;
            default -> null;
        };
    }

    private static String applyPanguOutsideCode(String html) {
        Matcher sectionMatcher = Pattern.compile("(?is)(<section style=\"[^\"]*\"[^>]*><code[^>]*>)(.*?)(</code></section>)")
            .matcher(html);
        StringBuffer result = new StringBuffer();
        int last = 0;
        while (sectionMatcher.find()) {
            result.append(applyPangu(html.substring(last, sectionMatcher.start())));
            result.append(sectionMatcher.group(1));
            result.append(sectionMatcher.group(2));
            result.append(sectionMatcher.group(3));
            last = sectionMatcher.end();
        }
        result.append(applyPangu(html.substring(last)));
        return result.toString();
    }

    private static String applyPangu(String text) {
        Matcher matcher = PANGU.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + " " + matcher.group(2)));
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(3) + " " + matcher.group(4)));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static String shortenUrl(String href) {
        return href.length() <= 48 ? href : href.substring(0, 45) + "...";
    }
}
