package com.catify.catify_api.dto;

/**
 * JSON-friendly subset of Spring AI response metadata.
 * Some providers (e.g., Ollama) may not report token usage; fields can be null.
 */
public record AiMetadata(
        String id,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}