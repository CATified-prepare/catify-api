package com.catify.catify_api.dto.response.chat;

import com.catify.catify_api.dto.request.chat.AiMetadata;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> sources,
        int tokensUsed,
        AiMetadata ai
) {}

