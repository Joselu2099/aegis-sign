package com.aegis.sign.infrastructure.adapter.db.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_trails")
public class AuditTrailEntity {
    @Id
    private UUID id;
    private UUID contractId;
    private UUID kycSessionId;
    private String trailManifest; // JSONB
    private String finalSignedPdfUri;
}
