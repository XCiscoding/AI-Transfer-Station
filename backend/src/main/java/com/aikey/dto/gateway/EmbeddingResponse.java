package com.aikey.dto.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI兼容的Embedding响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingResponse {

    private String object;

    private List<EmbeddingData> data;

    private String model;

    private UsageInfo usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddingData {
        private String object;
        private List<Double> embedding;
        private Integer index;
    }
}
