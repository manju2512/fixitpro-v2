package com.fixitpro.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends AppException {
    public InvalidStateTransitionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
