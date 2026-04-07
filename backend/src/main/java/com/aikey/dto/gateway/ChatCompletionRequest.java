package com.aikey.dto.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI兼容的Chat Completion请求DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionRequest {

    private String model;

    private List<ChatMessage> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    private Integer n;

    private Boolean stream = false;

    private List<String> stop;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    private String user;
}
