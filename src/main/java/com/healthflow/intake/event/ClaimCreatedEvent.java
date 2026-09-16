package com.healthflow.intake.event;

import com.healthflow.intake.enums.ClaimType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

//event DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCreatedEvent {

    private String eventId;
    private String claimId;
    private String patientId;
    private String providerId;
    private ClaimType claimType;
    private BigDecimal amount;
    private String correlationId;

}
