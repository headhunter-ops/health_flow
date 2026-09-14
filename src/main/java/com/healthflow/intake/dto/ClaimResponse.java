package com.healthflow.intake.dto;

import com.healthflow.intake.enums.ClaimStatus;
import com.healthflow.intake.enums.ClaimType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {

    private String claimId;
    private String patientId;
    private String providerId;
    private ClaimType claimType;
    private BigDecimal amount;
    private ClaimStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
