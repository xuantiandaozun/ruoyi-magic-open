package com.ruoyi.project.feishu.domain.dto;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 飞书多维表格记录字段DTO
 * 用于封装新增/更新记录时的字段数据
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Data
@Accessors(chain = true)
public class FeishuBitableRecordFieldsDto {
    
    /**
     * 文本类型字段
     * 示例: "任务名称": "拜访潜在客户"
     */
    private String text;
    
    /**
     * 数字类型字段
     * 示例: "工时": 10, "进度": 0.25
     */
    private Number number;
    
    /**
     * 单选类型字段
     * 示例: "单选": "选项1"
     */
    private String singleSelect;
    
    /**
     * 多选类型字段
     * 示例: "多选": ["选项1", "选项2"]
     */
    private List<String> multiSelect;
    
    /**
     * 日期类型字段（毫秒级时间戳）
     * 示例: "日期": 1674206443000
     */
    private Long date;
    
    /**
     * 复选框类型字段
     * 示例: "复选框": true
     */
    private Boolean checkbox;
    
    /**
     * 条码类型字段
     */
    private String barcode;
    
    /**
     * 人员类型字段
     * 示例: "人员": [{"id": "ou_xxxxxx"}]
     */
    private List<PersonField> person;
    
    /**
     * 电话号码类型字段
     * 示例: "电话号码": "1302616xxxx"
     */
    private String phoneNumber;
    
    /**
     * 超链接类型字段
     * 示例: "超链接": {"text": "飞书多维表格官网", "link": "https://www.feishu.cn/product/base"}
     */
    private LinkField link;
    
    /**
     * 附件类型字段
     * 示例: "附件": ["file_token1", "file_token2"]
     */
    private List<String> attachment;
    
    /**
     * 单向关联类型字段
     * 示例: "单向关联": ["record_id1", "record_id2"]
     */
    private List<String> lookup;
    
    /**
     * 双向关联类型字段
     * 示例: "双向关联": ["record_id1", "record_id2"]
     */
    private List<String> biLookup;
    
    /**
     * 地理位置类型字段
     * 示例: "地理位置": "116.397755,39.903179"
     */
    private String location;
    
    /**
     * 货币类型字段
     */
    private Number currency;
    
    /**
     * 评分类型字段
     */
    private Integer rating;
    
    /**
     * 群组类型字段
     */
    private List<String> group;
    
    /**
     * 人员字段内部类
     */
    @Data
    public static class PersonField {
        /** 用户ID */
        private String id;
    }
    
    /**
     * 超链接字段内部类
     */
    @Data
    public static class LinkField {
        /** 显示文本 */
        private String text;
        /** 链接地址 */
        private String link;
    }
}