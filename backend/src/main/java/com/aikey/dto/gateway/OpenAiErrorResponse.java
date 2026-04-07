package com.aikey.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI兼容的错误响应DTO
 *
 * <p>格式：{"error": {"message": "...", "type": "...", "code": "..."}}</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiErrorResponse {

    private ErrorBody error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorBody {
        private String message;
        private String type;
        private String code;
    }

    public static OpenAiErrorResponse of(String message, String type, String code) {
        return OpenAiErrorResponse.builder()
                .error(ErrorBody.builder()
                        .message(message)
                        .type(type)
                        .code(code)
                        .build())
                .build();
    }
}
