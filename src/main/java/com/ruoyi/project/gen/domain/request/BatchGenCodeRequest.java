package com.ruoyi.project.gen.domain.request;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 批量生成代码请求对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "批量生成代码请求")
public class BatchGenCodeRequest {

    /**
     * 表ID数组
     */
    @Schema(description = "表ID数组")
    @NotEmpty(message = "表ID数组不能为空")
    private List<Long> tableIds;

    /**
     * 生成类型：all-全部生成，java-只生成Java代码，vue-只生成Vue代码
     */
    @Schema(description = "生成类型：all-全部生成，java-只生成Java代码，vue-只生成Vue代码")
    @NotNull(message = "生成类型不能为空")
    private String genType;
}