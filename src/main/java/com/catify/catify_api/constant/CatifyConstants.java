package com.catify.catify_api.constant;

/**
 * Utility class that holds application-wide constants used across the Catify API.
 *
 * <p>This class is not meant to be instantiated.</p>
 */
public class CatifyConstants {

    /**
     * Central place for application-wide constants used by the Catify API.
     *
     * <p>This is a pure constants class and must not be instantiated.</p>
     */
    private CatifyConstants(){}

    public static class ErrorMessageConstants{
        private ErrorMessageConstants() {}

        public static final String ERROR_CODE_500 = "500";

        public static final String ERROR_CODE_404 = "404";

        public static final String ERROR_CODE_400 = "400";

        public static final String AI_EMPTY_RESPONSE_MESSAGE = "The AI service didn’t return a valid answer. Please try again in a moment.";

        public static final String ONLY_PDF_FILES_ACCEPTED_RESPONSE_MESSAGE = "Only PDF files are accepted";

        public static final String EMPTY_REQUEST_RECEIVED_RESPONSE_MESSAGE = "Empty request received to ingest data";
    }

    public static class CatifyGenericConstants{
        private CatifyGenericConstants() {}

        public static final String APPLICATION_PDF = "application/pdf";

        public static final String SECTION = "section";

        public static final String TOPIC = "topic";

        public static final String SUBTOPIC = "subtopic";

        public static final String TYPE = "type";

        public static final String DIFFICULTY = "difficulty";

        public static final String DATA_INGESTION_SUCCESS_RESPONSE = "PYQ data ingested successfully";

    }
}