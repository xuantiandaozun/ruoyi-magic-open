package com.ruoyi.project.wechatmp.service.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.service.IBlogService;
import com.ruoyi.project.wechatmp.client.WechatMpClient;
import com.ruoyi.project.wechatmp.config.WechatMpProperties;
import com.ruoyi.project.wechatmp.format.WechatMpHtmlFormatter;
import com.ruoyi.project.wechatmp.service.IWechatMpDraftService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.RequiredArgsConstructor;

/**
 * 博客内容同步微信公众号草稿箱
 */
@Service
@RequiredArgsConstructor
public class WechatMpDraftServiceImpl implements IWechatMpDraftService {

    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[([^\\]]*)]\\(([^)]+)\\)");
    private static final Pattern HTML_IMAGE = Pattern.compile(
        "(<img\\b[^>]*\\bsrc=[\"'])([^\"']+)([\"'][^>]*>)",
        Pattern.CASE_INSENSITIVE
    );

    private final WechatMpProperties properties;
    private final WechatMpClient wechatMpClient;
    private final IBlogService blogService;

    @Override
    public Map<String, Object> syncBlogToDraft(String blogId) {
        if (!properties.isConfigured()) {
            throw new ServiceException(
                "微信公众号未配置，请在 application-dev.yml 中设置 wechat.mp.enabled=true 及 app-id、app-secret");
        }

        Blog blog = blogService.getById(blogId);
        if (blog == null) {
            throw new ServiceException("博客不存在");
        }
        if (StrUtil.isBlank(blog.getTitle())) {
            throw new ServiceException("博客标题不能为空");
        }
        if (StrUtil.isBlank(blog.getContent())) {
            throw new ServiceException("博客内容不能为空，请先在编辑器中填写");
        }
        if (StrUtil.isBlank(blog.getCoverImage())) {
            throw new ServiceException("请先设置封面图片，微信公众号草稿需要封面素材");
        }

        String markdown = replaceMarkdownImages(blog.getContent());
        String contentHtml = markdownToHtml(markdown);
        contentHtml = replaceHtmlImages(contentHtml);
        contentHtml = WechatMpHtmlFormatter.format(contentHtml);

        String thumbMediaId = uploadRemoteImageAsCover(blog.getCoverImage());
        String digest = StrUtil.isNotBlank(blog.getSummary())
            ? truncate(blog.getSummary(), 120)
            : plainTextExcerpt(contentHtml, 120);
        String contentSourceUrl = buildContentSourceUrl(blogId);

        Map<String, Object> article = new HashMap<>();
        article.put("article_type", "news");
        article.put("title", blog.getTitle());
        article.put("author", StrUtil.blankToDefault(properties.getAuthor(), ""));
        article.put("digest", digest);
        article.put("content", contentHtml);
        article.put("content_source_url", contentSourceUrl);
        article.put("thumb_media_id", thumbMediaId);
        article.put("need_open_comment", 0);
        article.put("only_fans_can_comment", 0);

        Map<String, Object> payload = Map.of("articles", List.of(article));
        String draftMediaId = wechatMpClient.addDraft(payload);

        Map<String, Object> result = new HashMap<>();
        result.put("draftMediaId", draftMediaId);
        result.put("thumbMediaId", thumbMediaId);
        result.put("contentSourceUrl", contentSourceUrl);
        result.put("title", blog.getTitle());
        return result;
    }

    private String buildContentSourceUrl(String blogId) {
        String base = properties.getPublicBaseUrl();
        if (StrUtil.isBlank(base)) {
            return "";
        }
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return base + "/api/blog/articles/detail/" + blogId;
    }

    private String markdownToHtml(String markdown) {
        List<Extension> extensions = List.of(TablesExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    private String replaceMarkdownImages(String source) {
        Matcher matcher = MARKDOWN_IMAGE.matcher(source);
        StringBuffer buffer = new StringBuffer();
        Map<String, String> cache = new HashMap<>();
        while (matcher.find()) {
            String alt = matcher.group(1);
            String rawUrl = matcher.group(2).trim();
            String wechatUrl = resolveWechatImageUrl(rawUrl, cache);
            String replacement = wechatUrl != null
                ? "![" + alt + "](" + wechatUrl + ")"
                : matcher.group(0);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceHtmlImages(String source) {
        Matcher matcher = HTML_IMAGE.matcher(source);
        StringBuffer buffer = new StringBuffer();
        Map<String, String> cache = new HashMap<>();
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String rawUrl = matcher.group(2).trim();
            String suffix = matcher.group(3);
            String wechatUrl = resolveWechatImageUrl(rawUrl, cache);
            String replacement = wechatUrl != null
                ? prefix + wechatUrl + suffix
                : matcher.group(0);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String resolveWechatImageUrl(String rawUrl, Map<String, String> cache) {
        if (!isRemoteUrl(rawUrl)) {
            return null;
        }
        return cache.computeIfAbsent(rawUrl, this::uploadRemoteImageAsInline);
    }

    private String uploadRemoteImageAsInline(String imageUrl) {
        ImagePayload payload = downloadImage(imageUrl);
        return wechatMpClient.uploadInlineImage(payload.bytes(), payload.fileName());
    }

    private String uploadRemoteImageAsCover(String imageUrl) {
        ImagePayload payload = downloadImage(imageUrl);
        return wechatMpClient.uploadCoverMaterial(payload.bytes(), payload.fileName());
    }

    private ImagePayload downloadImage(String imageUrl) {
        HttpResponse response = HttpRequest.get(imageUrl)
            .timeout(120_000)
            .execute();
        if (!response.isOk()) {
            throw new ServiceException("下载图片失败: " + imageUrl + " (HTTP " + response.getStatus() + ")");
        }
        byte[] bytes = response.bodyBytes();
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException("图片内容为空: " + imageUrl);
        }
        String fileName = guessFileName(imageUrl, response);
        return new ImagePayload(bytes, fileName);
    }

    private String guessFileName(String imageUrl, HttpResponse response) {
        try {
            String path = URI.create(imageUrl).getPath();
            if (StrUtil.isNotBlank(path) && path.contains(".")) {
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (name.length() <= 120) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        String contentType = response.header("Content-Type");
        if (contentType != null && contentType.contains("png")) {
            return "cover.png";
        }
        if (contentType != null && contentType.contains("gif")) {
            return "cover.gif";
        }
        return "cover.jpg";
    }

    private boolean isRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private String plainTextExcerpt(String html, int limit) {
        String text = html.replaceAll("<[^>]+>", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return truncate(text, limit);
    }

    private String truncate(String text, int limit) {
        if (text == null) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit);
    }

    private record ImagePayload(byte[] bytes, String fileName) {
    }
}
