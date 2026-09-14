package com.healthflow.intake.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthflow.intake.dto.*;
import com.healthflow.intake.entity.Claim;
import com.healthflow.intake.entity.IdempotencyRecord;
import com.healthflow.intake.enums.ClaimStatus;
import com.healthflow.intake.enums.IdempotencyStatus;
import com.healthflow.intake.exception.ClaimAlreadyProcessingException;
import com.healthflow.intake.exception.ClaimNotFoundException;
import com.healthflow.intake.exception.IdempotencyConflictException;
import com.healthflow.intake.exception.InvalidClaimStateException;
import com.healthflow.intake.repository.ClaimRepository;
import com.healthflow.intake.repository.IdempotencyRepository;
import com.healthflow.intake.service.ClaimService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

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

    public ClaimServiceImpl(ClaimRepository claimRepository, IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.claimRepository = claimRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }


    @Override
    @Transactional
    public CreateClaimResponse createClaim(CreateClaimRequest request, String idempotencyKey) {
        // 1. Generate request hash
        String requestHash = generateRequestHash(request);

        // 2. Check whether idempotency key already exists
        Optional<IdempotencyRecord> existing =
                idempotencyRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord idempotencyRecord = existing.get();
            if (!idempotencyRecord.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key already used with different request"
                );
            }

            if (idempotencyRecord.getStatus() == IdempotencyStatus.COMPLETED) {
                return deserializeResponse(
                        idempotencyRecord.getResponseBody()
                );
            }
            // 5. Request is still being processed
            if (idempotencyRecord.getStatus() == IdempotencyStatus.PROCESSING) {

                throw new ClaimAlreadyProcessingException(
                        "Request is already being processed"
                );
            }
        }
        // 7. Create idempotency record
        IdempotencyRecord idempotencyRecord =
                new IdempotencyRecord();

        idempotencyRecord.setIdempotencyKey(idempotencyKey);
        idempotencyRecord.setRequestHash(requestHash);
        idempotencyRecord.setStatus(
                IdempotencyStatus.PROCESSING
        );
        idempotencyRecord.setCreatedAt(
                LocalDateTime.now()
        );

        idempotencyRepository.save(idempotencyRecord);

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

        String workflowId =
                "WF-" + UUID.randomUUID();

        // 10. Create response
        CreateClaimResponse response =
                new CreateClaimResponse(
                        claimId,
                        workflowId,
                        "PROCESSING"
                );

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
        Claim claim = claimRepository.findByClaimId(claimId)
                .orElseThrow(()-> new ClaimNotFoundException( "claim not found: " + claimId));

        return new ClaimResponse (
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
        Claim claim = claimRepository.findByClaimId(claimId).orElseThrow(()-> new ClaimNotFoundException("Claim not found : " + claimId));
        return new ClaimStatusResponse(
                claim.getClaimId(),
                claim.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RetryClaimResponse retryClaim(String claimId) {
      Claim claim = claimRepository.findByClaimId(claimId).orElseThrow(()-> new ClaimNotFoundException("Claim not found: " + claimId));
       if(claim.getStatus() != ClaimStatus.FAILED){
           throw new InvalidClaimStateException("Claim can only be retried when status is FAILED." + "Current status: " + claim.getStatus());
       }
       claim.setStatus(ClaimStatus.RECEIVED);
       claim.setUpdatedAt(LocalDateTime.now());
       claimRepository.save(claim);
       return new RetryClaimResponse(
               claim.getClaimId(),
               claim.getStatus(),
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
}
