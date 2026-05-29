package com.aegis.sign.infrastructure.adapter.db.entity;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_trails")
public class AuditTrailEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private UUID contractId;
    private UUID kycSessionId;
    private Json trailManifest; // JSONB
    private String finalSignedPdfUri;

    @Override
    public boolean isNew() {
        return true; // Audit trails are immutable
    }
}
