package com.aegis.sign.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrail {
    private UUID id;
    private UUID contractId;
    private UUID kycSessionId;
    private String ocrMrzResults;
    private Double biometricScore;
    private String preSignatureHash;
    private String postSignatureHash;
    private List<AuditTrailEvent> events;
    private String finalSignedPdfUri;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditTrailEvent {
        private String eventType;
        private LocalDateTime timestamp;
        private String description;
        private String ipAddress;
        private String userAgent;
    }
}
