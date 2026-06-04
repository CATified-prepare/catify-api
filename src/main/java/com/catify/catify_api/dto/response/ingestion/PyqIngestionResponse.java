package com.catify.catify_api.dto.response.ingestion;


public record PyqIngestionResponse(
        String message,
        Integer documentsIngested
) {}
