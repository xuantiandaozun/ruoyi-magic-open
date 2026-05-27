package com.ruoyi.project.miniapp.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.miniapp.domain.TranslateTask;
import com.ruoyi.project.miniapp.domain.dto.CreateTranslateTaskRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;

public interface ITranslateTaskService extends IService<TranslateTask> {
    TranslateTask createTask(CreateTranslateTaskRequest request, MiniAppLoginUser loginUser);

    TranslateTask getOwnedTask(Long taskId, MiniAppLoginUser loginUser);

    List<TranslateTask> listByOwner(MiniAppLoginUser loginUser);

    void downloadResult(Long taskId, MiniAppLoginUser loginUser, jakarta.servlet.http.HttpServletResponse response) throws Exception;
}
