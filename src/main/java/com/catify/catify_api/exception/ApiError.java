package com.catify.catify_api.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error payload returned by the API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, Object> details
) {}