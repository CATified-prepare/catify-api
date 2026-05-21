package com.catify.catify_api.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> sources,
        int tokensUsed
) {}

