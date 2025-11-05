package com.ruoyi.project.ai.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiBlogProductionRecord;
import com.ruoyi.project.ai.mapper.AiBlogProductionRecordMapper;
import com.ruoyi.project.ai.service.IAiBlogProductionRecordService;

/**
 * AI博客生产记录Service业务层处理
 * 
 * @author ruoyi
 */
@Service
@UseDataSource("MASTER")
public class AiBlogProductionRecordServiceImpl extends ServiceImpl<AiBlogProductionRecordMapper, AiBlogProductionRecord>
        implements IAiBlogProductionRecordService {

    @Override
    public List<AiBlogProductionRecord> listByRepoUrl(String repoUrl) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where(new QueryColumn("repo_url").eq(repoUrl))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("production_time", false);
        return list(qw);
    }

    @Override
    public List<AiBlogProductionRecord> listByProductionType(String productionType) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where(new QueryColumn("production_type").eq(productionType))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("production_time", false);
        return list(qw);
    }

    @Override
    public AiBlogProductionRecord getByBlogId(Long blogId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where(new QueryColumn("blog_id").eq(blogId))
            .and(new QueryColumn("del_flag").eq("0"));
        return getOne(qw);
    }

    @Override
    public List<AiBlogProductionRecord> listByTargetDate(Date targetDate) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where(new QueryColumn("target_date").eq(targetDate))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("production_time", false);
        return list(qw);
    }

    @Override
    public List<AiBlogProductionRecord> listFailedRecords() {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where(new QueryColumn("status").eq("2"))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("production_time", false);
        return list(qw);
    }

    @Override
    public List<AiBlogProductionRecord> listRunningRecords() {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_blog_production_record")
            .where(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("production_time", false);
        return list(qw);
    }
}
