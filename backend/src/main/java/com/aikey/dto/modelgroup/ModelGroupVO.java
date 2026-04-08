package com.aikey.dto.modelgroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型分组视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelGroupVO {

    private Long id;
    private String groupName;
    private String description;
    /** 原始模型ID列表 */
    private List<Long> modelIds;
    /** 模型简要信息列表（名称+编码） */
    private List<ModelSimpleVO> models;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 模型简要信息（用于分组详情中的模型展示）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelSimpleVO {
        private Long id;
        private String modelName;
        private String modelCode;
    }
}
