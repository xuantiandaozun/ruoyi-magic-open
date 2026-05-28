package com.ruoyi.project.miniapp.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.miniapp.domain.MiniFeedback;
import com.ruoyi.project.miniapp.domain.dto.MiniFeedbackSubmitRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;

public interface IMiniFeedbackService extends IService<MiniFeedback> {
    MiniFeedback submit(MiniFeedbackSubmitRequest request, MiniAppLoginUser loginUser);

    List<MiniFeedback> listByOwner(MiniAppLoginUser loginUser);
}
