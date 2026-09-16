package com.healthflow.intake.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthflow.intake.dto.*;
import com.healthflow.intake.entity.Claim;
import com.healthflow.intake.entity.IdempotencyRecord;
import com.healthflow.intake.entity.OutboxEvent;
import com.healthflow.intake.enums.ClaimStatus;
import com.healthflow.intake.enums.IdempotencyStatus;
import com.healthflow.intake.enums.OutboxStatus;
import com.healthflow.intake.event.ClaimCreatedEvent;
import com.healthflow.intake.exception.*;
import com.healthflow.intake.repository.ClaimRepository;
import com.healthflow.intake.repository.IdempotencyRepository;
import com.healthflow.intake.repository.OutboxEventRepository;
import com.healthflow.intake.service.ClaimService;
import com.healthflow.intake.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;
    private final OutboxEventRepository outboxEventRepository;

    public ClaimServiceImpl(ClaimRepository claimRepository, IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper, IdempotencyService idempotencyService, OutboxEventRepository outboxEventRepository) {
        this.claimRepository = claimRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
        this.outboxEventRepository = outboxEventRepository;
    }

    private static final Logger log = LoggerFactory.getLogger(ClaimServiceImpl.class);

    @Override
    @Transactional
    public CreateClaimResponse createClaim(CreateClaimRequest request, String idempotencyKey) {
        log.info(
                "Claim creation started, idempotencyKey={}",
                idempotencyKey
        );
        // 1. Generate request hash
        String requestHash = generateRequestHash(request);

        // 2. Check whether idempotency key already exists
        Optional<IdempotencyRecord> existing =
                idempotencyRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord idempotencyRecord = existing.get();
            log.info(
                    "Existing idempotency record found, key={}, status={}",
                    idempotencyKey,
                    idempotencyRecord.getStatus()
            );
            if (!idempotencyRecord.getRequestHash().equals(requestHash)) {
                log.warn(
                        "Idempotency conflict detected, key={}",
                        idempotencyKey
                );
                throw new IdempotencyConflictException(
                        "Idempotency key already used with different request"
                );
            }

            if (idempotencyRecord.getStatus() == IdempotencyStatus.COMPLETED) {
                log.info(
                        "Returning previously stored response, key={}",
                        idempotencyKey
                );
                return deserializeResponse(
                        idempotencyRecord.getResponseBody()
                );
            }
            // 5. Request is still being processed
            if (idempotencyRecord.getStatus() == IdempotencyStatus.PROCESSING) {
                log.info(
                        "Claim is already being processed, key={}",
                        idempotencyKey
                );

                throw new ClaimAlreadyProcessingException(
                        "Request is already being processed"
                );
            }
        }
        // 7. Create idempotency record
        boolean created = idempotencyService.createProcessingRecord(idempotencyKey, requestHash);

        if (!created) {
            throw new ConcurrentIdempotencyException(
                    "Another request with the same Idempotency-Key " +
                            "is being processed"
            );
        }

        String claimId = "CLM-" + UUID.randomUUID();
        Claim claim = new Claim();
        claim.setClaimId(claimId);
        claim.setPatientId(request.getPatientId());
        claim.setProviderId(request.getProviderId());
        claim.setClaimType(request.getClaimType());
        claim.setAmount(request.getAmount());
        claim.setStatus(ClaimStatus.RECEIVED);
        claim.setCreatedAt(LocalDateTime.now());
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        // 3. Create Outbox Event 
        String eventId = UUID.randomUUID().toString();
        ClaimCreatedEvent event = new ClaimCreatedEvent(
                eventId,
                claim.getClaimId(),
                claim.getPatientId(),
                claim.getProviderId(),
                claim.getClaimType(),
                claim.getAmount(),
                MDC.get("Correlation-ID")
        );

        String payload = serializeEvent(event);

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventId(eventId);
        outboxEvent.setAggregateType("CLAIM");
        outboxEvent.setAggregateId(claim.getClaimId());
        outboxEvent.setPayload(payload);
        outboxEvent.setEventType("CLAIM_CREATED");
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(LocalDateTime.now());

        outboxEventRepository.save(outboxEvent);

        String workflowId =
                "WF-" + UUID.randomUUID();

        // 10. Create response
        CreateClaimResponse response =
                new CreateClaimResponse(
                        claimId,
                        workflowId,
                        "PROCESSING"
                );

        IdempotencyRecord idempotencyRecord =
                idempotencyRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Idempotency record not found"));

        // 11. Store response in idempotency record
        idempotencyRecord.setResponseBody(
                serializeResponse(response)
        );

        idempotencyRecord.setStatus(
                IdempotencyStatus.COMPLETED
        );

        idempotencyRepository.save(idempotencyRecord);

        // 12. Return response
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimResponse getClaim(String claimId) {
        log.info(
                "Fetching claim, claimId={}",
                claimId
        );
        Claim claim = claimRepository
                .findByClaimId(claimId)
                .orElseThrow(() -> {

                    log.warn(
                            "Claim not found, claimId={}",
                            claimId
                    );

                    return new ClaimNotFoundException(
                            "Claim not found: " + claimId
                    );
                });

        return new ClaimResponse(
                claim.getClaimId(),
                claim.getPatientId(),
                claim.getProviderId(),
                claim.getClaimType(),
                claim.getAmount(),
                claim.getStatus(),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimStatusResponse getClaimStatus(String claimId) {
        Claim claim = claimRepository.findByClaimId(claimId).orElseThrow(() -> new ClaimNotFoundException("Claim not found : " + claimId));
        return new ClaimStatusResponse(
                claim.getClaimId(),
                claim.getStatus()
        );
    }

    @Override
    @Transactional
    public RetryClaimResponse retryClaim(String claimId) {

        claimRepository.findByClaimId(claimId).orElseThrow(() ->
                        new ClaimNotFoundException(
                                "Claim not found: " + claimId));

        int updatedRows = claimRepository.transitionStatus(
                        claimId,
                        ClaimStatus.FAILED,
                        ClaimStatus.RECEIVED
                );

        if (updatedRows == 0) {

            throw new InvalidClaimStateException(
                    "Claim is no longer in FAILED state"
            );
        }

        return new RetryClaimResponse(
                claimId,
                ClaimStatus.RECEIVED,
                "Claim retry initiated successfully"
        );
    }

    private String generateRequestHash(
            CreateClaimRequest request) {

        String requestPayload =
                request.getPatientId()
                        + "|"
                        + request.getProviderId()
                        + "|"
                        + request.getClaimType()
                        + "|"
                        + request.getAmount();

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            requestPayload.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }
    }

    private String serializeResponse(
            CreateClaimResponse response) {

        try {

            return objectMapper.writeValueAsString(
                    response
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to serialize response",
                    e
            );
        }
    }

    private CreateClaimResponse deserializeResponse(
            String responseBody) {

        try {

            return objectMapper.readValue(
                    responseBody,
                    CreateClaimResponse.class
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to deserialize response",
                    e
            );
        }
    }

    private String serializeEvent(ClaimCreatedEvent event) {

        try {
            return objectMapper.writeValueAsString(event);

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to serialize claim event",
                    e
            );
        }
    }
}
