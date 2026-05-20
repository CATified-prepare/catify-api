package com.catify.catify_api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Question must not be blank") String question,
        String sessionId,   // optional: for conversation memory
        Integer year,       // optional filter: 2018, 2019, etc.
        String topic        // optional filter: VARC, DILR, Quant
) {}

