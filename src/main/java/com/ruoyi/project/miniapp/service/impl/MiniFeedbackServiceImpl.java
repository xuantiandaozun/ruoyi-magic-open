package com.ruoyi.project.miniapp.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.miniapp.domain.MiniFeedback;
import com.ruoyi.project.miniapp.domain.dto.MiniFeedbackSubmitRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.mapper.MiniFeedbackMapper;
import com.ruoyi.project.miniapp.service.IMiniFeedbackService;

@Service
public class MiniFeedbackServiceImpl extends ServiceImpl<MiniFeedbackMapper, MiniFeedback>
        implements IMiniFeedbackService {

    @Override
    public MiniFeedback submit(MiniFeedbackSubmitRequest request, MiniAppLoginUser loginUser) {
        MiniFeedback feedback = new MiniFeedback();
        feedback.setMiniUserId(loginUser.getMiniUserId());
        feedback.setMiniAppId(loginUser.getMiniAppId());
        feedback.setAppCode(loginUser.getAppCode());
        feedback.setFeedbackType(StringUtils.hasText(request.getFeedbackType()) ? request.getFeedbackType() : "general");
        feedback.setContent(request.getContent().trim());
        feedback.setContact(StringUtils.hasText(request.getContact()) ? request.getContact().trim() : null);
        feedback.setStatus("pending");
        feedback.setDelFlag("0");
        save(feedback);
        return feedback;
    }

    @Override
    public List<MiniFeedback> listByOwner(MiniAppLoginUser loginUser) {
        return list(QueryWrapper.create()
                .from("mini_feedback")
                .where("mini_user_id = ?", loginUser.getMiniUserId())
                .and("mini_app_id = ?", loginUser.getMiniAppId())
                .orderBy("create_time desc"));
    }
}
