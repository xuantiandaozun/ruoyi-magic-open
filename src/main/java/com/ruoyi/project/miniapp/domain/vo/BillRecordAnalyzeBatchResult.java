package com.ruoyi.project.miniapp.domain.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class BillRecordAnalyzeBatchResult {

    /** 识别出的账单列表 */
    private List<BillRecordAnalyzeResult> records = new ArrayList<>();

    /** 原始输入文本 */
    private String rawText;

    /** 识别条数 */
    private Integer total;
}
