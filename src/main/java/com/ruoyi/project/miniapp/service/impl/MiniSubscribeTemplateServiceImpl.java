package com.ruoyi.project.miniapp.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.miniapp.domain.MiniSubscribeTemplate;
import com.ruoyi.project.miniapp.domain.vo.MiniSubscribeTemplateVo;
import com.ruoyi.project.miniapp.mapper.MiniSubscribeTemplateMapper;
import com.ruoyi.project.miniapp.service.IMiniSubscribeTemplateService;

import cn.hutool.core.util.StrUtil;

@Service
@UseDataSource("MASTER")
public class MiniSubscribeTemplateServiceImpl extends ServiceImpl<MiniSubscribeTemplateMapper, MiniSubscribeTemplate>
        implements IMiniSubscribeTemplateService {

    @Override
    public List<MiniSubscribeTemplate> listEnabledByMiniAppAndScene(Long miniAppId, String sceneCode) {
        if (miniAppId == null || StrUtil.isBlank(sceneCode)) {
            return Collections.emptyList();
        }

        QueryWrapper qw = QueryWrapper.create()
                .from("mini_subscribe_template")
                .where("mini_app_id = ?", miniAppId)
                .and("scene_code = ?", sceneCode)
                .and("enabled = 'Y'")
                .and("del_flag = '0'")
                .orderBy("sort_order asc, id asc");
        return list(qw);
    }

    @Override
    public List<MiniSubscribeTemplateVo> listEnabledVoByAppCodeAndScene(String appCode, String sceneCode) {
        if (StrUtil.isBlank(appCode)) {
            return Collections.emptyList();
        }

        QueryWrapper qw = QueryWrapper.create()
                .from("mini_subscribe_template")
                .where("app_code = ?", appCode)
                .and("enabled = 'Y'")
                .and("del_flag = '0'")
                .orderBy("sort_order asc, id asc");
        if (StrUtil.isNotBlank(sceneCode)) {
            qw.and("scene_code = ?", sceneCode);
        }

        return list(qw).stream().map(this::toVo).collect(Collectors.toList());
    }

    private MiniSubscribeTemplateVo toVo(MiniSubscribeTemplate template) {
        MiniSubscribeTemplateVo vo = new MiniSubscribeTemplateVo();
        vo.setSceneCode(template.getSceneCode());
        vo.setTemplateId(template.getTemplateId());
        vo.setTemplateNo(template.getTemplateNo());
        vo.setTitle(template.getTitle());
        vo.setPagePath(template.getPagePath());
        return vo;
    }
}
