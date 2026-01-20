package com.ruoyi.project.ai.tool.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.domain.BlogEn;
import com.ruoyi.project.article.service.IBlogEnService;
import com.ruoyi.project.article.service.IBlogService;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的博客封面更新工具
 * 用于更新已保存博客的封面图片
 * 
 * @author ruoyi-magic
 * @date 2024-12-04
 */
@Component
public class BlogCoverUpdateLangChain4jTool implements LangChain4jTool {

    @Autowired
    private IBlogService blogService;

    @Autowired
    private IBlogEnService blogEnService;

    @Override
    public String getToolName() {
        return "blog_cover_update";
    }

    @Override
    public String getToolDescription() {
        return "更新博客封面图片URL（中英文博客共用同一封面）。传入任意一个博客ID即可，系统会自动同步更新绑定的中英文博客封面";
    }

    @Override
    public ToolSpecification getToolSpecification() {
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
                .addStringProperty("coverImageUrl", "封面图片URL，必填")
                .addStringProperty("zhBlogId", "中文博客ID，可选，传入则更新中文博客封面")
                .addStringProperty("enBlogId", "英文博客ID，可选，传入则更新英文博客封面")
                .required("coverImageUrl")
                .build();

        return ToolSpecification.builder()
                .name(getToolName())
                .description(getToolDescription())
                .parameters(parametersSchema)
                .build();
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        // 获取封面图片URL
        String coverImageUrl = getStringParameter(parameters, "coverImageUrl");

        if (StrUtil.isBlank(coverImageUrl)) {
            return ToolExecutionResult.failure("update", "封面图片URL不能为空");
        }

        String zhBlogId = getStringParameter(parameters, "zhBlogId");
        String enBlogId = getStringParameter(parameters, "enBlogId");

        // 至少需要一个博客ID
        if (StrUtil.isBlank(zhBlogId) && StrUtil.isBlank(enBlogId)) {
            return ToolExecutionResult.failure("update", "至少需要提供一个博客ID（zhBlogId或enBlogId）");
        }

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("coverImageUrl", coverImageUrl);

        StringBuilder successMsg = new StringBuilder();
        boolean hasError = false;

        // 更新中文博客封面
        if (StrUtil.isNotBlank(zhBlogId)) {
            try {
                Blog blog = blogService.getById(zhBlogId);
                if (blog == null) {
                    resultData.put("zhBlogError", "中文博客不存在，ID: " + zhBlogId);
                    hasError = true;
                } else {
                    blog.setCoverImage(coverImageUrl);
                    boolean success = blogService.updateById(blog);
                    if (success) {
                        resultData.put("zhBlogId", zhBlogId);
                        resultData.put("zhBlogTitle", blog.getTitle());
                        successMsg.append("中文博客封面更新成功（ID: ").append(zhBlogId).append("）");

                        // 如果没有传英文博客ID，尝试根据中文博客ID查找绑定的英文博客
                        if (StrUtil.isBlank(enBlogId)) {
                            try {
                                QueryWrapper queryWrapper = QueryWrapper.create()
                                        .eq("zh_blog_id", zhBlogId);
                                BlogEn boundBlogEn = blogEnService.getOne(queryWrapper);
                                if (boundBlogEn != null) {
                                    enBlogId = boundBlogEn.getBlogId();
                                    resultData.put("autoFoundEnBlog", true);
                                    resultData.put("autoFoundEnBlogId", enBlogId);
                                }
                            } catch (Exception e) {
                                // 查找绑定的英文博客失败不影响中文博客的更新
                                resultData.put("enBlogWarning", "查找绑定的英文博客时发生错误: " + e.getMessage());
                            }
                        }
                    } else {
                        resultData.put("zhBlogError", "中文博客封面更新失败");
                        hasError = true;
                    }
                }
            } catch (Exception e) {
                resultData.put("zhBlogError", "更新中文博客封面时发生错误: " + e.getMessage());
                hasError = true;
            }
        }

        // 更新英文博客封面
        if (StrUtil.isNotBlank(enBlogId)) {
            try {
                BlogEn blogEn = blogEnService.getById(enBlogId);
                if (blogEn == null) {
                    resultData.put("enBlogError", "英文博客不存在，ID: " + enBlogId);
                    hasError = true;
                } else {
                    blogEn.setCoverImage(coverImageUrl);
                    boolean success = blogEnService.updateById(blogEn);
                    if (success) {
                        resultData.put("enBlogId", enBlogId);
                        resultData.put("enBlogTitle", blogEn.getTitle());
                        if (successMsg.length() > 0) {
                            successMsg.append("；");
                        }
                        successMsg.append("英文博客封面更新成功（ID: ").append(enBlogId).append("）");

                        // 如果没有传中文博客ID，尝试通过英文博客的zhBlogId字段反向查找中文博客并更新
                        if (StrUtil.isBlank(zhBlogId) && blogEn.getZhBlogId() != null) {
                            try {
                                Blog boundBlog = blogService.getById(blogEn.getZhBlogId());
                                if (boundBlog != null) {
                                    boundBlog.setCoverImage(coverImageUrl);
                                    boolean zhSuccess = blogService.updateById(boundBlog);
                                    if (zhSuccess) {
                                        resultData.put("autoFoundZhBlog", true);
                                        resultData.put("autoFoundZhBlogId", blogEn.getZhBlogId());
                                        resultData.put("autoFoundZhBlogTitle", boundBlog.getTitle());
                                        successMsg.append("；自动同步更新中文博客封面（ID: ").append(blogEn.getZhBlogId())
                                                .append("）");
                                    }
                                }
                            } catch (Exception e) {
                                // 反向查找中文博客失败不影响英文博客的更新
                                resultData.put("zhBlogWarning", "自动同步中文博客封面时发生错误: " + e.getMessage());
                            }
                        }
                    } else {
                        resultData.put("enBlogError", "英文博客封面更新失败");
                        hasError = true;
                    }
                }
            } catch (Exception e) {
                resultData.put("enBlogError", "更新英文博客封面时发生错误: " + e.getMessage());
                hasError = true;
            }
        }

        if (successMsg.length() > 0) {
            if (hasError) {
                return ToolExecutionResult.operationSuccess(resultData, "部分成功：" + successMsg.toString());
            } else {
                return ToolExecutionResult.operationSuccess(resultData, successMsg.toString());
            }
        } else {
            return ToolExecutionResult.failure("update", "封面更新失败，请检查博客ID是否正确");
        }
    }

    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }

        // 验证封面URL
        String coverImageUrl = getStringParameter(parameters, "coverImageUrl");
        if (StrUtil.isBlank(coverImageUrl)) {
            return false;
        }

        // 至少需要一个博客ID
        String zhBlogId = getStringParameter(parameters, "zhBlogId");
        String enBlogId = getStringParameter(parameters, "enBlogId");

        return StrUtil.isNotBlank(zhBlogId) || StrUtil.isNotBlank(enBlogId);
    }

    @Override
    public String getUsageExample() {
        return """
                示例用法：
                1. 只更新中文博客封面（如果有绑定的英文博客，会自动更新）：
                   {"coverImageUrl": "https://example.com/cover.png", "zhBlogId": "123"}

                2. 只更新英文博客封面：
                   {"coverImageUrl": "https://example.com/cover.png", "enBlogId": "456"}

                3. 同时更新中英文博客封面：
                   {"coverImageUrl": "https://example.com/cover.png", "zhBlogId": "123", "enBlogId": "456"}

                参数说明：
                - coverImageUrl: 必填，封面图片的URL地址
                - zhBlogId: 可选，中文博客的ID
                - enBlogId: 可选，英文博客的ID

                注意：
                - 至少需要提供 zhBlogId 或 enBlogId 中的一个
                - 当只传入 zhBlogId 时，系统会自动查找绑定的英文博客并同时更新其封面
                - 当只传入 enBlogId 时，系统会自动查找绑定的中文博客并同时更新其封面
                - 中英文博客共用同一个封面，只需传入任意一个博客ID即可完成双向同步
                """;
    }

    private String getStringParameter(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        return value == null ? null : StrUtil.trimToNull(Convert.toStr(value));
    }
}
