package com.healthflow.intake.dto;

import com.healthflow.intake.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetryClaimResponse {

    private String claimId;
    private ClaimStatus status;
    private String message;
}
