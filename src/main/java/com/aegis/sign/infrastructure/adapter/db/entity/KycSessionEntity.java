package com.aegis.sign.infrastructure.adapter.db.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("kyc_sessions")
public class KycSessionEntity {
    @Id
    private UUID id;

    private String status;

    @Column("expires_at")
    private OffsetDateTime expiresAt;

    @Column("extracted_data")
    private String extractedData; // JSONB

    @Column("biometric_score")
    private Double biometricScore;
}
