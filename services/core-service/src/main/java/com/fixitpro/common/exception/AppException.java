package com.fixitpro.common.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String field;

    public AppException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public AppException(String message, HttpStatus status, String field) {
        super(message);
        this.status = status;
        this.field = field;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Optional - the request field this error is about (e.g. "email",
     * "phone"), so GlobalExceptionHandler can surface it the same way
     * field-level validation errors already are. Null means "this error
     * isn't about one specific field" (e.g. review/schedule conflicts).
     */
    public String getField() {
        return field;
    }
}
