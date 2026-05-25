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
    private CatifyConstants(){

    }

    public static final String ERROR_CODE_500 = "500";

    public static final String AI_EMPTY_RESPONSE_MESSAGE = "The AI service didn’t return a valid answer. Please try again in a moment.";
}