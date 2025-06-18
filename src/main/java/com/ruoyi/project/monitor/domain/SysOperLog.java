package com.ruoyi.project.monitor.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.aspectj.lang.annotation.Excel.ColumnType;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 操作日志记录表 oper_log
 * 
 * @author ruoyi
 */
@Data
@Accessors(prefix = "")
@EqualsAndHashCode(callSuper = true)
@Table("sys_oper_log")
public class SysOperLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志主键 */
    @Excel(name = "操作序号", cellType = ColumnType.NUMERIC)
    @Id(keyType = KeyType.Auto)
    public Long operId;

    /** 操作模块 */
    @Excel(name = "操作模块")
    public String title;

    /** 业务类型（0其它 1新增 2修改 3删除） */
    @Excel(name = "业务类型", readConverterExp = "0=其它,1=新增,2=修改,3=删除,4=授权,5=导出,6=导入,7=强退,8=生成代码,9=清空数据")
    public Integer businessType;

    /** 业务类型数组 */
    @Column(ignore = true)
    public Integer[] businessTypes;

    /** 请求方法 */
    @Excel(name = "请求方法")
    public String method;

    /** 请求方式 */
    @Excel(name = "请求方式")
    public String requestMethod;

    /** 操作类别（0其它 1后台用户 2手机端用户） */
    @Excel(name = "操作类别", readConverterExp = "0=其它,1=后台用户,2=手机端用户")
    public Integer operatorType;

    /** 操作人员 */
    @Excel(name = "操作人员")
    public String operName;

    /** 部门名称 */
    @Excel(name = "部门名称")
    public String deptName;

    /** 请求url */
    @Excel(name = "请求地址")
    public String operUrl;

    /** 操作地址 */
    @Excel(name = "操作地址")
    public String operIp;

    /** 操作地点 */
    @Excel(name = "操作地点")
    public String operLocation;

    /** 请求参数 */
    @Excel(name = "请求参数")
    public String operParam;

    /** 返回参数 */
    @Excel(name = "返回参数")
    public String jsonResult;

    /** 操作状态（0正常 1异常） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=异常")
    public Integer status;

    /** 错误消息 */
    @Excel(name = "错误消息")
    public String errorMsg;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "操作时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    public Date operTime;

    /** 消耗时间 */
    @Excel(name = "消耗时间", suffix = "毫秒")
    public Long costTime;
}
