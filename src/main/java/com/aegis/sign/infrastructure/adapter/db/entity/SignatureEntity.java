package com.aegis.sign.infrastructure.adapter.db.entity;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("signatures")
public class SignatureEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private UUID contractId;
    private Json signerInfo; // JSONB
    private String x509CertificateSn;
    private LocalDateTime timestamp;

    @Override
    public boolean isNew() {
        return true; // Signatures are immutable
    }
}
