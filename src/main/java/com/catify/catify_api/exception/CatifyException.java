package com.catify.catify_api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Application-level exception that can be safely surfaced to API clients.
 */
@Getter
public class CatifyException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public CatifyException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

}