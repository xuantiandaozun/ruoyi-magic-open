package com.ruoyi.project.gen.service.impl;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.gen.domain.AsyncTaskInfo;
import com.ruoyi.project.gen.service.IAsyncTaskService;

import cn.hutool.core.util.StrUtil;

/**
 * 异步任务服务实现类
 */
@Service
public class AsyncTaskServiceImpl implements IAsyncTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskServiceImpl.class);
    
    private static final String TASK_CACHE_PREFIX = "async_task:";
    private static final long TASK_EXPIRE_TIME = 24 * 60 * 60; // 24小时过期
    
    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void saveTask(AsyncTaskInfo taskInfo) {
        try {
            String key = TASK_CACHE_PREFIX + taskInfo.getTaskId();
            String json = objectMapper.writeValueAsString(taskInfo);
            redisCache.setCacheObject(key, json, (int) TASK_EXPIRE_TIME, TimeUnit.SECONDS);
            logger.info("保存任务信息到Redis: taskId={}, status={}", taskInfo.getTaskId(), taskInfo.getStatus());
        } catch (JsonProcessingException e) {
            logger.error("保存任务信息失败", e);
            throw new RuntimeException("保存任务信息失败", e);
        }
    }
    
    @Override
    public AsyncTaskInfo getTask(String taskId) {
        try {
            String key = TASK_CACHE_PREFIX + taskId;
            String json = redisCache.getCacheObject(key);
            if (StrUtil.isBlank(json)) {
                return null;
            }
            return objectMapper.readValue(json, AsyncTaskInfo.class);
        } catch (Exception e) {
            logger.error("获取任务信息失败: taskId={}", taskId, e);
            return null;
        }
    }
    
    @Override
    public void updateTaskStatus(String taskId, String status) {
        AsyncTaskInfo taskInfo = getTask(taskId);
        if (taskInfo != null) {
            taskInfo.setStatus(status);
            if ("RUNNING".equals(status)) {
                taskInfo.setStartTime(LocalDateTime.now());
            } else if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                taskInfo.setEndTime(LocalDateTime.now());
            }
            saveTask(taskInfo);
        }
    }
    
    @Override
    public void updateTaskProgress(String taskId, Integer progress) {
        AsyncTaskInfo taskInfo = getTask(taskId);
        if (taskInfo != null) {
            taskInfo.setProgress(progress);
            saveTask(taskInfo);
        }
    }
    
    @Override
    public void updateTaskResult(String taskId, String result) {
        AsyncTaskInfo taskInfo = getTask(taskId);
        if (taskInfo != null) {
            taskInfo.setResult(result);
            taskInfo.markAsSuccess(result);
            saveTask(taskInfo);
        }
    }
    
    @Override
    public void updateTaskError(String taskId, String errorMessage) {
        AsyncTaskInfo taskInfo = getTask(taskId);
        if (taskInfo != null) {
            taskInfo.markAsFailed(errorMessage);
            saveTask(taskInfo);
        }
    }
    
    @Override
    public void deleteTask(String taskId) {
        String key = TASK_CACHE_PREFIX + taskId;
        redisCache.deleteObject(key);
        logger.info("删除任务信息: taskId={}", taskId);
    }
    
    @Override
    public boolean existsTask(String taskId) {
        String key = TASK_CACHE_PREFIX + taskId;
        return redisCache.hasKey(key);
    }
}
