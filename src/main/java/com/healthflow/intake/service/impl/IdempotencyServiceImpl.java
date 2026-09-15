package com.healthflow.intake.service.impl;

import com.healthflow.intake.entity.IdempotencyRecord;
import com.healthflow.intake.enums.IdempotencyStatus;
import com.healthflow.intake.repository.IdempotencyRepository;
import com.healthflow.intake.service.IdempotencyService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class IdempotencyServiceImpl
        implements IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyServiceImpl(
            IdempotencyRepository idempotencyRepository) {

        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) //Suspend the existing transaction and create a completely new transaction.
    public boolean createProcessingRecord(
            String idempotencyKey,
            String requestHash) {

        IdempotencyRecord record =
                new IdempotencyRecord();

        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyStatus.PROCESSING);
        record.setCreatedAt(LocalDateTime.now());

        try {

            idempotencyRepository.saveAndFlush(record); //forces Hibernate to synchronize with the database.

            return true;

        } catch (DataIntegrityViolationException ex) {

            return false;
        }
    }
}