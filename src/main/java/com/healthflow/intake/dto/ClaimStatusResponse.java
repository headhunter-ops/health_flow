package com.healthflow.intake.dto;

import com.healthflow.intake.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusResponse {

    private String claimId;
    private ClaimStatus status;
}
