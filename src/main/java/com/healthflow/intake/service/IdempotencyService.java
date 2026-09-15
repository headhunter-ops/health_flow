package com.healthflow.intake.service;

public interface IdempotencyService {
    boolean createProcessingRecord(String idempotencyKey, String requestHash);
}
