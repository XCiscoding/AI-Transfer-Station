package com.aikey.dto.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI兼容的消息DTO
 *
 * <p>content 使用 JsonNode 类型，兼容纯文本格式（String）和多模态格式（Array）：
 * <ul>
 *   <li>文本: "你好"</li>
 *   <li>多模态: [{"type":"text","text":"你好"}]</li>
 * </ul>
 * 转发时原样序列化，由上游厂商负责解析。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {

    private String role;

    /** 兼容 String（文本）和 Array（多模态）两种格式 */
    private JsonNode content;

    private String name;
}
