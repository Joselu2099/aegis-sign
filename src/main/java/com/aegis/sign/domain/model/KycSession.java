package com.aegis.sign.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSession {
    private UUID id;
    private KycStatus status;
    private Map<String, String> documentMetadata;
    private Double faceMatchScore;
    private String signerId;
    private boolean mrzValid = false; // Default to false
    private String mrzValidationErrorMessage;

    public void approve(Double score) {
        this.faceMatchScore = score;
        this.status = KycStatus.APPROVED;
    }

    public void reject() {
        this.status = KycStatus.REJECTED;
    }

    public enum KycStatus {
        PENDING, APPROVED, REJECTED, MRZ_FAILED
    }
}
