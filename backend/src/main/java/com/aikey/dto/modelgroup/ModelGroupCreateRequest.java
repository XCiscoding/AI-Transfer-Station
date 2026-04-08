package com.aikey.dto.modelgroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建模型分组请求
 */
@Data
public class ModelGroupCreateRequest {

    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    private String description;

    @NotNull(message = "模型ID列表不能为null")
    @NotEmpty(message = "模型ID列表不能为空")
    private List<Long> modelIds;
}
