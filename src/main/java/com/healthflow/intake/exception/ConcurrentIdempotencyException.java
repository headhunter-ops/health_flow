package com.healthflow.intake.exception;

public class ConcurrentIdempotencyException extends RuntimeException {

    public ConcurrentIdempotencyException(String message) {
        super(message);
    }
}
