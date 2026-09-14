package com.healthflow.intake.dto;

import com.healthflow.intake.enums.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
public class CreateClaimRequest {

    @NotBlank(message = "patientId must not be blank")
    private String patientId;
    @NotBlank(message = "providerId must not be blank")
    private String providerId;
    @NotNull(message = "claimType must not be null")
    private ClaimType claimType;
    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;
}
