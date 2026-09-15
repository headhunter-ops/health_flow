package com.healthflow.intake.repository;

import com.healthflow.intake.entity.Claim;
import com.healthflow.intake.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimId(String claimId);

    @Modifying
    @Query("""
        UPDATE Claim c
        SET c.status = :newStatus,
            c.updatedAt = CURRENT_TIMESTAMP
        WHERE c.claimId = :claimId
          AND c.status = :expectedStatus
    """)
    int transitionStatus(
            @Param("claimId") String claimId,
            @Param("expectedStatus") ClaimStatus expectedStatus,
            @Param("newStatus") ClaimStatus newStatus
    );
}
