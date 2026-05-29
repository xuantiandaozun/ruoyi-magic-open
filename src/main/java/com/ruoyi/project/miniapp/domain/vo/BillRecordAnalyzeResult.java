package com.ruoyi.project.miniapp.domain.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class BillRecordAnalyzeResult {

    /** 类型（0支出 1收入） */
    private String recordType;

    /** 金额 */
    private BigDecimal amount;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 分类图标 */
    private String categoryIcon;

    /** 账户ID */
    private Long accountId;

    /** 账户名称 */
    private String accountName;

    /** 记账日期 yyyy-MM-dd */
    private String recordDate;

    /** 备注 */
    private String remark;

    /** 原始输入文本 */
    private String rawText;

    /** 是否检测到可能重复的历史记录 */
    private Boolean duplicateDetected;

    /** 匹配到的历史记录 ID */
    private Long duplicateRecordId;

    /** 重复提示文案，如「午饭已记录」 */
    private String duplicateHint;

    /** 历史记录金额 */
    private BigDecimal duplicateAmount;

    /** 历史记录备注 */
    private String duplicateRemark;
}
