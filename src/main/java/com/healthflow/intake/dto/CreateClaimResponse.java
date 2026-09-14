package com.healthflow.intake.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateClaimResponse {

    private String claimId;
    private String workflowId;
    private String status;

    public CreateClaimResponse(
            String claimId,
            String workflowId,
            String status
    ){
        this.claimId = claimId;
        this.workflowId = workflowId;
        this.status = status;
    }
}
