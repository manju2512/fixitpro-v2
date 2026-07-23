package com.fixitpro.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedActionException extends AppException {
    public UnauthorizedActionException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
