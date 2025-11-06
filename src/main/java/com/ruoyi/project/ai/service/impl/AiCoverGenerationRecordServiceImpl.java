package com.ruoyi.project.ai.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiCoverGenerationRecord;
import com.ruoyi.project.ai.mapper.AiCoverGenerationRecordMapper;
import com.ruoyi.project.ai.service.IAiCoverGenerationRecordService;

/**
 * AI生图记录Service业务层处理
 * 
 * @author ruoyi
 */
@Service
@UseDataSource("MASTER")
public class AiCoverGenerationRecordServiceImpl extends ServiceImpl<AiCoverGenerationRecordMapper, AiCoverGenerationRecord>
        implements IAiCoverGenerationRecordService {

    @Override
    public List<AiCoverGenerationRecord> listByBlogId(Long blogId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_cover_generation_record")
            .where(new QueryColumn("blog_id").eq(blogId))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("generation_time", false);
        return list(qw);
    }

    @Override
    public List<AiCoverGenerationRecord> listByCoverType(String coverType) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_cover_generation_record")
            .where(new QueryColumn("cover_type").eq(coverType))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("generation_time", false);
        return list(qw);
    }

    @Override
    public List<AiCoverGenerationRecord> listByCategory(String category) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_cover_generation_record")
            .where(new QueryColumn("category").eq(category))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("generation_time", false);
        return list(qw);
    }

    @Override
    public List<AiCoverGenerationRecord> listFailedRecords() {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_cover_generation_record")
            .where(new QueryColumn("generation_status").eq("2"))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("generation_time", false);
        return list(qw);
    }

    @Override
    public List<AiCoverGenerationRecord> listUsedRecords() {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_cover_generation_record")
            .where(new QueryColumn("generation_status").eq("1"))
            .and(new QueryColumn("is_used").eq("1"))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("generation_time", false);
        return list(qw);
    }

    @Override
    public List<AiCoverGenerationRecord> listUnusedRecords() {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_cover_generation_record")
            .where(new QueryColumn("generation_status").eq("1"))
            .and(new QueryColumn("is_used").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("generation_time", false);
        return list(qw);
    }

    @Override
    public List<AiCoverGenerationRecord> listReusableGenericCovers(String category) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_cover_generation_record")
            .where(new QueryColumn("cover_type").eq("0"))
            .and(new QueryColumn("category").eq(category))
            .and(new QueryColumn("generation_status").eq("1"))
            .and(new QueryColumn("is_used").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("generation_time", false);
        return list(qw);
    }

    @Override
    public List<AiCoverGenerationRecord> listByPrompt(String prompt) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_cover_generation_record")
            .where(new QueryColumn("prompt").like(prompt))
            .and(new QueryColumn("generation_status").eq("1"))
            .and(new QueryColumn("is_used").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy("generation_time", false);
        return list(qw);
    }
}
