package com.aikey.dto.modelgroup;

import lombok.Data;

import java.util.List;

/**
 * 更新模型分组请求（所有字段可选，仅更新非null字段）
 */
@Data
public class ModelGroupUpdateRequest {

    private String groupName;

    private String description;

    private List<Long> modelIds;

    private Integer status;
}
