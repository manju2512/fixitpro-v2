package com.fixitpro.aichat.coreservice;

/** Wraps a non-2xx response from core-service, preserving its error message so the tool layer can hand it back to Claude verbatim. */
public class CoreServiceException extends RuntimeException {
    public CoreServiceException(String message) {
        super(message);
    }
}
