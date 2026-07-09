package com.ruoyi.project.mediaassistant.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.mediaassistant.domain.MediaSource;
import com.ruoyi.project.mediaassistant.mapper.MediaSourceMapper;
import com.ruoyi.project.mediaassistant.service.IMediaSourceService;

@Service
public class MediaSourceServiceImpl extends ServiceImpl<MediaSourceMapper, MediaSource>
        implements IMediaSourceService {
}
