package com.ruoyi.project.mediaassistant.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.mediaassistant.domain.MediaContentDraft;
import com.ruoyi.project.mediaassistant.mapper.MediaContentDraftMapper;
import com.ruoyi.project.mediaassistant.service.IMediaContentDraftService;

@Service
public class MediaContentDraftServiceImpl extends ServiceImpl<MediaContentDraftMapper, MediaContentDraft>
        implements IMediaContentDraftService {
}
