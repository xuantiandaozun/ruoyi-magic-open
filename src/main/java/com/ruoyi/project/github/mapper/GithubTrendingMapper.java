package com.ruoyi.project.github.mapper;

import com.mybatisflex.core.BaseMapper;
import com.ruoyi.project.github.domain.GithubTrending;
import com.mybatisflex.annotation.UseDataSource;

/**
 * github流行榜单Mapper接口
 * 
 * @author ruoyi
 * @date 2025-07-03 11:47:11
 */
@UseDataSource("爬虫")
public interface GithubTrendingMapper extends BaseMapper<GithubTrending>
{
    // 遵循MyBatis-Flex规范，保持Mapper接口简洁，复杂查询在Service层实现
}
