package com.ruoyi.project.miniapp.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.miniapp.domain.MiniSubscribeTemplate;
import com.ruoyi.project.miniapp.domain.vo.MiniSubscribeTemplateVo;

public interface IMiniSubscribeTemplateService extends IService<MiniSubscribeTemplate> {
    List<MiniSubscribeTemplate> listEnabledByMiniAppAndScene(Long miniAppId, String sceneCode);

    List<MiniSubscribeTemplateVo> listEnabledVoByAppCodeAndScene(String appCode, String sceneCode);
}
