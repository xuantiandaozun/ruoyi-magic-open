package com.ruoyi.project.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.ruoyi.project.ai.domain.AiChatMessage;

/**
 * AI聊天消息Mapper接口
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 * 
 * 注意：基础的CRUD操作已由MyBatis-Flex的BaseMapper提供，
 * 此接口仅保留复杂的业务查询方法，简单查询请在Service层使用QueryWrapper实现
 */
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
    
    // 注意：以下方法已被Service层的QueryWrapper实现替代，保留此接口仅为兼容性考虑
    // 建议在Service层直接使用MyBatis-Flex的QueryWrapper进行查询
    
}