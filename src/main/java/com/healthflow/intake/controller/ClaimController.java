package com.healthflow.intake.controller;

import com.healthflow.intake.dto.*;
import com.healthflow.intake.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimService claimService;
    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<CreateClaimResponse> createClaim( @Valid @RequestBody CreateClaimRequest createClaimRequest, @RequestHeader("Idempotency-Key") String idempotencyKey) {
        CreateClaimResponse response = claimService.createClaim(createClaimRequest, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<ClaimResponse> getClaim(@PathVariable String claimId){
        ClaimResponse response = claimService.getClaim(claimId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{claimId}/status")
    public ResponseEntity<ClaimStatusResponse> getClaimStatus(@PathVariable String claimId){
        ClaimStatusResponse response = claimService.getClaimStatus(claimId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{claimId}/retry")
    public ResponseEntity<RetryClaimResponse> retryClaim(@PathVariable String claimId){
        RetryClaimResponse response = claimService.retryClaim(claimId);
        return ResponseEntity.accepted().body(response);
    }
}
