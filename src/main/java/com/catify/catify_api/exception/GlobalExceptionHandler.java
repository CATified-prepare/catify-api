package com.catify.catify_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CatifyException.class)
    public ResponseEntity<ApiError> handleCatifyException(CatifyException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Keep first error per field for clarity.
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        details.put("fieldErrors", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", req.getRequestURI(), details);
    }

    /**
     * Common when the underlying model/provider returns a 4xx/5xx (e.g., missing Ollama model).
     */
    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<ApiError> handleAiNonTransient(NonTransientAiException ex, HttpServletRequest req) {
        // Keep provider message, but don't leak internals beyond that.
        return build(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected error", req.getRequestURI(), null);
    }

    private static ResponseEntity<ApiError> build(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, Object> details
    ) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}