package com.fixitpro.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends AppException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    /** field: the request field this duplicate is about (e.g. "email", "phone", "username"). */
    public DuplicateResourceException(String field, String message) {
        super(message, HttpStatus.CONFLICT, field);
    }
}
