package com.healthflow.intake.entity;

import com.healthflow.intake.enums.ClaimStatus;
import com.healthflow.intake.enums.ClaimType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "claims",
        indexes = {
            @Index(name = "idx_claim_patient", columnList = "patient_id"),
            @Index(name = "idx_claim_provider", columnList = "provider_id"),
            @Index(name = "idx_claim_status", columnList = "status")
})
@Data
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "claim_id",
            unique = true,
            nullable = false
    )
    private String claimId;

    @Column(
            name = "patient_id",
            nullable = false
    )
    private String patientId;

    @Column(
            name = "provider_id",
            nullable = false
    )
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "claim_type",
            nullable = false
    )
    private ClaimType claimType;

    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
