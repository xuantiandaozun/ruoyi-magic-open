package com.ruoyi.project.wechatmp.service;

import java.util.Map;

/**
 * 博客同步到微信公众号草稿箱
 */
public interface IWechatMpDraftService {

    /**
     * 将指定博客同步为微信公众号草稿
     *
     * @param blogId 博客 ID
     * @return 同步结果（draftMediaId、thumbMediaId、contentSourceUrl 等）
     */
    Map<String, Object> syncBlogToDraft(String blogId);
}
