package com.healthflow.intake.service;

import com.healthflow.intake.dto.*;
import com.healthflow.intake.enums.ClaimStatus;

public interface ClaimService {

    CreateClaimResponse createClaim(CreateClaimRequest createClaimRequest, String idempotencyKey);
    ClaimResponse getClaim(String claimId);
    ClaimStatusResponse getClaimStatus(String claimId);
    RetryClaimResponse retryClaim(String claimId);
}
