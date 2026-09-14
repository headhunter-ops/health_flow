package com.healthflow.intake.exception;

public class ClaimAlreadyProcessingException
        extends RuntimeException {

    public ClaimAlreadyProcessingException(String message) {
        super(message);
    }
}
